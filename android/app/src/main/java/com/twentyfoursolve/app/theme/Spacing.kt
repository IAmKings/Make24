package com.twentyfoursolve.app.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design system spacing tokens following "The Kinetic Playground" principles.
 */
object Spacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
    val xxl: Dp = 48.dp
    val xxxl: Dp = 64.dp
}

/**
 * Corner radius tokens.
 */
object CornerRadius {
    val sm: Dp = 8.dp
    val md: Dp = 24.dp
    val lg: Dp = 32.dp
    val xl: Dp = 48.dp
    val full: Dp = 9999.dp
}

/**
 * Elevation / shadow tokens following "tonal layering" principle.
 * Prefer surface color shifts over drop shadows.
 */
object Elevation {
    val none: Dp = 0.dp
    val sm: Dp = 2.dp
    val md: Dp = 4.dp
    val lg: Dp = 8.dp
}