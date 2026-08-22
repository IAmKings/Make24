package com.twentyfoursolve.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.twentyfoursolve.core.model.Difficulty
import com.twentyfoursolve.core.model.Language
import com.twentyfoursolve.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for user settings backed by DataStore.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val KEY_SOUND_ENABLED = stringPreferencesKey("sound_enabled")
        val KEY_DIFFICULTY = stringPreferencesKey("difficulty")
        val KEY_LANGUAGE = stringPreferencesKey("language")
    }

    val settings: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            soundEnabled = prefs[KEY_SOUND_ENABLED]?.toBoolean() ?: true,
            difficultyPreference = Difficulty.fromName(prefs[KEY_DIFFICULTY] ?: "medium"),
            language = Language.fromCode(prefs[KEY_LANGUAGE] ?: "zh")
        )
    }

    suspend fun updateSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_SOUND_ENABLED] = enabled.toString() }
    }

    suspend fun updateDifficulty(difficulty: Difficulty) {
        dataStore.edit { it[KEY_DIFFICULTY] = difficulty.name.lowercase() }
    }

    suspend fun updateLanguage(language: Language) {
        dataStore.edit { it[KEY_LANGUAGE] = language.code }
    }
}