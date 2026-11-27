package com.ashstride.app.ui.profile

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import android.provider.MediaStore
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.ashstride.app.domain.BadgeKind
import com.ashstride.app.domain.BadgeRim
import com.ashstride.app.domain.FlameInfo
import com.ashstride.app.domain.Tier
import com.ashstride.app.ui.Fmt
import com.ashstride.app.ui.components.FlameShapes
import com.ashstride.app.ui.components.crystalPalette
import com.ashstride.app.ui.components.drawFlame
import com.ashstride.app.ui.components.flameTierLabel
import com.ashstride.app.ui.components.rimColor
import com.ashstride.app.ui.theme.Gilt
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** One showcase badge on the card. */
data class CardBadge(val glyph: String, val kind: BadgeKind, val rim: BadgeRim, val stitched: Boolean)

/** Everything printed on the card and the stat sheet. Built in ProfileScreen from the same state the rest of the app uses. */
data class CardData(
    val name: String,
    val title: String,
    val rankTitle: String,
    val tier: Tier,
    val level: Int,
    val xpInto: Long,
    val xpFor: Long,
    val totalXp: Long,
    val flame: FlameInfo,
    val flameName: String,
    val streakTotal: Int,
    val streakPure: Int,
    val streakBest: Int,
    val totalSteps: Long,
    val totalKm: Double,
    val bestDay: Int,
    val journeyPct: Int,
    val chapter: String,
    val trialsWon: Int,
    val legendaryWon: Int,
    val mendersUsed: Int,
    val badgesEarned: Int,
    val showcase: List<CardBadge>,
    val goal: Int,
    val daysWalked: Int,
)

/** Design size of the card face; every coordinate below is in these units and scales with the canvas. */
private const val FACE_W = 1000f
private const val FACE_H = 1400f

/** Design size of the stat panel. */
private const val PANEL_W = 1000f
private const val PANEL_H = 1060f

private const val TAU = 6.2831855f

private fun flavor(t: Tier): String = when (t) {
    Tier.WANDERER -> "Every road starts as a single ember."
    Tier.PATHFINDER -> "The trail remembers those who keep walking."
    Tier.RANGER -> "Wind, rain, weather: all of it is just distance."
    Tier.TRAILBLAZER -> "Where you walked, others will follow."
    Tier.VANGUARD -> "The fire does not ask if you are ready."
    Tier.WARDEN -> "Keeper of the long watch and the longer road."
    Tier.PARAGON -> "The fire follows wherever you lead."
    Tier.IMMORTAL -> "Even the ashes walk beside you now."
}

/** Next round lifetime-step milestone above [total], the same ladder the badges use. */
private fun nextStepGoal(total: Long): Long {
    val ladder = longArrayOf(100_000L, 250_000L, 500_000L, 1_000_000L, 2_500_000L, 5_000_000L, 10_000_000L)
    for (m in ladder) if (total < m) return m
    return (total / 5_000_000L + 1L) * 5_000_000L
}

private fun compact(v: Long): String = when {
    v >= 1_000_000L -> String.format(java.util.Locale.US, "%.1fM", v / 1_000_000.0)
    v >= 10_000L -> String.format(java.util.Locale.US, "%.0fk", v / 1_000.0)
    else -> Fmt.n(v)
}

// ---------------------------------------------------------------------------------------------------------
// Small drawing helpers
// ---------------------------------------------------------------------------------------------------------

private fun DrawScope.txt(
    s: String,
    x: Float,
    y: Float,
    size: Float,
    color: Color,
    bold: Boolean = false,
    serif: Boolean = true,
    italic: Boolean = false,
    align: Paint.Align = Paint.Align.LEFT,
    maxW: Float = 0f,
    spacing: Float = 0f,
) {
    drawIntoCanvas { c ->
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val style = if (bold && italic) Typeface.BOLD_ITALIC else if (bold) Typeface.BOLD else if (italic) Typeface.ITALIC else Typeface.NORMAL
        p.typeface = Typeface.create(if (serif) Typeface.SERIF else Typeface.SANS_SERIF, style)
        p.textSize = size
        p.letterSpacing = spacing
        p.color = color.toArgb()
        p.textAlign = align
        if (maxW > 0f) {
            val w = p.measureText(s)
            if (w > maxW) p.textSize = size * maxW / w
        }
        c.nativeCanvas.drawText(s, x, y, p)
    }
}

private fun chamfer(l: Float, t: Float, r: Float, b: Float, c: Float): Path {
    val p = Path()
    p.moveTo(l + c, t)
    p.lineTo(r - c, t)
    p.lineTo(r, t + c)
    p.lineTo(r, b - c)
    p.lineTo(r - c, b)
    p.lineTo(l + c, b)
    p.lineTo(l, b - c)
    p.lineTo(l, t + c)
    p.close()
    return p
}

/** A banner: a long hexagon with pointed ends. */
private fun plate(l: Float, t: Float, r: Float, b: Float, point: Float): Path {
    val mid = (t + b) / 2f
    val p = Path()
    p.moveTo(l, mid)
    p.lineTo(l + point, t)
    p.lineTo(r - point, t)
    p.lineTo(r, mid)
    p.lineTo(r - point, b)
    p.lineTo(l + point, b)
    p.close()
    return p
}

private fun diamondPath(cx: Float, cy: Float, r: Float): Path {
    val p = Path()
    p.moveTo(cx, cy - r * 1.25f)
    p.lineTo(cx + r, cy)
    p.lineTo(cx, cy + r * 1.25f)
    p.lineTo(cx - r, cy)
    p.close()
    return p
}

private fun DrawScope.metal(shift: Float, l: Float, t: Float, r: Float, b: Float): Brush =
    Brush.linearGradient(
        Gilt.Metal,
        start = Offset(l + (r - l) * (shift - 0.7f), t),
        end = Offset(l + (r - l) * (shift + 0.7f), b),
        tileMode = TileMode.Mirror,
    )

private fun DrawScope.diamond(cx: Float, cy: Float, r: Float, fill: Brush, u: Float) {
    val p = diamondPath(cx, cy, r)
    drawPath(p, fill)
    drawPath(p, Color.Black.copy(alpha = 0.45f), style = Stroke(1.5f * u, join = StrokeJoin.Round))
}

private fun DrawScope.sparkle(x: Float, y: Float, s: Float, a: Float, c: Color, lw: Float) {
    if (a <= 0.02f) return
    val col = c.copy(alpha = a.coerceIn(0f, 1f))
    drawLine(col, Offset(x - s, y), Offset(x + s, y), strokeWidth = lw, cap = StrokeCap.Round)
    drawLine(col, Offset(x, y - s * 1.3f), Offset(x, y + s * 1.3f), strokeWidth = lw, cap = StrokeCap.Round)
    drawCircle(col, radius = lw * 1.2f, center = Offset(x, y))
}

private fun DrawScope.starShape(cx: Float, cy: Float, r: Float, color: Color) {
    val p = Path()
    for (i in 0 until 10) {
        val rr = if (i % 2 == 0) r else r * 0.45f
        val a = -PI / 2.0 + i * PI / 5.0
        val x = cx + rr * cos(a).toFloat()
        val y = cy + rr * sin(a).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    drawPath(p, color)
}

/** A plain horizontal rule with a diamond in the middle. */
private fun DrawScope.divider(l: Float, r: Float, y: Float, u: Float, shift: Float) {
    val mid = (l + r) / 2f
    drawLine(Gilt.Line.copy(alpha = 0.85f), Offset(l, y), Offset(mid - 26f * u, y), strokeWidth = 2f * u)
    drawLine(Gilt.Line.copy(alpha = 0.85f), Offset(mid + 26f * u, y), Offset(r, y), strokeWidth = 2f * u)
    diamond(mid, y, 9f * u, metal(shift, mid - 20f * u, y - 10f * u, mid + 20f * u, y + 10f * u), u)
}

// ---------------------------------------------------------------------------------------------------------
// The card face
// ---------------------------------------------------------------------------------------------------------

/**
 * The card: a gilded frame with chamfered corners, a name plate, a rank ribbon with one star per gem of the
 * tier, an arched window holding the live flame, a plate with the flame's name and the streak, and a line of
 * flavour. Drawn into a box of [w] x [h] at the current origin (use [translate] to place it); [h] should be
 * 1.4 x [w].
 *
 * [tiltX] and [tiltY] (-1..1) are where a finger rests or the idle sway currently points; they slide the gold
 * and the sheen so the card looks like it catches the light. [t] (0..1, a nine second loop) drives the flame,
 * the sparkles and the shimmer.
 */
fun DrawScope.drawCardFace(d: CardData, tiltX: Float, tiltY: Float, t: Float, w: Float, h: Float) {
    val u = w / FACE_W
    val pal = crystalPalette(d.flame)
    val shift = (tiltX + 1f) / 2f
    val rarity = d.tier.ordinal / 7f
    val foil = 0.35f + 0.65f * rarity
    val tc = lerp(pal.rim, Color.White, 0.2f)

    // 1. Frame: metal, then the night face, then a hairline inside it.
    val outer = chamfer(0f, 0f, w, h, 46f * u)
    drawPath(outer, metal(shift, 0f, 0f, w, h))
    if (d.tier == Tier.IMMORTAL) {
        drawPath(
            outer,
            Brush.linearGradient(
                Gilt.Prism,
                start = Offset(w * (shift - 0.5f), 0f),
                end = Offset(w * (shift + 0.5f), h),
                tileMode = TileMode.Mirror,
            ),
            style = Stroke(8f * u),
        )
    }
    val face = chamfer(14f * u, 14f * u, w - 14f * u, h - 14f * u, 36f * u)
    drawPath(face, Brush.verticalGradient(listOf(Gilt.NightMid, Gilt.Night), startY = 0f, endY = h))
    drawPath(chamfer(28f * u, 28f * u, w - 28f * u, h - 28f * u, 28f * u), Gilt.Line.copy(alpha = 0.8f), style = Stroke(2f * u))
    val gem = metal(shift, 0f, 0f, 60f * u, 60f * u)
    diamond(w / 2f, 14f * u, 13f * u, gem, u)
    diamond(w / 2f, h - 14f * u, 13f * u, gem, u)
    diamond(14f * u, h * 0.5f, 11f * u, gem, u)
    diamond(w - 14f * u, h * 0.5f, 11f * u, gem, u)

    // 2. Name plate.
    val plateL = 140f * u
    val plateR = w - 140f * u
    val np = plate(plateL, 62f * u, plateR, 176f * u, 56f * u)
    drawPath(np, Brush.verticalGradient(listOf(Gilt.NightLift, Gilt.Night), startY = 62f * u, endY = 176f * u))
    drawPath(np, metal(shift, plateL, 62f * u, plateR, 176f * u), style = Stroke(5f * u, join = StrokeJoin.Round))
    txt(d.name, w / 2f, 138f * u, 74f * u, Gilt.Cream, bold = true, align = Paint.Align.CENTER, maxW = 600f * u, spacing = 0.03f)

    // 3. Rank ribbon over the bottom of the plate: tier, one star per gem, level.
    val rl = 170f * u
    val rr = w - 170f * u
    val rt = 158f * u
    val rb = 212f * u
    val ribbon = plate(rl, rt, rr, rb, 30f * u)
    drawPath(ribbon, Gilt.Night)
    drawPath(ribbon, Brush.horizontalGradient(listOf(Gilt.NightLift, Gilt.Night, Gilt.NightLift), startX = rl, endX = rr))
    drawPath(ribbon, metal(shift, rl, rt, rr, rb), style = Stroke(4f * u, join = StrokeJoin.Round))
    txt(d.rankTitle.uppercase(), rl + 52f * u, 196f * u, 30f * u, Gilt.GoldBright, bold = true, serif = false, maxW = 250f * u, spacing = 0.12f)
    val gems = ((d.level - d.tier.firstLevel) / 5).coerceIn(0, 4)
    for (i in 0 until 5) {
        val sx = w / 2f + 40f * u + (i - 2) * 36f * u
        starShape(sx, 187f * u, 13f * u, if (i <= gems) Gilt.GoldBright else Gilt.Line.copy(alpha = 0.55f))
    }
    txt("LV. ${d.level}", rr - 48f * u, 197f * u, 32f * u, Gilt.GoldBright, bold = true, serif = false, align = Paint.Align.RIGHT, maxW = 160f * u, spacing = 0.08f)

    // 4. The arched window.
    val wl = 80f * u
    val wr = w - 80f * u
    val wt = 258f * u
    val wb = 1150f * u
    val arc = 300f * u
    val arch = Path()
    arch.moveTo(wl, wb)
    arch.lineTo(wl, wt + arc)
    arch.arcTo(Rect(wl, wt, wr, wt + 2f * arc), 180f, 180f, false)
    arch.lineTo(wr, wb)
    arch.close()
    clipPath(arch) {
        drawRect(
            Brush.radialGradient(
                listOf(lerp(Color.Black, pal.glow, 0.34f), lerp(Gilt.NightMid, pal.deep, 0.30f), Gilt.Night),
                center = Offset(w / 2f, 760f * u),
                radius = 640f * u,
            ),
            Offset(wl, wt),
            Size(wr - wl, wb - wt),
        )
        // Deep field of stars that drift with the tilt.
        for (i in 0 until 56) {
            val fx = ((i * 7919) % 1000) / 1000f
            val fy = ((i * 104729) % 1000) / 1000f
            val x = wl + fx * (wr - wl) + tiltX * (6f + (i % 4) * 4f) * u
            val y = wt + fy * (wb - wt) + tiltY * (4f + (i % 3) * 3f) * u
            drawCircle(Color.White.copy(alpha = 0.10f + 0.28f * ((i * 13) % 10) / 10f), radius = (0.8f + (i % 3) * 0.7f) * u, center = Offset(x, y))
        }
        // Faint beams of light from the heart of the window.
        for (i in 0 until 5) {
            val a = (-PI / 2.0 + (i - 2) * 0.33 + sin(TAU * t) * 0.02).toFloat()
            drawLine(
                pal.glow.copy(alpha = 0.07f),
                Offset(w / 2f, 760f * u),
                Offset(w / 2f + 900f * u * cos(a), 760f * u + 900f * u * sin(a)),
                strokeWidth = 46f * u,
            )
        }
        val sw = 0.5f + 0.5f * sin(TAU * 4f * t)
        val dn = 0.5f + 0.5f * sin(TAU * 7f * t)
        val fl = 0.5f + 0.5f * sin(TAU * 17f * t)
        inset(wl + 20f * u, wt + 70f * u, w - wr + 20f * u, h - wb + 6f * u) {
            drawFlame(d.flame, sw, dn, fl, (t * 3f) % 1f, t, true)
        }
        // Soft prismatic foil, stronger with rank.
        drawRect(
            Brush.linearGradient(
                Gilt.Prism,
                start = Offset(w * (shift - 0.45f), wt),
                end = Offset(w * (shift + 0.45f), wb),
                tileMode = TileMode.Mirror,
            ),
            Offset(wl, wt),
            Size(wr - wl, wb - wt),
            alpha = 0.10f * foil,
            blendMode = BlendMode.Screen,
        )
        for (i in 0 until 16) {
            val fx = ((i * 6007 + 211) % 1000) / 1000f
            val fy = ((i * 3001 + 97) % 1000) / 1000f
            val tw = max(0f, sin(TAU * (t * (2 + i % 3) + i * 0.173f) + tiltX * 3f))
            sparkle(wl + fx * (wr - wl), wt + fy * (wb - wt), (7f + (i % 4) * 3f) * u, tw * tw * tw * (0.45f + 0.5f * foil), Color(0xFFFFF3C4), 1.6f * u)
        }
        txt(flameTierLabel(d.flame), w / 2f, wt + 62f * u, 26f * u, Gilt.Muted, bold = true, serif = false, align = Paint.Align.CENTER, spacing = 0.28f)
    }
    drawPath(arch, metal(shift, wl, wt, wr, wb), style = Stroke(9f * u, join = StrokeJoin.Round))
    drawPath(arch, Color.Black.copy(alpha = 0.55f), style = Stroke(2f * u))
    val ginner = Path()
    ginner.moveTo(wl + 12f * u, wb - 12f * u)
    ginner.lineTo(wl + 12f * u, wt + arc)
    ginner.arcTo(Rect(wl + 12f * u, wt + 12f * u, wr - 12f * u, wt + 2f * arc - 12f * u), 180f, 180f, false)
    ginner.lineTo(wr - 12f * u, wb - 12f * u)
    drawPath(ginner, Gilt.Line.copy(alpha = 0.55f), style = Stroke(1.5f * u))
    diamond(w / 2f, wt, 15f * u, gem, u)
    diamond(wl, wt + arc, 11f * u, gem, u)
    diamond(wr, wt + arc, 11f * u, gem, u)

    // 5. Flame plate: its name and the streak, hung over the foot of the window.
    val pl = 150f * u
    val pr = w - 150f * u
    val pt = 1112f * u
    val pb = 1196f * u
    val fp = plate(pl, pt, pr, pb, 44f * u)
    drawPath(fp, Brush.verticalGradient(listOf(Gilt.NightLift, Gilt.Night), startY = pt, endY = pb))
    drawPath(fp, metal(shift, pl, pt, pr, pb), style = Stroke(5f * u, join = StrokeJoin.Round))
    txt(
        d.flameName.uppercase() + "  •  " + Fmt.n(d.streakTotal) + " DAYS",
        w / 2f, 1166f * u, 37f * u, Gilt.GoldBright, bold = true, serif = false, align = Paint.Align.CENTER, maxW = 640f * u, spacing = 0.08f,
    )

    // 6. Foot: divider, flavour, worn title.
    divider(190f * u, w - 190f * u, 1244f * u, u, shift)
    txt(flavor(d.tier), w / 2f, 1296f * u, 31f * u, Gilt.Muted, italic = true, align = Paint.Align.CENTER, maxW = 820f * u)
    txt(d.title.uppercase(), w / 2f, 1350f * u, 25f * u, tc, bold = true, serif = false, align = Paint.Align.CENTER, maxW = 700f * u, spacing = 0.22f)

    // 7. Light over everything: a sheen band that follows the tilt, plus glints on the metal.
    clipPath(outer) {
        val c = shift
        drawRect(
            Brush.linearGradient(
                listOf(Color.Transparent, Color.White.copy(alpha = 0.08f + 0.10f * foil), Color.Transparent),
                start = Offset(w * (c - 0.45f), h * (0.5f - tiltY * 0.35f - 0.5f)),
                end = Offset(w * (c + 0.25f), h * (0.5f - tiltY * 0.35f + 0.6f)),
            ),
            Offset.Zero,
            Size(w, h),
            blendMode = BlendMode.Screen,
        )
        for (i in 0 until 7) {
            val fx = ((i * 4001 + 33) % 1000) / 1000f
            val fy = ((i * 2003 + 71) % 1000) / 1000f
            val edge = i % 2 == 0
            val x = if (edge) (if (fx < 0.5f) 14f * u else w - 14f * u) else fx * w
            val y = if (edge) fy * h else (if (fy < 0.5f) 14f * u else h - 14f * u)
            val tw = max(0f, sin(TAU * (t * (1 + i % 3) + i * 0.29f) + tiltX * 4f))
            sparkle(x, y, (6f + (i % 3) * 3f) * u, tw * tw * tw * 0.9f, Color.White, 1.5f * u)
        }
    }
}

// ---------------------------------------------------------------------------------------------------------
// The stat panel
// ---------------------------------------------------------------------------------------------------------

private fun DrawScope.iconCompass(cx: Float, cy: Float, r: Float, c: Color, u: Float) {
    drawCircle(c, r, Offset(cx, cy), style = Stroke(3f * u))
    val p = Path()
    p.moveTo(cx + r * 0.55f, cy - r * 0.55f)
    p.lineTo(cx + r * 0.16f, cy + r * 0.16f)
    p.lineTo(cx - r * 0.55f, cy + r * 0.55f)
    p.lineTo(cx - r * 0.16f, cy - r * 0.16f)
    p.close()
    drawPath(p, c)
}

private fun DrawScope.iconSteps(cx: Float, cy: Float, r: Float, c: Color) {
    for (k in 0..1) {
        val x = cx + (if (k == 0) -0.36f else 0.36f) * r
        val y = cy + (if (k == 0) 0.22f else -0.22f) * r
        drawOval(c, Offset(x - r * 0.24f, y - r * 0.05f), Size(r * 0.48f, r * 0.74f))
        for (j in 0..2) {
            drawCircle(c, r * 0.085f, Offset(x - r * 0.15f + j * r * 0.15f, y - r * 0.22f - (if (j == 1) r * 0.05f else 0f)))
        }
    }
}

private fun DrawScope.iconSwords(cx: Float, cy: Float, r: Float, c: Color, u: Float) {
    drawLine(c, Offset(cx - r * 0.8f, cy - r * 0.8f), Offset(cx + r * 0.8f, cy + r * 0.8f), strokeWidth = 5f * u, cap = StrokeCap.Round)
    drawLine(c, Offset(cx + r * 0.8f, cy - r * 0.8f), Offset(cx - r * 0.8f, cy + r * 0.8f), strokeWidth = 5f * u, cap = StrokeCap.Round)
    drawLine(c, Offset(cx - r * 0.55f, cy + r * 0.25f), Offset(cx - r * 0.1f, cy + r * 0.7f), strokeWidth = 4f * u, cap = StrokeCap.Round)
    drawLine(c, Offset(cx + r * 0.55f, cy + r * 0.25f), Offset(cx + r * 0.1f, cy + r * 0.7f), strokeWidth = 4f * u, cap = StrokeCap.Round)
}

private fun DrawScope.progressBar(l: Float, r: Float, y: Float, hgt: Float, frac: Float, u: Float) {
    drawRoundRect(Color(0x66000000), Offset(l, y), Size(r - l, hgt), CornerRadius(hgt / 2f))
    drawRoundRect(Gilt.Line.copy(alpha = 0.6f), Offset(l, y), Size(r - l, hgt), CornerRadius(hgt / 2f), style = Stroke(1.5f * u))
    val f = frac.coerceIn(0f, 1f)
    if (f > 0f) {
        val fw = max(hgt, (r - l) * f)
        drawRoundRect(
            Brush.horizontalGradient(listOf(Color(0xFFFF8A3D), Gilt.GoldBright), startX = l, endX = l + fw),
            Offset(l, y), Size(fw, hgt), CornerRadius(hgt / 2f),
        )
    }
}

/**
 * The stat sheet that goes with the card: a banner with the flame name and streak, then one row each for
 * streak, road, steps, trials and XP, the three showcase badges, and a footer line. Drawn into a box of
 * [w] x [h] at the current origin; [h] should be [PANEL_H] / [PANEL_W] x [w].
 */
fun DrawScope.drawStatPanel(d: CardData, t: Float, w: Float, h: Float) {
    val u = w / PANEL_W
    val pal = crystalPalette(d.flame)
    val shift = 0.5f
    val tau = TAU

    drawPath(chamfer(0f, 0f, w, h, 34f * u), Brush.verticalGradient(listOf(Gilt.NightMid, Gilt.Night), startY = 0f, endY = h))
    drawPath(chamfer(0f, 0f, w, h, 34f * u), metal(shift, 0f, 0f, w, h), style = Stroke(5f * u, join = StrokeJoin.Round))
    drawPath(chamfer(12f * u, 12f * u, w - 12f * u, h - 12f * u, 26f * u), Gilt.Line.copy(alpha = 0.5f), style = Stroke(1.5f * u))

    // Banner.
    val bl = 36f * u
    val br = w - 36f * u
    val bt = 34f * u
    val bb = 142f * u
    val banner = plate(bl, bt, br, bb, 60f * u)
    drawPath(banner, Brush.verticalGradient(listOf(Gilt.NightLift, Gilt.Night), startY = bt, endY = bb))
    clipPath(banner) {
        val gx = bl + (br - bl) * ((t * 1.6f) % 1.6f - 0.3f)
        drawRect(
            Brush.horizontalGradient(listOf(Color.Transparent, Gilt.GoldBright.copy(alpha = 0.16f), Color.Transparent), startX = gx - 90f * u, endX = gx + 90f * u),
            Offset(bl, bt), Size(br - bl, bb - bt),
        )
    }
    drawPath(banner, metal(shift, bl, bt, br, bb), style = Stroke(5f * u, join = StrokeJoin.Round))
    txt(
        d.flameName.uppercase() + "  •  " + Fmt.n(d.streakTotal) + " DAYS",
        w / 2f, 105f * u, 48f * u, Gilt.GoldBright, bold = true, serif = false, align = Paint.Align.CENTER, maxW = 780f * u, spacing = 0.07f,
    )

    // Rows.
    val rl = 36f * u
    val rr = w - 36f * u
    val rowH = 118f * u
    val rowStep = 132f * u
    val top0 = 170f * u
    val nextGoal = nextStepGoal(d.totalSteps)
    val stepFrac = d.totalSteps.toFloat() / nextGoal.toFloat()
    val xpFrac = if (d.xpFor <= 0L) 0f else d.xpInto.toFloat() / d.xpFor.toFloat()
    val labels = listOf(
        "STREAK — ${Fmt.n(d.streakTotal)}",
        "ROAD — ${Fmt.n(Math.round(d.totalKm))} KM (${d.journeyPct}%)",
        "STEPS — ${Fmt.n(d.totalSteps)}",
        "TRIALS — ${d.trialsWon} WON",
        "EXP — ${Fmt.n(d.totalXp)}",
    )
    val details = listOf(
        "PURE ${Fmt.n(d.streakPure)}  |  BEST ${Fmt.n(d.streakBest)}",
        d.chapter.uppercase(),
        "GOAL ${Fmt.n(d.goal)}",
        "LEGENDARY ${d.legendaryWon}  |  MENDERS ${d.mendersUsed}",
        "(${Fmt.n(d.xpInto)} / ${Fmt.n(d.xpFor)})",
    )
    for (i in 0 until 5) {
        val ry = top0 + i * rowStep
        val rowPath = chamfer(rl, ry, rr, ry + rowH, 18f * u)
        drawPath(rowPath, Brush.horizontalGradient(listOf(Gilt.NightLift, Gilt.NightMid), startX = rl, endX = rr))
        drawPath(rowPath, Gilt.Line.copy(alpha = 0.9f), style = Stroke(2f * u, join = StrokeJoin.Round))
        drawLine(Gilt.GoldMid.copy(alpha = 0.9f), Offset(rl + 2f * u, ry + 24f * u), Offset(rl + 2f * u, ry + rowH - 24f * u), strokeWidth = 4f * u)
        val ic = Offset(rl + 62f * u, ry + rowH / 2f)
        val gold = Gilt.GoldBright
        when (i) {
            0 -> {
                val fhh = 66f * u
                drawPath(FlameShapes.silhouetteIn(ic.x - 20f * u, ic.y - fhh / 2f, 40f * u, fhh), pal.light)
                drawPath(FlameShapes.silhouetteIn(ic.x - 20f * u, ic.y - fhh / 2f, 40f * u, fhh), pal.rim, style = Stroke(1.5f * u, join = StrokeJoin.Round))
            }
            1 -> iconCompass(ic.x, ic.y, 27f * u, gold, u)
            2 -> iconSteps(ic.x, ic.y, 32f * u, gold)
            3 -> iconSwords(ic.x, ic.y, 27f * u, gold, u)
            else -> txt("XP", ic.x, ic.y + 15f * u, 44f * u, gold, bold = true, align = Paint.Align.CENTER)
        }
        val hasBar = i == 2 || i == 4
        val textX = rl + 118f * u
        val base = if (hasBar) ry + 56f * u else ry + 72f * u
        txt(labels[i], textX, base, 42f * u, Gilt.Ink, bold = true, serif = false, maxW = 560f * u, spacing = 0.04f)
        txt(details[i], rr - 26f * u, base, 28f * u, Gilt.Muted, bold = true, serif = false, align = Paint.Align.RIGHT, maxW = 340f * u, spacing = 0.06f)
        if (hasBar) progressBar(textX, rr - 26f * u, ry + 80f * u, 16f * u, if (i == 2) stepFrac else xpFrac, u)
    }

    // Showcase medallions.
    val my = top0 + 5 * rowStep + 56f * u
    val mr = 46f * u
    for (side in 0..1) {
        val x0 = if (side == 0) 60f * u else w - 60f * u
        val x1 = if (side == 0) 270f * u else w - 270f * u
        drawLine(Gilt.Line.copy(alpha = 0.8f), Offset(x0, my), Offset(x1, my), strokeWidth = 2f * u)
        diamond(x0, my, 7f * u, metal(shift, x0 - 10f * u, my - 10f * u, x0 + 10f * u, my + 10f * u), u)
    }
    for (i in 0 until 3) {
        val cx = w / 2f + (i - 1) * 140f * u
        val b = d.showcase.getOrNull(i)
        if (b == null) {
            drawCircle(Gilt.GoldMid.copy(alpha = 0.55f), mr, Offset(cx, my), style = Stroke(2.5f * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f * u, 8f * u))))
        } else {
            val rc = rimColor(b.rim)
            drawCircle(Brush.radialGradient(listOf(Color(0xFF2B2A36), Gilt.Night), center = Offset(cx, my), radius = mr), mr, Offset(cx, my))
            drawCircle(metal(shift, cx - mr, my - mr, cx + mr, my + mr), mr, Offset(cx, my), style = Stroke(6f * u))
            drawCircle(rc, mr - 9f * u, Offset(cx, my), style = Stroke(2.5f * u))
            if (b.stitched) drawCircle(Color.White.copy(alpha = 0.6f), mr - 16f * u, Offset(cx, my), style = Stroke(1.5f * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f * u, 5f * u))))
            if (b.kind == BadgeKind.CHALLENGE) {
                starShape(cx, my + 2f * u, 24f * u, rc)
            } else {
                txt(b.glyph, cx, my + 12f * u, 32f * u, rc, bold = true, align = Paint.Align.CENTER, maxW = 58f * u)
            }
        }
    }
    txt("${d.badgesEarned} BADGES EARNED", w / 2f, my + mr + 38f * u, 22f * u, Gilt.Muted, bold = true, serif = false, align = Paint.Align.CENTER, spacing = 0.18f)

    // Footer.
    val foot = "ASHSTRIDE  •  " + flavor(d.tier).uppercase().trimEnd('.')
    txt(foot, w / 2f, h - 34f * u, 22f * u, Gilt.Muted, serif = false, align = Paint.Align.CENTER, maxW = 900f * u, spacing = 0.12f)
}

// ---------------------------------------------------------------------------------------------------------
// Composables and export
// ---------------------------------------------------------------------------------------------------------

/** The card on screen. A finger resting on it tilts the light; left alone it sways gently on its own. */
@Composable
fun AshCardView(d: CardData, modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "card")
    val t by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "t")
    val idle by inf.animateFloat(-1f, 1f, infiniteRepeatable(tween(5200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "idle")
    var touching by remember { mutableStateOf(false) }
    var touch by remember { mutableStateOf(Offset.Zero) }
    val tx by animateFloatAsState(if (touching) touch.x else 0f, spring(dampingRatio = 0.7f, stiffness = 180f), label = "tx")
    val ty by animateFloatAsState(if (touching) touch.y else 0f, spring(dampingRatio = 0.7f, stiffness = 180f), label = "ty")
    val density = LocalDensity.current.density

    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(FACE_W / FACE_H)
            .pointerInput(Unit) {
                // Watches the finger without consuming it, so the page still scrolls normally.
                awaitPointerEventScope {
                    while (true) {
                        val e = awaitPointerEvent(PointerEventPass.Initial)
                        val p = e.changes.firstOrNull()
                        if (p != null && p.pressed) {
                            touching = true
                            touch = Offset(
                                ((p.position.x / size.width) * 2f - 1f).coerceIn(-1f, 1f),
                                ((p.position.y / size.height) * 2f - 1f).coerceIn(-1f, 1f),
                            )
                        } else {
                            touching = false
                        }
                    }
                }
            }
            .graphicsLayer {
                val x = if (touching) tx else tx + idle * 0.45f
                val y = if (touching) ty else ty + idle * 0.2f
                rotationY = x * 11f
                rotationX = -y * 9f
                cameraDistance = 16f * density
            },
    ) {
        val x = if (touching) tx else tx + idle * 0.45f
        val y = if (touching) ty else ty + idle * 0.2f
        drawCardFace(d, x, y, t, size.width, size.height)
    }
}

/** The stat sheet on screen, under the card. */
@Composable
fun AshStatPanelView(d: CardData, modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "panel")
    val t by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "pt")
    Canvas(modifier.fillMaxWidth().aspectRatio(PANEL_W / PANEL_H)) {
        drawStatPanel(d, t, size.width, size.height)
    }
}

/** Saves card and stat sheet side by side as one PNG in Pictures/Ashstride. Nothing leaves the device unless you share it. */
object ProfileCard {
    private const val PX_W = 2048
    private const val PX_H = 1152

    private fun DrawScope.drawSheet(d: CardData) {
        val w = size.width
        val h = size.height
        val u = w / 2048f
        drawRect(Brush.radialGradient(listOf(Color(0xFF151927), Color(0xFF07080D)), center = Offset(w / 2f, h / 2f), radius = w * 0.7f), Offset.Zero, Size(w, h))
        drawPath(chamfer(16f * u, 16f * u, w - 16f * u, h - 16f * u, 40f * u), Gilt.Line.copy(alpha = 0.8f), style = Stroke(3f * u))
        drawPath(chamfer(30f * u, 30f * u, w - 30f * u, h - 30f * u, 34f * u), Gilt.Line.copy(alpha = 0.4f), style = Stroke(1.5f * u))
        val cardH = 1036f * u
        val cardW = cardH / 1.4f
        val top = (h - cardH) / 2f
        translate(96f * u, top) { drawCardFace(d, 0.35f, -0.2f, 0.37f, cardW, cardH) }
        val panelW = 980f * u
        val panelH = panelW * PANEL_H / PANEL_W
        translate(w - 96f * u - panelW, (h - panelH) / 2f) { drawStatPanel(d, 0.45f, panelW, panelH) }
    }

    fun render(d: CardData): Bitmap {
        val image = ImageBitmap(PX_W, PX_H)
        val canvas = ComposeCanvas(image)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(PX_W.toFloat(), PX_H.toFloat())) {
            drawSheet(d)
        }
        return image.asAndroidBitmap()
    }

    fun save(context: Context, d: CardData): Boolean {
        return try {
            val bmp = render(d)
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "ashstride_card_" + System.currentTimeMillis() + ".png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Ashstride")
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri == null) {
                false
            } else {
                resolver.openOutputStream(uri)?.use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 100, out) }
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}
