package com.twentyfoursolve.core.logic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DailyPuzzleTest {

    @Test
    fun `same day always deals the same solvable hand`() {
        val first = dailyPuzzle(20_000L)
        assertEquals(first, dailyPuzzle(20_000L))
        assertEquals(4, first.size)
        assertTrue(first.all { it in 1..13 })
        assertTrue(solve24(first.map { it.toDouble() }))
        // 钉死种子，避免以后改随机顺序让同一天的题目变掉。
        assertEquals(listOf(5, 13, 1, 9), first)
    }
}
