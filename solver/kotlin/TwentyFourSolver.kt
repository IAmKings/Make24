/*
 * 24 点 Solver（Kotlin Multiplatform / Compose Multiplatform 可用）
 *
 * 依据 prd_24_point_solver.md 实现：
 *  - 标准模式（输入 1..13）：Long 精确分数 + 溢出检测、Fraction intern(uint16 ID)、
 *    uint64 canonical state、固定容量 open-addressing visited、allocation-free DFS、
 *    3-byte solution steps、命中后 replay 构造表达式（R-001..R-005, R-010..R-015,
 *    R-020..R-024, R-030..R-033, R-040..R-053, R-060..R-064, R-070..R-084）。
 *  - fallback（任意 Int 输入、自定义大目标、溢出）：任意精度分数，使用内置的
 *    纯 Kotlin BigNum（跨平台，无 java.* 依赖）。（R-006, R-014）
 *  - 1820 组合 LUT：lazy 生成 + 独立 evaluator 校验 + O(1) 查询。（R-090..R-094）
 *  - P1 扩展：自定义 target（含有理数）、自定义运算符集合、maxSolutions/全部去重解、
 *    紧凑/完全括号两种表达式格式。（R-004, R-007, R-008, R-084）
 *
 * 本文件为纯 Kotlin 公共代码，不依赖 java.*，可直接放入
 * src/commonMain/kotlin/ 使用。配套文件 BigNum.kt（任意精度整数）需一并拷贝。
 */

package com.make24.solver

// ============================================================
// 公共 API 类型
// ============================================================

/** 求解状态（PRD 5 节）。 */
enum class SolveStatus {
    SOLVED,
    UNSOLVABLE,
    INVALID_INPUT,
    OVERFLOW,
    RESOURCE_LIMIT,
}

/** 模式选择（PRD R-006 / R-101）。 */
enum class SolveMode {
    /** 输入在 1..13 走标准优化路径，否则自动转 fallback。 */
    AUTO,

    /** 强制标准路径；输入不在 1..13 时返回 INVALID_INPUT。 */
    STANDARD,

    /** 强制 BigInt fallback。 */
    FALLBACK,
}

/** 表达式输出格式（PRD R-084）。 */
enum class ExpressionStyle {
    /** 每个内部节点都带括号，无任何优先级依赖。 */
    FULLY_PARENTHESIZED,

    /** 只保留必要括号。 */
    COMPACT,
}

/** 四则运算（PRD R-008 自定义运算符集合；`-`/`/` 始终包含两个方向）。 */
enum class Op(val symbol: String) {
    ADD("+"),
    SUB("-"),
    MUL("*"),
    DIV("/");

    companion object {
        val ALL: Set<Op> = entries.toSet()
    }
}

/**
 * 精确有理数（PRD R-010..R-012 的公共表示）。构造时约分、分母恒正。
 * 仅用于 target 配置与结果展示，不参与求解热路径。
 */
class Rational private constructor(val numerator: Long, val denominator: Long) {

    companion object {
        operator fun invoke(numerator: Long, denominator: Long = 1L): Rational {
            require(denominator != 0L) { "denominator must not be zero" }
            var n = numerator
            var d = denominator
            if (d < 0) {
                n = -n
                d = -d
            }
            val g = gcd(n, d)
            if (g != 1L) {
                n /= g
                d /= g
            }
            return Rational(n, d)
        }

        val ZERO: Rational = Rational(0, 1)
        val ONE: Rational = Rational(1, 1)
    }

    override fun equals(other: Any?): Boolean =
        other is Rational && numerator == other.numerator && denominator == other.denominator

    override fun hashCode(): Int = 31 * numerator.hashCode() + denominator.hashCode()

    override fun toString(): String =
        if (denominator == 1L) numerator.toString() else "$numerator/$denominator"
}

/** 求解选项（PRD 5 节：target / mode / useLut / maxSolutions，外加 operators、style）。 */
data class SolveOptions(
    val target: Rational = Rational(24),
    val mode: SolveMode = SolveMode.AUTO,
    val useLut: Boolean = true,
    val maxSolutions: Int = 1,
    val operators: Set<Op> = Op.ALL,
    val style: ExpressionStyle = ExpressionStyle.FULLY_PARENTHESIZED,
) {
    init {
        require(maxSolutions >= 1) { "maxSolutions must be >= 1" }
        require(operators.isNotEmpty()) { "operators must not be empty" }
    }
}

/** 搜索统计（PRD 5 节 stats 字段；可用于调试与 benchmark）。 */
data class SearchStats(
    val nodes: Int,
    val candidates: Int,
    val internHits: Int,
    val visitedProbes: Long,
    val usedFallback: Boolean,
)

/** 求解结果（PRD 5 节）。R-102：expression != null 当且仅当 status == SOLVED。 */
data class SolveResult(
    val status: SolveStatus,
    val expression: String?,
    val target: Rational,
    val usedLut: Boolean,
    val stats: SearchStats?,
    val message: String? = null,
    /** 扩展：maxSolutions > 1 时携带全部去重解（PRD R-007）。 */
    val expressions: List<String>? = null,
) {
    val solved: Boolean get() = status == SolveStatus.SOLVED
}

/**
 * 可复用的求解工作区（PRD R-044：调用方显式传入以复用内存，避免每次 solve 分配数组）。
 *
 * 线程安全：workspace 不是线程安全的，同一实例不得被多个线程同时使用；
 * 不同线程应各自持有自己的 workspace（或每次调用不传，内部会新建）。
 */
class SolverWorkspace {
    internal val std = StdEngine()
}

// ============================================================
// 门面 API
// ============================================================

object TwentyFourSolver {

    private val lut: Lut by lazy { Lut() }

    /** 便捷入口：List<Int> + target（PRD 5 节 solve 的简化形式）。 */
    fun solve(numbers: List<Int>, target: Int = 24): SolveResult =
        solve(numbers.toIntArray(), SolveOptions(target = Rational(target.toLong())))

    /** 主入口（PRD 5 节 solve(numbers: Int[4], options): SolveResult）。 */
    fun solve(
        numbers: IntArray,
        options: SolveOptions = SolveOptions(),
        workspace: SolverWorkspace? = null,
    ): SolveResult {
        if (numbers.size != 4) {
            return invalid(SolveStatus.INVALID_INPUT, "输入数量必须为 4，实际为 ${numbers.size}", options.target)
        }
        val allStd = numbers.all { it in 1..13 }

        // LUT 命中条件（PRD R-093）：标准输入 + 默认目标 24 + 全部运算符 + 单解 + 非强制 fallback。
        if (options.useLut && options.maxSolutions == 1 && allStd &&
            options.target.numerator == 24L && options.target.denominator == 1L &&
            options.operators == Op.ALL &&
            (options.mode == SolveMode.AUTO || options.mode == SolveMode.STANDARD)
        ) {
            val r = lut.lookup(numbers, options.style)
            if (r != null) return r
        }

        val mode = when (options.mode) {
            SolveMode.STANDARD ->
                if (!allStd) return invalid(SolveStatus.INVALID_INPUT, "STANDARD 模式要求所有输入在 1..13 之间", options.target)
                else SolveMode.STANDARD
            SolveMode.FALLBACK -> SolveMode.FALLBACK
            SolveMode.AUTO -> if (allStd) SolveMode.STANDARD else SolveMode.FALLBACK
        }

        val inputs = LongArray(4) { numbers[it].toLong() }
        val ones = LongArray(4) { 1L }
        return runWithFallback(inputs, ones, 4, options, workspace)
    }

    /**
     * 3 数快速求解：判断 {a, b, c} 能否通过 + - * / 得到 target，并返回一条解法。
     * 输入可为分数（如第一步合并产生 8/3 后剩余 {8/3, x, y}），供 UI 在玩家
     * 完成一次合并后实时提示剩余 3 个数是否有解。不走 LUT。
     */
    fun solveThree(
        a: Rational,
        b: Rational,
        c: Rational,
        options: SolveOptions = SolveOptions(),
        workspace: SolverWorkspace? = null,
    ): SolveResult {
        val ns = longArrayOf(a.numerator, b.numerator, c.numerator)
        val ds = longArrayOf(a.denominator, b.denominator, c.denominator)
        return runWithFallback(ns, ds, 3, options, workspace)
    }

    /**
     * 2 数快速求解：判断 {a, b}（可含分数）能否通过一次 + - * / 得到 target，并返回该解法。
     * 枚举 ≤ 6 个候选（+、a-b、b-a、*、a/b、b/a，跳过除零、去重），O(1)。
     * 用于玩家完成第二次合并、仅剩 2 个数时的实时提示。不走 LUT。
     */
    fun solveTwo(
        a: Rational,
        b: Rational,
        options: SolveOptions = SolveOptions(),
        workspace: SolverWorkspace? = null,
    ): SolveResult {
        val ns = longArrayOf(a.numerator, b.numerator)
        val ds = longArrayOf(a.denominator, b.denominator)
        return runWithFallback(ns, ds, 2, options, workspace)
    }

    /** 返回全部去重解（PRD R-007 / R-115 的字符串去重简化版）。 */
    fun solveAll(numbers: IntArray, options: SolveOptions = SolveOptions()): List<SolveResult> {
        val r = solve(numbers, options.copy(maxSolutions = Int.MAX_VALUE))
        if (r.status != SolveStatus.SOLVED || r.expressions == null) return listOf(r)
        return r.expressions.map { expr ->
            SolveResult(SolveStatus.SOLVED, expr, r.target, r.usedLut, null, null, null)
        }
    }

    // ---------------- 内部编排 ----------------

    /**
     * 统一执行：FALLBACK 强制任意精度；否则先标准路径，溢出/资源限制时
     * AUTO 自动转 fallback，STANDARD 返回明确错误。
     */
    private fun runWithFallback(
        inputsN: LongArray,
        inputsD: LongArray,
        count: Int,
        options: SolveOptions,
        workspace: SolverWorkspace?,
    ): SolveResult {
        if (options.mode == SolveMode.FALLBACK) {
            return runFallback(inputsN, inputsD, count, options)
        }
        return try {
            runStd(inputsN, inputsD, count, options, workspace)
        } catch (e: OverflowSignal) {
            if (options.mode == SolveMode.AUTO) runFallback(inputsN, inputsD, count, options)
            else invalid(SolveStatus.OVERFLOW, "中间值超出 Long 范围，无法在标准路径求解", options.target)
        } catch (e: InternLimit) {
            if (options.mode == SolveMode.AUTO) runFallback(inputsN, inputsD, count, options)
            else invalid(SolveStatus.RESOURCE_LIMIT, "intern ID 数量超出 65535", options.target)
        } catch (e: ResourceLimit) {
            if (options.mode == SolveMode.AUTO) runFallback(inputsN, inputsD, count, options)
            else invalid(SolveStatus.RESOURCE_LIMIT, "visited 表容量不足", options.target)
        }
    }

    private fun runStd(
        inputsN: LongArray,
        inputsD: LongArray,
        count: Int,
        options: SolveOptions,
        workspace: SolverWorkspace?,
    ): SolveResult {
        val engine = workspace?.std ?: StdEngine()
        val res = engine.solve(
            inputsN,
            inputsD,
            count,
            options.target.numerator,
            options.target.denominator,
            options.maxSolutions,
            options.operators,
            options.style,
        )
        return toResult(res, inputsN, inputsD, options)
    }

    private fun runFallback(
        inputsN: LongArray,
        inputsD: LongArray,
        count: Int,
        options: SolveOptions,
    ): SolveResult {
        val engine = FallbackEngine()
        val res = engine.solve(
            inputsN,
            inputsD,
            count,
            options.target.numerator,
            options.target.denominator,
            options.maxSolutions,
            options.operators,
            options.style,
        )
        return toResult(res, inputsN, inputsD, options)
    }

    private fun toResult(
        res: EngineResult,
        inputsN: LongArray,
        inputsD: LongArray,
        options: SolveOptions,
    ): SolveResult {
        return when (res.status) {
            SolveStatus.SOLVED -> {
                if (res.expressions != null) {
                    SolveResult(SolveStatus.SOLVED, res.expressions.firstOrNull(), options.target, false, res.stats, null, res.expressions)
                } else {
                    val expr = formatExpr(replayExpr(inputsN, inputsD, res.stepsDepth + 1, res.steps!!, res.stepsDepth), options.style)
                    SolveResult(SolveStatus.SOLVED, expr, options.target, false, res.stats, null, null)
                }
            }
            else -> SolveResult(res.status, null, options.target, false, res.stats, res.message)
        }
    }

    private fun invalid(status: SolveStatus, message: String, target: Rational): SolveResult =
        SolveResult(status, null, target, false, null, message, null)
}

// ============================================================
// 内部引擎结果
// ============================================================

internal data class EngineResult(
    val status: SolveStatus,
    val steps: ByteArray?,
    val expressions: List<String>?,
    val stats: SearchStats,
    val message: String? = null,
    /** replay 所需的合并步数（= 输入个数 - 1；4 数 = 3，3 数 = 2）。 */
    val stepsDepth: Int = 3,
)

// ============================================================
// 内部：精确 Long 运算（跨平台，JVM 无 java.lang.Math 依赖）
// ============================================================

/** 溢出信号：标准路径中间值或比较溢出时抛出，由门面捕获并转 fallback/OVERFLOW。 */
internal class OverflowSignal : Throwable()

/** intern ID 超出 65535 时抛出（标准模式理论不可达，防御 R-032）。 */
internal class InternLimit : Throwable()

/** visited 表满等资源限制（防御 R-063）。 */
internal class ResourceLimit : Throwable()

internal fun mulExact(a: Long, b: Long): Long {
    if (a == Long.MIN_VALUE && b == -1L) throw OverflowSignal()
    if (b == Long.MIN_VALUE && a == -1L) throw OverflowSignal()
    val r = a * b
    if (a != 0L && r / a != b) throw OverflowSignal()
    return r
}

internal fun addExact(a: Long, b: Long): Long {
    val r = a + b
    if (((a xor r) and (b xor r)) < 0L) throw OverflowSignal()
    return r
}

internal fun subExact(a: Long, b: Long): Long {
    val r = a - b
    if (((a xor b) and (a xor r)) < 0L) throw OverflowSignal()
    return r
}

internal fun gcd(a0: Long, b0: Long): Long {
    var a = if (a0 < 0) -a0 else a0
    var b = if (b0 < 0) -b0 else b0
    while (b != 0L) {
        val t = a % b
        a = b
        b = t
    }
    return if (a < 0) -a else a
}

// ============================================================
// 内部：Fraction intern（PRD 2.4）
// 开放寻址 hash 表；id 从 1 开始（0 保留给空槽 / 空状态）。
// ============================================================

internal class InternTable(private val capacity: Int = 8192) {

    private val mask: Int = capacity - 1
    private val nKeys = LongArray(capacity)
    private val dKeys = LongArray(capacity) // 0 表示空槽
    private val idKeys = IntArray(capacity)
    private val nById = LongArray(capacity + 1)
    private val dById = LongArray(capacity + 1)
    private var maxId = 0
    var hits = 0
        private set

    fun reset() {
        nKeys.fill(0)
        dKeys.fill(0)
        idKeys.fill(0)
        maxId = 0
        hits = 0
    }

    fun n(id: Int): Long = nById[id]
    fun d(id: Int): Long = dById[id]

    /** 规范化并 intern，返回 uint16 ID（1..65535）。R-023：同值同 ID。 */
    fun intern(n0: Long, d0: Long): Int {
        var n = n0
        var d = d0
        if (d < 0) {
            n = -n
            d = -d
        }
        if (d == 0L) throw IllegalStateException("intern: zero denominator") // 内部错误，不应发生
        val g = gcd(n, d)
        if (g != 1L) {
            n /= g
            d /= g
        }
        var idx = (hash(n, d) and mask.toLong()).toInt()
        while (true) {
            val dd = dKeys[idx]
            if (dd == 0L) {
                if (maxId >= 65_535) throw InternLimit()
                val id = maxId + 1
                nKeys[idx] = n
                dKeys[idx] = d
                idKeys[idx] = id
                nById[id] = n
                dById[id] = d
                maxId = id
                return id
            }
            if (nKeys[idx] == n && dd == d) {
                hits++
                return idKeys[idx]
            }
            idx = (idx + 1) and mask
        }
    }

    companion object {
        private fun hash(n: Long, d: Long): Long {            var h = n * 31 + d
            h = h xor (h ushr 33)
            h *= -0x61C8864680B583EBL
            h = h xor (h ushr 33)
            h *= -0x3B20A5D3CAB35325L
            h = h xor (h ushr 33)
            return h
        }
    }
}

// ============================================================
// 内部：fixed open-addressing visited（PRD 2.7）
// key 为 uint64 canonical state；0 表示空槽。key=0 由调用方保证不会查询
// （状态至少含 1 个 id>=1 的分数），此处额外防御。
// ============================================================

internal class VisitedSet(private val pow2: Int) {

    private val keys = LongArray(1 shl pow2)
    private val mask: Int = keys.size - 1
    private var count = 0
    var probes = 0L
        private set

    fun reset() {
        keys.fill(0)
        count = 0
        probes = 0
    }

    /** 返回 true 表示 key 首次加入；false 表示已存在。 */
    fun add(key: Long): Boolean {
        if (key == 0L) return false
        if (count * 2 >= keys.size) throw ResourceLimit() // 负载 >= 0.5 防御（正常远达不到）
        var idx = (mix(key) and mask.toLong()).toInt()
        while (true) {
            probes++
            val v = keys[idx]
            if (v == key) return false
            if (v == 0L) {
                keys[idx] = key
                count++
                return true
            }
            idx = (idx + 1) and mask
        }
    }

    private fun mix(k: Long): Long {
        var h = k
        h = h xor (h ushr 33)
        h *= -0x61C8864680B583EBL
        return h xor (h ushr 33)
    }
}

// ============================================================
// 内部：标准模式 DFS 引擎（PRD 2.5 / 2.6 / 2.8 / 2.9）
// 热路径无对象分配：全部使用预分配数组。
// ============================================================

internal class StdEngine {

    private val ids = IntArray(4)          // 当前分数 ID 数组
    private val tmp = IntArray(4)          // state 编码排序缓冲
    private val steps = ByteArray(9)       // 3-byte steps（i, j, op）
    private val solSteps = ByteArray(9)    // 单解模式命中后复制
    // 候选缓冲区按递归深度分层（深度 0..2），避免子调用覆盖父层候选。
    private val candOp = Array(3) { ByteArray(6) }
    private val candN = Array(3) { LongArray(6) }
    private val candD = Array(3) { LongArray(6) }
    private val intern = InternTable(8192)
    private val visited = VisitedSet(11)   // 2048 槽；状态数上界 ~684
    private var targetN = 24L
    private var targetD = 1L
    private var opsMask = 0b1111
    private var style = ExpressionStyle.FULLY_PARENTHESIZED
    private var collect: LinkedHashSet<String>? = null
    private var maxSols = 1
    private var nodes = 0
    private var candidates = 0
    private var inputsN = LongArray(4)
    private var inputsD = LongArray(4)
    private var stepsDepth = 3

    fun solve(
        inputsN: LongArray,
        inputsD: LongArray,
        inputCount: Int,
        targetN: Long,
        targetD: Long,
        maxSolutions: Int,
        operators: Set<Op>,
        style: ExpressionStyle,
    ): EngineResult {
        this.inputsN = inputsN
        this.inputsD = inputsD
        stepsDepth = inputCount - 1
        intern.reset()
        visited.reset()
        nodes = 0
        candidates = 0
        this.targetN = targetN
        this.targetD = targetD
        this.style = style
        opsMask = maskOf(operators)
        maxSols = maxSolutions
        collect = if (maxSolutions > 1) LinkedHashSet() else null
        for (k in 0 until inputCount) ids[k] = intern.intern(inputsN[k], inputsD[k])
        val found = dfs(inputCount, 0)
        val stats = SearchStats(nodes, candidates, intern.hits, visited.probes, false)
        val exprs: List<String>? = collect?.takeIf { it.isNotEmpty() }?.toList()
        val stepsOut = if (collect == null && found) solSteps else null
        return if (exprs != null || stepsOut != null) {
            EngineResult(SolveStatus.SOLVED, stepsOut, exprs, stats, null, stepsDepth)
        } else {
            EngineResult(SolveStatus.UNSOLVABLE, null, null, stats, null, stepsDepth)
        }
    }

    /**
     * 递归两两合并（PRD 2.1）。返回 true 表示需要停止（单解命中 / 达到 maxSolutions）。
     */
    private fun dfs(count: Int, depth: Int): Boolean {
        nodes++
        if (count == 1) {
            val id = ids[0]
            // R-015：n/d == targetN/targetD，精确判断，无浮点。
            if (mulExact(intern.n(id), targetD) == mulExact(targetN, intern.d(id))) {
                if (collect != null) {
                    collect!!.add(formatExpr(replayExpr(inputsN, inputsD, depth + 1, steps, depth), style))
                    return collect!!.size >= maxSols
                }
                for (k in 0 until depth * 3) solSteps[k] = steps[k]
                return true
            }
            return false
        }
        for (i in 0 until count - 1) {
            for (j in i + 1 until count) {
                val cc = genCandidates(ids[i], ids[j], depth)
                val opBuf = candOp[depth]
                val nBuf = candN[depth]
                val dBuf = candD[depth]
                for (k in 0 until cc) {
                    candidates++
                    val nid = intern.intern(nBuf[k], dBuf[k])
                    // compact：新值放 ids[i]；ids[j] 与末尾交换后活动区缩短。
                    val savedI = ids[i]
                    val savedJ = ids[j]
                    val savedLast = ids[count - 1]
                    if (j != count - 1) ids[j] = savedLast
                    ids[count - 1] = savedJ
                    ids[i] = nid
                    steps[depth * 3] = i.toByte()
                    steps[depth * 3 + 1] = j.toByte()
                    steps[depth * 3 + 2] = opBuf[k]
                    // R-064：visited 语义为“已处理过的 canonical multiset 状态”。
                    // 只对 3/2 元素状态剪枝（1 元素状态是目标判定叶子，不进入 visited）。
                    if (count - 1 >= 2) {
                        val key = encodeState(count - 1)
                        if (visited.add(key) && dfs(count - 1, depth + 1)) {
                            restore(i, j, savedI, savedJ, savedLast, count)
                            return true
                        }
                    } else if (dfs(count - 1, depth + 1)) {
                        restore(i, j, savedI, savedJ, savedLast, count)
                        return true
                    }
                    restore(i, j, savedI, savedJ, savedLast, count)
                }
            }
        }
        return false
    }

    private fun restore(i: Int, j: Int, savedI: Int, savedJ: Int, savedLast: Int, count: Int) {
        ids[i] = savedI
        if (j != count - 1) {
            // compact 时 ids[j] <- savedLast、ids[last] <- savedJ，这里还原
            ids[j] = savedJ
            ids[count - 1] = savedLast
        }
    }

    /** 候选生成 + local dedup（PRD 2.6）。顺序固定：+、a-b、b-a、*、a/b、b/a。返回候选数。 */
    private fun genCandidates(ia: Int, ib: Int, depth: Int): Int {
        val nA = intern.n(ia)
        val dA = intern.d(ia)
        val nB = intern.n(ib)
        val dB = intern.d(ib)
        var cc = 0
        val opBuf = candOp[depth]
        val nBuf = candN[depth]
        val dBuf = candD[depth]
        fun addCandidate(op: Int, n0: Long, d0: Long) {
            if (d0 == 0L) return
            var n = n0
            var d = d0
            if (d < 0) {
                n = -n
                d = -d
            }
            val g = gcd(n, d)
            if (g != 1L) {
                n /= g
                d /= g
            }
            for (k in 0 until cc) {
                if (nBuf[k] == n && dBuf[k] == d) return
            }
            opBuf[cc] = op.toByte()
            nBuf[cc] = n
            dBuf[cc] = d
            cc++
        }
        val dBdA = mulExact(dA, dB)
        if (opsMask and OP_ADD != 0) {
            addCandidate(0, addExact(mulExact(nA, dB), mulExact(nB, dA)), dBdA)
        }
        if (opsMask and OP_SUB != 0) {
            addCandidate(1, subExact(mulExact(nA, dB), mulExact(nB, dA)), dBdA)
            addCandidate(2, subExact(mulExact(nB, dA), mulExact(nA, dB)), dBdA)
        }
        if (opsMask and OP_MUL != 0) {
            addCandidate(3, mulExact(nA, nB), dBdA)
        }
        if (opsMask and OP_DIV != 0) {
            addCandidate(4, mulExact(nA, dB), mulExact(dA, nB)) // b 为 0 时分母 0 → 跳过
            addCandidate(5, mulExact(nB, dA), mulExact(nA, dB)) // a 为 0 时跳过
        }
        return cc
    }

    /**
     * canonical uint64 state（PRD 2.3）：将活动区分数按数值升序排序后打包
     * 4 × 16bit，未使用槽位填 0。排序使用独立缓冲 tmp，不改动 ids 布局。
     */
    private fun encodeState(count: Int): Long {
        for (k in 0 until count) tmp[k] = ids[k]
        // 插入排序（<= 4 元素）
        for (i in 1 until count) {
            val v = tmp[i]
            var j = i - 1
            while (j >= 0 && fracCmp(tmp[j], v) > 0) {
                tmp[j + 1] = tmp[j]
                j--
            }
            tmp[j + 1] = v
        }
        var key = 0L
        for (k in 0 until 4) {
            val id = if (k < count) tmp[k].toLong() else 0L
            key = key or (id shl (16 * k))
        }
        return key
    }

    /** 分数精确比较（按数值，(n,d) 全序）。标准模式 n,d < 2^31，乘积 < 2^62 不会溢出。 */
    private fun fracCmp(aId: Int, bId: Int): Int {
        val lhs = mulExact(intern.n(aId), intern.d(bId))
        val rhs = mulExact(intern.n(bId), intern.d(aId))
        return lhs.compareTo(rhs)
    }

    private companion object {
        const val OP_ADD = 1
        const val OP_SUB = 2
        const val OP_MUL = 4
        const val OP_DIV = 8
    }
}

internal fun maskOf(operators: Set<Op>): Int {
    var m = 0
    if (Op.ADD in operators) m = m or 1
    if (Op.SUB in operators) m = m or 2
    if (Op.MUL in operators) m = m or 4
    if (Op.DIV in operators) m = m or 8
    return m
}

// ============================================================
// 内部：任意精度 fallback 引擎（PRD 2.2 / 第 6 节）
// 任意 Int 输入、大 target、标准路径溢出时使用。无 intern/visited，
// 状态空间小（<= 4572 个分数实例），BigNum 运算无溢出。
// ============================================================

private data class BF(val n: BigNum, val d: BigNum) {
    companion object {
        val ZERO: BF = BF(BigNum.ZERO, BigNum.ONE)
    }
}

internal class FallbackEngine {

    private val vals = Array(4) { BF.ZERO }
    private val steps = ByteArray(9)
    private val solSteps = ByteArray(9)
    // 候选缓冲区按递归深度分层，避免子调用覆盖父层候选。
    private val candOp = Array(3) { ByteArray(6) }
    private val candVals = Array(3) { Array(6) { BF.ZERO } }
    private var targetN = BigNum.ZERO
    private var targetD = BigNum.ONE
    private var opsMask = 0b1111
    private var style = ExpressionStyle.FULLY_PARENTHESIZED
    private var collect: LinkedHashSet<String>? = null
    private var maxSols = 1
    private var nodes = 0
    private var candidates = 0
    private var inputsN = LongArray(4)
    private var inputsD = LongArray(4)
    private var stepsDepth = 3

    fun solve(
        inputsN: LongArray,
        inputsD: LongArray,
        inputCount: Int,
        targetN: Long,
        targetD: Long,
        maxSolutions: Int,
        operators: Set<Op>,
        style: ExpressionStyle,
    ): EngineResult {
        this.inputsN = inputsN
        this.inputsD = inputsD
        stepsDepth = inputCount - 1
        this.targetN = BigNum.fromLong(targetN)
        this.targetD = BigNum.fromLong(targetD)
        opsMask = maskOf(operators)
        this.style = style
        maxSols = maxSolutions
        collect = if (maxSolutions > 1) LinkedHashSet() else null
        nodes = 0
        candidates = 0
        for (k in 0 until inputCount) {
            vals[k] = normBF(BigNum.fromLong(inputsN[k]), BigNum.fromLong(inputsD[k]))
        }
        val found = dfs(inputCount, 0)
        val stats = SearchStats(nodes, candidates, 0, 0L, true)
        val exprs: List<String>? = collect?.takeIf { it.isNotEmpty() }?.toList()
        val stepsOut = if (collect == null && found) solSteps else null
        return if (exprs != null || stepsOut != null) {
            EngineResult(SolveStatus.SOLVED, stepsOut, exprs, stats, null, stepsDepth)
        } else {
            EngineResult(SolveStatus.UNSOLVABLE, null, null, stats, null, stepsDepth)
        }
    }

    private fun dfs(count: Int, depth: Int): Boolean {
        nodes++
        if (count == 1) {
            val a = vals[0]
            // R-015 的任意精度版本：n * targetD == targetN * d
            if (a.n * targetD == targetN * a.d) {
                if (collect != null) {
                    collect!!.add(formatExpr(replayExpr(inputsN, inputsD, depth + 1, steps, depth), style))
                    return collect!!.size >= maxSols
                }
                for (k in 0 until depth * 3) solSteps[k] = steps[k]
                return true
            }
            return false
        }
        for (i in 0 until count - 1) {
            for (j in i + 1 until count) {
                val cc = genCandidates(vals[i], vals[j], depth)
                val opBuf = candOp[depth]
                val vBuf = candVals[depth]
                for (k in 0 until cc) {
                    candidates++
                    val savedI = vals[i]
                    val savedJ = vals[j]
                    val savedLast = vals[count - 1]
                    if (j != count - 1) vals[j] = savedLast
                    vals[count - 1] = savedJ
                    vals[i] = vBuf[k]
                    steps[depth * 3] = i.toByte()
                    steps[depth * 3 + 1] = j.toByte()
                    steps[depth * 3 + 2] = opBuf[k]
                    if (dfs(count - 1, depth + 1)) {
                        restore(i, j, savedI, savedJ, savedLast, count)
                        return true
                    }
                    restore(i, j, savedI, savedJ, savedLast, count)
                }
            }
        }
        return false
    }

    private fun restore(i: Int, j: Int, savedI: BF, savedJ: BF, savedLast: BF, count: Int) {
        vals[i] = savedI
        if (j != count - 1) {
            // compact 时 vals[j] <- savedLast、vals[last] <- savedJ，这里还原
            vals[j] = savedJ
            vals[count - 1] = savedLast
        }
    }

    private fun genCandidates(a: BF, b: BF, depth: Int): Int {
        var cc = 0
        val opBuf = candOp[depth]
        val vBuf = candVals[depth]
        fun addCand(op: Int, v: BF?) {
            if (v == null) return // 除零跳过
            for (k in 0 until cc) {
                if (vBuf[k] == v) return // local dedup（BF 已规范化，(n,d) 等价即值等价）
            }
            opBuf[cc] = op.toByte()
            vBuf[cc] = v
            cc++
        }
        if (opsMask and 1 != 0) addCand(0, addBF(a, b))
        if (opsMask and 2 != 0) {
            addCand(1, subBF(a, b))
            addCand(2, subBF(b, a))
        }
        if (opsMask and 4 != 0) addCand(3, mulBF(a, b))
        if (opsMask and 8 != 0) {
            addCand(4, divBF(a, b))
            addCand(5, divBF(b, a))
        }
        return cc
    }

    private fun addBF(a: BF, b: BF): BF = normBF(a.n * b.d + b.n * a.d, a.d * b.d)
    private fun subBF(a: BF, b: BF): BF = normBF(a.n * b.d - b.n * a.d, a.d * b.d)
    private fun mulBF(a: BF, b: BF): BF = normBF(a.n * b.n, a.d * b.d)
    private fun divBF(a: BF, b: BF): BF? {
        if (b.n == BigNum.ZERO) return null // 除零候选跳过
        return normBF(a.n * b.d, a.d * b.n)
    }
}

private fun normBF(n0: BigNum, d0: BigNum): BF {
    var n = n0
    var d = d0
    if (d < BigNum.ZERO) {
        n = -n
        d = -d
    }
    if (d == BigNum.ZERO) return BF.ZERO // 防御，不应发生
    val g = n.gcd(d)
    if (g != BigNum.ONE) {
        n = n / g
        d = d / g
    }
    return BF(n, d)
}

// ============================================================
// 内部：命中后 replay 构造表达式（PRD 2.9）
// 只有 DFS 命中后才创建表达式节点/字符串（R-080）。
// 布局规则与 DFS compact 一致：新节点放位置 i，j 与末尾交换。
// ============================================================

internal sealed interface Expr {
    /** d == 1 时为整数叶子，否则为分数叶子（如 1/3），格式化时始终加括号保证无歧义。 */
    data class Leaf(val n: Long, val d: Long = 1L) : Expr
    data class Node(val op: Op, val left: Expr, val right: Expr) : Expr
}

internal fun replayExpr(inputsN: LongArray, inputsD: LongArray, n: Int, steps: ByteArray, depth: Int): Expr {
    val exprs = arrayOfNulls<Expr>(4)
    var count = n
    for (k in 0 until n) exprs[k] = Expr.Leaf(inputsN[k], inputsD[k])
    for (d in 0 until depth) {
        val i = steps[d * 3].toInt() and 0xFF
        val j = steps[d * 3 + 1].toInt() and 0xFF
        val op = steps[d * 3 + 2].toInt() and 0xFF
        // op code：0=a+b, 1=a-b, 2=b-a, 3=a*b, 4=a/b, 5=b/a（方向在此恢复）
        val left = exprs[i]!!
        val right = exprs[j]!!
        val node = when (op) {
            0 -> Expr.Node(Op.ADD, left, right)
            1 -> Expr.Node(Op.SUB, left, right)
            2 -> Expr.Node(Op.SUB, right, left)
            3 -> Expr.Node(Op.MUL, left, right)
            4 -> Expr.Node(Op.DIV, left, right)
            5 -> Expr.Node(Op.DIV, right, left)
            else -> throw IllegalStateException("invalid op code: $op")
        }
        exprs[i] = node
        if (j != count - 1) exprs[j] = exprs[count - 1]
        count--
    }
    return exprs[0]!!
}

internal fun formatExpr(e: Expr, style: ExpressionStyle): String = when (style) {
    ExpressionStyle.FULLY_PARENTHESIZED -> formatFull(e)
    ExpressionStyle.COMPACT -> formatCompact(e, null, false)
}

/** 完全括号：每个内部节点都加括号，无优先级歧义（R-083 / R-084）。分数叶子恒加括号。 */
private fun formatFull(e: Expr): String = when (e) {
    is Expr.Leaf -> if (e.d == 1L) e.n.toString() else "(${e.n}/${e.d})"
    is Expr.Node -> "(${formatFull(e.left)} ${e.op.symbol} ${formatFull(e.right)})"
}

/** 紧凑：只保留必要括号。分数叶子恒加括号。 */
private fun formatCompact(e: Expr, parent: Op?, rightSide: Boolean): String = when (e) {
    is Expr.Leaf -> if (e.d == 1L) e.n.toString() else "(${e.n}/${e.d})"
    is Expr.Node -> {
        val needParen = when (parent) {
            null -> false
            Op.ADD -> false
            Op.SUB -> rightSide && (e.op == Op.ADD || e.op == Op.SUB)
            Op.MUL -> e.op == Op.ADD || e.op == Op.SUB
            Op.DIV -> rightSide || e.op == Op.ADD || e.op == Op.SUB
        }
        val body = formatCompact(e.left, e.op, false) + " ${e.op.symbol} " +
            formatCompact(e.right, e.op, true)
        if (needParen) "($body)" else body
    }
}

/**
 * 独立表达式求值（分数语义，任意精度）：用于 LUT 校验与测试。
 * 与 DFS 的 intern 分数表完全无关，属于“独立 solver”角色（R-092）。
 * 返回 null 表示中途除零。
 */
internal fun evalExprFrac(e: Expr): Pair<BigNum, BigNum>? = when (e) {
    is Expr.Leaf -> BigNum.fromLong(e.n) to BigNum.fromLong(e.d)
    is Expr.Node -> {
        val l = evalExprFrac(e.left) ?: return null
        val r = evalExprFrac(e.right) ?: return null
        val (ln, ld) = l
        val (rn, rd) = r
        when (e.op) {
            Op.ADD -> ln * rd + rn * ld to ld * rd
            Op.SUB -> ln * rd - rn * ld to ld * rd
            Op.MUL -> ln * rn to ld * rd
            Op.DIV -> if (rn == BigNum.ZERO) null else ln * rd to ld * rn
        }
    }
}

// ============================================================
// 内部：1820 组合 LUT（PRD 3 节）
// lazy 生成：同一 Solver 生成，生成时用独立 evaluator 校验；
// 查询 O(1)。只对“标准输入 + 目标 24 + 全部运算符 + 单解”生效。
// ============================================================

private class Lut {

    /** key = 排序后 4 个数字各 4bit；index[key] = -1 无解，0 未生成，>0 条目下标+1。 */
    private val index = IntArray(1 shl 16)
    private val entries = IntArray(1820 * 4)
    private var ready = false
    private val engine = StdEngine()

    fun lookup(numbers: IntArray, style: ExpressionStyle): SolveResult? {
        ensureReady()
        val s = intArrayOf(numbers[0], numbers[1], numbers[2], numbers[3])
        s.sort()
        val key = s[0] or (s[1] shl 4) or (s[2] shl 8) or (s[3] shl 12)
        val v = index[key]
        if (v == 0) return null // 理论上不会发生（1820 全量已生成）
        if (v < 0) return SolveResult(SolveStatus.UNSOLVABLE, null, Rational(24), true, null, null)
        val base = (v - 1) * 4
        val steps = ByteArray(9)
        for (k in 0 until 3) decodeStep(entries[base + 1 + k], steps, k)
        val inputs = LongArray(4) { s[it].toLong() }
        val inputsOnes = LongArray(4) { 1L }
        val expr = formatExpr(replayExpr(inputs, inputsOnes, 4, steps, 3), style)
        return SolveResult(SolveStatus.SOLVED, expr, Rational(24), true, null, null)
    }

    private fun ensureReady() {
        if (!ready) build()
    }

    private fun build() {
        var entryIdx = 0
        var solvedCount = 0
        for (a in 1..13) {
            for (b in a..13) {
                for (c in b..13) {
                    for (d in c..13) {
                        val key = a or (b shl 4) or (c shl 8) or (d shl 12)
                        val inputs = longArrayOf(a.toLong(), b.toLong(), c.toLong(), d.toLong())
                        val inputsOnes = longArrayOf(1L, 1L, 1L, 1L)
                        val res = engine.solve(inputs, inputsOnes, 4, 24, 1, 1, Op.ALL, ExpressionStyle.FULLY_PARENTHESIZED)
                        if (res.status == SolveStatus.SOLVED) {
                            // 独立校验（R-092）：用任意精度分数 evaluator 验证表达式 == 24。
                            val expr = replayExpr(inputs, inputsOnes, 4, res.steps!!, 3)
                            val v = evalExprFrac(expr)
                            check(v != null) { "LUT 校验失败：表达式含除零" }
                            val (n, dd) = v
                            check(n == BigNum.fromLong(24) * dd) { "LUT 校验失败：表达式结果不为 24 ($a,$b,$c,$d)" }
                            for (k in 0 until 3) entries[entryIdx * 4 + 1 + k] = encodeStep(res.steps, k)
                            index[key] = entryIdx + 1
                            entryIdx++
                            solvedCount++
                        } else {
                            index[key] = -1
                        }
                    }
                }
            }
        }
        check(entryIdx == solvedCount && entryIdx > 0) { "LUT 构建异常" }
        ready = true
    }
}

private fun encodeStep(steps: ByteArray, k: Int): Int =
    ((steps[k * 3].toInt() and 0xFF) shl 6) or
        ((steps[k * 3 + 1].toInt() and 0xFF) shl 3) or
        (steps[k * 3 + 2].toInt() and 0xFF)

private fun decodeStep(code: Int, steps: ByteArray, k: Int) {
    steps[k * 3] = ((code ushr 6) and 0x3).toByte()
    steps[k * 3 + 1] = ((code ushr 3) and 0x3).toByte()
    steps[k * 3 + 2] = (code and 0x7).toByte()
}
