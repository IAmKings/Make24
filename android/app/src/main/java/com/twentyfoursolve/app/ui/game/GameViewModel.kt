package com.twentyfoursolve.app.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twentyfoursolve.app.audio.SoundManager
import com.twentyfoursolve.app.audio.SoundType
import com.twentyfoursolve.core.logic.createCards
import com.twentyfoursolve.core.logic.evaluateEquation
import com.twentyfoursolve.core.logic.generatePuzzle
import com.make24.solver.ExpressionStyle
import com.make24.solver.SolveOptions
import com.make24.solver.Rational
import com.make24.solver.SolveResult
import com.make24.solver.SolveStatus
import com.make24.solver.TwentyFourSolver
import com.twentyfoursolve.core.model.Card
import com.twentyfoursolve.core.model.Difficulty
import com.twentyfoursolve.core.model.GameRecord
import com.twentyfoursolve.core.model.GameState
import com.twentyfoursolve.core.model.Operator
import com.twentyfoursolve.data.repository.GameRepository
import com.twentyfoursolve.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.math.abs
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
    private var allowUnsolvable = true

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                soundManager.enabled = settings.soundEnabled
                allowUnsolvable = settings.allowUnsolvable
            }
        }
    }

    fun startNewGame(difficulty: Difficulty, isPractice: Boolean) {
        currentIsPractice = isPractice
        timerJob?.cancel()

        // 统一难度阶梯：普通局与练习局都由难度决定数字范围；
        // 是否允许无解由设置配置（简单难度始终有解）
        val puzzle = generatePuzzle(difficulty, allowUnsolvable)

        val cards = createCards(puzzle)
        totalGameTime = if (isPractice) Int.MAX_VALUE else 120

        _state.value = GameState(
            cards = cards,
            difficulty = difficulty,
            timeRemaining = if (isPractice) Int.MAX_VALUE else 120,
            allowUnsolvable = allowUnsolvable,
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

        // 精确分数计算（n1/d1 op n2/d2）
        val n1 = firstCard.numerator
        val d1 = firstCard.denominator
        val n2 = secondCard.numerator
        val d2 = secondCard.denominator

        var rn: Long
        var rd: Long
        when (operator) {
            Operator.PLUS -> {
                rn = n1 * d2 + n2 * d1
                rd = d1 * d2
            }
            Operator.MINUS -> {
                rn = n1 * d2 - n2 * d1
                rd = d1 * d2
            }
            Operator.MULTIPLY -> {
                rn = n1 * n2
                rd = d1 * d2
            }
            Operator.DIVIDE -> {
                if (n2 == 0L) return // 除零
                rn = n1 * d2
                rd = d1 * n2
            }
        }
        // 符号归一 + 约分
        if (rd < 0) {
            rn = -rn
            rd = -rd
        }
        val g = gcd(abs(rn), rd)
        if (g != 1L) {
            rn /= g
            rd /= g
        }

        val result = rn.toDouble() / rd.toDouble()
        if (result.isNaN() || result.isInfinite()) return

        // 简单难度：合并后剩余 3 张牌必须仍可解，否则拒绝该步（低难度不出现无解死局）
        if (current.difficulty == Difficulty.EASY) {
            val others = current.cards.filterIndexed { idx, c ->
                idx != firstIdx && idx != secondIdx && !c.isUsed
            }
            val solvable = if (others.size == 2) {
                TwentyFourSolver.solveThree(
                    Rational(others[0].numerator, others[0].denominator),
                    Rational(others[1].numerator, others[1].denominator),
                    Rational(rn, rd),
                    SolveOptions(style = ExpressionStyle.FULLY_PARENTHESIZED)
                ).status == SolveStatus.SOLVED
            } else {
                true
            }
            if (!solvable) {
                _state.value = current.copy(mergeRejected = true)
                return
            }
        }

        // Save history for undo
        val newHistory: List<List<Card>> = current.history + listOf(current.cards.map { it.copy() })

        // 结果标签：整数直接显示，分数显示 n/d
        val resultLabel = if (rd == 1L) rn.toString() else "$rn/$rd"

        val newCards = current.cards.toMutableList()
        newCards[firstIdx] = firstCard.copy(isUsed = true, id = "used-${System.currentTimeMillis()}")
        newCards[secondIdx] = secondCard.copy(
            id = "card-res-${System.currentTimeMillis()}",
            value = result,
            label = resultLabel,
            numerator = rn,
            denominator = rd
        )

        val remainingCards = newCards.filter { !it.isUsed }

        // 精确目标判断：n/d == 24/1
        val isSuccess = remainingCards.size == 1 &&
            remainingCards[0].numerator == 24L * remainingCards[0].denominator
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

    /** 请求提示：按当前剩余牌数分派求解器（4 张 → solve，3 张 → solveThree，2 张 → solveTwo）。 */
    fun requestHint() {
        val hint = solveCurrentBoard(ExpressionStyle.COMPACT)?.expression
        _state.value = _state.value.copy(hint = hint)
    }

    /** 无解按钮：检查当前剩余牌面是否有解（生成器保证初始有解，合并后可能走错）。 */
    fun checkUnsolvable() {
        val solved = solveCurrentBoard(ExpressionStyle.FULLY_PARENTHESIZED)?.status == SolveStatus.SOLVED
        _state.value = _state.value.copy(solvable = solved)
    }

    private fun solveCurrentBoard(style: ExpressionStyle): SolveResult? {
        val remaining = _state.value.cards.filter { !it.isUsed }
        return when (remaining.size) {
            4 -> TwentyFourSolver.solve(
                remaining.map { it.numerator.toInt() }.toIntArray(),
                SolveOptions(style = style)
            )
            3 -> TwentyFourSolver.solveThree(
                Rational(remaining[0].numerator, remaining[0].denominator),
                Rational(remaining[1].numerator, remaining[1].denominator),
                Rational(remaining[2].numerator, remaining[2].denominator),
                SolveOptions(style = style)
            )
            2 -> TwentyFourSolver.solveTwo(
                Rational(remaining[0].numerator, remaining[0].denominator),
                Rational(remaining[1].numerator, remaining[1].denominator),
                SolveOptions(style = style)
            )
            else -> null
        }
    }

    fun clearHint() {
        _state.value = _state.value.copy(hint = null)
    }

    fun clearSolvable() {
        _state.value = _state.value.copy(solvable = null)
    }

    fun clearMergeRejected() {
        _state.value = _state.value.copy(mergeRejected = false)
    }

    private fun gcd(a0: Long, b0: Long): Long {
        var a = abs(a0)
        var b = abs(b0)
        while (b != 0L) {
            val t = a % b
            a = b
            b = t
        }
        return a
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