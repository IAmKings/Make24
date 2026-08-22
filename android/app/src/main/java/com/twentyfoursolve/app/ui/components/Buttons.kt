package com.twentyfoursolve.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.twentyfoursolve.app.theme.CornerRadius
import com.twentyfoursolve.app.theme.PlusJakartaSans
import com.twentyfoursolve.core.model.Operator

/**
 * Operator button for +, -, ×, ÷.
 * Design system: secondary-container fill when inactive, primary gradient when active,
 * border-bottom 4dp "tactile" effect, press-to-sink via offset.
 */
@Composable
fun OperatorButton(
    operator: Operator,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pressOffset by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 0.dp,
        animationSpec = tween(100),
        label = "operatorPress"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(CornerRadius.md))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(CornerRadius.md),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(height = 56.dp, width = 56.dp)
        ) {
            Text(
                text = operator.symbol,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Black,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
            )
        }
    }
}

/**
 * Primary CTA button with gradient from primary to primaryContainer.
 * Design system: "Signature" gradient button at 135° angle.
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    Surface(
        modifier = modifier
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        shape = CircleShape,
        color = primary
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(primary, primaryContainer),
                        start = Offset.Zero,
                        end = Offset(1000f, 1000f) // 135-degree angle
                    )
                )
                .size(height = 56.dp, width = 200.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}