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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.ashstride.app.domain.FlameInfo
import com.ashstride.app.domain.FlameLook
import com.ashstride.app.ui.theme.Gold
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** The three colours of a flame: outer tongue, inner flame, ember heart. */
class FlameColors(val outer: Color, val mid: Color, val core: Color) {
    /** Darker than [outer]: the back tongues and the shadow side of the flame. */
    val deep: Color get() = lerp(outer, Color.Black, 0.5f)

    /** Between [mid] and [core]: the third layer inside the body. */
    val inner: Color get() = lerp(mid, core, 0.5f)
}

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
 * Solar corona and rings from 100, prismatic hue cycle from 200, gold stitched seam if the run has a mend.
 *
 * Effects: a slim flame with a sweeping tip and side tongues that sway out of step, nested glowing layers,
 * bright energy streaks that flow up the body, a neon edge, a pulsing halo, rising embers with the odd
 * four-point sparkle, and from 100 days a rotating corona of rays.
 */
@Composable
fun FlameView(info: FlameInfo, modifier: Modifier = Modifier, halo: Boolean = true) {
    val transition = rememberInfiniteTransition(label = "flame")
    val sway by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sway",
    )
    val dance by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dance",
    )
    val flicker by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(520, easing = LinearEasing), RepeatMode.Reverse),
        label = "flicker",
    )
    val rise by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3400, easing = LinearEasing), RepeatMode.Restart),
        label = "rise",
    )
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Restart),
        label = "spin",
    )

    Canvas(modifier) {
        drawFlame(info, sway, dance, flicker, rise, spin, halo)
    }
}

internal fun DrawScope.drawFlame(
    info: FlameInfo,
    sway: Float,
    dance: Float,
    flicker: Float,
    rise: Float,
    spin: Float,
    halo: Boolean,
) {
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
    val baseY = oy + fh
    val centre = Offset(w / 2f, oy + fh * 0.60f)
    val lw = maxOf(1.dp.toPx(), fh * 0.0085f)
    val pulse = 0.82f + 0.18f * flicker

    // Halo.
    if (halo) {
        val r = fh * 0.95f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(p.mid.copy(alpha = 0.34f * pulse), p.outer.copy(alpha = 0.10f), Color.Transparent),
                center = centre,
                radius = r,
            ),
            radius = r,
            center = centre,
        )
    }

    // Solar corona: rotating rays, rings, orbiting sparks.
    if (solar) {
        val rays = 12 + min(info.solarLayers, 6) * 2
        for (i in 0 until rays) {
            val a = Math.toRadians(i * 360.0 / rays + spin * 360.0).toFloat()
            val r0 = fh * 0.54f
            val r1 = fh * (if (i % 2 == 0) 0.76f else 0.66f)
            drawLine(
                color = p.mid.copy(alpha = 0.34f * pulse),
                start = Offset(centre.x + r0 * cos(a), centre.y + r0 * sin(a)),
                end = Offset(centre.x + r1 * cos(a), centre.y + r1 * sin(a)),
                strokeWidth = lw,
                cap = StrokeCap.Round,
            )
        }
        val ringCount = 1 + min(info.solarLayers, 6)
        for (i in 0 until ringCount) {
            drawCircle(
                color = p.mid.copy(alpha = 0.50f - 0.05f * i),
                radius = fh * 0.58f + i * 5.dp.toPx(),
                center = centre,
                style = Stroke(width = lw),
            )
        }
        val sparks = min(6 + info.solarLayers, 16)
        for (i in 0 until sparks) {
            val a = Math.toRadians(137.5 * i - spin * 720.0).toFloat()
            val r = fh * (0.68f + 0.05f * (i % 3))
            drawCircle(
                color = p.core.copy(alpha = 0.85f),
                radius = (1.5f + (i % 3)) * lw * 0.7f,
                center = Offset(centre.x + r * cos(a), centre.y + r * sin(a)),
            )
        }
    }

    // A gentle breathing pulse, anchored at the base.
    val breathe = 1f + 0.028f * (dance - 0.5f) * 2f
    val mainLean = (sway - 0.5f) * 0.10f
    scale(1f, breathe, Offset(w / 2f, baseY)) {
        // Back tongues: fewer on a young flame.
        val tongues = if (info.stage < 2) 1 else if (info.stage < 5) 2 else 3
        drawTongue(ox, oy, fw, fh, lw, pulse, 0.21f, 0.36f, 0.60f, false, (dance - 0.5f) * 0.16f, p.deep, lerp(p.deep, p.outer, 0.55f), p.mid)
        if (tongues >= 2) {
            drawTongue(ox, oy, fw, fh, lw, pulse, 0.79f, 0.40f, 0.72f, true, (0.5f - sway) * 0.14f, lerp(p.outer, p.deep, 0.55f), p.outer, p.mid)
        }
        if (tongues >= 3) {
            drawTongue(ox, oy, fw, fh, lw, pulse, 0.66f, 0.28f, 0.52f, true, (sway - 0.5f) * 0.12f, lerp(p.outer, p.deep, 0.25f), p.mid, p.inner)
        }

        // Main body.
        val left = FlameShapes.LEFT
        val right = FlameShapes.RIGHT
        val body = FlameShapes.leaf(left, right, ox, oy, fw, fh, mainLean)

        // Neon glow under the body.
        drawPath(body, p.mid.copy(alpha = 0.07f * pulse), style = Stroke(width = lw * 7f, join = StrokeJoin.Round, cap = StrokeCap.Round))
        drawPath(body, p.mid.copy(alpha = 0.11f * pulse), style = Stroke(width = lw * 4f, join = StrokeJoin.Round, cap = StrokeCap.Round))

        fun layer(a: Float, b: Float, c: Color) {
            val l = FlameShapes.mix(left, right, a, a)
            val r = FlameShapes.mix(left, right, b, b)
            val path = FlameShapes.leaf(l, r, ox, oy, fw, fh, mainLean)
            drawPath(path, Brush.verticalGradient(listOf(c, lerp(c, p.core, 0.30f)), startY = oy, endY = baseY))
        }
        drawPath(body, Brush.verticalGradient(listOf(p.outer, lerp(p.outer, p.core, 0.22f)), startY = oy, endY = baseY))
        layer(0.16f, 0.84f, p.mid)
        layer(0.33f, 0.67f, p.inner)
        layer(0.45f, 0.55f, p.core)

        // Contour lines that follow the curve of the flame, then bright streaks flowing up them.
        val ks = floatArrayOf(0.08f, 0.20f, 0.32f, 0.44f, 0.56f, 0.68f, 0.80f, 0.92f)
        val on = fh * 0.30f
        val off = fh * 0.12f
        val period = on + off
        val flow = PathEffect.dashPathEffect(floatArrayOf(on, off), period * (1f - rise))
        val calm = Brush.verticalGradient(
            listOf(p.core.copy(alpha = 0.10f), p.core.copy(alpha = 0.45f)),
            startY = oy, endY = baseY,
        )
        val bright = Brush.verticalGradient(
            listOf(p.core.copy(alpha = 0.30f), p.core.copy(alpha = 1f)),
            startY = oy, endY = baseY,
        )
        for (k in ks) {
            val chain = FlameShapes.mix(left, right, k, 0.30f + (k - 0.5f) * 0.4f)
            val line = FlameShapes.line(chain, ox, oy, fw, fh, mainLean)
            drawPath(line, calm, style = Stroke(width = lw * 0.8f, cap = StrokeCap.Round))
            drawPath(line, bright, style = Stroke(width = lw * 1.2f, cap = StrokeCap.Round, pathEffect = flow))
        }

        if (info.hasMendSeam) {
            val seam = FlameShapes.line(FlameShapes.mix(left, right, 0.5f, 0.5f), ox, oy, fw, fh, mainLean)
            drawPath(
                seam,
                color = Gold,
                style = Stroke(width = lw * 1.4f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))),
            )
        }

        // Neon edge.
        drawPath(body, p.inner.copy(alpha = 0.95f), style = Stroke(width = lw * 1.6f, join = StrokeJoin.Round))
    }

    // Embers drifting up past the tip, with the odd four-point sparkle. More of them as the flame grows.
    val embers = min(5 + info.stage / 2, 22)
    for (i in 0 until embers) {
        val speed = 1 + (i % 2)
        val phase = (rise * speed + i * 0.137f) % 1f
        val x = w / 2f + sin(i * 2.1f + phase * 6.2832f) * fw * 0.42f
        val y = baseY - fh * 0.10f - phase * fh * 1.05f
        val r = (1.0f + (i % 3) * 0.8f) * lw * (1f - phase * 0.6f)
        val a = ((1f - phase) * (0.55f + 0.45f * flicker)).coerceIn(0f, 1f)
        val c = if (i % 3 == 0) p.core else p.mid
        if (i % 4 == 0) {
            val s = r * 3.2f
            drawLine(c.copy(alpha = a), Offset(x - s, y), Offset(x + s, y), strokeWidth = lw * 0.7f, cap = StrokeCap.Round)
            drawLine(c.copy(alpha = a), Offset(x, y - s), Offset(x, y + s), strokeWidth = lw * 0.7f, cap = StrokeCap.Round)
        }
        drawCircle(color = c.copy(alpha = a), radius = r, center = Offset(x, y))
    }
}

/** One back tongue: a scaled, possibly mirrored copy of the flame body with a lighter layer inside and a glowing edge. */
private fun DrawScope.drawTongue(
    ox: Float,
    oy: Float,
    fw: Float,
    fh: Float,
    lw: Float,
    pulse: Float,
    bx: Float,
    sx: Float,
    sy: Float,
    mirror: Boolean,
    lean: Float,
    fill: Color,
    inside: Color,
    edge: Color,
) {
    val b = FlameShapes.body(bx, sx, sy, mirror)
    val outline = FlameShapes.leaf(b.left, b.right, ox, oy, fw, fh, lean)
    val top = oy + fh * (1f - sy)
    val bottom = oy + fh
    drawPath(outline, Brush.verticalGradient(listOf(fill, lerp(fill, inside, 0.35f)), startY = top, endY = bottom))
    val inner = FlameShapes.leaf(
        FlameShapes.mix(b.left, b.right, 0.2f, 0.2f),
        FlameShapes.mix(b.left, b.right, 0.8f, 0.8f),
        ox, oy, fw, fh, lean,
    )
    drawPath(inner, Brush.verticalGradient(listOf(inside, lerp(inside, edge, 0.35f)), startY = top, endY = bottom))
    drawPath(outline, edge.copy(alpha = 0.40f * pulse), style = Stroke(width = lw * 3.2f, join = StrokeJoin.Round))
    drawPath(outline, edge.copy(alpha = 0.95f), style = Stroke(width = lw * 1.1f, join = StrokeJoin.Round))
}
