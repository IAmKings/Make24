package com.twentyfoursolve.core.logic

import com.make24.solver.SolveStatus
import com.make24.solver.TwentyFourSolver
import kotlin.random.Random

/**
 * 用本地日期的 epoch day 做种子，抽出当天唯一的一组有解牌。
 * 随机顺序固定，改动这里会让所有人的每日题目一起变。
 */
fun dailyPuzzle(epochDay: Long): List<Int> {
    val random = Random(epochDay)
    while (true) {
        val nums = List(4) { random.nextInt(1, 14) }
        if (TwentyFourSolver.solve(nums.toIntArray()).status == SolveStatus.SOLVED) {
            return nums
        }
    }
}
