package com.twentyfoursolve.app.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twentyfoursolve.app.audio.SoundManager
import com.twentyfoursolve.app.audio.SoundType
import com.twentyfoursolve.core.logic.createCards
import com.twentyfoursolve.core.logic.evaluateEquation
import com.twentyfoursolve.core.logic.generatePuzzle
import com.twentyfoursolve.core.logic.isTwentyFour
import com.twentyfoursolve.core.model.Card
import com.twentyfoursolve.core.model.Difficulty
import com.twentyfoursolve.core.model.GameRecord
import com.twentyfoursolve.core.model.GameState
import com.twentyfoursolve.core.model.Operator
import com.twentyfoursolve.data.repository.GameRepository
import com.twentyfoursolve.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MVI ViewModel for the Game screen.
 * Handles all game logic: card selection, operator application, scoring, timer, undo.
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val settingsRepository: SettingsRepository,
    private val soundManager: SoundManager
) : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var totalGameTime = 120 // seconds
    private var currentIsPractice = false

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                soundManager.enabled = settings.soundEnabled
            }
        }
    }

    fun startNewGame(difficulty: Difficulty, isPractice: Boolean) {
        currentIsPractice = isPractice
        timerJob?.cancel()

        // 统一难度阶梯：普通局与练习局都由难度决定数字范围
        val puzzle = generatePuzzle(difficulty)

        val cards = createCards(puzzle)
        totalGameTime = if (isPractice) Int.MAX_VALUE else 120

        _state.value = GameState(
            cards = cards,
            difficulty = difficulty,
            timeRemaining = if (isPractice) Int.MAX_VALUE else 120,
            isGameOver = false,
            isSuccess = false,
            history = emptyList(),
            selectedCardIndices = emptySet(),
            currentOperator = null,
            score = 0
        )

        if (!isPractice) {
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _state.value
                if (current.timeRemaining > 0 && !current.isGameOver) {
                    val newTime = current.timeRemaining - 1
                    _state.value = current.copy(timeRemaining = newTime)
                    if (newTime == 0) {
                        onTimeUp()
                    }
                } else {
                    break
                }
            }
        }
    }

    private fun onTimeUp() {
        _state.value = _state.value.copy(
            isGameOver = true,
            isSuccess = false
        )
        saveGameRecord()
    }

    fun onCardClick(index: Int) {
        val current = _state.value
        if (current.isGameOver) return

        val card = current.cards.getOrNull(index) ?: return
        if (card.isUsed) return

        soundManager.play(SoundType.SELECT)

        when {
            // Deselect if already selected
            current.selectedCardIndices.contains(index) -> {
                _state.value = current.copy(
                    selectedCardIndices = current.selectedCardIndices - index,
                    currentOperator = null
                )
            }
            // First card selection
            current.selectedCardIndices.isEmpty() -> {
                _state.value = current.copy(
                    selectedCardIndices = setOf(index)
                )
            }
            // Second card selected with operator → apply operation
            current.selectedCardIndices.size == 1 && current.currentOperator != null -> {
                val op: Operator = checkNotNull(current.currentOperator)
                val firstIdx = current.selectedCardIndices.first()
                applyOperation(firstIdx, index, op)
            }
            // Second card selected without operator → change selection
            current.selectedCardIndices.size == 1 && current.currentOperator == null -> {
                _state.value = current.copy(
                    selectedCardIndices = setOf(index)
                )
            }
        }
    }

    fun onOperatorClick(operator: Operator) {
        val current = _state.value
        if (current.isGameOver) return
        if (current.selectedCardIndices.size != 1) return

        soundManager.play(SoundType.OPERATOR)

        // Check for division by zero
        val selectedIdx = current.selectedCardIndices.first()
        val selectedValue = current.cards[selectedIdx].value
        val otherCards = current.cards.filterIndexed { idx, c -> idx != selectedIdx && !c.isUsed }
        if (operator == Operator.DIVIDE && otherCards.any { it.value == 0.0 }) {
            // Allow selection, but will handle at apply time
        }

        _state.value = current.copy(currentOperator = operator)
    }

    private fun applyOperation(firstIdx: Int, secondIdx: Int, operator: Operator) {
        val current = _state.value
        val firstCard = current.cards[firstIdx]
        val secondCard = current.cards[secondIdx]

        // Division by zero check
        if (operator == Operator.DIVIDE && secondCard.value == 0.0) return

        val result = when (operator) {
            Operator.PLUS -> firstCard.value + secondCard.value
            Operator.MINUS -> firstCard.value - secondCard.value
            Operator.MULTIPLY -> firstCard.value * secondCard.value
            Operator.DIVIDE -> firstCard.value / secondCard.value
        }

        if (result.isNaN() || result.isInfinite()) return

        // Save history for undo
        val newHistory: List<List<Card>> = current.history + listOf(current.cards.map { it.copy() })

        // Create new card list
        val resultLabel = if (result == result.toInt().toDouble()) {
            result.toInt().toString()
        } else {
            String.format("%.1f", result).trimEnd('0').trimEnd('.')
        }

        val newCards = current.cards.toMutableList()
        newCards[firstIdx] = firstCard.copy(isUsed = true, id = "used-${System.currentTimeMillis()}")
        newCards[secondIdx] = secondCard.copy(
            id = "card-res-${System.currentTimeMillis()}",
            value = result,
            label = resultLabel
        )

        val remainingCards = newCards.filter { !it.isUsed }

        val isSuccess = remainingCards.size == 1 && isTwentyFour(remainingCards[0].value)
        val isGameOver = remainingCards.size <= 1

        _state.value = current.copy(
            cards = newCards,
            selectedCardIndices = emptySet(),
            currentOperator = null,
            history = newHistory,
            score = if (isSuccess) current.score + 1250 else current.score,
            isGameOver = isGameOver,
            isSuccess = isSuccess
        )

        if (isGameOver) {
            timerJob?.cancel()
            if (isSuccess) soundManager.play(SoundType.SUCCESS)
            else soundManager.play(SoundType.FAIL)
            saveGameRecord()
        }
    }

    fun onUndo() {
        val current = _state.value
        if (current.history.isEmpty()) return

        soundManager.play(SoundType.UNDO)

        val previousCards = current.history.last()
        _state.value = current.copy(
            cards = previousCards,
            history = current.history.dropLast(1),
            selectedCardIndices = emptySet(),
            currentOperator = null
        )
    }

    fun onReset() {
        startNewGame(_state.value.difficulty, currentIsPractice)
    }

    fun onNextRound() {
        startNewGame(_state.value.difficulty, currentIsPractice)
    }

    private fun saveGameRecord() {
        val current = _state.value
        if (currentIsPractice) return // Practice mode doesn't count towards stats

        viewModelScope.launch {
            val timeTaken = totalGameTime - current.timeRemaining
            gameRepository.saveGameRecord(
                GameRecord(
                    isSuccess = current.isSuccess,
                    score = current.score,
                    timeTaken = timeTaken,
                    difficulty = current.difficulty.name.lowercase(),
                    mode = "timed"
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}