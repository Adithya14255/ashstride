package com.ashstride.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashstride.app.domain.LevelInfo
import com.ashstride.app.domain.Tier
import com.ashstride.app.ui.theme.Ash
import com.ashstride.app.ui.theme.AshHigh
import com.ashstride.app.ui.theme.Cinder
import com.ashstride.app.ui.theme.Ember
import com.ashstride.app.ui.theme.Gold
import com.ashstride.app.ui.theme.Smoke
import kotlin.math.cos
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

/** Daily step ring: eases up to the day's fraction and turns gold, with a soft glow, once the goal is met. */
@Composable
fun StepRing(steps: Int, goal: Int, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val target = if (goal <= 0) 0f else (steps.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
    val fraction by animateFloatAsState(target, tween(1100, easing = FastOutSlowInEasing), label = "ring")
    val track = MaterialTheme.colorScheme.surfaceVariant
    val met = goal > 0 && steps >= goal
    val fill = if (met) Gold else Ember
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 16.dp.toPx()
            val inset = stroke / 2f + 6.dp.toPx()
            val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
            drawArc(
                color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke),
            )
            if (fraction > 0f) {
                // Glow underlay, then the ring itself.
                drawArc(
                    color = fill.copy(alpha = 0.22f), startAngle = -90f, sweepAngle = 360f * fraction, useCenter = false,
                    topLeft = Offset(inset, inset), size = arcSize,
                    style = Stroke(stroke + 8.dp.toPx(), cap = StrokeCap.Round),
                )
                drawArc(
                    color = fill, startAngle = -90f, sweepAngle = 360f * fraction, useCenter = false,
                    topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

private fun polygon(cx: Float, cy: Float, r: Float, n: Int, startDeg: Double): Path {
    val p = Path()
    for (i in 0 until n) {
        val a = Math.toRadians(startDeg + 360.0 * i / n)
        val x = cx + r * cos(a).toFloat()
        val y = cy + r * sin(a).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    return p
}

private fun star(cx: Float, cy: Float, rOuter: Float, rInner: Float, points: Int, startDeg: Double): Path {
    val p = Path()
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) rOuter else rInner
        val a = Math.toRadians(startDeg + 180.0 * i / points)
        val x = cx + r * cos(a).toFloat()
        val y = cy + r * sin(a).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    return p
}

private fun shield(cx: Float, cy: Float, r: Float): Path {
    val p = Path()
    p.moveTo(cx - r * 0.9f, cy - r * 0.85f)
    p.lineTo(cx + r * 0.9f, cy - r * 0.85f)
    p.cubicTo(cx + r * 0.9f, cy + r * 0.3f, cx + r * 0.45f, cy + r * 0.8f, cx, cy + r * 1.05f)
    p.cubicTo(cx - r * 0.45f, cy + r * 0.8f, cx - r * 0.9f, cy + r * 0.3f, cx - r * 0.9f, cy - r * 0.85f)
    p.close()
    return p
}

private fun circle(cx: Float, cy: Float, r: Float): Path {
    val p = Path()
    p.addOval(Rect(cx - r, cy - r, cx + r, cy + r))
    return p
}

/** One distinct silhouette per tier, so a tier reads at a glance before you read the name. */
private fun tierShape(tier: Tier, cx: Float, cy: Float, r: Float): Path = when (tier) {
    Tier.WANDERER -> circle(cx, cy, r)
    Tier.PATHFINDER -> polygon(cx, cy, r * 1.08f, 4, -90.0)
    Tier.RANGER -> polygon(cx, cy, r, 5, -90.0)
    Tier.TRAILBLAZER -> polygon(cx, cy, r, 6, -90.0)
    Tier.VANGUARD -> polygon(cx, cy, r, 8, -67.5)
    Tier.WARDEN -> shield(cx, cy, r)
    Tier.PARAGON -> star(cx, cy, r * 1.12f, r * 0.8f, 8, -90.0)
    Tier.IMMORTAL -> star(cx, cy, r * 1.15f, r * 0.84f, 12, -90.0)
}

/**
 * Tier emblem: a shape per tier, the level in the middle, gems underneath that fill as you climb
 * through the tier, and aura rings around an Immortal.
 */
@Composable
fun TierEmblem(level: LevelInfo, modifier: Modifier = Modifier, emblemSize: Dp = 80.dp) {
    val color = tierColor(level.tier)
    val sz = emblemSize.value
    Box(modifier.size(emblemSize), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height * 0.46f
            val r = size.minDimension * 0.33f
            if (level.immortalRank > 0) {
                for (i in 1..minOf(level.immortalRank, 4)) {
                    drawCircle(
                        color = color.copy(alpha = 0.30f - 0.05f * i), radius = r * 1.2f + i * (sz * 0.05f).dp.toPx(),
                        center = Offset(cx, cy), style = Stroke(1.5.dp.toPx()),
                    )
                }
            }
            drawCircle(
                brush = Brush.radialGradient(listOf(color.copy(alpha = 0.28f), Color.Transparent), Offset(cx, cy), r * 1.7f),
                radius = r * 1.7f, center = Offset(cx, cy),
            )
            val body = tierShape(level.tier, cx, cy, r)
            drawPath(body, Brush.verticalGradient(listOf(Color(0xFF4A413A), Ash), startY = cy - r, endY = cy + r))
            drawPath(body, color, style = Stroke(width = (sz * 0.05f).dp.toPx(), join = StrokeJoin.Round))
            drawPath(
                tierShape(level.tier, cx, cy, r * 0.78f), color.copy(alpha = 0.35f),
                style = Stroke(width = 1.2.dp.toPx(), join = StrokeJoin.Round),
            )
            if (level.immortalRank == 0) {
                val gemR = (sz * 0.045f).dp.toPx()
                val gap = (sz * 0.14f).dp.toPx()
                for (g in 0 until 4) {
                    drawCircle(
                        color = if (g < level.gems) color else Cinder,
                        radius = gemR,
                        center = Offset(cx + (g - 1.5f) * gap, size.height * 0.93f),
                    )
                }
            }
        }
        Text(
            "${level.level}",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = (sz * 0.27f).sp, lineHeight = (sz * 0.3f).sp),
            modifier = Modifier.padding(bottom = (sz * 0.06f).dp),
        )
    }
}
