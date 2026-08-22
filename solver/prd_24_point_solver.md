# PRD：极致优化 24 点 Solver

版本：1.0  
状态：可开发、可验收  
目标实现：C++17+、Kotlin/JVM 或 Kotlin Multiplatform  
优先级：P0 = 首版必须完成；P1 = 首版可选、但接口应预留

## 1. 产品目标与范围

### 1.1 目标

构建一个面向标准 24 点游戏的高性能、确定性、无浮点误差 Solver：输入四个数字，判断是否能通过 `+ - × ÷` 和任意括号得到 24，并在有解时返回一条合法表达式。

### 1.2 范围

| 编号 | 优先级 | 需求 |
|---|---|---|
| R-001 | P0 | 支持四个整数输入，标准模式范围为 1–13。 |
| R-002 | P0 | 支持 `+`、`-`、`*`、`/`，每个输入必须恰好使用一次。 |
| R-003 | P0 | 支持任意合法二叉括号结构，不限制中间结果为整数。 |
| R-004 | P0 | 默认目标值为 24；API 应允许配置其他整数目标。 |
| R-005 | P0 | 有解时返回至少一条表达式；无解时返回明确的无解结果。 |
| R-006 | P0 | 标准 1–13 四数字模式使用优化路径；任意数字或超出安全范围时使用 fallback。 |
| R-007 | P1 | 支持返回全部去重解或限制返回数量。 |
| R-008 | P1 | 支持自定义运算符集合和目标有理数。 |

不在范围内：幂运算、阶乘、拼接数字、隐式负数输入、概率推荐、自然语言题目解析。

## 2. 核心行为与算法

### 2.1 搜索模型

使用递归两两合并搜索。每一步从当前数字集合选择两个 Fraction，生成有效候选结果，删除两个元素并加入一个结果，直到剩余一个值。

对有序对 `a,b`，候选运算为：

`a+b`、`a-b`、`b-a`、`a*b`、`a/b`（b ≠ 0）、`b/a`（a ≠ 0）。

交换律运算在候选层只保留一次；非交换运算保留两个方向。

### 2.2 精确 Fraction

| 编号 | 优先级 | 需求 |
|---|---|---|
| R-010 | P0 | Fraction 使用整数分子 `n` 和正整数分母 `d`，禁止使用 Float/Double 参与求解。 |
| R-011 | P0 | 每次构造后约分，`gcd(abs(n), d) = 1`；零统一表示为 `0/1`。 |
| R-012 | P0 | 分母始终大于 0；若输入分母为负，必须同时翻转分子和分母。 |
| R-013 | P0 | 除零候选必须被跳过，不得抛出未处理异常。 |
| R-014 | P0 | 计算溢出必须可检测；标准路径溢出时转 fallback 或返回明确错误。 |
| R-015 | P0 | 目标判断使用 `n == target * d`，禁止 epsilon 比较。 |

标准路径推荐使用 `int64`；实现可在中间乘加时使用 `__int128` 或 Kotlin 的安全乘法检查。任意数字 fallback 可使用 BigInteger/BigInt Fraction。

### 2.3 uint64 canonical state

| 编号 | 优先级 | 需求 |
|---|---|---|
| R-020 | P0 | 每次搜索状态最多包含 4 个 Fraction ID，每个 ID 为 `uint16`。 |
| R-021 | P0 | State 先按 Fraction 的规范序排序，再按固定顺序打包为 4 × 16 bit 的 `uint64`。 |
| R-022 | P0 | 未使用槽位填 0；状态编码不得包含字符串、指针或哈希随机值。 |
| R-023 | P0 | 在同一次 solve 调用内，相同 Fraction 必须得到相同 ID；不同 Fraction 不得共享 ID。 |
| R-024 | P0 | `uint64` 只作为 canonical key；Fraction 表仍负责真实值校验，避免跨 solve 复用 ID。 |

规范排序键为 `(n, d)` 的数值二元组，不能仅按 ID 排序。若实现选择按 intern ID 排序，必须证明 ID 分配顺序与 Fraction 全序一致。

### 2.4 Fraction intern

| 编号 | 优先级 | 需求 |
|---|---|---|
| R-030 | P0 | 每次产生新 Fraction 后先查 intern 表，命中则复用已有 `uint16` ID。 |
| R-031 | P0 | intern 表必须使用数值等价判断，不得使用未约分表示判断。 |
| R-032 | P0 | 标准输入的 intern 数量超过 `65535` 时必须安全失败或切换 fallback。 |
| R-033 | P1 | C++ 使用固定容量或可预留容量的 hash table；Kotlin 使用可复用数组/原始类型结构，避免热路径装箱。 |

### 2.5 allocation-free DFS

| 编号 | 优先级 | 需求 |
|---|---|---|
| R-040 | P0 | 搜索热路径不得创建 String、表达式对象、集合对象、lambda、异常或临时堆对象。 |
| R-041 | P0 | 当前 Fraction ID 数组、原始值表、路径记录和 visited 表在 solve 开始时一次性分配或复用。 |
| R-042 | P0 | DFS 使用固定长度数组和原地 compact/restore；递归深度最多为 3。 |
| R-043 | P0 | 找到解后立即停止搜索，不继续构造其他表达式。 |
| R-044 | P1 | Kotlin 版本在标准模式下单次 solve 不产生可观察的短命对象；允许调用方显式传入 workspace 复用内存。 |

### 2.6 local candidate dedup

| 编号 | 优先级 | 需求 |
|---|---|---|
| R-050 | P0 | 对每个选中的数对，候选结果按 Fraction 值去重。 |
| R-051 | P0 | `a+b` 与 `b+a`、`a*b` 与 `b*a` 不得重复执行。 |
| R-052 | P0 | 相同结果来自不同运算时，保留第一条稳定顺序路径，保证结果确定性。 |
| R-053 | P0 | 候选顺序固定为 `+,-(a-b),-(b-a),*,/(a/b),/(b/a)`，跳过无效项。 |

### 2.7 fixed open-addressing visited

| 编号 | 优先级 | 需求 |
|---|---|---|
| R-060 | P0 | visited 使用固定容量 open addressing 表，不得在 DFS 中扩容。 |
| R-061 | P0 | key 为 `uint64`；使用明确的空槽标记，并正确处理 key=0。 |
| R-062 | P0 | 负载因子不得超过 0.70；容量采用 2 的幂，线性探测或二次探测均可。 |
| R-063 | P0 | 表满时必须返回容量错误或切换到 fallback，不得死循环。 |
| R-064 | P0 | visited 的语义为“当前 solve 中已处理过的 canonical multiset 状态”。 |

### 2.8 3-byte solution steps

每个合并步骤固定 3 bytes：

`byte 0 = i`、`byte 1 = j`、`byte 2 = operation code`。

| op code | 运算 |
|---:|---|
| 0 | `a+b` |
| 1 | `a-b` |
| 2 | `b-a` |
| 3 | `a*b` |
| 4 | `a/b` |
| 5 | `b/a` |

| 编号 | 优先级 | 需求 |
|---|---|---|
| R-070 | P0 | 路径最多保存 3 个步骤，共 9 bytes；不得在 DFS 中保存字符串。 |
| R-071 | P0 | 步骤中的索引必须对应当前数组布局，并可在 replay 时复现。 |
| R-072 | P0 | 回溯覆盖父层路径，不为每个分支复制路径对象。 |

### 2.9 命中后 replay 构造表达式

| 编号 | 优先级 | 需求 |
|---|---|---|
| R-080 | P0 | 只有 DFS 命中目标后才创建表达式节点或字符串。 |
| R-081 | P0 | replay 使用保存的 3 个步骤重放 Fraction 和表达式结构。 |
| R-082 | P0 | 输出表达式必须使用全部四个原始输入各一次，且计算结果精确等于目标。 |
| R-083 | P0 | 表达式必须保留必要括号，不能依赖运算符优先级产生歧义。 |
| R-084 | P1 | 提供紧凑格式和完全括号格式；默认返回完全括号格式。 |

示例：输入 `[3,3,8,8]` 必须允许返回等价于 `8/(3-8/3)` 的表达式。

## 3. 1820 组合预计算 LUT

标准模式的无序四元组数量为 `C(16,4)`（允许重复）=`1820`。

| 编号 | 优先级 | 需求 |
|---|---|---|
| R-090 | P1 | 预计算 1820 个无序输入组合的可解性和一个稳定解。 |
| R-091 | P1 | 组合 key 使用四个已排序的 4-bit/5-bit 数字编码，查表 O(1)。 |
| R-092 | P1 | LUT 必须由同一 Solver 生成并通过独立 solver 校验，禁止手工维护结果。 |
| R-093 | P1 | LUT 命中时直接返回缓存结果；若请求不同目标、运算符或输出策略，必须绕过 LUT。 |
| R-094 | P1 | LUT 启用后仍保留通用 DFS，以支持调试、验证和非标准配置。 |

建议条目包含：`solvable`、步骤编码或紧凑表达式模板、版本号、算法配置指纹。若保存表达式字符串，字符串只存在于 LUT 构建/返回阶段，不进入 DFS 热路径。

## 4. C++ / Kotlin 实现架构

### 4.1 分层

1. `Fraction`：规范化、四则运算、比较、溢出检测。
2. `InternTable`：Fraction 到 `uint16` ID。
3. `StateCodec`：canonical 排序和 `uint64` 编码。
4. `VisitedSet`：固定容量 open addressing。
5. `SolverWorkspace`：固定数组、候选缓冲区、路径和表。
6. `DfsEngine`：无分配搜索。
7. `ReplayFormatter`：命中后的表达式构造。
8. `Lut`：标准模式组合预计算和查询。
9. `Public API`：参数校验、模式选择、错误映射。

### 4.2 C++ 要求

使用 `std::array`、`std::gcd`、固定容量原始数组和 `std::optional`/结果结构。禁止在 DFS 中使用 `std::vector` 扩容、`std::string` 拼接或异常控制流。

### 4.3 Kotlin 要求

标准路径优先使用 `Long`，通过 `Math.multiplyExact` 等方式检查溢出；fallback 使用 `BigInteger`。热路径使用预分配数组、`IntArray`/`LongArray`，避免 `Fraction` data class、装箱集合和字符串生成。公共 API 可提供易用对象，但内部转为 workspace 表示。

## 5. API 契约

逻辑接口：

```text
solve(numbers: Int[4], options: SolveOptions = default): SolveResult
```

`SolveOptions` 至少包含：`target=24`、`mode=AUTO|STANDARD|FALLBACK`、`useLut=true`、`maxSolutions=1`。

`SolveResult` 至少包含：

```text
status: SOLVED | UNSOLVABLE | INVALID_INPUT | OVERFLOW | RESOURCE_LIMIT
expression: nullable String
target: Rational
usedLut: Boolean
stats: optional SearchStats
```

| 编号 | 优先级 | 验收标准 |
|---|---|---|
| R-100 | P0 | 输入数量不是 4 时返回 `INVALID_INPUT`。 |
| R-101 | P0 | 标准模式输入不在 1–13 时自动进入 fallback；强制 STANDARD 则返回明确错误。 |
| R-102 | P0 | `expression != null` 当且仅当 `status == SOLVED`。 |
| R-103 | P0 | API 多次输入相同数据和配置，返回状态与默认表达式完全一致。 |
| R-104 | P0 | API 不向调用方暴露内部 ID、visited key 或可变 workspace。 |

## 6. 错误与边界处理

| 场景 | 预期行为 |
|---|---|
| 数量不为 4 | `INVALID_INPUT` |
| 输入不是整数 | 类型层拒绝；文本适配层返回 `INVALID_INPUT` |
| 标准范围外数字 | AUTO 转 BigInteger fallback |
| 除数为 0 | 跳过候选，不视为 API 错误 |
| 中间值超出 int64 | 标准路径转 fallback；无法 fallback 则 `OVERFLOW` |
| intern ID 超限 | `RESOURCE_LIMIT` 或 fallback |
| visited 表满 | `RESOURCE_LIMIT` 或 fallback，不得无限循环 |
| 目标不可达 | `UNSOLVABLE`，expression 为 null |
| 空 workspace/并发共享 workspace | API 拒绝或提供线程隔离 workspace |

## 7. 性能、内存与 GC 指标

基准环境必须记录 CPU、OS、编译器/JVM、优化级别和运行温度。

| 指标 | P0 验收目标 |
|---|---:|
| 标准单次 solve 延迟（C++，1820 组合 p50） | ≤ 2 µs，不含进程启动 |
| 标准单次 solve 延迟（C++，p99） | ≤ 10 µs |
| 标准单次 solve 延迟（Kotlin/JVM，预热后 p50） | ≤ 20 µs |
| 标准模式 DFS 堆分配 | 0 bytes/solve（不含首次 workspace 初始化） |
| Kotlin 标准模式临时对象 | 0 个可观察对象/solve |
| 标准 workspace 常驻内存 | ≤ 64 KiB/实例，LUT 除外 |
| LUT 内存 | ≤ 256 KiB，具体布局需 benchmark 记录 |
| 1820 组合全量查询 | C++ ≤ 5 ms；Kotlin ≤ 20 ms |

指标以 1,000,000 次随机标准输入、预热后测量；不得把表达式格式化时间混入 DFS 指标。另测包含 replay 的端到端延迟。

## 8. 正确性要求

| 编号 | 优先级 | 需求 |
|---|---|---|
| R-110 | P0 | 标准路径与 BigInteger 参考实现对全部 1820 个无序组合的可解性一致。 |
| R-111 | P0 | 每条返回表达式由独立 parser/evaluator 验证，结果精确等于 24。 |
| R-112 | P0 | 表达式中每个输入值的多重集合与原输入完全一致。 |
| R-113 | P0 | 不得因整数剪枝漏掉需要分数中间值的解。 |
| R-114 | P0 | 同一配置下结果确定，不能因 hash 表布局导致不稳定。 |
| R-115 | P1 | 全部解模式下，按规范化表达式去重且数量与参考实现一致。 |

## 9. 测试策略与验收用例

### 9.1 单元测试

- Fraction：正负数、零、负分母、最大值、约分和等价比较。
- 四则运算：`1/3 + 1/6 = 1/2`、`8/(1/3)=24`、除零跳过。
- State：不同排列编码相同；不同 Fraction 编码不同；空槽处理正确。
- intern：约分前后相同值共享 ID；跨 solve 不复用 ID。
- visited：碰撞、删除不支持、满表、key=0。
- replay：全部 6 种运算代码和索引变化。

### 9.2 必测功能用例

| 用例 | 预期 |
|---|---|
| `[3,3,8,8]` | SOLVED；表达式等价于 `8/(3-8/3)`；精确值 24 |
| `[1,1,1,1]` | UNSOLVABLE |
| `[1,2,3,4]` | SOLVED；例如 `1*2*3*4` |
| `[5,5,5,1]` | SOLVED；例如 `5*(5-1/5)` |
| `[1,1,1,24]` | 标准模式转 fallback 或按配置拒绝；正确处理目标 |
| `[0,1,2,3]` | AUTO fallback；不因零输入崩溃 |
| `[13,13,13,13]` | 正确返回 SOLVED/UNSOLVABLE，不能溢出 |
| 含除零分支的输入 | 跳过除零分支，仍搜索其他分支 |
| 所有 1820 组合 | 与参考实现逐项一致 |

### 9.3 属性测试

随机生成 1–13 四元组，比较优化实现、无优化 DFS、BigInteger 参考实现：状态一致、可解性一致、返回表达式可验证。随机测试至少 100,000 组。

### 9.4 并发测试

多线程同时调用；验证无共享可变状态、无数据竞争、结果确定、workspace 复用时线程隔离。

## 10. Benchmark 方法

1. C++ 使用 Release、`-O2` 或 `-O3`；Kotlin 使用固定 JVM 版本和 JMH。
2. 每个 benchmark 预热，再执行至少 1,000,000 次；报告 p50/p95/p99、吞吐和分配。
3. 分别测：LUT 命中、LUT 关闭 DFS、无解、早命中、最深搜索、fallback、端到端含 replay。
4. 固定输入集与随机输入集各一套，避免只优化少数样例。
5. 记录 visited 探测次数、候选数、intern 命中率、DFS 节点数。
6. 对比基线：字符串表达式树 DFS、Double 实现、BigInteger 参考实现。

## 11. 分阶段实施计划

### Phase 0：契约与参考实现

交付 Fraction(BigInteger)、朴素 DFS、表达式 evaluator、API 草案和 1820 组合 golden data。

验收：参考实现通过全部功能用例，golden data 可重复生成。

### Phase 1：标准路径核心

交付 int64 Fraction、溢出检测、固定数组 DFS、local candidate dedup、3-byte steps、replay。

验收：R-010–R-015、R-040–R-053、R-070–R-084 和全部 P0 测试通过。

### Phase 2：状态与内存优化

交付 Fraction intern、uint64 canonical state、fixed open-addressing visited、workspace 复用。

验收：R-020–R-024、R-030–R-033、R-060–R-064；标准路径 0 bytes/solve。

### Phase 3：fallback 与跨语言实现

交付任意数字 BigInteger fallback、C++ 版本、Kotlin 版本、统一 API 行为。

验收：两语言与参考实现一致，边界和溢出行为符合第 6 节。

### Phase 4：1820 LUT

交付 LUT 生成器、版本化二进制/源码表、校验工具和 O(1) 查询。

验收：1820 项全量一致，查询性能和内存满足第 7 节。

### Phase 5：性能与发布

完成 benchmark、并发测试、文档、CI、静态检查和发布包。

## 12. Definition of Done

- 所有 P0 需求均有实现、单元测试和可追溯验收结果。
- C++ 与 Kotlin 公共 API 的状态、表达式合法性和错误语义一致。
- 全部 1820 个标准组合与独立 BigInteger 参考实现一致。
- `[3,3,8,8]` 等分数中间值用例通过。
- 标准路径满足零浮点、零 DFS 分配、uint64 canonical state、3-byte steps。
- fallback 能正确处理范围外数字、零和大整数，不静默溢出。
- benchmark 报告包含环境、输入集、p50/p95/p99、分配和内存数据，并达到目标或记录批准的偏差。
- CI 运行单元、属性、golden、并发和 benchmark smoke tests。
- API、配置、错误码、线程安全和限制已文档化。
- 代码通过格式化、静态分析、review；无未解释的 P0/P1 缺陷。
- 发布产物包含源码、测试、LUT 生成/校验工具和版本信息。
