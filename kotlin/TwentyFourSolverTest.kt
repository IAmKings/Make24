/*
 * 24 点 Solver 测试（PRD 9 节）。
 * 运行方式：kotlinc TwentyFourSolver.kt BigNum.kt TwentyFourSolverTest.kt -include-runtime -d test.jar && java -jar test.jar
 */

package com.make24.solver

import kotlin.random.Random
import kotlin.system.exitProcess

// ============================================================
// 独立表达式验证器（测试用：解析字符串 + 分数求值 + 数字 multiset 校验）
// ============================================================

private data class TFrac(val n: BigNum, val d: BigNum)

private fun tfAdd(a: TFrac, b: TFrac) = TFrac(a.n * b.d + b.n * a.d, a.d * b.d)
private fun tfSub(a: TFrac, b: TFrac) = TFrac(a.n * b.d - b.n * a.d, a.d * b.d)
private fun tfMul(a: TFrac, b: TFrac) = TFrac(a.n * b.n, a.d * b.d)
private fun tfDiv(a: TFrac, b: TFrac): TFrac? =
    if (b.n == BigNum.ZERO) null else TFrac(a.n * b.d, a.d * b.n)

private class ExprParser(private val text: String) {
    private var pos = 0
    val digits = ArrayList<Long>()

    fun parse(): TFrac? {
        val v = parseExpr()
        skipWs()
        return if (v != null && pos == text.length) v else null
    }

    private fun skipWs() {
        while (pos < text.length && text[pos] == ' ') pos++
    }

    private fun parseExpr(): TFrac? {
        var left = parseTerm() ?: return null
        while (true) {
            skipWs()
            if (pos >= text.length) break
            when (text[pos]) {
                '+' -> { pos++; left = tfAdd(left, parseTerm() ?: return null) }
                '-' -> { pos++; left = tfSub(left, parseTerm() ?: return null) }
                else -> break
            }
        }
        return left
    }

    private fun parseTerm(): TFrac? {
        var left = parseFactor() ?: return null
        while (true) {
            skipWs()
            if (pos >= text.length) break
            when (text[pos]) {
                '*' -> { pos++; left = tfMul(left, parseFactor() ?: return null) }
                '/' -> { pos++; left = tfDiv(left, parseFactor() ?: return null) ?: return null }
                else -> break
            }
        }
        return left
    }

    private fun parseFactor(): TFrac? {
        skipWs()
        if (pos >= text.length) return null
        return when (text[pos]) {
            '(' -> {
                pos++
                val v = parseExpr()
                skipWs()
                if (pos < text.length && text[pos] == ')') {
                    pos++
                    v
                } else null
            }
            else -> parseNumber()
        }
    }

    private fun parseNumber(): TFrac? {
        skipWs()
        if (pos >= text.length) return null
        var sign = 1L
        if (text[pos] == '-') {
            sign = -1
            pos++
        }
        var v = 0L
        var any = false
        while (pos < text.length && text[pos].isDigit()) {
            v = v * 10 + (text[pos] - '0')
            pos++
            any = true
        }
        if (!any) return null
        v *= sign
        digits.add(v)
        return TFrac(BigNum.fromLong(v), BigNum.ONE)
    }
}

/** 验证表达式：值精确等于 target，且叶子数字 multiset 与输入一致（R-111 / R-112）。 */
private fun verify(nums: IntArray, expr: String, targetN: Long = 24, targetD: Long = 1): Boolean {
    val p = ExprParser(expr)
    val f = p.parse() ?: return false
    val valueOk = f.n * BigNum.fromLong(targetD) == BigNum.fromLong(targetN) * f.d
    val multisetOk = p.digits.sorted() == nums.map { it.toLong() }.sorted()
    return valueOk && multisetOk
}

// ============================================================
// 测试框架
// ============================================================

private var pass = 0
private var fail = 0

private fun check(name: String, cond: Boolean, detail: Any? = "") {
    if (cond) pass++ else {
        fail++
        println("FAIL  $name  $detail")
    }
}

private fun statusOf(nums: IntArray, opts: SolveOptions = SolveOptions()) =
    TwentyFourSolver.solve(nums, opts).status

// ============================================================
// 测试
// ============================================================

private fun testP0FunctionalCases() {
    // PRD 9.2 必测功能用例
    run {
        val nums = intArrayOf(3, 3, 8, 8)
        val r = TwentyFourSolver.solve(nums)
        check("[3,3,8,8] SOLVED", r.status == SolveStatus.SOLVED, r)
        check("[3,3,8,8] expr==24 & multiset", verify(nums, r.expression ?: ""), r.expression)
        check("[3,3,8,8] compact 格式", verify(nums, TwentyFourSolver.solve(nums, SolveOptions(style = ExpressionStyle.COMPACT)).expression ?: ""))
    }
    run {
        val nums = intArrayOf(1, 1, 1, 1)
        check("[1,1,1,1] UNSOLVABLE", statusOf(nums) == SolveStatus.UNSOLVABLE)
    }
    run {
        val nums = intArrayOf(1, 2, 3, 4)
        val r = TwentyFourSolver.solve(nums)
        check("[1,2,3,4] SOLVED", r.status == SolveStatus.SOLVED, r)
        check("[1,2,3,4] expr==24", verify(nums, r.expression ?: ""), r.expression)
    }
    run {
        val nums = intArrayOf(5, 5, 5, 1)
        val r = TwentyFourSolver.solve(nums)
        check("[5,5,5,1] SOLVED", r.status == SolveStatus.SOLVED, r)
        check("[5,5,5,1] expr==24", verify(nums, r.expression ?: ""), r.expression)
    }
    run {
        val nums = intArrayOf(1, 1, 1, 24)
        val r = TwentyFourSolver.solve(nums) // AUTO -> fallback（24 超出 1..13）
        check("[1,1,1,24] AUTO fallback SOLVED", r.status == SolveStatus.SOLVED, r)
        check("[1,1,1,24] expr==24", verify(nums, r.expression ?: ""), r.expression)
    }
    run {
        val nums = intArrayOf(0, 1, 2, 3)
        val r = TwentyFourSolver.solve(nums) // AUTO -> fallback，零输入不崩溃
        check("[0,1,2,3] 不崩溃", r.status == SolveStatus.SOLVED || r.status == SolveStatus.UNSOLVABLE, r)
    }
    run {
        val nums = intArrayOf(13, 13, 13, 13)
        val r = TwentyFourSolver.solve(nums)
        check("[13,13,13,13] 无溢出", r.status == SolveStatus.SOLVED || r.status == SolveStatus.UNSOLVABLE, r)
    }
    // 含除零分支的输入：[3,3,8,8] 的搜索包含 8/3、3/3 等除零/可除路径，仍应找到解
    run {
        val nums = intArrayOf(3, 3, 8, 8)
        val r = TwentyFourSolver.solve(nums, SolveOptions(useLut = false))
        check("[3,3,8,8] DFS 分数中间值解", r.status == SolveStatus.SOLVED, r)
    }
    // R-100 输入数量
    check("数量为 3 -> INVALID_INPUT", statusOf(intArrayOf(1, 2, 3)) == SolveStatus.INVALID_INPUT)
    // R-101 强制 STANDARD + 范围外
    check("STANDARD + [0,1,2,3] -> INVALID_INPUT",
        statusOf(intArrayOf(0, 1, 2, 3), SolveOptions(mode = SolveMode.STANDARD)) == SolveStatus.INVALID_INPUT)
}

private fun testTargetAndOps() {
    // R-004 自定义整数 target
    run {
        val nums = intArrayOf(1, 2, 3, 4)
        val r = TwentyFourSolver.solve(nums, SolveOptions(target = Rational(1), useLut = false))
        check("target=1 [1,2,3,4] SOLVED", r.status == SolveStatus.SOLVED, r)
        check("target=1 expr==1", verify(nums, r.expression ?: "", 1, 1), r.expression)
    }
    // R-008 自定义有理数 target（fallback 与标准路径）
    run {
        val nums = intArrayOf(1, 1, 1, 2)
        for (mode in listOf(SolveMode.STANDARD, SolveMode.FALLBACK)) {
            val r = TwentyFourSolver.solve(nums, SolveOptions(target = Rational(1, 2), mode = mode, useLut = false))
            check("target=1/2 mode=$mode SOLVED", r.status == SolveStatus.SOLVED, r)
            check("target=1/2 expr", verify(nums, r.expression ?: "", 1, 2), r.expression)
        }
    }
    // R-008 自定义运算符集合
    run {
        val nums = intArrayOf(1, 2, 3, 4)
        val r = TwentyFourSolver.solve(nums, SolveOptions(target = Rational(10), operators = setOf(Op.ADD), useLut = false))
        check("{+} target=10 SOLVED (1+2+3+4)", r.status == SolveStatus.SOLVED, r)
        val r2 = TwentyFourSolver.solve(nums, SolveOptions(target = Rational(24), operators = setOf(Op.ADD), useLut = false))
        check("{+} target=24 UNSOLVABLE", r2.status == SolveStatus.UNSOLVABLE, r2)
    }
}

private fun testDeterminism() {
    val nums = intArrayOf(3, 3, 8, 8)
    val opts = SolveOptions(useLut = false)
    val r1 = TwentyFourSolver.solve(nums, opts)
    val r2 = TwentyFourSolver.solve(nums, opts)
    check("R-103 确定性", r1.status == r2.status && r1.expression == r2.expression)
}

private fun testAllSolutions() {
    val nums = intArrayOf(1, 2, 3, 4)
    val all = TwentyFourSolver.solveAll(nums, SolveOptions(useLut = false))
    check("solveAll 至少 2 个解", all.size >= 2, all.size)
    check("solveAll 全部有效且去重",
        all.all { it.status == SolveStatus.SOLVED && verify(nums, it.expression ?: "") } &&
            all.map { it.expression }.distinct().size == all.size,
        all.map { it.expression })
    val limited = TwentyFourSolver.solve(nums, SolveOptions(maxSolutions = 3, useLut = false))
    check("maxSolutions=3 返回 <=3 个解",
        limited.status == SolveStatus.SOLVED && (limited.expressions?.size ?: 0) <= 3 &&
            (limited.expressions?.size ?: 0) >= 1, limited)
}

private fun test1820Consistency() {
    // R-110：标准路径与 BigInteger fallback 对全部 1820 组合可解性一致
    var mismatch = 0
    var solved = 0
    var total = 0
    for (a in 1..13) for (b in a..13) for (c in b..13) for (d in c..13) {
        val nums = intArrayOf(a, b, c, d)
        total++
        val std = TwentyFourSolver.solve(nums, SolveOptions(useLut = false, mode = SolveMode.STANDARD))
        val fb = TwentyFourSolver.solve(nums, SolveOptions(mode = SolveMode.FALLBACK))
        if (std.status != fb.status) mismatch++
        if (std.status == SolveStatus.SOLVED) {
            solved++
            if (!verify(nums, std.expression ?: "")) {
                mismatch++
                println("  表达式验证失败: $a,$b,$c,$d -> ${std.expression}")
            }
        }
    }
    check("1820 组合 standard==fallback 且表达式有效 (mismatch=0)", mismatch == 0, "mismatch=$mismatch")
    println("  1820 组合中可解数量: $solved / $total")
}

private fun testProperty100k() {
    val rng = Random(20240821)
    var ok = 0
    var unsol = 0
    var lutMismatch = 0
    for (i in 0 until 100_000) {
        val nums = intArrayOf(rng.nextInt(1, 14), rng.nextInt(1, 14), rng.nextInt(1, 14), rng.nextInt(1, 14))
        val r = TwentyFourSolver.solve(nums, SolveOptions(useLut = false))
        when (r.status) {
            SolveStatus.SOLVED -> {
                if (verify(nums, r.expression ?: "")) ok++
                else {
                    fail++
                    println("FAIL 属性: 表达式无效 $nums -> ${r.expression}")
                }
            }
            SolveStatus.UNSOLVABLE -> unsol++
            else -> {
                fail++
                println("FAIL 属性: 意外状态 ${r.status} for $nums")
            }
        }
        // LUT 与 DFS 一致性（R-090 系列）
        val rl = TwentyFourSolver.solve(nums, SolveOptions(useLut = true))
        if (rl.status != r.status) lutMismatch++
    }
    check("100k 随机标准输入: SOLVED 表达式全部有效", ok > 0)
    check("100k LUT 与 DFS status 一致", lutMismatch == 0, "lutMismatch=$lutMismatch")
    println("  100k 属性测试: solved=$ok unsolvable=$unsol")
}

private fun testConcurrency() {
    // 固定 200 个输入集的串行参考结果
    val rng = Random(7)
    val inputs = Array(200) { intArrayOf(rng.nextInt(1, 14), rng.nextInt(1, 14), rng.nextInt(1, 14), rng.nextInt(1, 14)) }
    val refs = Array(200) { i ->
        val r = TwentyFourSolver.solve(inputs[i], SolveOptions(useLut = false))
        r.status to r.expression
    }
    val errors = java.util.concurrent.atomic.AtomicInteger(0)
    val threads = (0 until 8).map { t ->
        Thread {
            val ws = SolverWorkspace()
            for (i in 0 until 200) {
                val r = TwentyFourSolver.solve(inputs[i], SolveOptions(useLut = false), ws)
                if (r.status != refs[i].first || r.expression != refs[i].second) {
                    errors.incrementAndGet()
                }
            }
        }.apply { name = "t$t" }
    }
    threads.forEach { it.start() }
    threads.forEach { it.join() }
    check("并发 8 线程结果与串行一致", errors.get() == 0, "errors=${errors.get()}")
}

/** 验证表达式（3 数场景）：值精确等于 target（输入可能含分数叶子，不做 multiset 校验）。 */
private fun verify3(expr: String, targetN: Long = 24, targetD: Long = 1): Boolean {
    val p = ExprParser(expr)
    val f = p.parse() ?: return false
    return f.n * BigNum.fromLong(targetD) == BigNum.fromLong(targetN) * f.d
}

/** 独立暴力 3 数求解参考（枚举 ≤ 108 组合，与生产引擎实现完全独立）。 */
private fun brute3(a: Rational, b: Rational, c: Rational, tn: Long, td: Long): Boolean {
    fun tf(r: Rational) = TFrac(BigNum.fromLong(r.numerator), BigNum.fromLong(r.denominator))
    val xs = arrayOf(tf(a), tf(b), tf(c))
    for (i in 0 until 3) {
        for (j in 0 until 3) {
            if (i == j) continue
            val k = 3 - i - j
            val x = xs[i]
            val y = xs[j]
            val z = xs[k]
            val first = listOf(tfAdd(x, y), tfSub(x, y), tfSub(y, x), tfMul(x, y), tfDiv(x, y), tfDiv(y, x))
            for (v in first) {
                if (v == null) continue
                val second = listOf(tfAdd(v, z), tfSub(v, z), tfSub(z, v), tfMul(v, z), tfDiv(v, z), tfDiv(z, v))
                for (f in second) {
                    if (f != null && f.n * BigNum.fromLong(td) == BigNum.fromLong(tn) * f.d) return true
                }
            }
        }
    }
    return false
}

private fun testBigNumVsBigInteger() {
    // 用 JVM 的 java.math.BigInteger 作为参考，验证跨平台 BigNum 的四则/比较/gcd/字符串往返。
    val jrnd = java.util.Random(2024)
    fun rndBig(): java.math.BigInteger {
        val bits = jrnd.nextInt(1, 200)
        var v = java.math.BigInteger(bits, jrnd)
        if (jrnd.nextBoolean()) v = v.negate()
        return v
    }
    fun toBN(v: java.math.BigInteger): BigNum = BigNum.fromString(v.toString())
    var bad = 0
    fun assertEq(name: String, x: BigNum, y: java.math.BigInteger) {
        if (x != toBN(y)) {
            bad++
            if (bad <= 5) println("  BigNum 不匹配 [$name]: x=$x y=$y")
        }
    }
    for (i in 0 until 30_000) {
        val a = rndBig()
        val b = rndBig()
        val bnA = toBN(a)
        val bnB = toBN(b)
        assertEq("add", bnA + bnB, a + b)
        assertEq("sub", bnA - bnB, a - b)
        assertEq("mul", bnA * bnB, a * b)
        if (b.signum() != 0) {
            val qr = a.divideAndRemainder(b)
            assertEq("div", bnA / bnB, qr[0])
            assertEq("rem", bnA % bnB, qr[1])
        }
        assertEq("cmpLT", if (bnA < bnB) BigNum.ONE else BigNum.ZERO, if (a < b) java.math.BigInteger.ONE else java.math.BigInteger.ZERO)
        assertEq("gcd", bnA.gcd(bnB), a.gcd(b))
        if (i % 7 == 0) assertEq("toString", BigNum.fromString(bnA.toString()), a)
    }
    // Long 边界
    assertEq("Long.MAX", BigNum.fromLong(Long.MAX_VALUE), java.math.BigInteger.valueOf(Long.MAX_VALUE))
    assertEq("Long.MIN", BigNum.fromLong(Long.MIN_VALUE), java.math.BigInteger.valueOf(Long.MIN_VALUE))
    check("BigNum 与 BigInteger 30k 随机对照", bad == 0, "bad=$bad")
}

private fun testSolveThree() {
    // 功能用例
    run {
        val r = TwentyFourSolver.solveThree(Rational(8), Rational(4), Rational(2))
        check("solveThree(8,4,2) SOLVED", r.status == SolveStatus.SOLVED && verify3(r.expression ?: ""), r.expression)
    }
    run {
        val r = TwentyFourSolver.solveThree(Rational(24), Rational(1), Rational(1))
        check("solveThree(24,1,1) SOLVED", r.status == SolveStatus.SOLVED && verify3(r.expression ?: ""), r.expression)
    }
    run {
        // 分数输入：8 / (1/3) * 1 = 24
        val r = TwentyFourSolver.solveThree(Rational(8), Rational(1, 3), Rational(1))
        check("solveThree(8,1/3,1) SOLVED", r.status == SolveStatus.SOLVED && verify3(r.expression ?: ""), r.expression)
        // compact 格式也有效
        val rc = TwentyFourSolver.solveThree(Rational(8), Rational(1, 3), Rational(1), SolveOptions(style = ExpressionStyle.COMPACT))
        check("solveThree compact 有效", rc.status == SolveStatus.SOLVED && verify3(rc.expression ?: ""), rc.expression)
    }
    run {
        val r = TwentyFourSolver.solveThree(Rational(1), Rational(1), Rational(1))
        check("solveThree(1,1,1) UNSOLVABLE", r.status == SolveStatus.UNSOLVABLE, r)
    }
    run {
        // (1/3)/(1/3)*24 = 24
        val r = TwentyFourSolver.solveThree(Rational(1, 3), Rational(1, 3), Rational(24))
        check("solveThree(1/3,1/3,24) SOLVED", r.status == SolveStatus.SOLVED && verify3(r.expression ?: ""), r.expression)
    }
    // 与独立暴力参考对照（5000 组随机，含分数输入；standard 与 fallback 各验证一次）
    val rng = Random(4242)
    var mismatch = 0
    for (i in 0 until 5000) {
        fun rn(): Rational =
            if (rng.nextBoolean()) Rational(rng.nextInt(1, 14).toLong())
            else Rational(rng.nextInt(1, 14).toLong(), rng.nextInt(1, 14).toLong())
        val a = rn()
        val b = rn()
        val c = rn()
        val st = TwentyFourSolver.solveThree(a, b, c)
        val stFb = TwentyFourSolver.solveThree(a, b, c, SolveOptions(mode = SolveMode.FALLBACK))
        val brute = brute3(a, b, c, 24, 1)
        if ((st.status == SolveStatus.SOLVED) != brute || st.status != stFb.status) {
            mismatch++
            if (mismatch <= 5) println("  solveThree 不一致: ($a,$b,$c) std=${st.status} fb=${stFb.status} brute=$brute")
        }
    }
    check("solveThree 5000 组与暴力参考及 fallback 一致", mismatch == 0, "mismatch=$mismatch")

    // 性能 smoke：100k 次 3 数快速判定
    val rng2 = Random(7)
    val triples = Array(1000) { Triple(rng2.nextInt(1, 14), rng2.nextInt(1, 14), rng2.nextInt(1, 14)) }
    for (i in 0 until 5000) TwentyFourSolver.solveThree(Rational(triples[i % 1000].first.toLong()), Rational(triples[i % 1000].second.toLong()), Rational(triples[i % 1000].third.toLong()))
    var t0 = System.nanoTime()
    for (i in 0 until 100_000) {
        val t = triples[i % 1000]
        TwentyFourSolver.solveThree(Rational(t.first.toLong()), Rational(t.second.toLong()), Rational(t.third.toLong()))
    }
    val ms = (System.nanoTime() - t0) / 1e6
    println("  solveThree 100k: ${"%.1f".format(ms)} ms -> avg ${"%.3f".format(ms * 10)} µs")
}

/** 独立暴力 2 数求解参考（枚举 ≤ 6 候选，与生产实现完全独立）。 */
private fun brute2(a: Rational, b: Rational, tn: Long, td: Long): Boolean {
    fun tf(r: Rational) = TFrac(BigNum.fromLong(r.numerator), BigNum.fromLong(r.denominator))
    val x = tf(a)
    val y = tf(b)
    val combos = listOf(tfAdd(x, y), tfSub(x, y), tfSub(y, x), tfMul(x, y), tfDiv(x, y), tfDiv(y, x))
    for (f in combos) {
        if (f != null && f.n * BigNum.fromLong(td) == BigNum.fromLong(tn) * f.d) return true
    }
    return false
}

private fun testSolveTwo() {
    // 功能用例
    run {
        val r = TwentyFourSolver.solveTwo(Rational(8), Rational(3))
        check("solveTwo(8,3) SOLVED (8*3)", r.status == SolveStatus.SOLVED && verify3(r.expression ?: ""), r.expression)
    }
    run {
        val r = TwentyFourSolver.solveTwo(Rational(5), Rational(5))
        check("solveTwo(5,5) UNSOLVABLE", r.status == SolveStatus.UNSOLVABLE, r)
    }
    run {
        // 分数：8 / (1/3) = 24
        val r = TwentyFourSolver.solveTwo(Rational(8), Rational(1, 3))
        check("solveTwo(8,1/3) SOLVED", r.status == SolveStatus.SOLVED && verify3(r.expression ?: ""), r.expression)
        val rc = TwentyFourSolver.solveTwo(Rational(8), Rational(1, 3), SolveOptions(style = ExpressionStyle.COMPACT))
        check("solveTwo compact 有效", rc.status == SolveStatus.SOLVED && verify3(rc.expression ?: ""), rc.expression)
    }
    run {
        // 自定义 target：10 - 4 = 6
        val r = TwentyFourSolver.solveTwo(Rational(10), Rational(4), SolveOptions(target = Rational(6)))
        check("solveTwo(10,4,target=6) SOLVED", r.status == SolveStatus.SOLVED && verify3(r.expression ?: "", 6, 1), r.expression)
    }
    run {
        // 除零：0/5 跳过；0*5=0 → target 0 有解、target 24 无解
        val r0 = TwentyFourSolver.solveTwo(Rational(0), Rational(5), SolveOptions(target = Rational(0)))
        check("solveTwo(0,5,target=0) SOLVED", r0.status == SolveStatus.SOLVED, r0)
        val r24 = TwentyFourSolver.solveTwo(Rational(0), Rational(5))
        check("solveTwo(0,5,target=24) UNSOLVABLE", r24.status == SolveStatus.UNSOLVABLE, r24)
    }
    // 与独立暴力参考及 fallback 三方一致（2000 组随机，含分数与较大值）
    val rng = Random(99)
    var mismatch = 0
    for (i in 0 until 2000) {
        fun rn(): Rational =
            if (rng.nextBoolean()) Rational(rng.nextInt(1, 100).toLong())
            else Rational(rng.nextInt(1, 100).toLong(), rng.nextInt(1, 100).toLong())
        val a = rn()
        val b = rn()
        val st = TwentyFourSolver.solveTwo(a, b)
        val fb = TwentyFourSolver.solveTwo(a, b, SolveOptions(mode = SolveMode.FALLBACK))
        val brute = brute2(a, b, 24, 1)
        if ((st.status == SolveStatus.SOLVED) != brute || st.status != fb.status) {
            mismatch++
            if (mismatch <= 5) println("  solveTwo 不一致: ($a,$b) std=${st.status} fb=${fb.status} brute=$brute")
        }
    }
    check("solveTwo 2000 组与暴力参考及 fallback 一致", mismatch == 0, "mismatch=$mismatch")

    // 性能 smoke：100k 次 2 数快速判定
    val rng2 = Random(3)
    val pairs = Array(1000) { rng2.nextInt(1, 14) to rng2.nextInt(1, 14) }
    for (i in 0 until 5000) {
        val p = pairs[i % 1000]
        TwentyFourSolver.solveTwo(Rational(p.first.toLong()), Rational(p.second.toLong()))
    }
    var t0 = System.nanoTime()
    for (i in 0 until 100_000) {
        val p = pairs[i % 1000]
        TwentyFourSolver.solveTwo(Rational(p.first.toLong()), Rational(p.second.toLong()))
    }
    val ms = (System.nanoTime() - t0) / 1e6
    println("  solveTwo 100k: ${"%.1f".format(ms)} ms -> avg ${"%.3f".format(ms * 10)} µs")
}

private fun testPerfSmoke() {
    val rng = Random(99)
    val inputs = Array(1000) { intArrayOf(rng.nextInt(1, 14), rng.nextInt(1, 14), rng.nextInt(1, 14), rng.nextInt(1, 14)) }

    // 预热
    for (i in 0 until 2000) TwentyFourSolver.solve(inputs[i % 1000], SolveOptions(useLut = false))

    val n = 100_000
    var t0 = System.nanoTime()
    for (i in 0 until n) TwentyFourSolver.solve(inputs[i % 1000], SolveOptions(useLut = false))
    val dfsMs = (System.nanoTime() - t0) / 1e6
    println("  DFS 100k solves: ${"%.1f".format(dfsMs)} ms -> avg ${"%.2f".format(dfsMs * 10)} µs/solve")

    t0 = System.nanoTime()
    for (i in 0 until n) TwentyFourSolver.solve(inputs[i % 1000], SolveOptions(useLut = true))
    val lutMs = (System.nanoTime() - t0) / 1e6
    println("  LUT 100k solves: ${"%.1f".format(lutMs)} ms -> avg ${"%.2f".format(lutMs * 10)} µs/solve")

    // 1820 组合 LUT 全量查询（PRD 7 节：Kotlin <= 20ms 参考）
    t0 = System.nanoTime()
    var c = 0
    for (a in 1..13) for (b in a..13) for (c2 in b..13) for (d in c2..13) {
        val r = TwentyFourSolver.solve(intArrayOf(a, b, c2, d), SolveOptions(useLut = true))
        if (r.status == SolveStatus.SOLVED) c++
    }
    val allMs = (System.nanoTime() - t0) / 1e6
    println("  1820 组合 LUT 查询: ${"%.1f".format(allMs)} ms, solved=$c")
    check("1820 全量 LUT 查询", c > 0)
}

// ============================================================

fun main() {
    val t0 = System.nanoTime()
    testP0FunctionalCases()
    testTargetAndOps()
    testDeterminism()
    testAllSolutions()
    testSolveThree()
    testSolveTwo()
    testBigNumVsBigInteger()
    test1820Consistency()
    testProperty100k()
    testConcurrency()
    testPerfSmoke()
    val totalMs = (System.nanoTime() - t0) / 1e6
    println("总耗时 ${"%.1f".format(totalMs)} ms | PASS=$pass FAIL=$fail")
    if (fail > 0) exitProcess(1)
    println("ALL TESTS PASSED")
}
