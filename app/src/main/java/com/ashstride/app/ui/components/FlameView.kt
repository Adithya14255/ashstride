package com.ashstride.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.ashstride.app.domain.FlameInfo
import com.ashstride.app.domain.FlameLook
import com.ashstride.app.ui.theme.Gold
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private class Palette(val outer: Color, val mid: Color, val core: Color)

private fun hsv(h: Float, s: Float, v: Float): Color =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)))

private fun pal(a: Long, b: Long, c: Long) = Palette(Color(a), Color(b), Color(c))

private fun paletteFor(info: FlameInfo): Palette = when (info.look) {
    FlameLook.ASH_GREY -> pal(0xFF6E655E, 0xFF8F847B, 0xFFE8581C)
    FlameLook.RED -> pal(0xFFB3261E, 0xFFE0402A, 0xFFFF8A65)
    FlameLook.DEEP_ORANGE -> pal(0xFFD84315, 0xFFF4511E, 0xFFFFAB91)
    FlameLook.ORANGE -> pal(0xFFE8581C, 0xFFFF8F2B, 0xFFFFD08A)
    FlameLook.AMBER -> pal(0xFFF08A00, 0xFFFFB300, 0xFFFFE082)
    FlameLook.GOLD -> pal(0xFFE0A100, 0xFFF2B544, 0xFFFFF0B3)
    FlameLook.YELLOW -> pal(0xFFF2C400, 0xFFFFE04D, 0xFFFFFBD1)
    FlameLook.WHITE_HOT -> pal(0xFFF3E9D2, 0xFFFFF8E6, 0xFFFFFFFF)
    FlameLook.PALE_BLUE -> pal(0xFF8EC5E8, 0xFFBFE0F5, 0xFFF2FAFF)
    FlameLook.ELECTRIC_BLUE -> pal(0xFF1E6BFF, 0xFF4D94FF, 0xFFCFE2FF)
    FlameLook.CYAN -> pal(0xFF00B8D4, 0xFF4DD0E1, 0xFFD6F7FB)
    FlameLook.TEAL -> pal(0xFF00897B, 0xFF26A69A, 0xFFC8F1EC)
    FlameLook.EMERALD -> pal(0xFF1B9E4B, 0xFF3DDC84, 0xFFD2F8E0)
    FlameLook.LIME -> pal(0xFF8BC34A, 0xFFB5E04B, 0xFFEBF9C9)
    FlameLook.VIOLET -> pal(0xFF6A3DE8, 0xFF8E6BFF, 0xFFDCD0FF)
    FlameLook.PURPLE -> pal(0xFF8E24AA, 0xFFBA68C8, 0xFFEBCFF2)
    FlameLook.MAGENTA -> pal(0xFFC2185B, 0xFFEC407A, 0xFFF8BBD0)
    FlameLook.ROSE -> pal(0xFFE57394, 0xFFF4A3B8, 0xFFFCE4EC)
    FlameLook.CRIMSON_GOLD -> pal(0xFFB71C1C, 0xFFE8A317, 0xFFFFE9A8)
    FlameLook.GOLDEN_WHITE -> pal(0xFFF2B544, 0xFFFFE9A8, 0xFFFFFFF0)
    FlameLook.SOLAR -> pal(0xFFFFB300, 0xFFFFD54F, 0xFFFFFDE7)
    FlameLook.PRISMATIC -> {
        val h = info.hueShiftDegrees.toFloat()
        Palette(
            hsv(h, 0.85f, 0.95f),
            hsv((h + 30f) % 360f, 0.7f, 1f),
            hsv((h + 60f) % 360f, 0.3f, 1f),
        )
    }
}

/** A teardrop flame in unit coordinates, scaled about its base centre. */
private fun flamePath(scale: Float, ox: Float, oy: Float, w: Float, h: Float): Path {
    fun x(u: Float) = ox + (0.5f + (u - 0.5f) * scale) * w
    fun y(v: Float) = oy + (1f - (1f - v) * scale) * h
    return Path().apply {
        moveTo(x(0.5f), y(0f))
        cubicTo(x(0.62f), y(0.22f), x(0.95f), y(0.45f), x(0.88f), y(0.70f))
        cubicTo(x(0.82f), y(0.92f), x(0.65f), y(1f), x(0.5f), y(1f))
        cubicTo(x(0.35f), y(1f), x(0.18f), y(0.92f), x(0.12f), y(0.70f))
        cubicTo(x(0.05f), y(0.45f), x(0.38f), y(0.22f), x(0.5f), y(0f))
        close()
    }
}

/**
 * The streak flame, drawn entirely in code. New look every 5 days, slight daily growth,
 * Solar rings and sparks from 100, prismatic hue cycle from 200, gold stitched seam if the run has a mend.
 */
@Composable
fun FlameView(info: FlameInfo, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "flame")
    val sway by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sway",
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val p = paletteFor(info)
        val grow = 0.82f + 0.18f * info.growth
        val flick = 1f + 0.03f * sway
        val solar = info.look == FlameLook.SOLAR || info.look == FlameLook.PRISMATIC

        if (solar) {
            val ringCount = 1 + min(info.solarLayers, 6)
            val centre = Offset(w / 2f, h * 0.58f)
            for (i in 0 until ringCount) {
                drawCircle(
                    color = p.mid.copy(alpha = 0.55f - 0.06f * i),
                    radius = h * 0.40f + i * 5.dp.toPx(),
                    center = centre,
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
            val sparks = min(6 + info.solarLayers, 16)
            for (i in 0 until sparks) {
                val a = Math.toRadians(137.5 * i)
                val r = h * (0.44f + 0.05f * (i % 3)) + (sway * 3.dp.toPx())
                drawCircle(
                    color = p.core.copy(alpha = 0.85f),
                    radius = (1.5f + (i % 3)).dp.toPx(),
                    center = Offset(centre.x + r * cos(a).toFloat(), centre.y + r * sin(a).toFloat()),
                )
            }
        }

        val fw = w * 0.62f * grow
        val fh = h * 0.86f * grow * flick
        val ox = (w - fw) / 2f
        val oy = h * 0.96f - fh
        drawPath(flamePath(1f, ox, oy, fw, fh), p.outer)
        drawPath(flamePath(0.66f, ox, oy, fw, fh), p.mid)
        drawPath(flamePath(0.34f, ox, oy, fw, fh), p.core)

        if (info.hasMendSeam) {
            val seam = Path().apply {
                moveTo(w / 2f, oy + fh * 0.98f)
                cubicTo(w / 2f - fw * 0.07f, oy + fh * 0.70f, w / 2f + fw * 0.07f, oy + fh * 0.42f, w / 2f, oy + fh * 0.12f)
            }
            drawPath(
                seam,
                color = Gold,
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
                ),
            )
        }
    }
}
