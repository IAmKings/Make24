package com.twentyfoursolve.app.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twentyfoursolve.app.audio.SoundManager
import com.twentyfoursolve.app.audio.SoundType
import com.twentyfoursolve.core.logic.createCards
import com.twentyfoursolve.core.logic.dailyPuzzle
import com.twentyfoursolve.core.logic.evaluateEquation
import com.twentyfoursolve.core.logic.firstSolutionStep
import com.twentyfoursolve.core.logic.formatPlayerFormula
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
import com.twentyfoursolve.data.repository.DailyRepository
import com.twentyfoursolve.data.repository.GameRepository
import com.twentyfoursolve.data.repository.SettingsRepository
import java.time.LocalDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.math.abs
import kotlin.math.roundToInt
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
    private val dailyRepository: DailyRepository,
    private val settingsRepository: SettingsRepository,
    private val soundManager: SoundManager
) : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var totalGameTime = 120 // seconds
    private var currentIsPractice = false
    private var currentIsDaily = false
    private var dailyEpochDay = 0L
    private var allowUnsolvable = true
    private var cachedStreak = 0

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                soundManager.enabled = settings.soundEnabled
                allowUnsolvable = settings.allowUnsolvable
            }
        }
    }

    fun startDailyGame() {
        currentIsDaily = true
        currentIsPractice = false
        dailyEpochDay = LocalDate.now().toEpochDay()
        startRound(
            difficulty = Difficulty.HARD,
            puzzle = dailyPuzzle(dailyEpochDay),
            isPractice = false,
            allowUnsolvableHands = false,
            carryScore = false,
        )
    }

    fun startNewGame(difficulty: Difficulty, isPractice: Boolean, carryScore: Boolean = false) {
        currentIsDaily = false
        currentIsPractice = isPractice
        startRound(
            difficulty = difficulty,
            puzzle = generatePuzzle(difficulty, allowUnsolvable),
            isPractice = isPractice,
            allowUnsolvableHands = allowUnsolvable && !difficulty.alwaysSolvable,
            carryScore = carryScore,
        )
    }

    private fun startRound(
        difficulty: Difficulty,
        puzzle: List<Int>,
        isPractice: Boolean,
        allowUnsolvableHands: Boolean,
        carryScore: Boolean,
    ) {
        timerJob?.cancel()

        val limit = difficulty.timeLimitSeconds

        val cards = createCards(puzzle)
        totalGameTime = if (isPractice) Int.MAX_VALUE else limit

        _state.value = GameState(
            cards = cards,
            difficulty = difficulty,
            timeLimit = limit,
            timeRemaining = if (isPractice) Int.MAX_VALUE else limit,
            allowUnsolvable = allowUnsolvableHands,
            isDaily = currentIsDaily,
            initialPuzzle = puzzle,
            isGameOver = false,
            isSuccess = false,
            history = emptyList(),
            selectedCardIndices = emptySet(),
            currentOperator = null,
            // 闯关模式：成功进入下一关时延续累计分数与累计用时，否则从 0 开始；本关得分重置
            score = if (carryScore) _state.value.score else 0,
            roundScore = 0,
            accumulatedTime = if (carryScore) _state.value.accumulatedTime else 0
        )

        if (!isPractice) {
            refreshStreak()
            startTimer()
        }
    }

    /** 预取当前连胜（缓存字段，供成功计分同步使用，避免异步竞态）。 */
    private fun refreshStreak() {
        viewModelScope.launch {
            cachedStreak = gameRepository.getCurrentStreak()
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
        val resultFormula = formatPlayerFormula(
            left = firstCard.formula.ifEmpty { n1.toString() },
            leftOp = firstCard.formulaOp,
            op = operator,
            right = secondCard.formula.ifEmpty { n2.toString() },
            rightOp = secondCard.formulaOp
        )

        val newCards = current.cards.toMutableList()
        newCards[firstIdx] = firstCard.copy(isUsed = true, id = "used-${System.currentTimeMillis()}")
        newCards[secondIdx] = secondCard.copy(
            id = "card-res-${System.currentTimeMillis()}",
            value = result,
            label = resultLabel,
            numerator = rn,
            denominator = rd,
            formula = resultFormula,
            formulaOp = operator
        )

        val remainingCards = newCards.filter { !it.isUsed }

        // 精确目标判断：n/d == 24/1
        val isSuccess = remainingCards.size == 1 &&
            remainingCards[0].numerator == 24L * remainingCards[0].denominator
        val isGameOver = remainingCards.size <= 1

        // 本关得分（成功时按难度系数/时间/步数/连胜计算），score 为累计总分
        val roundScore = if (isSuccess) {
            computeFinalScore(
                difficulty = current.difficulty,
                timeRemaining = current.timeRemaining,
                mergeSteps = current.history.size + 1,
                streak = if (currentIsDaily) 0 else cachedStreak
            )
        } else {
            0
        }
        // 当关用时（秒），游戏结束时累加到累计用时
        val roundTime = if (isGameOver) (totalGameTime - current.timeRemaining).coerceAtLeast(0) else 0

        val deadEnd = current.difficulty != Difficulty.EASY &&
            !isGameOver &&
            !remainingHasSolution(remainingCards)

        _state.value = current.copy(
            cards = newCards,
            selectedCardIndices = emptySet(),
            currentOperator = null,
            history = newHistory,
            score = current.score + roundScore,
            roundScore = roundScore,
            accumulatedTime = current.accumulatedTime + roundTime,
            isGameOver = isGameOver,
            isSuccess = isSuccess,
            mergeRejected = false,
            mergeDeadEnd = deadEnd
        )

        if (isGameOver) {
            timerJob?.cancel()
            if (isSuccess) soundManager.play(SoundType.SUCCESS)
            else soundManager.play(SoundType.FAIL)
            if (isSuccess && currentIsDaily) {
                val timeTaken = (totalGameTime - current.timeRemaining).coerceAtLeast(0)
                viewModelScope.launch {
                    dailyRepository.saveIfBetter(dailyEpochDay, roundScore, timeTaken)
                }
            }
            saveGameRecord()
        }
    }

    /**
     * 挑战模式计分：
     * 总分 = (基础 1250 + 剩余秒数×5 + 步数奖励 + 连胜×100) × 难度系数
     * 步数奖励：完美 3 步完成 +300，每多一步 -100（最低 0）；练习模式无时间/连胜奖励。
     */
    private fun computeFinalScore(
        difficulty: Difficulty,
        timeRemaining: Int,
        mergeSteps: Int,
        streak: Int
    ): Int {
        val timeBonus = if (currentIsPractice) 0 else timeRemaining.coerceAtLeast(0) * 5
        val stepsBonus = maxOf(0, 300 - (mergeSteps - 3) * 100)
        val streakBonus = if (currentIsPractice) 0 else streak * 100
        val base = 1250 + timeBonus + stepsBonus + streakBonus
        return (base * difficulty.multiplier).roundToInt()
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
            currentOperator = null,
            mergeDeadEnd = false,
            mergeRejected = false
        )
    }

    fun onReset() {
        if (currentIsDaily) {
            startDailyGame()
            return
        }
        abandonToNewRound()
    }

    fun onNextRound() {
        // 成功过关：下一关延续累计分数（闯关模式）；失败/无解换题：放弃并清零
        if (_state.value.isSuccess) {
            startNewGame(_state.value.difficulty, currentIsPractice, carryScore = true)
        } else {
            abandonToNewRound()
        }
    }

    /**
     * 重置/换题：普通模式下放弃未结束的当前局（保存失败记录以打断连胜），再进入下一局。
     * 提示与无解确认均为纯参考，不在此打断连胜。
     */
    private fun abandonToNewRound() {
        val current = _state.value
        val abandonTime = current.accumulatedTime +
            (totalGameTime - current.timeRemaining).coerceAtLeast(0)
        if (!currentIsPractice && !current.isGameOver) {
            viewModelScope.launch {
                gameRepository.saveGameRecord(
                    GameRecord(
                        isSuccess = false,
                        score = current.score,
                        timeTaken = abandonTime,
                        difficulty = current.difficulty.name.lowercase(),
                        mode = "timed"
                    )
                )
            }
        }
        startNewGame(current.difficulty, currentIsPractice)
    }

    /**
     * 请求提示：按当前剩余牌数分派求解器。
     * 练习局和计时简单给完整解法。计时中等、困难、超难只给下一步，
     * 并且按难度每局限扣一次分（超难还扣时间）。
     */
    fun requestHint() {
        val current = _state.value
        val penalty = current.difficulty
        val stepHint = !currentIsPractice &&
            (penalty.hintPointPenalty > 0 || penalty.hintTimePenaltySeconds > 0)
        val result = solveCurrentBoard(
            if (stepHint) ExpressionStyle.FULLY_PARENTHESIZED else ExpressionStyle.COMPACT
        )
        when (result?.status) {
            SolveStatus.SOLVED -> {
                if (stepHint) {
                    val expression = result.expression
                    if (expression == null) {
                        _state.value = current.copy(hint = null, hintUnsolvable = false, hintIsStep = false)
                        return
                    }
                    val charge = !current.hintCharged
                    val points = if (charge) penalty.hintPointPenalty else 0
                    val seconds = if (charge) penalty.hintTimePenaltySeconds else 0
                    _state.value = current.copy(
                        hint = firstSolutionStep(expression),
                        hintIsStep = true,
                        hintPenaltyPoints = points,
                        hintPenaltySeconds = seconds,
                        hintCharged = current.hintCharged || charge,
                        hintUnsolvable = false,
                        score = if (points > 0) maxOf(0, current.score - points) else current.score,
                        timeRemaining = if (seconds > 0) {
                            maxOf(1, current.timeRemaining - seconds)
                        } else {
                            current.timeRemaining
                        }
                    )
                } else {
                    _state.value = current.copy(
                        hint = result.expression,
                        hintIsStep = false,
                        hintPenaltyPoints = 0,
                        hintPenaltySeconds = 0,
                        hintUnsolvable = false
                    )
                }
            }
            SolveStatus.UNSOLVABLE -> {
                // 当前牌面无解也是提示的一种回答（不打断连胜、不重置、不扣分）
                _state.value = current.copy(
                    hint = null,
                    hintIsStep = false,
                    hintPenaltyPoints = 0,
                    hintPenaltySeconds = 0,
                    hintUnsolvable = true
                )
            }
            else -> {
                // 剩余 1 张等无提示场景
                _state.value = current.copy(
                    hint = null,
                    hintIsStep = false,
                    hintPenaltyPoints = 0,
                    hintPenaltySeconds = 0,
                    hintUnsolvable = false
                )
            }
        }
    }

    /**
     * 无解按钮：仅回答开局发牌是否无解（在允许无解牌局的模式下）。
     * 视为一次"回答"：牌面其实有解时视为答错，扣 300 分 + 15 秒（练习模式仅扣分），
     * 避免玩家用无解按钮免费试探开局可解性。
     */
    fun checkUnsolvable() {
        val current = _state.value
        val initial = current.initialPuzzle
        if (initial.isEmpty()) return
        val solved = TwentyFourSolver.solve(initial.toIntArray()).status == SolveStatus.SOLVED
        if (solved) {
            // 答错：扣分 + 扣时
            val newScore = maxOf(0, current.score - 300)
            val newTime = if (currentIsPractice) {
                current.timeRemaining
            } else {
                maxOf(1, current.timeRemaining - 15)
            }
            _state.value = current.copy(
                solvable = true,
                unsolvablePenalty = true,
                score = newScore,
                timeRemaining = newTime
            )
        } else {
            _state.value = current.copy(solvable = false, unsolvablePenalty = false)
        }
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
        _state.value = _state.value.copy(
            hint = null,
            hintUnsolvable = false,
            hintIsStep = false,
            hintPenaltyPoints = 0,
            hintPenaltySeconds = 0
        )
    }

    fun clearMergeDeadEnd() {
        _state.value = _state.value.copy(mergeDeadEnd = false)
    }

    /** 剩余 3 张或 2 张时，判断还能不能凑成 24。其他张数视为不需要提示。 */
    private fun remainingHasSolution(cards: List<Card>): Boolean {
        return when (cards.size) {
            3 -> TwentyFourSolver.solveThree(
                Rational(cards[0].numerator, cards[0].denominator),
                Rational(cards[1].numerator, cards[1].denominator),
                Rational(cards[2].numerator, cards[2].denominator)
            ).status == SolveStatus.SOLVED
            2 -> TwentyFourSolver.solveTwo(
                Rational(cards[0].numerator, cards[0].denominator),
                Rational(cards[1].numerator, cards[1].denominator)
            ).status == SolveStatus.SOLVED
            else -> true
        }
    }

    fun clearSolvable() {
        _state.value = _state.value.copy(solvable = null, unsolvablePenalty = false)
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

    /**
     * 正确识别无解 = 过关胜利：保存成功记录（连胜 +1）、按难度给奖励分、
     * 分数延续进入下一关。
     * 奖励 = (500 + 当前连胜×100) × 难度系数。
     */
    fun confirmUnsolvableWin() {
        val current = _state.value
        timerJob?.cancel()
        if (currentIsPractice) {
            _state.value = current.copy(
                isGameOver = true,
                isSuccess = true,
                wonByUnsolvable = true,
                solvable = null,
                unsolvablePenalty = false,
                mergeDeadEnd = false
            )
            soundManager.play(SoundType.SUCCESS)
            return
        }
        viewModelScope.launch {
            val streak = gameRepository.getCurrentStreak()
            val reward = ((500 + streak * 100) * current.difficulty.multiplier).roundToInt()
            val roundTime = (totalGameTime - current.timeRemaining).coerceAtLeast(0)
            _state.value = current.copy(
                score = current.score + reward,
                roundScore = reward,
                accumulatedTime = current.accumulatedTime + roundTime,
                isGameOver = true,
                isSuccess = true,
                wonByUnsolvable = true,
                solvable = null,
                unsolvablePenalty = false,
                mergeDeadEnd = false
            )
            saveGameRecord()
            soundManager.play(SoundType.SUCCESS)
        }
    }

    private fun saveGameRecord() {
        val current = _state.value
        if (currentIsPractice || currentIsDaily) return

        viewModelScope.launch {
            val timeTaken = current.accumulatedTime
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