package com.twentyfoursolve.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for a game record.
 * Stored in the local SQLite database for stats aggregation.
 */
@Entity(tableName = "game_records")
data class GameRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val isSuccess: Boolean,
    val score: Int,
    val timeTaken: Int, // seconds
    val difficulty: String, // "easy", "medium", "hard", "extreme"
    val mode: String // "timed" or "practice"
)