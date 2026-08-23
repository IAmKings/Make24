package com.twentyfoursolve.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap

/**
 * 四则运算符号（+ − × ÷）的 Canvas 自绘。
 *
 * 背景：字体中的数学符号按基线对齐设计，视觉中心低于几何中心，
 * 用 Text 渲染时无论 lineHeight/includeFontPadding 如何调整都无法做到
 * 视觉居中。这里改为在容器中心按几何精确绘制：符号的几何中心即容器中心，
 * 与字体度量无关，跨设备稳定居中。
 *
 * 尺寸按比例归一化（基于 Canvas 实际尺寸），symbol 支持两种减号表示
 * （ASCII "-" 与 Unicode "−"）。
 */
@Composable
fun OperatorSymbol(
    symbol: String,
    color: Color,
    modifier: Modifier = Modifier,
    /** 符号总跨度占画布尺寸的比例。 */
    glyphFraction: Float = 0.62f,
    /** 线宽占画布尺寸的比例。 */
    strokeWidthFraction: Float = 0.12f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val c = Offset(w / 2f, h / 2f)
        val half = w * glyphFraction / 2f
        val stroke = w * strokeWidthFraction

        when (symbol) {
            "+" -> {
                drawLine(color, Offset(c.x - half, c.y), Offset(c.x + half, c.y), stroke, StrokeCap.Round)
                drawLine(color, Offset(c.x, c.y - half), Offset(c.x, c.y + half), stroke, StrokeCap.Round)
            }
            "-", "−" -> {
                drawLine(color, Offset(c.x - half, c.y), Offset(c.x + half, c.y), stroke, StrokeCap.Round)
            }
            "×", "*" -> {
                drawLine(color, Offset(c.x - half, c.y - half), Offset(c.x + half, c.y + half), stroke, StrokeCap.Round)
                drawLine(color, Offset(c.x - half, c.y + half), Offset(c.x + half, c.y - half), stroke, StrokeCap.Round)
            }
            "÷", "/" -> {
                drawLine(color, Offset(c.x - half, c.y), Offset(c.x + half, c.y), stroke, StrokeCap.Round)
                val dotRadius = w * 0.075f
                val dotOffset = half * 0.85f
                drawCircle(color, dotRadius, Offset(c.x, c.y - dotOffset))
                drawCircle(color, dotRadius, Offset(c.x, c.y + dotOffset))
            }
        }
    }
}
