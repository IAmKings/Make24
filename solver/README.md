# 24 点 Solver（Kotlin Multiplatform / Compose Multiplatform）

依据 `prd_24_point_solver.md` 实现的纯 Kotlin 24 点求解算法，可直接嵌入
Compose Multiplatform 项目的 `src/commonMain/kotlin/` 使用（无 `java.*` 依赖，
Android / iOS / Desktop / Web 通用）。

## 文件

| 文件 | 说明 |
|---|---|
| `TwentyFourSolver.kt` | 公共 API、标准路径引擎、fallback 引擎、replay、LUT |
| `BigNum.kt` | 内置任意精度整数（跨平台，仅 fallback 使用），与 `TwentyFourSolver.kt` 一起拷贝 |
| `TwentyFourSolverTest.kt` | 测试入口（PRD 9 节用例 + 1820 组合一致性 + 属性测试 + 并发 + 性能 smoke） |

## 快速开始

```kotlin
import com.make24.solver.TwentyFourSolver
import com.make24.solver.SolveOptions
import com.make24.solver.SolveStatus

// Compose 层直接调用
val result = TwentyFourSolver.solve(intArrayOf(3, 3, 8, 8))
// result.status == SOLVED
// result.expression == "(8 / (3 - (8 / 3)))"   （完全括号格式）
// result.usedLut == true
```

常用形式：

```kotlin
// 便捷入口（List<Int> + target）
TwentyFourSolver.solve(listOf(1, 2, 3, 4))                  // target 默认 24

// 自定义目标 / 模式 / 样式 / 运算符
TwentyFourSolver.solve(
    intArrayOf(5, 5, 5, 1),
    SolveOptions(
        target = Rational(24),
        mode = SolveMode.AUTO,               // AUTO / STANDARD / FALLBACK
        useLut = true,                       // 1820 组合 LUT（标准输入 + target 24 + 全运算符 + 单解时生效）
        maxSolutions = 1,                    // >1 时返回多条去重解（见 SolveResult.expressions）
        operators = Op.ALL,                  // 可自定义 {ADD, SUB, MUL, DIV}
        style = ExpressionStyle.FULLY_PARENTHESIZED, // 或 COMPACT
    ),
)

// 全部去重解
TwentyFourSolver.solveAll(intArrayOf(1, 2, 3, 4))

// 3 数快速求解：玩家完成一次合并后，实时判断剩余 3 个数是否有解（输入可为分数）
TwentyFourSolver.solveThree(Rational(8), Rational(1, 3), Rational(1))   // SOLVED，如 "(8 / (1/3)) * 1"
TwentyFourSolver.solveThree(Rational(1), Rational(1), Rational(1))     // UNSOLVABLE

// 2 数快速求解：再合并一次、仅剩 2 个数时的实时提示
TwentyFourSolver.solveTwo(Rational(8), Rational(3))                    // SOLVED，如 "(8 * 3)"
TwentyFourSolver.solveTwo(Rational(5), Rational(5))                    // UNSOLVABLE

// 复用 workspace（避免每次 solve 分配数组；非线程安全，单线程使用）
val ws = SolverWorkspace()
TwentyFourSolver.solve(intArrayOf(3, 3, 8, 8), SolveOptions(useLut = false), ws)
```

## API

- `solve(numbers: IntArray, options: SolveOptions = ..., workspace: SolverWorkspace? = null): SolveResult`
- `solveThree(a: Rational, b: Rational, c: Rational, options = ..., workspace = ...): SolveResult`
  —— 3 数快速求解：判断 `{a, b, c}`（可含分数，如一次合并后的 `8/3`）能否得到 target
  并返回一条解法；不走 LUT。枚举规模 ≤ 3 对 × 6 候选 × 6 运算 = 108 组合，实测约 15 µs/次，
  适合在玩家完成一次合并后实时提示"剩余 3 个数是否有解 / 提示解法"。
- `solveTwo(a: Rational, b: Rational, options = ..., workspace = ...): SolveResult`
  —— 2 数快速求解：判断 `{a, b}` 能否通过一次运算得到 target 并返回该解法（≤ 6 候选，
  O(1)）；用于玩家完成第二次合并、仅剩 2 个数时的实时提示。不走 LUT。
- `SolveStatus`：`SOLVED | UNSOLVABLE | INVALID_INPUT | OVERFLOW | RESOURCE_LIMIT`
- `SolveResult.status / expression / target / usedLut / stats / expressions`
- 约定（PRD R-102）：`expression != null` 当且仅当 `status == SOLVED`。

错误与边界（PRD 第 6 节）：

| 场景 | 行为 |
|---|---|
| 输入数量不为 4 | `INVALID_INPUT` |
| `STANDARD` 模式输入不在 1..13 | `INVALID_INPUT` |
| `AUTO` 模式输入含 0 / 大数 / 负数 | 自动转任意精度 fallback |
| 标准路径中间值溢出（含大 target） | `AUTO` 转 fallback；`STANDARD` 返回 `OVERFLOW` |
| 除数为 0 的候选分支 | 静默跳过，不视为错误 |
| intern / visited 资源上限 | `RESOURCE_LIMIT`（标准输入下理论不可达） |

## 实现要点（对应 PRD）

- **标准路径**（输入 1..13，`Long` 精确分数）：
  - Fraction intern（uint16 ID，数值等价去重，ID 每次 solve 重建）
  - canonical `uint64` state（分数按数值排序打包 4×16bit，未用槽位填 0）
  - 固定容量 open-addressing visited（2 的幂、线性探测、负载 < 0.5）
  - allocation-free DFS：预分配数组、原地 compact/restore、递归深度 ≤ 3
  - 候选去重与固定顺序 `+ a-b b-a * a/b b/a`（R-050..R-053）
  - 3-byte steps（i, j, op），命中后 replay 构造表达式（R-070..R-084）
  - 无浮点：目标判定用整数交叉乘法，溢出用 `mulExact/addExact/subExact` 检测
- **fallback**（任意 Int / 大 target）：内置 BigNum 任意精度分数，与标准路径共用
  搜索结构与 replay，保证两种实现可解性一致（R-110）。
- **1820 组合 LUT**：首次标准求解时 lazy 生成（同一 Solver 生成 + 独立 BigNum
  evaluator 校验每个解 == 24），查询 O(1)；不同目标/运算符/多解请求自动绕过。
- **确定性**：候选顺序、visited 线性探测、intern 数值排序均固定，同输入同配置
  结果完全一致（R-103 / R-114）。

## 测试

```bash
# 需要 Kotlin 2.x 编译器（测试运行于 JVM）
kotlinc TwentyFourSolver.kt BigNum.kt TwentyFourSolverTest.kt -include-runtime -d test.jar
java -jar test.jar
```

覆盖（全部通过）：
- PRD 9.2 必测用例：`[3,3,8,8]`、`[1,1,1,1]`、`[1,2,3,4]`、`[5,5,5,1]`、
  `[1,1,1,24]`（fallback）、`[0,1,2,3]`、`[13,13,13,13]`、除零分支、输入数量/模式校验
- `solveThree`：整数/分数输入功能用例、5000 组随机与独立暴力参考及 fallback 三方一致性
- `solveTwo`：整数/分数/自定义 target/除零功能用例、2000 组随机与独立暴力参考及 fallback 三方一致性
- 1820 组合：标准路径与 fallback 可解性逐项一致（1362 可解），每个解表达式独立验证 == 24
- BigNum 与 `java.math.BigInteger` 30k 随机对照（四则、比较、gcd、字符串往返、Long 边界）
- 100k 随机标准输入属性测试：表达式值/数字多重集合校验，LUT 与 DFS 状态一致
- 8 线程并发：结果与串行完全一致（各自 workspace）
- 自定义 target（含分数 target 1/2）、自定义运算符集合、maxSolutions / solveAll、表达式格式

## 性能（本机 JVM 实测，informational）

| 场景 | 结果 |
|---|---|
| 标准 DFS 单次 solve（workspace 复用 + 预热后，200k 次均值） | ≈ 21 µs |
| LUT 命中单次 solve | ≈ 0.12 µs |
| 1820 组合 LUT 全量查询 | < 1 ms |
| 100k 属性测试整体（含表达式验证） | ≈ 6 s |

PRD 第 7 节的 0 分配 / 64 KiB workspace 等硬指标为 C++ 级别的 benchmark 目标；
本 Kotlin 实现遵循其架构（热路径无对象分配、预分配数组、uint64 state、3-byte
steps），未在非 JVM 目标上做 JMH 级别的测量。

## 说明

- fallback 使用自研 `BigNum`（Kotlin 2.1 的实验性 `kotlin.math.BigInt` 已在
  新版本标准库中移除，故内置实现以保持跨平台零依赖）。
- `SolverWorkspace` 不是线程安全的：并发调用请为每个线程创建独立实例，
  或每次调用不传 workspace（内部新建）。
- 全部解模式（`maxSolutions > 1`）的“去重”按表达式字符串实现（PRD R-115
  的规范化表达式去重为 P1 增强项）。
