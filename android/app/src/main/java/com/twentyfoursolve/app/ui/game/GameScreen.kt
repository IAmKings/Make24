package com.twentyfoursolve.app.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.twentyfoursolve.app.R
import com.twentyfoursolve.app.theme.CornerRadius
import com.twentyfoursolve.app.theme.LocalStringProvider
import com.twentyfoursolve.app.theme.PlusJakartaSans
import com.twentyfoursolve.app.theme.Spacing
import com.twentyfoursolve.app.ui.components.NumberCard
import com.twentyfoursolve.app.ui.components.OperatorSymbol
import com.twentyfoursolve.core.model.Difficulty
import com.twentyfoursolve.core.model.GameState
import com.twentyfoursolve.core.model.Operator
import com.twentyfoursolve.core.model.Suit

/** 成功展示完整算式；只剩一张牌却没到 24 时展示走到的式子。时间到且牌还没并完则不展示。 */
private fun resultFormula(state: GameState): String? {
    if (state.wonByUnsolvable) return null
    val live = state.cards.filter { !it.isUsed }
    if (live.size != 1) return null
    val formula = live[0].formula
    if (formula.isBlank()) return null
    if (!state.isSuccess && !formula.any { it == '+' || it == '−' || it == '×' || it == '÷' || it == '-' }) {
        return null
    }
    return formula
}

internal fun hintPenaltyText(strings: Map<String, String>, points: Int, seconds: Int): String? {
    if (points <= 0 && seconds <= 0) return null
    return if (seconds > 0) {
        (strings["hintStepPenaltyBoth"] ?: "−%1\$d points and −%2\$d seconds.").format(points, seconds)
    } else {
        (strings["hintStepPenaltyPoints"] ?: "−%d points.").format(points)
    }
}

internal fun buildShareText(
    numbers: List<Int>,
    formula: String?,
    score: Int,
    timeLabel: String?,
): String {
    val cards = numbers.joinToString(" ")
    val formulaLine = formula?.takeIf { it.isNotBlank() }?.let { "\n$it" }.orEmpty()
    val scoreLine = if (timeLabel == null) "Score: $score" else "Score: $score | Time: $timeLabel"
    return "24 SOLVE\n$cards$formulaLine\n$scoreLine"
}

@Composable
fun GameScreen(
    difficulty: String,
    isPractice: Boolean,
    isDaily: Boolean = false,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalStringProvider.current
    var showExitDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    // 系统返回键：游戏进行中先弹确认，防止误触丢失进度（确认后由 onExit 退出）
    BackHandler { showExitDialog = true }

    // Initialize the game when composable enters
    LaunchedEffect(difficulty, isPractice, isDaily) {
        if (isDaily) {
            viewModel.startDailyGame()
        } else {
            viewModel.startNewGame(Difficulty.fromName(difficulty), isPractice)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // Game HUD (Glassmorphism)
        GameHud(
            timeRemaining = state.timeRemaining,
            score = state.score,
            isPractice = isPractice,
            timeRemainingLabel = strings["timeRemaining"] ?: "Time Remaining",
            scoreLabel = strings["currentScore"] ?: "Score",
            onBack = { showExitDialog = true }
        )

        // Target Preview
        TargetPreview(
            selectedCard = state.selectedCardIndices.firstOrNull()?.let { state.cards.getOrNull(it) },
            currentOperator = state.currentOperator,
            hint = strings["combineCards"] ?: "Combine cards to reach 24"
        )

        // Cards Grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            state.cards.forEachIndexed { idx, card ->
                if (!card.isUsed) {
                    NumberCard(
                        value = card.value,
                        label = card.label,
                        suit = card.suit,
                        isSelected = state.selectedCardIndices.contains(idx),
                        isUsed = card.isUsed,
                        onClick = { viewModel.onCardClick(idx) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Operators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Operator.entries.forEach { op ->
                OperatorButton(
                    operator = op,
                    isSelected = state.currentOperator == op,
                    onClick = { viewModel.onOperatorClick(op) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Hint & No-Solution
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActionButton(
                text = strings["unsolvable"] ?: "No Solution",
                icon = Icons.Filled.HelpOutline,
                // 仅回答开局发牌是否无解：配置允许 + 中等/困难 + 尚未操作（未合并过）
                enabled = state.allowUnsolvable &&
                    !state.difficulty.alwaysSolvable &&
                    state.history.isEmpty() &&
                    !state.isGameOver,
                onClick = { viewModel.checkUnsolvable() },
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                text = strings["hint"] ?: "Hint",
                icon = Icons.Filled.Lightbulb,
                enabled = !state.isGameOver,
                onClick = { viewModel.requestHint() },
                modifier = Modifier.weight(1f)
            )
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Undo
            ActionButton(
                text = strings["undo"] ?: "Undo",
                icon = Icons.AutoMirrored.Filled.Undo,
                enabled = state.history.isNotEmpty(),
                onClick = { viewModel.onUndo() },
                modifier = Modifier.weight(1f)
            )
            // Reset
            ActionButton(
                text = strings["reset"] ?: "Reset",
                icon = Icons.Filled.Refresh,
                enabled = true,
                onClick = { showResetDialog = true },
                modifier = Modifier.weight(1f),
                isError = true
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    // Result Modal
    AnimatedVisibility(
        visible = state.isGameOver,
        enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.9f, animationSpec = tween(300)),
        exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.9f, animationSpec = tween(200))
    ) {
        ResultModal(
            isSuccess = state.isSuccess,
            timeTaken = if (isPractice) 0 else (state.timeLimit - state.timeRemaining).coerceAtLeast(0),
            score = state.roundScore,
            formula = resultFormula(state),
            wonByUnsolvable = state.wonByUnsolvable,
            isDaily = state.isDaily,
            puzzleNumbers = state.initialPuzzle,
            onNextRound = { viewModel.onNextRound() },
            onExit = onExit,
            strings = strings,
            isPractice = isPractice
        )
    }

    // 提示弹窗（显示求解器给出的解法表达式）
    state.hint?.let { hint ->
        AlertDialog(
            onDismissRequest = { viewModel.clearHint() },
            title = { Text(strings["hint"] ?: "Hint") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (state.hintIsStep) {
                            (strings["hintStep"] ?: "Next: %s").format(hint)
                        } else {
                            (strings["hintText"] ?: "Solution: %s").format(hint)
                        },
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    val penaltyText = hintPenaltyText(
                        strings,
                        state.hintPenaltyPoints,
                        state.hintPenaltySeconds,
                    )
                    if (penaltyText != null) {
                        Text(
                            penaltyText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearHint() }) {
                    Text(strings["gotIt"] ?: "Got It")
                }
            }
        )
    }

    // 提示结果为"当前无解"（作为回答展示，纯参考，不提供操作）
    if (state.hintUnsolvable) {
        AlertDialog(
            onDismissRequest = { viewModel.clearHint() },
            title = { Text(strings["unsolvable"] ?: "No Solution") },
            text = {
                Text(strings["hintUnsolvable"] ?: "No solution for the current hand — try undoing a step or starting over.")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearHint() }) {
                    Text(strings["gotIt"] ?: "Got It")
                }
            }
        )
    }

    // 无解检查弹窗（有解 / 确实无解）
    state.solvable?.let { solvable ->
        AlertDialog(
            onDismissRequest = { viewModel.clearSolvable() },
            title = { Text(strings["unsolvable"] ?: "No Solution") },
            text = {
                Text(
                    if (solvable) {
                        if (state.unsolvablePenalty) {
                            strings["unsolvablePenalty"]
                                ?: "Wrong! This hand IS solvable. -300 points and -15 seconds."
                        } else {
                            strings["unsolvableHasSolution"] ?: "This hand is solvable — keep trying!"
                        }
                    } else {
                        strings["unsolvableNone"] ?: "This hand has no solution."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearSolvable()
                    // 确实无解 = 正确识别，视为过关胜利（保存成功记录 + 奖励分 + 延续下一关）
                    if (!solvable) viewModel.confirmUnsolvableWin()
                }) {
                    Text(strings["gotIt"] ?: "Got It")
                }
            }
        )
    }

    // 中等及以上：死步已经落下，提示可以撤销
    if (state.mergeDeadEnd) {
        AlertDialog(
            onDismissRequest = { viewModel.clearMergeDeadEnd() },
            title = { Text(strings["mergeDeadEndTitle"] ?: "No path left") },
            text = {
                Text(
                    strings["mergeDeadEnd"]
                        ?: "No solution after this step. You can undo and try again."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearMergeDeadEnd() }) {
                    Text(strings["gotIt"] ?: "Got It")
                }
            }
        )
    }

    // 简单难度合并被拒绝提示（此步后无解）
    if (state.mergeRejected) {
        AlertDialog(
            onDismissRequest = { viewModel.clearMergeRejected() },
            title = { Text(strings["unsolvable"] ?: "No Solution") },
            text = {
                Text(
                    strings["mergeRejected"]
                        ?: "No solution after this step — please try another combination."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearMergeRejected() }) {
                    Text(strings["gotIt"] ?: "Got It")
                }
            }
        )
    }

    // 重置确认（防止误触打断连胜/清空分数）
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(strings["resetConfirmTitle"] ?: "Reset Round") },
            text = {
                Text(
                    strings["resetConfirmText"]
                        ?: "Reset will clear current score and break your win streak. Continue?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    viewModel.onReset()
                }) {
                    Text(strings["reset"] ?: "Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(strings["cancel"] ?: "Cancel")
                }
            }
        )
    }

    // 退出确认（HUD 返回按钮 / 系统返回键触发）
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(strings["exitConfirmTitle"] ?: "Exit Game") },
            text = { Text(strings["exitConfirmText"] ?: "Are you sure you want to exit? Current progress will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onExit()
                }) {
                    Text(strings["exitConfirm"] ?: "Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(strings["cancel"] ?: "Cancel")
                }
            }
        )
    }
}

@Composable
private fun GameHud(
    timeRemaining: Int,
    score: Int,
    isPractice: Boolean,
    timeRemainingLabel: String,
    scoreLabel: String,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CornerRadius.xl)),
        shape = RoundedCornerShape(CornerRadius.xl),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button（紧凑，省出空间给剩余时间与得分）
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack)
            )

            // 返回按钮与剩余时间之间的间隔
            Spacer(modifier = Modifier.width(12.dp))

            // 剩余时间与得分各占一半，避免五位数分数把倒计时挤到换行
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = timeRemainingLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPractice) "∞" else formatTime(timeRemaining),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp,
                            fontFeatureSettings = "tnum"
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Score（右侧留出边距，不紧贴 HUD 边缘）
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 4.dp)
            ) {
                Text(
                    text = scoreLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = score.toString(),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp,
                            fontFeatureSettings = "tnum"
                        ),
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TargetPreview(
    selectedCard: com.twentyfoursolve.core.model.Card?,
    currentOperator: Operator?,
    hint: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // First card slot
            Surface(
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(CornerRadius.md),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = androidx.compose.foundation.BorderStroke(
                    2.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (selectedCard != null) {
                        Text(
                            text = selectedCard.label,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Black
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = "?",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Operator
            Text(
                text = currentOperator?.symbol ?: "+",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Second card slot
            Surface(
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(CornerRadius.md),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = androidx.compose.foundation.BorderStroke(
                    2.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "?",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }

        Text(
            text = hint,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OperatorButton(
    operator: Operator,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(CornerRadius.md))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(CornerRadius.md),
        color = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.secondaryContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            OperatorSymbol(
                symbol = operator.symbol,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSecondaryContainer,
                // 符号画布 38dp，几何中心与 64dp 按钮容器中心重合，视觉精确居中
                modifier = Modifier.size(38.dp)
            )
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    val bgColor = if (isError) MaterialTheme.colorScheme.errorContainer
    else MaterialTheme.colorScheme.surfaceContainerHighest
    val contentColor = if (isError) MaterialTheme.colorScheme.onError
    else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(CornerRadius.full))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(CornerRadius.full),
        color = bgColor.copy(alpha = if (enabled) 1f else 0.3f)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = contentColor.copy(alpha = if (enabled) 1f else 0.3f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Black
                ),
                color = contentColor.copy(alpha = if (enabled) 1f else 0.3f)
            )
        }
    }
}

@Composable
private fun ResultModal(
    isSuccess: Boolean,
    timeTaken: Int,
    score: Int,
    formula: String?,
    wonByUnsolvable: Boolean,
    isDaily: Boolean,
    puzzleNumbers: List<Int>,
    onNextRound: () -> Unit,
    onExit: () -> Unit,
    strings: Map<String, String>,
    isPractice: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(CornerRadius.xl),
            color = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Result icon：成功用 Congratulations 图标，失败用 failure 图标
                if (isSuccess) {
                    Image(
                        painter = painterResource(R.drawable.congratulations),
                        contentDescription = null,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.failure),
                        contentDescription = null,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Title
                Text(
                    text = (strings[if (isSuccess) "success" else "timesUp"]
                        ?: if (isSuccess) "Success!" else "Time's Up!"),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Black,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        letterSpacing = (-1).sp
                    ),
                    color = if (isSuccess) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Description
                Text(
                    text = when {
                        wonByUnsolvable -> strings["unsolvableWin"] ?: "You spotted that this hand has no solution."
                        isSuccess -> strings["masteredLevel"] ?: ""
                        else -> strings["failedLevel"] ?: ""
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (!wonByUnsolvable && !formula.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = strings[if (isSuccess) "yourSolution" else "yourAttempt"]
                            ?: if (isSuccess) "Your solution" else "You reached",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formula,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Black
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Stats grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        icon = Icons.Filled.Timer,
                        label = strings["timeTaken"] ?: "Time taken",
                        value = if (isPractice) "—" else formatTime(timeTaken),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Filled.Star,
                        label = strings["scoreLabel"] ?: "Score",
                        value = if (isSuccess) "+$score" else "0",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Next Round button (gradient)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(CornerRadius.full))
                        .clickable(onClick = { if (isDaily) onExit() else onNextRound() }),
                    shape = RoundedCornerShape(CornerRadius.full),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primaryContainer
                                ),
                                start = Offset.Zero,
                                end = Offset(1000f, 1000f)
                            )
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isDaily) {
                                    strings["done"] ?: "Done"
                                } else {
                                    strings["nextRound"] ?: "Next Round"
                                },
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Black
                                ),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Share button
                if (isSuccess) {
                    val context = LocalContext.current
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(CornerRadius.full))
                            .clickable {
                                val shareText = buildShareText(
                                    numbers = puzzleNumbers,
                                    formula = formula,
                                    score = score,
                                    timeLabel = if (isPractice) null else formatTime(timeTaken),
                                )
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(
                                    android.content.Intent.createChooser(intent, null)
                                )
                            },
                        shape = RoundedCornerShape(CornerRadius.full),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings["shareResult"] ?: "Share Result",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Exit button
                Text(
                    text = strings["exitMenu"] ?: "Exit to Menu",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(onClick = onExit)
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(CornerRadius.lg),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                // 单行：4 位数分数/时间不换行，避免 UI 异常
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}