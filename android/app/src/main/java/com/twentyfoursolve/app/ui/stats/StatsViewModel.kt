package com.twentyfoursolve.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twentyfoursolve.core.model.Difficulty
import com.twentyfoursolve.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatsUiModel(
    val totalGames: Int = 0,
    val winRate: Float = 0f,
    val avgTime: Double = 0.0,
    val currentStreak: Int = 0,
    val topSolves: List<TopSolve> = emptyList(),
    val dailyWins: List<DailyWin> = emptyList(),
    val byDifficulty: List<DifficultyStat> = emptyList(),
) {
    data class DifficultyStat(
        val difficulty: Difficulty,
        val games: Int,
        val winRate: Float?,
        val avgTimeSeconds: Double?,
    )
    data class TopSolve(
        val rank: Int,
        val score: Int,
        val timeTaken: Int,
        val date: Long,
        val isBest: Boolean = false
    )

    data class DailyWin(
        val dayIndex: Int,
        val wins: Int
    )
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _stats = MutableStateFlow(StatsUiModel())
    val stats: StateFlow<StatsUiModel> = _stats.asStateFlow()

    init {
        loadStats()
    }

    /** 每次进入统计页时调用，刷新活跃度等数据（ViewModel 被 tab 复用，init 不会重跑）。 */
    fun refresh() {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            try {
                val winRate = gameRepository.getWinRate()
                val totalGames = gameRepository.getTotalTimedGames()
                val avgTime = gameRepository.getAverageTime() ?: 0.0
                val streak = gameRepository.getCurrentStreak()
                val topSolves = gameRepository.getTopSolves(3)
                val byDifficulty = gameRepository.getDifficultyStats()

                val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
                val dailyActivity = gameRepository.getDailyActivity(sevenDaysAgo)

                _stats.value = StatsUiModel(
                    totalGames = totalGames,
                    winRate = winRate,
                    avgTime = avgTime,
                    currentStreak = streak,
                    topSolves = topSolves.mapIndexed { idx, record ->
                        StatsUiModel.TopSolve(
                            rank = idx + 1,
                            score = record.score,
                            timeTaken = record.timeTaken,
                            date = record.date,
                            isBest = idx == 0
                        )
                    },
                    dailyWins = dailyActivity.mapIndexed { idx, day ->
                        StatsUiModel.DailyWin(dayIndex = idx, wins = day.winCount)
                    }.takeIf { it.isNotEmpty() } ?: (0..6).map {
                        StatsUiModel.DailyWin(dayIndex = it, wins = 0)
                    },
                    byDifficulty = byDifficulty.map { row ->
                        StatsUiModel.DifficultyStat(
                            difficulty = row.difficulty,
                            games = row.games,
                            winRate = if (row.games == 0) null else row.wins.toFloat() / row.games,
                            avgTimeSeconds = row.avgTimeSeconds,
                        )
                    },
                )
            } catch (_: Exception) {
                // Keep default empty state
            }
        }
    }
}