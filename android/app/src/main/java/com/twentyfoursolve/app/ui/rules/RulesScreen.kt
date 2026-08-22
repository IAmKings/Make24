package com.twentyfoursolve.app.ui.rules

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun RulesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStringProvider.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg, vertical = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        // Header
        Text(
            text = strings["howToPlay"] ?: "How to Play",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Black,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                letterSpacing = (-1).sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = strings["masterNumbers"] ?: "Master the numbers and hit the magic 24.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Rule 1
        RuleCard(
            icon = "🔢",
            title = strings["rule1Title"] ?: "1. Use Four Numbers",
            description = strings["rule1Desc"]
                ?: "Each round gives you four random numbers. You must use every number exactly once.",
            numberBadges = listOf("6", "4", "3", "2")
        )

        // Rule 2
        RuleCard(
            icon = "⭐",
            title = strings["rule2Title"] ?: "2. Get Exactly 24",
            description = strings["rule2Desc"]
                ?: "Combine the numbers using math to reach 24. No more, no less.",
            highlight = "24"
        )

        // Rule 3
        RuleCard(
            icon = "🧮",
            title = strings["rule3Title"] ?: "3. The Operators",
            description = strings["rule3Desc"]
                ?: "Use basic arithmetic: Addition, Subtraction, Multiplication, and Division.",
            operators = listOf("+", "−", "×", "÷")
        )

        // Example
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(CornerRadius.lg)),
            shape = RoundedCornerShape(CornerRadius.lg),
            color = MaterialTheme.colorScheme.primary
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = strings["exampleTitle"]?.uppercase() ?: "EXAMPLE CHALLENGE",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "8, 8, 3, 3",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Example equation
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(CornerRadius.md),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "8 ÷ ( 3 − 8 ÷ 3 ) = 24",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Black
                        ),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Back to Home
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(CornerRadius.full))
                .clickable(onClick = onBack),
            shape = RoundedCornerShape(CornerRadius.full),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = strings["backToHome"] ?: "Back to Home",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }
        }
    }
}

@Composable
private fun RuleCard(
    icon: String,
    title: String,
    description: String,
    numberBadges: List<String> = emptyList(),
    highlight: String? = null,
    operators: List<String> = emptyList()
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CornerRadius.lg)),
        shape = RoundedCornerShape(CornerRadius.lg),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(CornerRadius.md),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = icon, style = MaterialTheme.typography.headlineSmall)
                    }
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.sp
                        )
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Visual elements
            if (numberBadges.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    numberBadges.forEach { badge ->
                        Surface(
                            modifier = Modifier.size(width = 40.dp, height = 48.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = badge,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }

            if (highlight != null) {
                Text(
                    text = highlight,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Black,
                        fontSize = 64.sp,
                        letterSpacing = (-2).sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    ),
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            if (operators.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    operators.forEach { op ->
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = op,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}