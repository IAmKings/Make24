package com.twentyfoursolve.app.ui.home

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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twentyfoursolve.app.theme.CornerRadius
import com.twentyfoursolve.app.theme.LocalStringProvider
import com.twentyfoursolve.app.theme.PlusJakartaSans
import com.twentyfoursolve.app.theme.Spacing

@Composable
fun HomeScreen(
    onStartGame: () -> Unit,
    onPracticeMode: () -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStringProvider.current
    var showRulesDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl)
    ) {
        // Hero Section
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
            // Glow effect behind "24"
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                // Ambient glow
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                        )
                )
                // "24" hero text
                Text(
                    text = "24",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 128.sp,
                        lineHeight = 128.sp,
                        letterSpacing = (-4).sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Tagline
            Text(
                text = strings["tagline"] ?: "The Game of Logic",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp
                ),
                color = MaterialTheme.colorScheme.secondary,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Start Game Button (gradient CTA)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(CornerRadius.full))
                    .clickable(onClick = onStartGame),
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
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = strings["startGame"] ?: "Start Game",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Practice Mode Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(CornerRadius.full))
                    .clickable(onClick = onPracticeMode),
                shape = RoundedCornerShape(CornerRadius.full),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = strings["practiceMode"] ?: "Practice Mode",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        // 设置入口：首页右上角小图标（位于 Hero Box 右上角）
        IconButton(
            onClick = { onNavigate("settings") },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = strings["settings"] ?: "Settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        }

        // Bento Grid Navigation（根 Column 子项，Hero 下方原位）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BentoCard(
                icon = Icons.Filled.Lightbulb,
                title = strings["quickTip"] ?: "Quick Tip",
                subtitle = strings["howToPlay"] ?: "How to Play",
                onClick = { showRulesDialog = true },
                modifier = Modifier.weight(1f)
            )
            BentoCard(
                icon = Icons.Filled.Settings,
                title = strings["settings"] ?: "Settings",
                subtitle = strings["audioSettings"] ?: "Audio, Difficulty & Account",
                onClick = { onNavigate("settings") },
                modifier = Modifier.weight(1f)
            )
        }
    }

    // 温馨提示弹窗：展示游戏规则内容
    if (showRulesDialog) {
        AlertDialog(
            onDismissRequest = { showRulesDialog = false },
            title = {
                Text(
                    text = strings["howToPlay"] ?: "How to Play",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Black
                    )
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RuleLine(
                        title = strings["rule1Title"] ?: "1. Use Four Numbers",
                        desc = strings["rule1Desc"]
                            ?: "Each round gives you four random numbers. You must use every number exactly once."
                    )
                    RuleLine(
                        title = strings["rule2Title"] ?: "2. Get Exactly 24",
                        desc = strings["rule2Desc"]
                            ?: "Combine the numbers using math to reach 24. No more, no less."
                    )
                    RuleLine(
                        title = strings["rule3Title"] ?: "3. The Operators",
                        desc = strings["rule3Desc"]
                            ?: "Use basic arithmetic: Addition, Subtraction, Multiplication, and Division."
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CornerRadius.md),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "8 ÷ ( 3 − 8 ÷ 3 ) = 24",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Black
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRulesDialog = false }) {
                    Text(strings["gotIt"] ?: "Got It")
                }
            }
        )
    }
}

@Composable
private fun RuleLine(title: String, desc: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = desc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BentoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(CornerRadius.lg))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(CornerRadius.lg),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(CornerRadius.md),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}