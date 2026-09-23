package com.twentyfoursolve.data.repository

import com.twentyfoursolve.data.local.dao.DailyResultDao
import com.twentyfoursolve.data.local.entity.DailyResultEntity
import javax.inject.Inject
import javax.inject.Singleton

data class DailyResult(
    val epochDay: Long,
    val score: Int,
    val timeTaken: Int,
)

interface DailyRepository {
    suspend fun get(epochDay: Long): DailyResult?
    /** 更高分写入；分数相同则只在用时更短时写入。返回这次是否成为当天记录。 */
    suspend fun saveIfBetter(epochDay: Long, score: Int, timeTaken: Int): Boolean
    /**
     * 从 [epochDay] 往前数连续有成绩的天数。
     * 当天还没有成绩时，从昨天开始数，这样未完成的今天不会把连续天数清掉。
     */
    suspend fun streakAsOf(epochDay: Long): Int
}

@Singleton
class LocalDailyRepository @Inject constructor(
    private val dao: DailyResultDao,
) : DailyRepository {

    override suspend fun get(epochDay: Long): DailyResult? {
        return dao.get(epochDay)?.toDomain()
    }

    override suspend fun saveIfBetter(epochDay: Long, score: Int, timeTaken: Int): Boolean {
        val existing = dao.get(epochDay)
        val better = existing == null ||
            score > existing.score ||
            (score == existing.score && timeTaken < existing.timeTaken)
        if (!better) return false
        dao.upsert(DailyResultEntity(epochDay = epochDay, score = score, timeTaken = timeTaken))
        return true
    }

    override suspend fun streakAsOf(epochDay: Long): Int {
        val days = dao.epochDaysAtOrBefore(epochDay).toSet()
        var cursor = if (epochDay in days) epochDay else epochDay - 1
        var count = 0
        while (cursor in days) {
            count++
            cursor--
        }
        return count
    }

    private fun DailyResultEntity.toDomain() = DailyResult(epochDay, score, timeTaken)
}
