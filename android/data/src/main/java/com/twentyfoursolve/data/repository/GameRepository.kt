package com.twentyfoursolve.data.repository

import com.twentyfoursolve.core.model.Difficulty
import com.twentyfoursolve.core.model.GameRecord
import com.twentyfoursolve.data.local.dao.GameRecordDao
import com.twentyfoursolve.data.local.entity.GameRecordEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository interface for game records.
 * Designed to support Local/Remote dual implementation (MVP = Local only).
 */
interface GameRepository {
    suspend fun saveGameRecord(record: GameRecord): Long
    suspend fun getAllRecords(): List<GameRecord>
    suspend fun getWinRate(): Float
    suspend fun getTotalTimedGames(): Int
    suspend fun getAverageTime(): Double?
    suspend fun getCurrentStreak(): Int
    suspend fun getTopSolves(limit: Int): List<GameRecord>
    suspend fun getDailyActivity(sinceTimestamp: Long): List<DailyActivityResult>
}

data class DailyActivityResult(
    val date: Long,
    val winCount: Int
)

/**
 * Local implementation using Room database.
 */
@Singleton
class LocalGameRepository @Inject constructor(
    private val dao: GameRecordDao
) : GameRepository {

    override suspend fun saveGameRecord(record: GameRecord): Long {
        return dao.insert(
            GameRecordEntity(
                date = record.date,
                isSuccess = record.isSuccess,
                score = record.score,
                timeTaken = record.timeTaken,
                difficulty = record.difficulty,
                mode = record.mode
            )
        )
    }

    override suspend fun getAllRecords(): List<GameRecord> {
        return dao.getAllRecords().map { it.toDomain() }
    }

    override suspend fun getWinRate(): Float {
        val total = dao.getTimedGameCount()
        if (total == 0) return 0f
        val wins = dao.getWinCount()
        return wins.toFloat() / total.toFloat()
    }

    override suspend fun getTotalTimedGames(): Int {
        return dao.getTimedGameCount()
    }

    override suspend fun getAverageTime(): Double? {
        return dao.getAverageTime()
    }

    override suspend fun getCurrentStreak(): Int {
        return dao.getCurrentStreak() ?: 0
    }

    override suspend fun getTopSolves(limit: Int): List<GameRecord> {
        return dao.getTopSolves(limit).map { it.toDomain() }
    }

    override suspend fun getDailyActivity(sinceTimestamp: Long): List<DailyActivityResult> {
        return dao.getDailyActivity(sinceTimestamp).map {
            DailyActivityResult(date = it.date, winCount = it.win_count)
        }
    }

    private fun GameRecordEntity.toDomain() = GameRecord(
        id = id,
        date = date,
        isSuccess = isSuccess,
        score = score,
        timeTaken = timeTaken,
        difficulty = difficulty,
        mode = mode
    )
}