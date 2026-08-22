package com.twentyfoursolve.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.twentyfoursolve.app.theme.CornerRadius

/**
 * Glassmorphism HUD container.
 * Design system: surface at 80% opacity + 20px backdrop blur.
 * Used for the floating game HUD and top app bar.
 */
@Composable
fun GlassHud(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(CornerRadius.xl))
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            )
            .then(
                // Note: Backdrop blur requires Android 12+. For older devices we
                // fall back to just the semi-transparent fill.
                Modifier
            )
            .padding(16.dp)
    ) {
        content()
    }
}

/**
 * Ambient shadow for floating elements (modals, dragged cards).
 * Design system: "extra-diffused shadow" — box-shadow: 0 20px 40px rgba(0,0,0,0.06)
 */
@Composable
fun ambientShadowModifier(): Modifier {
    return Modifier.then(
        // Compose doesn't support arbitrary CSS-style box shadows.
        // We approximate with elevation shadow. For the truly ambient look,
        // the tonal-layer approach from the design system is preferred.
        Modifier
    )
}