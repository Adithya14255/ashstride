package com.ashstride.app.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.ashstride.app.domain.FlameInfo
import com.ashstride.app.ui.theme.Gold
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val TAU = 6.2831855f
private const val PIF = 3.1415927f

/** Cheap deterministic noise in 0..1, used for lightning and other jitter that must not change between frames. */
private fun hash(n: Int): Float {
    var x = n * 374761393 + 668265263
    x = (x xor (x ushr 13)) * 1274126177
    x = x xor (x ushr 16)
    return (x and 0xFFFF) / 65535f
}

/** What every ornament needs to know about where and how big the flame is. Units of [fh] are "flame heights". */
private class Ctx(
    val pal: CrystalPalette,
    val st: FlameStyle,
    val cx: Float,
    val cy: Float,
    val fh: Float,
    val lw: Float,
    val pulse: Float,
    val rise: Float,
    val spin: Float,
    val sway: Float,
    val dance: Float,
    val flicker: Float,
) {
    val fw: Float get() = fh * FlameShapes.ASPECT
    fun x(v: Float): Float = cx + v * fh
    fun y(v: Float): Float = cy + v * fh
}

private class Orbit(val rx: Float, val ry: Float, phiDeg: Float, val cy0: Float) {
    private val cp = cos(Math.toRadians(phiDeg.toDouble())).toFloat()
    private val sp = sin(Math.toRadians(phiDeg.toDouble())).toFloat()
    fun px(th: Float): Float = rx * cos(th) * cp - ry * sin(th) * sp
    fun py(th: Float): Float = cy0 + rx * cos(th) * sp + ry * sin(th) * cp
}

private val WING = arrayOf(
    floatArrayOf(-0.19f, -0.08f, -0.52f, -0.40f, -0.49f, -0.22f, -0.21f, 0.10f),
    floatArrayOf(-0.21f, 0.00f, -0.52f, -0.20f, -0.52f, -0.08f, -0.22f, 0.16f),
    floatArrayOf(-0.22f, 0.10f, -0.48f, -0.02f, -0.48f, 0.07f, -0.23f, 0.22f),
    floatArrayOf(-0.23f, 0.20f, -0.40f, 0.12f, -0.41f, 0.20f, -0.24f, 0.30f),
)

private val CLAWS = arrayOf(
    floatArrayOf(-0.12f, 0.50f, -0.30f, 0.40f, -0.52f, 0.04f),
    floatArrayOf(-0.14f, 0.38f, -0.32f, 0.26f, -0.47f, -0.20f),
    floatArrayOf(-0.16f, 0.20f, -0.28f, 0.08f, -0.36f, -0.36f),
)

/** Small crystals around the base of Shadowfire: x, y, height, half width, tilt in degrees (left side; mirrored). */
private val BED = arrayOf(
    floatArrayOf(-0.34f, 0.40f, 0.14f, 0.05f, -35f),
    floatArrayOf(-0.30f, 0.52f, 0.10f, 0.04f, -60f),
    floatArrayOf(-0.40f, 0.24f, 0.12f, 0.045f, -20f),
    floatArrayOf(-0.20f, 0.56f, 0.09f, 0.035f, -15f),
)

private val BOB = floatArrayOf(0.007f, 0.016f, 0.006f, 0.006f, 0.002f)
private val TILT = floatArrayOf(-1.6f, 6f, 1.2f, -1.4f, 0f)
private val PHASE = floatArrayOf(0f, 0.35f, 0.6f, 0.15f, 0f)

private fun DrawScope.sparkle4(x: Float, y: Float, s: Float, a: Float, c: Color, lw: Float) {
    if (a <= 0.02f) return
    val col = c.copy(alpha = a.coerceIn(0f, 1f))
    drawLine(col, Offset(x - s, y), Offset(x + s, y), strokeWidth = lw, cap = StrokeCap.Round)
    drawLine(col, Offset(x, y - s * 1.4f), Offset(x, y + s * 1.4f), strokeWidth = lw, cap = StrokeCap.Round)
    drawCircle(col, radius = lw * 1.1f, center = Offset(x, y))
}

private fun kitePath(into: Path, c: Ctx, cx: Float, cy: Float, h: Float, w: Float, deg: Float) {
    val a = Math.toRadians(deg.toDouble())
    val ca = cos(a).toFloat()
    val sa = sin(a).toFloat()
    val xs = floatArrayOf(0f, w, 0f, -w)
    val ys = floatArrayOf(-h, 0f, h * 0.5f, 0f)
    into.reset()
    for (i in 0 until 4) {
        val px = cx + xs[i] * ca - ys[i] * sa
        val py = cy + xs[i] * sa + ys[i] * ca
        if (i == 0) into.moveTo(c.x(px), c.y(py)) else into.lineTo(c.x(px), c.y(py))
    }
    into.close()
}

/** A closed polygon from flat unit coordinates, mirrored sideways when [sgn] is -1. */
private fun polyPath(into: Path, c: Ctx, f: FloatArray, sgn: Float, scale: Float) {
    into.reset()
    var i = 0
    while (i < f.size) {
        val px = c.x(sgn * f[i] * scale)
        val py = c.y(f[i + 1] * scale)
        if (i == 0) into.moveTo(px, py) else into.lineTo(px, py)
        i += 2
    }
    into.close()
}

// ---------------------------------------------------------------------------------------------------------
// Ornaments behind the shards
// ---------------------------------------------------------------------------------------------------------

private fun DrawScope.rays(c: Ctx, halfWidth: Float) {
    val n = 10 + min(c.st.layers, 6) * 2
    val rMax = halfWidth * c.fh * 0.98f
    val r0 = rMax * 0.64f
    val dir = if (c.st.tier >= 8) 1f else -1f
    for (i in 0 until n) {
        val a = TAU * (i.toFloat() / n + c.spin * dir)
        val long = i % 2 == 0
        val r1 = if (long) rMax else r0 + (rMax - r0) * 0.55f
        drawLine(
            color = c.pal.glow.copy(alpha = (if (long) 0.30f else 0.16f) * c.pulse),
            start = Offset(c.cx + r0 * cos(a), c.cy + r0 * sin(a)),
            end = Offset(c.cx + r1 * cos(a), c.cy + r1 * sin(a)),
            strokeWidth = c.lw,
            cap = StrokeCap.Round,
        )
    }
}

/** Cobalt Blaze: a wide ornamental chevron under the crystal. */
private fun DrawScope.wideChevron(c: Ctx) {
    val p = Path()
    for (sgn in floatArrayOf(1f, -1f)) {
        p.reset()
        p.moveTo(c.x(0f), c.y(0.60f))
        p.lineTo(c.x(sgn * 0.46f), c.y(0.40f))
        p.lineTo(c.x(sgn * 0.40f), c.y(0.49f))
        p.lineTo(c.x(0f), c.y(0.68f))
        p.close()
        drawPath(p, Brush.horizontalGradient(listOf(c.pal.light, c.pal.deep), startX = c.x(0f), endX = c.x(sgn * 0.46f)))
        drawPath(p, c.pal.glow.copy(alpha = 0.25f * c.pulse), style = Stroke(c.lw * 5f, join = StrokeJoin.Round))
        drawPath(p, c.pal.rim, style = Stroke(c.lw * 1.4f, join = StrokeJoin.Round))
    }
}

/** Wildfire: a ring of thorns that breathes. */
private fun DrawScope.thornRing(c: Ctx) {
    val n = 26
    val rx = 0.40f
    val ry = 0.52f
    val ccy = 0.04f
    val ring = Path()
    for (i in 0..96) {
        val a = TAU * i / 96f
        val px = c.x(rx * cos(a))
        val py = c.y(ccy + ry * sin(a))
        if (i == 0) ring.moveTo(px, py) else ring.lineTo(px, py)
    }
    drawPath(ring, c.pal.glow.copy(alpha = 0.22f * c.pulse), style = Stroke(c.lw * 5f))
    drawPath(ring, c.pal.light, style = Stroke(c.lw * 1.6f))
    val thorn = Path()
    for (i in 0 until n) {
        val a = TAU * i / n
        if (sin(a) < -0.93f) continue
        val len = (0.065f + 0.055f * (0.5f + 0.5f * sin(i * 2.3f))) * (0.92f + 0.08f * sin(TAU * (c.rise + i / n.toFloat())))
        val bx = rx * cos(a)
        val by = ccy + ry * sin(a)
        var nx = cos(a) / rx
        var ny = sin(a) / ry
        val l = sqrt(nx * nx + ny * ny)
        nx /= l
        ny /= l
        val tx = -ny
        val ty = nx
        thorn.reset()
        thorn.moveTo(c.x(bx + tx * 0.020f), c.y(by + ty * 0.020f))
        thorn.lineTo(c.x(bx + nx * len + tx * 0.006f), c.y(by + ny * len + ty * 0.006f))
        thorn.lineTo(c.x(bx - tx * 0.020f), c.y(by - ty * 0.020f))
        thorn.close()
        drawPath(thorn, c.pal.base)
        drawPath(thorn, c.pal.rim.copy(alpha = 0.9f), style = Stroke(c.lw * 0.9f, join = StrokeJoin.Round))
    }
}

/** Slow translucent smoke that climbs past the flame. */
private fun DrawScope.smoke(c: Ctx, col: Color, count: Int) {
    for (i in 0 until count) {
        val ph = (c.rise * (1 + i % 2) + i * 0.23f) % 1f
        val x = c.x(sin(i * 2.7f + ph * TAU) * 0.20f)
        val y = c.y(0.46f - ph * 0.9f)
        val r = c.fh * (0.07f + 0.10f * ph)
        val a = sin(ph * PIF).coerceIn(0f, 1f) * 0.24f
        drawCircle(
            brush = Brush.radialGradient(listOf(col.copy(alpha = a), Color.Transparent), center = Offset(x, y), radius = r),
            radius = r,
            center = Offset(x, y),
        )
    }
}

/** Dragonfire: black iron claws that sweep up the sides, edged with ember light. */
private fun DrawScope.claws(c: Ctx) {
    val breath = 1f + 0.03f * (c.dance - 0.5f) * 2f
    val p = Path()
    withTransform({ scale(breath, breath, Offset(c.x(0f), c.y(0.5f))) }) {
        for (cl in CLAWS) {
            for (sgn in floatArrayOf(1f, -1f)) {
                val x0 = c.x(sgn * cl[0])
                val y0 = c.y(cl[1])
                val x1 = c.x(sgn * cl[2])
                val y1 = c.y(cl[3])
                val x2 = c.x(sgn * cl[4])
                val y2 = c.y(cl[5])
                p.reset()
                p.moveTo(x0, y0)
                p.quadTo(x1, y1, x2, y2)
                p.quadTo((x0 + x2) / 2f + sgn * c.fh * 0.02f, (y0 + y2) / 2f - c.fh * 0.02f, x0, y0)
                p.close()
                drawPath(p, Brush.linearGradient(listOf(Color(0xFF2A1612), Color(0xFF110908)), start = Offset(x2, y2), end = Offset(x0, y0)))
                drawPath(p, c.pal.glow.copy(alpha = 0.22f * c.pulse), style = Stroke(c.lw * 5f, join = StrokeJoin.Round))
                drawPath(p, c.pal.glow.copy(alpha = 0.55f + 0.35f * c.flicker), style = Stroke(c.lw * 1.2f, join = StrokeJoin.Round))
            }
        }
    }
}

/** The feather blades of the two top tiers. [count] 3 for Celestial Flame, 4 for Eternal Singularity. */
private fun DrawScope.wings(c: Ctx, count: Int, scale: Float) {
    val p = Path()
    val flap = (c.sway - 0.5f) * 2f
    for (sgn in floatArrayOf(1f, -1f)) {
        val pivot = Offset(c.x(sgn * 0.20f), c.y(0.16f))
        withTransform({ rotate(sgn * flap * 1.8f, pivot) }) {
            for (i in 0 until count) {
                val f = WING[i]
                polyPath(p, c, f, sgn, scale)
                val tip = Offset(c.x(sgn * f[2] * scale), c.y(f[3] * scale))
                val root = Offset(c.x(sgn * f[0] * scale), c.y(f[1] * scale))
                drawPath(p, Brush.linearGradient(listOf(lerp(c.pal.light, c.pal.base, i * 0.18f), c.pal.deep), start = tip, end = root))
                drawPath(p, c.pal.glow.copy(alpha = 0.20f * c.pulse), style = Stroke(c.lw * 5f, join = StrokeJoin.Round))
                drawPath(p, c.pal.trim.copy(alpha = 0.95f), style = Stroke(c.lw * 1.1f, join = StrokeJoin.Round))
            }
        }
    }
}

/** A thin gilded ring with broken inner arcs turning slowly, and (top tier) four gems on it. */
private fun DrawScope.haloRing(c: Ctx, gems: Boolean) {
    val r = c.fh * 0.60f
    val centre = Offset(c.cx, c.y(0.02f))
    drawCircle(c.pal.trim.copy(alpha = 0.50f), radius = r, center = centre, style = Stroke(c.lw))
    for (k in 0 until 3) {
        val rr = r * (0.90f - 0.06f * k)
        val start = 360f * (c.spin * (if (k % 2 == 0) 1f else -1f) + k * 0.31f)
        drawArc(
            color = c.pal.glow.copy(alpha = 0.40f - 0.08f * k),
            startAngle = start,
            sweepAngle = 70f + 30f * k,
            useCenter = false,
            topLeft = Offset(centre.x - rr, centre.y - rr),
            size = Size(rr * 2f, rr * 2f),
            style = Stroke(c.lw * 1.1f, cap = StrokeCap.Round),
        )
    }
    if (gems) {
        val p = Path()
        for (k in 0 until 4) {
            val a = TAU * k / 4f + c.spin * TAU * 0.25f
            val gx = centre.x + r * cos(a)
            val gy = centre.y + r * sin(a)
            val s = c.fh * 0.024f
            p.reset()
            p.moveTo(gx, gy - s * 1.5f)
            p.lineTo(gx + s, gy)
            p.lineTo(gx, gy + s * 1.5f)
            p.lineTo(gx - s, gy)
            p.close()
            drawPath(p, c.pal.light)
            drawPath(p, c.pal.trim, style = Stroke(c.lw))
        }
    }
}

private fun DrawScope.crown(c: Ctx, full: Boolean) {
    val p = Path()
    val spikes = if (full) 5 else 1
    val xs = floatArrayOf(0f, 0.13f, -0.13f, 0.24f, -0.24f)
    val ys = floatArrayOf(-0.62f, -0.56f, -0.56f, -0.47f, -0.47f)
    val hs = floatArrayOf(0.08f, 0.07f, 0.07f, 0.065f, 0.065f)
    val ws = floatArrayOf(0.035f, 0.03f, 0.03f, 0.028f, 0.028f)
    val ang = floatArrayOf(0f, 28f, -28f, 55f, -55f)
    for (i in 0 until spikes) {
        val dy = 0.006f * sin(TAU * (c.rise + i * 0.2f))
        kitePath(p, c, xs[i], ys[i] + dy, hs[i], ws[i], ang[i])
        drawPath(p, Brush.verticalGradient(listOf(c.pal.hot, c.pal.base), startY = c.y(ys[i] - hs[i]), endY = c.y(ys[i] + hs[i])))
        drawPath(p, c.pal.glow.copy(alpha = 0.25f * c.pulse), style = Stroke(c.lw * 4f, join = StrokeJoin.Round))
        drawPath(p, c.pal.trim, style = Stroke(c.lw, join = StrokeJoin.Round))
    }
}

private fun DrawScope.orbitArc(c: Ctx, o: Orbit, front: Boolean, doubled: Boolean) {
    val p = Path()
    val start = if (front) 0f else PIF
    for (i in 0..40) {
        val th = start + PIF * i / 40f
        val px = c.x(o.px(th))
        val py = c.y(o.py(th))
        if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
    }
    drawPath(p, c.pal.glow.copy(alpha = 0.18f * c.pulse), style = Stroke(c.lw * 6f, cap = StrokeCap.Round))
    drawPath(p, c.pal.hot.copy(alpha = 0.92f), style = Stroke(c.lw * 1.5f, cap = StrokeCap.Round))
    if (doubled) {
        val q = Path()
        for (i in 0..40) {
            val th = start + PIF * i / 40f
            val px = c.x(o.px(th) * 1.05f)
            val py = c.y(o.cy0 + (o.py(th) - o.cy0) * 1.12f)
            if (i == 0) q.moveTo(px, py) else q.lineTo(px, py)
        }
        drawPath(q, c.pal.trim.copy(alpha = 0.55f), style = Stroke(c.lw * 0.9f, cap = StrokeCap.Round))
    }
}

private fun orbitsFor(layers: Int): List<Orbit> {
    val out = ArrayList<Orbit>()
    out.add(Orbit(0.56f, 0.18f, -28f, 0.10f))
    if (layers >= 1) out.add(Orbit(0.56f, 0.18f, 28f, 0.10f))
    if (layers >= 3) out.add(Orbit(0.58f, 0.10f, 0f, 0.10f))
    return out
}

// ---------------------------------------------------------------------------------------------------------
// Ornaments in front of the shards
// ---------------------------------------------------------------------------------------------------------

private fun DrawScope.wisp(c: Ctx, u: Float, v: Float, ph: Float, big: Float) {
    val t = (c.rise + ph) % 1f
    val x0 = c.cx + (u - 0.5f) * c.fw + sin(TAU * (t + ph)) * c.fh * 0.02f
    val y0 = c.cy + (v - 0.5f) * c.fh - t * c.fh * 0.20f
    val hh = c.fh * 0.11f * big * (1f - 0.4f * t)
    val ww = c.fh * 0.028f * big
    val a = sin(t * PIF).coerceIn(0f, 1f)
    val p = Path()
    p.moveTo(x0, y0)
    p.cubicTo(x0 - ww * 1.6f, y0 - hh * 0.3f, x0 + ww * 0.2f, y0 - hh * 0.75f, x0 + ww * 0.9f, y0 - hh)
    p.cubicTo(x0 + ww * 1.5f, y0 - hh * 0.55f, x0 + ww * 1.8f, y0 - hh * 0.2f, x0, y0)
    p.close()
    drawPath(p, Brush.verticalGradient(listOf(c.pal.hot.copy(alpha = 0.9f * a), c.pal.glow.copy(alpha = 0.8f * a)), startY = y0 - hh, endY = y0))
}

/** Cobalt Blaze: a few jagged arcs that crackle round the crystal and re-roll about six times a loop. */
private fun DrawScope.lightning(c: Ctx) {
    val steps = 6
    val slot = (c.rise * steps).toInt()
    val local = (c.rise * steps) % 1f
    val fade = (1f - local).coerceIn(0f, 1f)
    val p = Path()
    for (arc in 0 until 3) {
        val seed = (slot + arc * 17) * 31 + arc
        val a0 = TAU * hash(seed)
        val sweep = (0.6f + 0.9f * hash(seed + 1)) * (if (hash(seed + 2) > 0.5f) 1f else -1f)
        p.reset()
        val segs = 6
        for (i in 0..segs) {
            val k = i.toFloat() / segs
            val a = a0 + sweep * k
            val r = 0.30f + 0.16f * hash(seed + 10 + i) + 0.05f * sin(k * PIF)
            val px = c.x(r * 0.85f * cos(a))
            val py = c.y(0.02f + r * 1.05f * sin(a))
            if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
        }
        drawPath(p, c.pal.glow.copy(alpha = 0.35f * fade), style = Stroke(c.lw * 5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(p, c.pal.hot.copy(alpha = 0.95f * fade), style = Stroke(c.lw * 1.3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Little crystals around the foot of Shadowfire. */
private fun DrawScope.bedCrystals(c: Ctx) {
    val p = Path()
    for ((i, b) in BED.withIndex()) {
        for (sgn in floatArrayOf(1f, -1f)) {
            val dy = 0.008f * sin(TAU * (c.rise + i * 0.25f + (if (sgn > 0f) 0f else 0.5f)))
            kitePath(p, c, sgn * b[0], b[1] + dy, b[2], b[3], b[4] * sgn * -1f)
            drawPath(p, Brush.verticalGradient(listOf(c.pal.light, c.pal.deep), startY = c.y(b[1] - b[2]), endY = c.y(b[1] + b[2])))
            drawPath(p, c.pal.glow.copy(alpha = 0.22f * c.pulse), style = Stroke(c.lw * 4f, join = StrokeJoin.Round))
            drawPath(p, c.pal.rim, style = Stroke(c.lw, join = StrokeJoin.Round))
        }
    }
}

// ---------------------------------------------------------------------------------------------------------
// The flame
// ---------------------------------------------------------------------------------------------------------

/**
 * Paints the flame into the whole canvas: a halo, the ornaments of the tier behind, five crystal shards that
 * drift and tilt out of step, the ornaments in front, and embers. Parameters are the loop values of the
 * caller: [sway] and [dance] and [flicker] swing 0..1 and back, [rise] and [spin] run 0..1 and restart, so
 * anything built from whole multiples of them loops without a jump.
 */
internal fun DrawScope.drawFlame(
    info: FlameInfo,
    sway: Float,
    dance: Float,
    flicker: Float,
    rise: Float,
    spin: Float,
    halo: Boolean,
) {
    val st = flameStyle(info)
    val pal = crystalPalette(info)
    val tier = st.tier

    val extW: Float
    val extTop: Float
    val extBot: Float
    if (tier <= 3) {
        extW = 0.86f; extTop = 0.64f; extBot = 0.56f
    } else if (tier <= 7) {
        extW = 1.04f; extTop = 0.66f; extBot = 0.60f
    } else {
        extW = 1.22f; extTop = 0.78f; extBot = 0.62f
    }
    val grow = 0.93f + 0.07f * st.heat
    val fh = min(size.height / (extTop + extBot), size.width / extW) * grow
    val fw = fh * FlameShapes.ASPECT
    val cx = size.width / 2f
    val cy = (size.height - (extTop + extBot) * fh) / 2f + extTop * fh
    val ox = cx - fw / 2f
    val oy = cy - fh / 2f
    val lw = max(1.dp.toPx(), fh * 0.010f)
    val pulse = 0.82f + 0.18f * flicker
    val c = Ctx(pal, st, cx, cy, fh, lw, pulse, rise, spin, sway, dance, flicker)
    val sway2 = (sway - 0.5f) * 2f
    val dance2 = (dance - 0.5f) * 2f

    val glowK = when (tier) { 0 -> 0f; 1 -> 0.45f; 2 -> 0.8f; else -> 1f }
    val heartK = when (tier) { 0 -> 0f; 1 -> 0.4f; 2 -> 0.75f; 3 -> 1f; 8, 9 -> 0.55f; else -> 0.85f }
    val shimmer = when (tier) { 0 -> 0.03f; 8, 9 -> 0.18f; else -> 0.12f }
    val centre = Offset(cx, cy)

    // 1. Halo.
    if (halo && glowK > 0f) {
        val r = fh * (if (tier >= 8) 0.95f else 0.80f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(pal.glow.copy(alpha = 0.34f * glowK * pulse), pal.base.copy(alpha = 0.10f * glowK), Color.Transparent),
                center = centre,
                radius = r,
            ),
            radius = r,
            center = centre,
        )
    }

    // 2. Ornaments behind the crystal.
    if (tier >= 3) rays(c, extW / 2f)
    when (tier) {
        4 -> wideChevron(c)
        5 -> {
            thornRing(c)
            smoke(c, pal.glow, 4)
        }
        6 -> smoke(c, Color(0xFF3A1766), 5)
        7 -> claws(c)
        8 -> {
            haloRing(c, false)
            wings(c, 3, 0.88f)
        }
        9 -> {
            haloRing(c, true)
            wings(c, 4, 1f)
        }
    }
    val orbits: List<Orbit> = if (tier == 9) orbitsFor(st.layers) else emptyList()
    for (o in orbits) orbitArc(c, o, front = false, doubled = true)

    // 3. The five shards.
    val facetPath = Path()
    val outline = Path()
    val iri = floatArrayOf(320f, 190f, 260f)
    for ((si, s) in FlameShapes.SHARDS.withIndex()) {
        val dy = BOB[si] * fh * sin(TAU * (rise + PHASE[si]))
        val tiltSource = if (si == 1) dance2 else sway2
        val pivot = Offset(ox + s.hx * fw, oy + s.hy * fh)
        withTransform({
            translate(0f, dy)
            rotate(TILT[si] * tiltSource, pivot)
        }) {
            FlameShapes.outline(outline, s, ox, oy, fw, fh)
            if (glowK > 0f) {
                drawPath(outline, pal.glow.copy(alpha = 0.07f * glowK * pulse), style = Stroke(lw * 8f, join = StrokeJoin.Round))
                drawPath(outline, pal.glow.copy(alpha = 0.12f * glowK * pulse), style = Stroke(lw * 4f, join = StrokeJoin.Round))
            }
            for (f in s.facets.indices) {
                FlameShapes.facet(facetPath, s, f, ox, oy, fw, fh)
                val wave = sin(TAU * (rise * 2f + f * 0.31f + si * 0.17f))
                val shade = (s.light[f] + shimmer * wave).coerceIn(0f, 1f)
                drawPath(facetPath, lerp(pal.deep, pal.light, shade))
                if (tier >= 8) {
                    val hue = iri[(si + f) % 3]
                    val a = 0.10f + 0.08f * sin(TAU * (rise + si * 0.13f + f * 0.07f))
                    drawPath(facetPath, Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.35f, 1f))).copy(alpha = a))
                }
                if (tier == 1) {
                    drawPath(facetPath, pal.glow.copy(alpha = 0.30f + 0.40f * flicker), style = Stroke(lw * 1.1f, join = StrokeJoin.Round))
                } else {
                    drawPath(facetPath, pal.rim.copy(alpha = if (tier == 0) 0.12f else 0.18f), style = Stroke(lw * 0.7f, join = StrokeJoin.Round))
                }
            }
            if (heartK > 0f) {
                val hr = s.hr * fh * (0.9f + 0.2f * flicker)
                val hc = Offset(ox + s.hx * fw, oy + s.hy * fh + sin(TAU * (rise + si * 0.2f)) * 0.012f * fh)
                clipPath(outline) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(
                                pal.hot.copy(alpha = (0.90f * heartK * pulse).coerceIn(0f, 1f)),
                                pal.glow.copy(alpha = 0.25f * heartK),
                                Color.Transparent,
                            ),
                            center = hc,
                            radius = hr,
                        ),
                        radius = hr,
                        center = hc,
                    )
                }
            }
            drawPath(outline, pal.rim, style = Stroke(lw * 1.5f, join = StrokeJoin.Round))
        }
    }

    // 4. A glint sweeping across the whole crystal.
    if (tier >= 2) {
        val sil = FlameShapes.silhouette(ox, oy, fw, fh)
        val sheenK = if (tier >= 8) 1f else 0.6f
        clipPath(sil) {
            val x0 = ox + fw * (rise * 1.8f - 0.55f)
            drawRect(
                brush = Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.30f * sheenK), Color.Transparent),
                    start = Offset(x0, oy),
                    end = Offset(x0 + fw * 0.55f, oy + fh * 0.30f),
                ),
                topLeft = Offset(ox, oy),
                size = Size(fw, fh),
            )
        }
    }

    // 5. The dull spark: Tier I is a stone with one living point in its hook.
    if (tier == 0) {
        val sx = ox + FlameShapes.D.hx * fw
        val sy = oy + (FlameShapes.D.hy - 0.02f) * fh
        val r = lw * (2.4f + 1.4f * flicker)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFFFFB066).copy(alpha = 0.95f * pulse), Color(0xFFFF6A1A).copy(alpha = 0.35f), Color.Transparent),
                center = Offset(sx, sy),
                radius = r * 3.2f,
            ),
            radius = r * 3.2f,
            center = Offset(sx, sy),
        )
        drawCircle(Color(0xFFFFE2B0), radius = r * 0.6f, center = Offset(sx, sy))
    }

    // 6. Ornaments in front.
    when (tier) {
        1 -> wisp(c, FlameShapes.A.pts[0], FlameShapes.A.pts[1], 0.1f, 0.7f)
        2 -> {
            wisp(c, FlameShapes.A.pts[0], FlameShapes.A.pts[1], 0.1f, 1f)
            wisp(c, FlameShapes.D.pts[0], FlameShapes.D.pts[1], 0.55f, 1f)
        }
        3 -> {
            wisp(c, FlameShapes.A.pts[0], FlameShapes.A.pts[1], 0.1f, 1.2f)
            wisp(c, FlameShapes.D.pts[0], FlameShapes.D.pts[1], 0.45f, 1.1f)
            wisp(c, FlameShapes.C.pts[0], FlameShapes.C.pts[1], 0.75f, 0.9f)
        }
        4 -> lightning(c)
        6 -> bedCrystals(c)
    }
    for (o in orbits) orbitArc(c, o, front = true, doubled = true)
    if (tier == 9 || tier == 8) crown(c, tier == 9)
    if (tier == 9) {
        for ((i, o) in orbits.withIndex()) {
            val th = TAU * (spin * (2 + i) + i * 0.37f)
            val sx = c.x(o.px(th))
            val sy = c.y(o.py(th))
            drawCircle(pal.hot, radius = lw * 1.6f, center = Offset(sx, sy))
            sparkle4(sx, sy, lw * 5f, 0.9f, pal.hot, lw * 0.9f)
        }
    }

    // 7. Twinkling glints on the sharp tips.
    if (tier == 3 || tier >= 8) {
        val tipsU = floatArrayOf(FlameShapes.A.pts[0], FlameShapes.A.pts[1], 0.73f, 0.33f, 0.69f, 0.013f, 0.02f, 0.47f)
        for (i in 0 until 4) {
            val tw = max(0f, sin(TAU * (rise * 2f + i * 0.27f)))
            val a = tw * tw * tw
            sparkle4(ox + tipsU[i * 2] * fw, oy + tipsU[i * 2 + 1] * fh, lw * (4f + i), a, pal.hot, lw * 0.9f)
        }
    }

    // 8. Mended streak: a gold thread stitched up the gap between the shards.
    if (info.hasMendSeam) {
        val seam = Path()
        val sp = FlameShapes.SEAM
        var i = 0
        while (i < sp.size) {
            val px = ox + sp[i] * fw
            val py = oy + sp[i + 1] * fh
            if (i == 0) seam.moveTo(px, py) else seam.lineTo(px, py)
            i += 2
        }
        drawPath(seam, Gold.copy(alpha = 0.28f), style = Stroke(lw * 5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(
            seam,
            Gold,
            style = Stroke(
                lw * 1.6f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(lw * 5f, lw * 3.5f), rise * lw * 8.5f),
            ),
        )
    }

    // 9. Embers drifting up, the odd four-point sparkle among them.
    val embers = if (tier == 0) 4 else min(6 + tier + (st.heat * 4f).toInt(), 22)
    val spread = if (tier >= 8) 0.80f else 0.55f
    for (i in 0 until embers) {
        val speed = 1 + (i % 2)
        val phase = (rise * speed + i * 0.137f) % 1f
        val x = cx + sin(i * 2.1f + phase * TAU) * fw * spread
        val y = cy + fh * 0.40f - phase * fh
        val r = (1.0f + (i % 3) * 0.8f) * lw * (1f - phase * 0.6f)
        val a = ((1f - phase) * (0.55f + 0.45f * flicker)).coerceIn(0f, 1f)
        val col = if (tier == 0 && i % 3 != 0) Color(0xFF8A8A90) else if (i % 3 == 0) pal.hot else pal.glow
        if (tier >= 2 && i % 4 == 0) sparkle4(x, y, r * 3f, a, col, lw * 0.7f)
        drawCircle(color = col.copy(alpha = a), radius = r, center = Offset(x, y))
    }
}
