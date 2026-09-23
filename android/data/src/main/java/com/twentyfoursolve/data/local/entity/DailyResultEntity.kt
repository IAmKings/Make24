package com.twentyfoursolve.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_results")
data class DailyResultEntity(
    @PrimaryKey val epochDay: Long,
    val score: Int,
    val timeTaken: Int,
)
