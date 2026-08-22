package com.twentyfoursolve.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.twentyfoursolve.data.local.AppDatabase
import com.twentyfoursolve.data.local.dao.GameRecordDao
import com.twentyfoursolve.data.repository.GameRepository
import com.twentyfoursolve.data.repository.LocalGameRepository
import com.twentyfoursolve.data.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by androidx.datastore.preferences.preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "solve24_database"
        ).build()
    }

    @Provides
    fun provideGameRecordDao(database: AppDatabase): GameRecordDao {
        return database.gameRecordDao()
    }

    @Provides
    @Singleton
    fun provideGameRepository(dao: GameRecordDao): GameRepository {
        return LocalGameRepository(dao)
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(dataStore: DataStore<Preferences>): SettingsRepository {
        return SettingsRepository(dataStore)
    }
}