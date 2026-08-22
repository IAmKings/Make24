package com.twentyfoursolve.data.repository

import com.twentyfoursolve.core.model.Difficulty
import com.twentyfoursolve.core.model.Language
import com.twentyfoursolve.core.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class SettingsRepositoryTest {

    private lateinit var fakeDataStore: FakeSettingsDataStore
    private lateinit var repository: FakeSettingsRepository

    @Before
    fun setup() {
        fakeDataStore = FakeSettingsDataStore()
        repository = FakeSettingsRepository(fakeDataStore)
    }

    @Test
    fun `default settings are correct`() = runTest {
        val settings = repository.settings.first()
        assertEquals(true, settings.soundEnabled)
        assertEquals(Difficulty.MEDIUM, settings.difficultyPreference)
        assertEquals(Language.ZH, settings.language)
    }

    @Test
    fun `updateSoundEnabled persists value`() = runTest {
        repository.updateSoundEnabled(false)
        val settings = repository.settings.first()
        assertEquals(false, settings.soundEnabled)
    }

    @Test
    fun `updateDifficulty persists value`() = runTest {
        repository.updateDifficulty(Difficulty.HARD)
        val settings = repository.settings.first()
        assertEquals(Difficulty.HARD, settings.difficultyPreference)
    }

    @Test
    fun `updateLanguage persists value`() = runTest {
        repository.updateLanguage(Language.EN)
        val settings = repository.settings.first()
        assertEquals(Language.EN, settings.language)
    }

    @Test
    fun `multiple updates persist all values`() = runTest {
        repository.updateSoundEnabled(false)
        repository.updateDifficulty(Difficulty.EASY)
        repository.updateLanguage(Language.EN)

        val settings = repository.settings.first()
        assertEquals(false, settings.soundEnabled)
        assertEquals(Difficulty.EASY, settings.difficultyPreference)
        assertEquals(Language.EN, settings.language)
    }
}

class FakeSettingsDataStore {
    private val _settings = MutableStateFlow(UserSettings())
    val settings: MutableStateFlow<UserSettings> = _settings

    fun update(transform: (UserSettings) -> UserSettings) {
        _settings.update(transform)
    }
}

class FakeSettingsRepository(private val dataStore: FakeSettingsDataStore) {
    val settings = dataStore.settings

    suspend fun updateSoundEnabled(enabled: Boolean) {
        dataStore.update { it.copy(soundEnabled = enabled) }
    }

    suspend fun updateDifficulty(difficulty: Difficulty) {
        dataStore.update { it.copy(difficultyPreference = difficulty) }
    }

    suspend fun updateLanguage(language: Language) {
        dataStore.update { it.copy(language = language) }
    }
}
