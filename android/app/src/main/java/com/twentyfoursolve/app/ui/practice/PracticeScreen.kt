package com.twentyfoursolve.app.ui.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twentyfoursolve.app.theme.CornerRadius
import com.twentyfoursolve.app.theme.LocalStringProvider
import com.twentyfoursolve.app.theme.PlusJakartaSans
import com.twentyfoursolve.app.theme.Spacing

@Composable
fun PracticeScreen(
    onStartPractice: (difficulty: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStringProvider.current
    var selectedDifficulty by remember { mutableStateOf("easy") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg, vertical = Spacing.lg)
    ) {
        // 配置内容可滚动（避免小屏下底部被截断）；开始按钮固定在底部
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl)
        ) {
            // Title
            Text(
                text = strings["practice"] ?: "Practice",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Black,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    letterSpacing = (-1).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        Text(
            text = strings["practiceDesc"] ?: "Hone your math skills at your own pace.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Difficulty Selection
        Text(
            text = strings["selectIntensity"]?.uppercase() ?: "SELECT INTENSITY",
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        DifficultyOption(
            title = strings["easy"] ?: "Easy",
            description = strings["easyDesc"] ?: "Simple numbers (1–10), basic operations.",
            icon = "\uD83D\uDC76", // baby emoji
            isSelected = selectedDifficulty == "easy",
            onClick = { selectedDifficulty = "easy" }
        )
        DifficultyOption(
            title = strings["medium"] ?: "Medium",
            description = strings["mediumDesc"] ?: "Mixed operations, numbers up to 20.",
            icon = "\uD83E\uDDE0", // brain emoji
            isSelected = selectedDifficulty == "medium",
            onClick = { selectedDifficulty = "medium" }
        )
        DifficultyOption(
            title = strings["hard"] ?: "Hard",
            description = strings["hardDesc"] ?: "Fractions, large numbers, complex solves.",
            icon = "\uD83D\uDD25", // fire emoji
            isSelected = selectedDifficulty == "hard",
            onClick = { selectedDifficulty = "hard" }
        )

        // 为底部固定开始按钮预留空间
        Spacer(modifier = Modifier.height(88.dp))
        }

        // Start Practice Button（固定在底部，始终可见）
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(CornerRadius.full))
                .clickable { onStartPractice(selectedDifficulty) },
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
                Text(
                    text = strings["startPractice"] ?: "Start Practice",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Black
                    ),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun DifficultyOption(
    title: String,
    description: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CornerRadius.lg))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(CornerRadius.lg),
        color = if (isSelected) MaterialTheme.colorScheme.surfaceContainerLowest
        else MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = icon, style = MaterialTheme.typography.headlineMedium)
                    }
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Black
                        )
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}