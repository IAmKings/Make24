package com.twentyfoursolve.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twentyfoursolve.app.theme.CornerRadius
import com.twentyfoursolve.app.theme.PlusJakartaSans
import com.twentyfoursolve.core.model.Suit

/**
 * Hero number card component, faithfully replicating the web demo's CardUI.
 * Design system: 3:4 aspect ratio, surface-container-lowest fill, "Ghost Border" fallback,
 * press-to-sink animation, selection highlight ring.
 */
@Composable
fun NumberCard(
    value: Double,
    label: String,
    suit: Suit,
    isSelected: Boolean,
    isUsed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRed = suit == Suit.HEARTS || suit == Suit.DIAMONDS
    val suitColor = if (isRed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val suitIcon = when (suit) {
        Suit.HEARTS -> "♥"
        Suit.DIAMONDS -> "♦"
        Suit.CLUBS -> "♣"
        Suit.SPADES -> "♠"
    }

    val scaleOffset by animateDpAsState(
        targetValue = if (isUsed) 2.dp else if (isSelected) (-2).dp else 0.dp,
        animationSpec = tween(150),
        label = "cardPress"
    )

    Surface(
        modifier = modifier
            .aspectRatio(3f / 4f)
            .then(
                if (isSelected) Modifier.shadow(12.dp, RoundedCornerShape(CornerRadius.md))
                else Modifier.shadow(4.dp, RoundedCornerShape(CornerRadius.md))
            )
            .offset(y = scaleOffset)
            .clip(RoundedCornerShape(CornerRadius.md))
            .clickable(enabled = !isUsed, onClick = onClick),
        shape = RoundedCornerShape(CornerRadius.md),
        color = if (isUsed) MaterialTheme.colorScheme.surfaceDim
        else MaterialTheme.colorScheme.surfaceContainerLowest,
        border = if (isSelected) {
            // Selection ring: primary color border
            androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
        } else {
            // Ghost border fallback: outlineVariant at ~15% opacity
            androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            // Top-left corner
            Column(
                modifier = Modifier.align(Alignment.TopStart),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Black,
                        color = if (isUsed) suitColor.copy(alpha = 0.3f) else suitColor
                    )
                )
                Text(
                    text = suitIcon,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isUsed) suitColor.copy(alpha = 0.3f) else suitColor
                    )
                )
            }

            // Center value (large hero)
            Text(
                text = label,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Black,
                    fontSize = 56.sp,
                    lineHeight = 64.sp,
                    color = if (isUsed) suitColor.copy(alpha = 0.3f) else suitColor
                ),
                textAlign = TextAlign.Center
            )

            // Bottom-right corner (rotated)
            Column(
                modifier = Modifier.align(Alignment.BottomEnd),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Black,
                        color = if (isUsed) suitColor.copy(alpha = 0.3f) else suitColor
                    )
                )
                Text(
                    text = suitIcon,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isUsed) suitColor.copy(alpha = 0.3f) else suitColor
                    )
                )
            }
        }
    }
}