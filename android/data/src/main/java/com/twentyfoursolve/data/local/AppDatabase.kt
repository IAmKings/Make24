package com.twentyfoursolve.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.twentyfoursolve.data.local.dao.GameRecordDao
import com.twentyfoursolve.data.local.entity.GameRecordEntity

@Database(
    entities = [GameRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameRecordDao(): GameRecordDao
}