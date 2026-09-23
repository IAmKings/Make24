package com.twentyfoursolve.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twentyfoursolve.data.repository.DailyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DailyHomeState(
    val completed: Boolean = false,
    val streak: Int = 0,
    val bestScore: Int? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dailyRepository: DailyRepository,
) : ViewModel() {

    private val _daily = MutableStateFlow(DailyHomeState())
    val daily: StateFlow<DailyHomeState> = _daily.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val today = LocalDate.now().toEpochDay()
            val result = dailyRepository.get(today)
            _daily.value = DailyHomeState(
                completed = result != null,
                streak = dailyRepository.streakAsOf(today),
                bestScore = result?.score,
            )
        }
    }
}
