package com.ashstride.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.ashstride.app.domain.LevelInfo
import com.ashstride.app.domain.Tier
import com.ashstride.app.ui.theme.AshHigh
import com.ashstride.app.ui.theme.Cinder
import com.ashstride.app.ui.theme.Ember
import com.ashstride.app.ui.theme.Gold
import com.ashstride.app.ui.theme.Smoke
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

fun tierColor(tier: Tier): Color = when (tier) {
    Tier.WANDERER -> Smoke
    Tier.PATHFINDER -> Color(0xFF8DB580)
    Tier.RANGER -> Color(0xFF5FA8D3)
    Tier.TRAILBLAZER -> Color(0xFF9B7ED8)
    Tier.VANGUARD -> Color(0xFFD98C5F)
    Tier.WARDEN -> Color(0xFFC7CDD4)
    Tier.PARAGON -> Gold
    Tier.IMMORTAL -> Color(0xFFFF6E9C)
}

/** Daily step ring: fills to the goal and turns gold once the goal is met. */
@Composable
fun StepRing(steps: Int, goal: Int, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val fraction = if (goal <= 0) 0f else (steps.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
    val track = MaterialTheme.colorScheme.surfaceVariant
    val fill = if (goal > 0 && steps >= goal) Gold else Ember
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 18.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke),
            )
            if (fraction > 0f) {
                drawArc(
                    color = fill, startAngle = -90f, sweepAngle = 360f * fraction, useCenter = false,
                    topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

/** Placeholder tier emblem (final art arrives later): hexagon in the tier colour, gems below, aura rings for Immortal. */
@Composable
fun TierEmblem(level: LevelInfo, modifier: Modifier = Modifier) {
    val color = tierColor(level.tier)
    Box(modifier.size(80.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height * 0.46f
            val r = size.minDimension * 0.36f
            if (level.immortalRank > 0) {
                for (i in 1..min(level.immortalRank, 4)) {
                    drawCircle(
                        color = color.copy(alpha = 0.28f), radius = r + i * 4.dp.toPx(),
                        center = Offset(cx, cy), style = Stroke(1.5.dp.toPx()),
                    )
                }
            }
            val hex = Path()
            for (i in 0 until 6) {
                val a = Math.toRadians(60.0 * i - 90.0)
                val px = cx + r * cos(a).toFloat()
                val py = cy + r * sin(a).toFloat()
                if (i == 0) hex.moveTo(px, py) else hex.lineTo(px, py)
            }
            hex.close()
            drawPath(hex, AshHigh)
            drawPath(hex, color, style = Stroke(width = 4.dp.toPx(), join = StrokeJoin.Round))
            for (g in 0 until 4) {
                drawCircle(
                    color = if (g < level.gems) color else Cinder,
                    radius = 3.5.dp.toPx(),
                    center = Offset(cx + (g - 1.5f) * 11.dp.toPx(), size.height * 0.93f),
                )
            }
        }
        Text("${level.level}", style = MaterialTheme.typography.titleLarge)
    }
}
