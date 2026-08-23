package com.twentyfoursolve.app.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twentyfoursolve.app.theme.CornerRadius
import com.twentyfoursolve.app.theme.LocalStringProvider
import com.twentyfoursolve.app.theme.PlusJakartaSans
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow

/**
 * Bottom navigation bar matching the web demo's design.
 * Glassmorphism styling with active tab pill highlight.
 */
@Composable
fun BottomNavBar(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    val strings = LocalStringProvider.current

    // tab 路由直接使用 NavHost 中的实际路由；"practice" tab 对应练习配置页 practice_config
    val tabs = listOf(
        NavTab(Routes.HOME, "play", Icons.Filled.PlayArrow),
        NavTab(Routes.PRACTICE_CONFIG, "practice", Icons.Filled.FitnessCenter),
        NavTab(Routes.RULES, "rules", Icons.AutoMirrored.Filled.MenuBook),
        NavTab(Routes.STATS, "stats", Icons.Filled.BarChart)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // 背景透明 + 去掉阴影：底部栏完全透出页面统一背景，消除非圆角区的灰色色带；
            // 仅保留顶部大圆角裁剪，作为与内容区的视觉过渡。
            .clip(RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp))
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = currentTab == tab.route
                val bgColor = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(tab.route) }
                        )
                        .then(
                            if (isSelected) Modifier.background(
                                bgColor,
                                RoundedCornerShape(24.dp)
                            ) else Modifier
                        )
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = contentColor,
                        modifier = Modifier.size(if (isSelected) 22.dp else 20.dp)
                    )
                    Text(
                        text = strings[tab.labelKey] ?: tab.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = PlusJakartaSans,
                            letterSpacing = 1.5.sp
                        ),
                        color = contentColor
                    )
                }
            }
        }
    }
}

data class NavTab(
    val route: String,
    val labelKey: String,
    val icon: ImageVector,
    val label: String = labelKey.replaceFirstChar { it.uppercase() }
)