package com.twentyfoursolve.core.logic

import com.make24.solver.SolveStatus
import com.make24.solver.TwentyFourSolver
import kotlin.math.abs

/**
 * 超难发牌：数字仍在 1–13，但只保留窄解。
 *
 * 一张牌入选，当且仅当：
 * - 四个数都在 1–13，并且有解；
 * - 不存在只用加法和乘法的解；
 * - 不存在「两张牌直接得到 24，另外两张相同、可以抵消」的拆法；
 * - 不同走法（按中间结果的取值序列去重）不超过两条。
 */
fun isExtremeHand(nums: List<Int>): Boolean {
    if (nums.size != 4 || nums.any { it !in 1..13 }) return false
    if (TwentyFourSolver.solve(nums.toIntArray()).status != SolveStatus.SOLVED) return false
    if (obviousSplit(nums)) return false
    return solutionWays(nums) <= 2
}

/**
 * 从完全括号的解法里取出一步「两张原始牌」的合并，供超难计时提示使用。
 * 运算符显示为 + − × ÷。解析失败时退回原表达式。
 */
fun firstSolutionStep(fullyParenthesized: String): String {
    val expr = runCatching { ExprParser(fullyParenthesized).parse() }.getOrNull() ?: return fullyParenthesized
    val step = findLeafStep(expr) ?: return fullyParenthesized
    return "${displayLeaf(step.left)} ${opGlyph(step.op)} ${displayLeaf(step.right)}"
}

private fun obviousSplit(nums: List<Int>): Boolean {
    if (plusMulOnly(nums)) return true
    val xs = nums
    for (i in 0 until 4) {
        for (j in i + 1 until 4) {
            val rest = (0 until 4).filter { it != i && it != j }.map { xs[it] }
            if (rest[0] != rest[1]) continue
            val a = xs[i]
            val b = xs[j]
            val makes24 = a * b == 24 || a + b == 24 ||
                (b != 0 && a % b == 0 && a / b == 24) ||
                (a != 0 && b % a == 0 && b / a == 24)
            if (makes24) return true
        }
    }
    return false
}

private fun plusMulOnly(nums: List<Int>): Boolean {
    fun rec(cur: List<Int>): Boolean {
        if (cur.size == 1) return cur[0] == 24
        for (i in cur.indices) {
            for (j in i + 1 until cur.size) {
                val rest = cur.filterIndexed { idx, _ -> idx != i && idx != j }
                val a = cur[i]
                val b = cur[j]
                if (rec(rest + (a + b)) || rec(rest + (a * b))) return true
            }
        }
        return false
    }
    return rec(nums)
}

/** 不同走法数：每一步留下的分数取值序列，相同序列只算一条。 */
private fun solutionWays(nums: List<Int>): Int {
    val found = HashSet<List<Frac>>()
    fun rec(cur: List<Frac>, trace: List<Frac>) {
        if (cur.size == 1) {
            if (cur[0].n == 24L * cur[0].d) found.add(trace)
            return
        }
        for (i in cur.indices) {
            for (j in i + 1 until cur.size) {
                val rest = cur.filterIndexed { idx, _ -> idx != i && idx != j }
                val seen = HashSet<Frac>()
                for (v in candidates(cur[i], cur[j])) {
                    if (!seen.add(v)) continue
                    rec(rest + v, trace + v)
                }
            }
        }
    }
    rec(nums.map { frac(it.toLong(), 1L) }, emptyList())
    return found.size
}

private data class Frac(val n: Long, val d: Long)

private fun frac(numerator: Long, denominator: Long): Frac {
    var n = numerator
    var d = denominator
    if (d < 0) {
        n = -n
        d = -d
    }
    val g = gcdLong(abs(n), d)
    return if (g > 1L) Frac(n / g, d / g) else Frac(n, d)
}

private fun candidates(a: Frac, b: Frac): List<Frac> {
    val an = a.n
    val ad = a.d
    val bn = b.n
    val bd = b.d
    val dab = ad * bd
    val raw = ArrayList<Frac>(6)
    raw.add(frac(an * bd + bn * ad, dab))
    raw.add(frac(an * bd - bn * ad, dab))
    raw.add(frac(bn * ad - an * bd, dab))
    raw.add(frac(an * bn, dab))
    if (bn != 0L) raw.add(frac(an * bd, ad * bn))
    if (an != 0L) raw.add(frac(bn * ad, an * bd))
    return raw
}

private fun gcdLong(a0: Long, b0: Long): Long {
    var a = a0
    var b = b0
    while (b != 0L) {
        val t = a % b
        a = b
        b = t
    }
    return if (a < 0) -a else a
}

private sealed interface HExpr {
    data class Leaf(val text: String) : HExpr
    data class Node(val op: String, val left: HExpr, val right: HExpr) : HExpr
}

private class ExprParser(private val s: String) {
    private var i = 0

    fun parse(): HExpr {
        val expr = parseExpr()
        check(i == s.length) { "trailing input" }
        return expr
    }

    private fun parseExpr(): HExpr {
        if (s[i] == '(') {
            if (isFractionLeaf()) {
                val start = i
                i = s.indexOf(')', i) + 1
                return HExpr.Leaf(s.substring(start, i))
            }
            i++
            val left = parseExpr()
            expect(' ')
            val op = s[i].toString()
            i++
            expect(' ')
            val right = parseExpr()
            expect(')')
            return HExpr.Node(op, left, right)
        }
        val start = i
        if (s[i] == '-') i++
        check(i < s.length && s[i].isDigit())
        while (i < s.length && s[i].isDigit()) i++
        return HExpr.Leaf(s.substring(start, i))
    }

    /** `(n/d)` 没有空格，和运算节点 `(8 / 3)` 区分开。 */
    private fun isFractionLeaf(): Boolean {
        var j = i + 1
        if (j < s.length && s[j] == '-') j++
        if (j >= s.length || !s[j].isDigit()) return false
        while (j < s.length && s[j].isDigit()) j++
        if (j >= s.length || s[j] != '/') return false
        j++
        if (j < s.length && s[j] == '-') j++
        if (j >= s.length || !s[j].isDigit()) return false
        while (j < s.length && s[j].isDigit()) j++
        return j < s.length && s[j] == ')'
    }

    private fun expect(ch: Char) {
        check(i < s.length && s[i] == ch) { "expected '$ch'" }
        i++
    }
}

private fun findLeafStep(expr: HExpr): HExpr.Node? = when (expr) {
    is HExpr.Leaf -> null
    is HExpr.Node ->
        if (expr.left is HExpr.Leaf && expr.right is HExpr.Leaf) expr
        else findLeafStep(expr.left) ?: findLeafStep(expr.right)
}

private fun displayLeaf(expr: HExpr): String {
    val text = (expr as HExpr.Leaf).text
    return if (text.startsWith("(") && text.endsWith(")")) text.substring(1, text.length - 1) else text
}

private fun opGlyph(op: String): String = when (op) {
    "+" -> "+"
    "-" -> "−"
    "*" -> "×"
    "/" -> "÷"
    else -> op
}
