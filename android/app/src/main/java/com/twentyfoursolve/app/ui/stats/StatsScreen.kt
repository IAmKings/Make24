package com.twentyfoursolve.app.ui.stats

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.twentyfoursolve.app.theme.CornerRadius
import com.twentyfoursolve.app.theme.LocalStringProvider
import com.twentyfoursolve.app.theme.PlusJakartaSans
import com.twentyfoursolve.app.theme.Spacing

@Composable
fun StatsScreen(
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val strings = LocalStringProvider.current
    val stats by viewModel.stats.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg, vertical = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        if (stats.totalGames == 0) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings["noData"] ?: "No games played yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }

        // Header
        Text(
            text = strings["performance"] ?: "Performance",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Black,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                letterSpacing = (-1).sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = strings["kineticStats"] ?: "Your kinetic playground stats.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Win Rate Large Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(CornerRadius.lg),
            color = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = strings["winRate"]?.uppercase() ?: "WIN RATE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Ring chart
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val strokeWidth = 16.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        val topLeft = Offset(
                            (size.width - diameter) / 2f,
                            (size.height - diameter) / 2f
                        )

                        // Background circle
                        drawCircle(
                            color = Color(0xFFE7E8EE),
                            radius = diameter / 2f,
                            center = center
                        )

                        // Foreground arc
                        val sweepAngle = 360f * stats.winRate
                        drawArc(
                            color = Color(0xFF0058BB),
                            startAngle = -90f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round
                            ),
                            topLeft = topLeft,
                            size = androidx.compose.ui.geometry.Size(diameter, diameter)
                        )
                    }
                    Text(
                        text = "${(stats.winRate * 100).toInt()}%",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-2).sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Small stats cards row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmallStatCard(
                icon = Icons.Filled.CalendarMonth,
                label = strings["totalGames"] ?: "Total Games",
                value = stats.totalGames.toString(),
                modifier = Modifier.weight(1f)
            )
            SmallStatCard(
                icon = Icons.Filled.Timer,
                label = strings["avgTime"] ?: "Avg Time",
                value = if (stats.avgTime > 0) "${stats.avgTime.toInt()}s" else "—",
                modifier = Modifier.weight(1f)
            )
        }

        // Streak Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(CornerRadius.lg),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings["currentStreak"]?.uppercase() ?: "CURRENT STREAK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stats.currentStreak.toString(),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        // Activity placeholder (simplified bar chart)
        ActivityChart(
            label = strings["activity"] ?: "Activity",
            subtitle = strings["last7Days"] ?: "Last 7 Days",
            values = stats.dailyWins,
            modifier = Modifier.fillMaxWidth()
        )

        // Top Solves
        if (stats.topSolves.isNotEmpty()) {
            Text(
                text = strings["topSolves"] ?: "Top Solves",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Black,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )

            stats.topSolves.take(3).forEachIndexed { idx, record ->
                TopSolveRow(
                    rank = idx + 1,
                    title = "${strings["level_format"]?.format(record.score) ?: "Level ${record.score}"}",
                    date = formatTimestamp(record.date),
                    time = "${record.timeTaken}s",
                    isBest = idx == 0
                )
            }
        }
    }
}

@Composable
private fun SmallStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(CornerRadius.lg),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(CornerRadius.md),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column {
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
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ActivityChart(
    label: String,
    subtitle: String,
    values: List<StatsUiModel.DailyWin>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(CornerRadius.lg),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                )
                Surface(
                    shape = RoundedCornerShape(CornerRadius.full),
                    color = Color.White.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Simplified bar chart
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                val days = listOf("M", "T", "W", "T", "F", "S", "S")
                val displayValues = if (values.size == 7) values
                else values.take(7) + List(7 - values.size.coerceAtMost(7)) {
                    StatsUiModel.DailyWin(0, 0)
                }

                displayValues.forEachIndexed { idx, dayData ->
                    val maxValue = displayValues.maxOfOrNull { it.wins }?.coerceAtLeast(1) ?: 1
                    val barHeight = (dayData.wins.toFloat() / maxValue * 120f).dp.coerceAtLeast(4.dp)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.height(150.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .width(24.dp)
                                .height(barHeight),
                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                            color = if (idx == displayValues.indexOf(displayValues.maxByOrNull { it.wins })) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            }
                        ) {}
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = days.getOrElse(idx) { "" },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopSolveRow(
    rank: Int,
    title: String,
    date: String,
    time: String,
    isBest: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CornerRadius.md),
        color = if (isBest) MaterialTheme.colorScheme.surfaceContainerLowest
        else MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = if (isBest) MaterialTheme.colorScheme.tertiaryContainer
                    else MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = rank.toString(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Black
                            ),
                            color = if (isBest) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black
                        )
                    )
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            Text(
                text = time,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                ),
                color = if (isBest) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatTimestamp(millis: Long): String {
    val date = java.util.Date(millis)
    return java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(date)
}