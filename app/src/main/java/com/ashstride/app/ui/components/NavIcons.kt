package com.ashstride.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class NavGlyph { HOME, STREAK, SHRINE, JOURNEY, PROFILE }

/** Bottom-bar icons, drawn in code on a 26-unit grid so the app needs no icon library. */
@Composable
fun NavIcon(glyph: NavGlyph, color: Color, filled: Boolean, modifier: Modifier = Modifier, iconSize: Dp = 26.dp) {
    Canvas(modifier.size(iconSize)) {
        val s = size.minDimension / 26f
        val stroke = Stroke(width = 2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun pt(x: Float, y: Float) = Offset(x * s, y * s)
        when (glyph) {
            NavGlyph.HOME -> {
                val path = FlameShapes.silhouetteIn(3f * s, 1.5f * s, 20f * s, 23f * s)
                drawPath(path, color, style = if (filled) Fill else stroke)
            }
            NavGlyph.STREAK -> {
                drawRoundRect(
                    color = color,
                    topLeft = pt(3f, 5f),
                    size = Size(20f * s, 18f * s),
                    cornerRadius = CornerRadius(4f * s),
                    style = if (filled) Fill else stroke,
                )
                val mark = if (filled) Color(0xFF0A0C13) else color
                drawLine(mark, pt(3f, 11f), pt(23f, 11f), strokeWidth = 2f * s)
                drawLine(color, pt(8f, 2.5f), pt(8f, 7f), strokeWidth = 2f * s, cap = StrokeCap.Round)
                drawLine(color, pt(18f, 2.5f), pt(18f, 7f), strokeWidth = 2f * s, cap = StrokeCap.Round)
                drawCircle(mark, radius = 1.6f * s, center = pt(9f, 17f))
                drawCircle(mark, radius = 1.6f * s, center = pt(17f, 17f))
            }
            NavGlyph.SHRINE -> {
                val top = Path()
                top.moveTo(1.5f * s, 8.5f * s)
                top.cubicTo(8f * s, 8f * s, 18f * s, 8f * s, 24.5f * s, 8.5f * s)
                top.lineTo(23f * s, 5f * s)
                top.cubicTo(17f * s, 6.5f * s, 9f * s, 6.5f * s, 3f * s, 5f * s)
                top.close()
                drawPath(top, color, style = if (filled) Fill else stroke)
                drawLine(color, pt(5.5f, 12f), pt(20.5f, 12f), strokeWidth = 2f * s, cap = StrokeCap.Round)
                drawLine(color, pt(8f, 8.5f), pt(8f, 23f), strokeWidth = 2.2f * s, cap = StrokeCap.Round)
                drawLine(color, pt(18f, 8.5f), pt(18f, 23f), strokeWidth = 2.2f * s, cap = StrokeCap.Round)
            }
            NavGlyph.JOURNEY -> {
                val trail = Path()
                trail.moveTo(6f * s, 24f * s)
                trail.cubicTo(6f * s, 16f * s, 21f * s, 18f * s, 20f * s, 11f * s)
                drawPath(trail, color, style = Stroke(width = 2f * s, cap = StrokeCap.Round))
                drawCircle(color, radius = 3.4f * s, center = pt(20f, 6.5f), style = if (filled) Fill else stroke)
                drawCircle(color, radius = 1.7f * s, center = pt(6f, 24f))
            }
            NavGlyph.PROFILE -> {
                drawCircle(color, radius = 4.6f * s, center = pt(13f, 8.5f), style = if (filled) Fill else stroke)
                val body = Path()
                body.moveTo(4f * s, 24f * s)
                body.cubicTo(4f * s, 15f * s, 22f * s, 15f * s, 22f * s, 24f * s)
                drawPath(body, color, style = if (filled) Fill else stroke)
            }
        }
    }
}
