package com.ashstride.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashstride.app.domain.BadgeKind
import com.ashstride.app.domain.BadgeRim
import com.ashstride.app.ui.theme.AshHigh
import com.ashstride.app.ui.theme.Bronze
import com.ashstride.app.ui.theme.Cinder
import com.ashstride.app.ui.theme.Gold
import com.ashstride.app.ui.theme.Parchment
import com.ashstride.app.ui.theme.Silver
import com.ashstride.app.ui.theme.Smoke
import kotlin.math.cos
import kotlin.math.sin

private val PRISM = listOf(
    Color(0xFFFF5252), Color(0xFFFFD740), Color(0xFF69F0AE),
    Color(0xFF40C4FF), Color(0xFFE040FB), Color(0xFFFF5252),
)

private fun rimBrush(rim: BadgeRim, centre: Offset): Brush = when (rim) {
    BadgeRim.BRONZE -> SolidColor(Bronze)
    BadgeRim.SILVER -> SolidColor(Silver)
    BadgeRim.GOLD -> SolidColor(Gold)
    BadgeRim.PRISMATIC -> Brush.sweepGradient(PRISM, centre)
}

fun rimColor(rim: BadgeRim): Color = when (rim) {
    BadgeRim.BRONZE -> Bronze
    BadgeRim.SILVER -> Silver
    BadgeRim.GOLD -> Gold
    BadgeRim.PRISMATIC -> Color(0xFFE040FB)
}

private fun starPath(cx: Float, cy: Float, rOuter: Float, rInner: Float): Path {
    val p = Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) rOuter else rInner
        val a = Math.toRadians(-90.0 + 36.0 * i)
        val x = cx + r * cos(a).toFloat()
        val y = cy + r * sin(a).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    return p
}

/**
 * Badge art drawn in code. The rim says how it was earned: gold Pure, silver stitched Mended, bronze Lifetime,
 * and bronze / silver / gold / prismatic for Easy / Medium / Hard / Legendary challenges.
 * The centre shows a flame (streaks), a pair of peaks (lifetime steps) or a star (challenges).
 */
@Composable
fun BadgeArt(
    kind: BadgeKind,
    rim: BadgeRim,
    glyph: String,
    stitched: Boolean,
    modifier: Modifier = Modifier,
    locked: Boolean = false,
    badgeSize: Dp = 56.dp,
) {
    val sz = badgeSize.value
    Box(modifier.size(badgeSize), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension / 2f - 3.dp.toPx()
            val art = if (locked) Cinder else rimColor(rim)
            val rimStroke: Brush = if (locked) SolidColor(Cinder) else rimBrush(rim, c)

            drawCircle(
                brush = Brush.radialGradient(listOf(Color(0xFF2B3042), AshHigh), c, r),
                radius = r, center = c,
            )
            drawCircle(brush = rimStroke, radius = r, center = c, style = Stroke(width = (sz * 0.07f).dp.toPx()))
            if (stitched && !locked) {
                drawCircle(
                    color = Silver.copy(alpha = 0.85f), radius = r - (sz * 0.11f).dp.toPx(), center = c,
                    style = Stroke(width = 1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))),
                )
            }

            when (kind) {
                BadgeKind.STREAK -> {
                    val fh = r * 1.05f
                    val fw = fh * FlameShapes.ASPECT
                    drawPath(
                        FlameShapes.silhouette(c.x - fw / 2f, c.y - r * 0.60f - fh * 0.20f, fw, fh),
                        art.copy(alpha = if (locked) 0.5f else 0.95f),
                    )
                }
                BadgeKind.STEPS -> {
                    val base = c.y - r * 0.12f
                    val peaks = Path()
                    peaks.moveTo(c.x - r * 0.55f, base)
                    peaks.lineTo(c.x - r * 0.18f, base - r * 0.55f)
                    peaks.lineTo(c.x + 0.02f * r, base - r * 0.18f)
                    peaks.lineTo(c.x + r * 0.22f, base - r * 0.7f)
                    peaks.lineTo(c.x + r * 0.58f, base)
                    peaks.close()
                    drawPath(peaks, art.copy(alpha = if (locked) 0.5f else 0.95f))
                }
                BadgeKind.CHALLENGE -> {
                    val star = starPath(c.x, c.y + r * 0.04f, r * 0.62f, r * 0.27f)
                    if (locked) {
                        drawPath(star, Cinder)
                    } else {
                        drawPath(star, rimStroke)
                        drawPath(star, Color(0x33000000), style = Stroke(width = 1.dp.toPx(), join = StrokeJoin.Round))
                    }
                }
            }
        }
        if (glyph.isNotEmpty()) {
            Text(
                glyph,
                color = if (locked) Smoke else Parchment,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = (sz * 0.25f).sp,
                    lineHeight = (sz * 0.28f).sp,
                ),
                modifier = Modifier.padding(top = (sz * 0.40f).dp),
            )
        }
    }
}
