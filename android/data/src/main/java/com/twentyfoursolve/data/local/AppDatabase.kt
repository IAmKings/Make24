package com.twentyfoursolve.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.twentyfoursolve.data.local.dao.DailyResultDao
import com.twentyfoursolve.data.local.dao.GameRecordDao
import com.twentyfoursolve.data.local.entity.DailyResultEntity
import com.twentyfoursolve.data.local.entity.GameRecordEntity

@Database(
    entities = [GameRecordEntity::class, DailyResultEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameRecordDao(): GameRecordDao
    abstract fun dailyResultDao(): DailyResultDao
}