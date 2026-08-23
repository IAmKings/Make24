package com.twentyfoursolve.core.logic

import com.twentyfoursolve.core.model.Card
import com.twentyfoursolve.core.model.Difficulty
import com.twentyfoursolve.core.model.Suit
import kotlin.math.abs
import kotlin.random.Random

/**
 * Core game logic ported from the web demo's game-logic.ts
 */

private const val EPSILON = 0.001

/**
 * Evaluate an arithmetic operation on two numbers.
 * Returns Double.NaN for division by zero.
 */
fun evaluateEquation(a: Double, operator: String, b: Double): Double {
    return when (operator) {
        "+" -> a + b
        "-" -> a - b
        "*" -> a * b
        "/" -> if (b != 0.0) a / b else Double.NaN
        else -> Double.NaN
    }
}

/**
 * Check if a set of 4 numbers can be combined to make 24.
 * Uses exhaustive recursive search, matching the web demo's solve24.
 */
fun solve24(numbers: List<Double>): Boolean {
    if (numbers.size == 1) {
        return abs(numbers[0] - 24.0) < EPSILON
    }

    for (i in numbers.indices) {
        for (j in numbers.indices) {
            if (i == j) continue

            val remaining = numbers.filterIndexed { idx, _ -> idx != i && idx != j }
            val a = numbers[i]
            val b = numbers[j]

            val operators = listOf("+", "-", "*", "/")
            for (op in operators) {
                if (op == "/" && b == 0.0) continue
                val result = evaluateEquation(a, op, b)
                if (!result.isNaN() && solve24(remaining + result)) {
                    return true
                }
            }
        }
    }
    return false
}

/**
 * Generate a valid puzzle that can be solved to 24.
 * @param difficulty Controls the range of random numbers（统一难度阶梯）
 * @return List of 4 integers guaranteed to be solvable for 24
 */
fun generatePuzzle(difficulty: Difficulty): List<Int> {
    val maxRange = difficulty.range.last
    var attempts = 0
    while (attempts < 10000) {
        val nums = List(4) { Random.nextInt(1, maxRange + 1) }
        if (solve24(nums.map { it.toDouble() })) {
            return nums
        }
        attempts++
    }
    // Fallback: return a known solvable puzzle (1, 2, 3, 4)
    return listOf(1, 2, 3, 4)
}

/**
 * Available suits for random selection.
 */
private val SUITS = Suit.entries

/**
 * Get display label for a card value (A, J, Q, K mapping).
 */
fun getCardLabel(value: Double): String {
    val intValue = value.toInt()
    return when {
        value != value.toInt().toDouble() -> {
            // Fraction/decimal: show up to 2 decimal places, strip trailing zeros
            val formatted = String.format("%.2f", value).trimEnd('0').trimEnd('.')
            formatted
        }
        intValue == 1 -> "A"
        intValue == 11 -> "J"
        intValue == 12 -> "Q"
        intValue == 13 -> "K"
        else -> intValue.toString()
    }
}

/**
 * Create initial card list from puzzle numbers.
 */
fun createCards(numbers: List<Int>): List<Card> {
    return numbers.mapIndexed { idx, value ->
        Card(
            id = "card-$idx-${System.currentTimeMillis()}",
            value = value.toDouble(),
            label = getCardLabel(value.toDouble()),
            suit = SUITS[Random.nextInt(SUITS.size)]
        )
    }
}

/**
 * Check if a result value equals 24 (with epsilon tolerance).
 */
fun isTwentyFour(value: Double): Boolean {
    return !value.isNaN() && abs(value - 24.0) < EPSILON
}