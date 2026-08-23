package com.twentyfoursolve.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twentyfoursolve.core.model.Difficulty
import com.twentyfoursolve.core.model.Language
import com.twentyfoursolve.core.model.UserSettings
import com.twentyfoursolve.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    fun updateSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSoundEnabled(enabled) }
    }

    fun updateDifficulty(difficulty: Difficulty) {
        viewModelScope.launch { settingsRepository.updateDifficulty(difficulty) }
    }

    fun updateLanguage(language: Language) {
        viewModelScope.launch { settingsRepository.updateLanguage(language) }
    }

    fun updateAllowUnsolvable(allow: Boolean) {
        viewModelScope.launch { settingsRepository.updateAllowUnsolvable(allow) }
    }
}
