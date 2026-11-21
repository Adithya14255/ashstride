package com.ashstride.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import com.ashstride.app.domain.FlameInfo
import com.ashstride.app.domain.FlameLook
import com.ashstride.app.ui.theme.Gold
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** The three colours of a flame: outer tongue, inner flame, ember heart. */
class FlameColors(val outer: Color, val mid: Color, val core: Color)

private fun hsv(h: Float, s: Float, v: Float): Color =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)))

private fun pal(a: Long, b: Long, c: Long) = FlameColors(Color(a), Color(b), Color(c))

fun flameColors(info: FlameInfo): FlameColors = when (info.look) {
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
        FlameColors(
            hsv(h, 0.85f, 0.95f),
            hsv((h + 30f) % 360f, 0.7f, 1f),
            hsv((h + 60f) % 360f, 0.3f, 1f),
        )
    }
}

/** Name shown under the streak number, following the colour bands in the spec. */
fun flameName(info: FlameInfo): String = when (info.look) {
    FlameLook.ASH_GREY -> "Smolder"
    FlameLook.RED -> "Red flame"
    FlameLook.DEEP_ORANGE -> "Deep orange flame"
    FlameLook.ORANGE -> "Orange flame"
    FlameLook.AMBER -> "Amber flame"
    FlameLook.GOLD -> "Gold flame"
    FlameLook.YELLOW -> "Yellow flame"
    FlameLook.WHITE_HOT -> "White-hot flame"
    FlameLook.PALE_BLUE -> "Pale blue flame"
    FlameLook.ELECTRIC_BLUE -> "Electric blue flame"
    FlameLook.CYAN -> "Cyan flame"
    FlameLook.TEAL -> "Teal flame"
    FlameLook.EMERALD -> "Emerald flame"
    FlameLook.LIME -> "Lime flame"
    FlameLook.VIOLET -> "Violet flame"
    FlameLook.PURPLE -> "Purple flame"
    FlameLook.MAGENTA -> "Magenta flame"
    FlameLook.ROSE -> "Rose flame"
    FlameLook.CRIMSON_GOLD -> "Crimson-gold flame"
    FlameLook.GOLDEN_WHITE -> "Golden-white flame"
    FlameLook.SOLAR -> "Solar flame"
    FlameLook.PRISMATIC -> "Prismatic flame"
}

/**
 * The streak flame, drawn entirely in code. New look every 5 days, slight daily growth,
 * Solar rings and sparks from 100, prismatic hue cycle from 200, gold stitched seam if the run has a mend.
 *
 * It sways from the tip (the base stays planted), the inner layers dance a little out of step with the
 * outer one, and embers drift upward. [halo] adds a soft glow behind it.
 */
@Composable
fun FlameView(info: FlameInfo, modifier: Modifier = Modifier, halo: Boolean = true) {
    val transition = rememberInfiniteTransition(label = "flame")
    val sway by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sway",
    )
    val dance by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dance",
    )
    val rise by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart),
        label = "rise",
    )

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val p = flameColors(info)
        val grow = 0.84f + 0.16f * info.growth
        val solar = info.look == FlameLook.SOLAR || info.look == FlameLook.PRISMATIC

        // Flame box: as tall as the canvas allows, never wider than it.
        var fh = h * 0.86f * grow
        var fw = fh * FlameShapes.ASPECT
        if (fw > w * 0.94f) {
            fw = w * 0.94f
            fh = fw / FlameShapes.ASPECT
        }
        val ox = (w - fw) / 2f
        val oy = h * 0.97f - fh
        val baseCentre = Offset(w / 2f, oy + fh)

        if (halo) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(p.mid.copy(alpha = 0.30f), Color.Transparent),
                    center = Offset(w / 2f, oy + fh * 0.62f),
                    radius = fh * 0.78f,
                ),
                radius = fh * 0.78f,
                center = Offset(w / 2f, oy + fh * 0.62f),
            )
        }

        if (solar) {
            val ringCount = 1 + min(info.solarLayers, 6)
            val centre = Offset(w / 2f, oy + fh * 0.58f)
            for (i in 0 until ringCount) {
                drawCircle(
                    color = p.mid.copy(alpha = 0.55f - 0.06f * i),
                    radius = fh * 0.62f + i * 5.dp.toPx(),
                    center = centre,
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
            val sparks = min(6 + info.solarLayers, 16)
            for (i in 0 until sparks) {
                val a = Math.toRadians(137.5 * i)
                val r = fh * (0.66f + 0.05f * (i % 3)) + (sway * 3.dp.toPx())
                drawCircle(
                    color = p.core.copy(alpha = 0.85f),
                    radius = (1.5f + (i % 3)).dp.toPx(),
                    center = Offset(centre.x + r * cos(a).toFloat(), centre.y + r * sin(a).toFloat()),
                )
            }
        }

        // A gentle breathing pulse, anchored at the base.
        val pulse = 1f + 0.025f * (dance - 0.5f) * 2f
        scale(1f, pulse, baseCentre) {
            drawPath(
                FlameShapes.path(FlameShapes.OUTER, ox, oy, fw, fh, lean = (sway - 0.5f) * 0.07f),
                p.outer,
            )
            drawPath(
                FlameShapes.path(FlameShapes.MID, ox, oy, fw, fh, lean = (0.5f - dance) * 0.06f),
                p.mid,
            )
            drawPath(
                FlameShapes.path(FlameShapes.CORE, ox, oy, fw, fh, lean = (sway - 0.5f) * -0.05f),
                p.core,
            )

            if (info.hasMendSeam) {
                drawPath(
                    FlameShapes.path(FlameShapes.SEAM, ox, oy, fw, fh, lean = (0.5f - dance) * 0.06f, close = false),
                    color = Gold,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
                    ),
                )
            }
        }

        // Embers drifting up past the tip. More of them as the flame grows.
        val embers = min(2 + info.stage / 4, 9)
        for (i in 0 until embers) {
            val phase = (rise + i * 0.37f) % 1f
            val x = w / 2f + sin(i * 2.1f + phase * 6f) * fw * 0.34f
            val y = oy + fh * 0.80f - phase * fh * 1.02f
            val r = (1.1f + (i % 3) * 0.7f).dp.toPx() * (1f - phase * 0.5f)
            drawCircle(color = p.core.copy(alpha = (1f - phase) * 0.9f), radius = r, center = Offset(x, y))
        }
    }
}
