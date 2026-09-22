package com.twentyfoursolve.core.logic

import com.twentyfoursolve.core.model.Difficulty
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameLogicTest {

    // ─── evaluateEquation ────────────────────────────────────────────

    @Test
    fun `evaluateEquation addition`() {
        assertEquals(5.0, evaluateEquation(2.0, "+", 3.0))
    }

    @Test
    fun `evaluateEquation subtraction`() {
        assertEquals(1.0, evaluateEquation(4.0, "-", 3.0))
    }

    @Test
    fun `evaluateEquation multiplication`() {
        assertEquals(12.0, evaluateEquation(3.0, "*", 4.0))
    }

    @Test
    fun `evaluateEquation division`() {
        assertEquals(2.5, evaluateEquation(5.0, "/", 2.0))
    }

    @Test
    fun `evaluateEquation division by zero returns NaN`() {
        assertTrue(evaluateEquation(5.0, "/", 0.0).isNaN())
    }

    @Test
    fun `evaluateEquation unknown operator returns NaN`() {
        assertTrue(evaluateEquation(1.0, "%", 2.0).isNaN())
    }

    // ─── solve24 ─────────────────────────────────────────────────────

    @Test
    fun `solve24 - classic 1 2 3 4 is solvable`() {
        assertTrue(solve24(listOf(1.0, 2.0, 3.0, 4.0)))
    }

    @Test
    fun `solve24 - 8 8 3 3 is solvable`() {
        assertTrue(solve24(listOf(8.0, 8.0, 3.0, 3.0)))
    }

    @Test
    fun `solve24 - 5 5 5 1 is solvable`() {
        assertTrue(solve24(listOf(5.0, 5.0, 5.0, 1.0)))
    }

    @Test
    fun `solve24 - 6 6 6 6 is solvable`() {
        assertTrue(solve24(listOf(6.0, 6.0, 6.0, 6.0)))
    }

    @Test
    fun `solve24 - 1 1 1 1 is not solvable`() {
        assertFalse(solve24(listOf(1.0, 1.0, 1.0, 1.0)))
    }

    @Test
    fun `solve24 - 10 10 10 10 is not solvable`() {
        assertFalse(solve24(listOf(10.0, 10.0, 10.0, 10.0)))
    }

    // ─── isTwentyFour ────────────────────────────────────────────────

    @Test
    fun `isTwentyFour - exact 24`() {
        assertTrue(isTwentyFour(24.0))
    }

    @Test
    fun `isTwentyFour - 23 point 999 is close enough`() {
        assertTrue(isTwentyFour(23.9995))
    }

    @Test
    fun `isTwentyFour - 24 point 001 is close enough`() {
        assertTrue(isTwentyFour(24.0005))
    }

    @Test
    fun `isTwentyFour - 23 is not 24`() {
        assertFalse(isTwentyFour(23.0))
    }

    @Test
    fun `isTwentyFour - NaN is not 24`() {
        assertFalse(isTwentyFour(Double.NaN))
    }

    // ─── generatePuzzle ──────────────────────────────────────────────

    @Test
    fun `generatePuzzle returns 4 numbers`() {
        val puzzle = generatePuzzle(Difficulty.MEDIUM, allowUnsolvable = false)
        assertEquals(4, puzzle.size)
    }

    @Test
    fun `generatePuzzle numbers are positive`() {
        val puzzle = generatePuzzle(Difficulty.EASY, allowUnsolvable = false)
        assertTrue(puzzle.all { it > 0 })
    }

    @Test
    fun `generatePuzzle easy range is within 1-10`() {
        val puzzle = generatePuzzle(Difficulty.EASY, allowUnsolvable = false)
        assertTrue(puzzle.all { it in 1..10 })
    }

    @Test
    fun `generatePuzzle medium range is within 1-13`() {
        val puzzle = generatePuzzle(Difficulty.MEDIUM, allowUnsolvable = false)
        assertTrue(puzzle.all { it in 1..13 })
    }

    @Test
    fun `generatePuzzle hard range is within 1-20`() {
        val puzzle = generatePuzzle(Difficulty.HARD, allowUnsolvable = false)
        assertTrue(puzzle.all { it in 1..20 })
    }

    @Test
    fun `generatePuzzle result is solvable for 24`() {
        val puzzle = generatePuzzle(Difficulty.MEDIUM, allowUnsolvable = false)
        assertTrue(solve24(puzzle.map { it.toDouble() }))
    }

    @Test
    fun `extreme hand keeps narrow solvable deals and rejects obvious ones`() {
        assertTrue(isExtremeHand(listOf(3, 3, 8, 8)))
        assertTrue(isExtremeHand(listOf(1, 5, 5, 5)))
        assertTrue(isExtremeHand(listOf(3, 3, 3, 3)))
        assertFalse(isExtremeHand(listOf(1, 1, 1, 1)))
        assertFalse(isExtremeHand(listOf(1, 2, 3, 4)))
        assertFalse(isExtremeHand(listOf(8, 3, 1, 1)))
        assertFalse(isExtremeHand(listOf(6, 4, 2, 2)))
    }

    @Test
    fun `generatePuzzle extreme stays inside 1-13 and passes the narrow filter`() {
        repeat(8) {
            val puzzle = generatePuzzle(Difficulty.EXTREME, allowUnsolvable = true)
            assertEquals(4, puzzle.size)
            assertTrue(puzzle.all { it in 1..13 })
            assertTrue(isExtremeHand(puzzle))
        }
    }

    @Test
    fun `firstSolutionStep names one leaf merge`() {
        assertEquals("8 ÷ 3", firstSolutionStep("(8 / (3 - (8 / 3)))"))
        assertEquals("3 × 3", firstSolutionStep("(((3 * 3) * 3) - 3)"))
        assertEquals("8/3 × 1", firstSolutionStep("((8/3) * 1)"))
    }

    // ─── getCardLabel ────────────────────────────────────────────────

    @Test
    fun `getCardLabel - 1 maps to A`() {
        assertEquals("A", getCardLabel(1.0))
    }

    @Test
    fun `getCardLabel - 11 maps to J`() {
        assertEquals("J", getCardLabel(11.0))
    }

    @Test
    fun `getCardLabel - 12 maps to Q`() {
        assertEquals("Q", getCardLabel(12.0))
    }

    @Test
    fun `getCardLabel - 13 maps to K`() {
        assertEquals("K", getCardLabel(13.0))
    }

    @Test
    fun `getCardLabel - 7 maps to 7`() {
        assertEquals("7", getCardLabel(7.0))
    }

    @Test
    fun `getCardLabel - decimal formats correctly`() {
        assertEquals("2.5", getCardLabel(2.5))
    }

    // ─── createCards ─────────────────────────────────────────────────

    @Test
    fun `createCards returns correct count`() {
        val cards = createCards(listOf(1, 2, 3, 4))
        assertEquals(4, cards.size)
    }

    @Test
    fun `createCards preserves values`() {
        val cards = createCards(listOf(3, 7, 5, 2))
        assertEquals(3.0, cards[0].value)
        assertEquals(7.0, cards[1].value)
        assertEquals(5.0, cards[2].value)
        assertEquals(2.0, cards[3].value)
    }

    @Test
    fun `createCards assigns labels`() {
        val cards = createCards(listOf(1, 11, 12, 13))
        assertEquals("A", cards[0].label)
        assertEquals("J", cards[1].label)
        assertEquals("Q", cards[2].label)
        assertEquals("K", cards[3].label)
    }
}
