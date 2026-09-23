package com.twentyfoursolve.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.twentyfoursolve.data.local.entity.GameRecordEntity

/**
 * Data access object for game records.
 */
@Dao
interface GameRecordDao {

    @Insert
    suspend fun insert(record: GameRecordEntity): Long

    @Query("SELECT * FROM game_records ORDER BY date DESC")
    suspend fun getAllRecords(): List<GameRecordEntity>

    @Query("SELECT COUNT(*) FROM game_records WHERE mode = 'timed'")
    suspend fun getTimedGameCount(): Int

    @Query("SELECT COUNT(*) FROM game_records WHERE isSuccess = 1 AND mode = 'timed'")
    suspend fun getWinCount(): Int

    @Query("SELECT AVG(CAST(timeTaken AS REAL)) FROM game_records WHERE isSuccess = 1 AND mode = 'timed'")
    suspend fun getAverageTime(): Double?

    @Query("SELECT * FROM game_records WHERE isSuccess = 1 AND mode = 'timed' ORDER BY score DESC LIMIT :limit")
    suspend fun getTopSolves(limit: Int): List<GameRecordEntity>

    @Query("""
        SELECT COUNT(*) FROM game_records 
        WHERE isSuccess = 1 AND mode = 'timed' AND date >= :sinceTimestamp
    """)
    suspend fun getRecentWinCount(sinceTimestamp: Long): Int

    @Query("""
        SELECT COUNT(*) FROM game_records 
        WHERE isSuccess = 1 AND mode = 'timed'
        AND date >= (SELECT MAX(date) FROM game_records WHERE isSuccess = 0 AND mode = 'timed')
    """)
    suspend fun getCurrentStreak(): Int?

    @Query("""
        SELECT date, SUM(CASE WHEN isSuccess = 1 THEN 1 ELSE 0 END) AS win_count
        FROM game_records 
        WHERE date >= :sinceTimestamp AND mode = 'timed'
        GROUP BY date / 86400000
        ORDER BY date ASC
    """)
    suspend fun getDailyActivity(sinceTimestamp: Long): List<DailyActivity>

    @Query("""
        SELECT difficulty AS difficulty,
               COUNT(*) AS games,
               SUM(CASE WHEN isSuccess = 1 THEN 1 ELSE 0 END) AS wins,
               AVG(CASE WHEN isSuccess = 1 THEN CAST(timeTaken AS REAL) END) AS avgTime
        FROM game_records
        WHERE mode = 'timed'
        GROUP BY difficulty
    """)
    suspend fun getTimedStatsByDifficulty(): List<DifficultyStatRow>

    /**
     * Daily activity summary for the activity chart.
     */
    data class DailyActivity(
        val date: Long,
        val win_count: Int
    )

    data class DifficultyStatRow(
        val difficulty: String,
        val games: Int,
        val wins: Int,
        val avgTime: Double?,
    )
}