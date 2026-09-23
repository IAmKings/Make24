package com.twentyfoursolve.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.twentyfoursolve.data.local.entity.DailyResultEntity

@Dao
interface DailyResultDao {
    @Query("SELECT * FROM daily_results WHERE epochDay = :epochDay")
    suspend fun get(epochDay: Long): DailyResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(result: DailyResultEntity)

    @Query("SELECT epochDay FROM daily_results WHERE epochDay <= :epochDay ORDER BY epochDay DESC")
    suspend fun epochDaysAtOrBefore(epochDay: Long): List<Long>
}
