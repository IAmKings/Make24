package com.twentyfoursolve.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS daily_results (
                epochDay INTEGER NOT NULL PRIMARY KEY,
                score INTEGER NOT NULL,
                timeTaken INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}
