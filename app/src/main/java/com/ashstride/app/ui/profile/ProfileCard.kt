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
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.inset
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
import com.ashstride.app.ui.components.drawFlame
import com.ashstride.app.ui.components.flameColors
import com.ashstride.app.ui.components.rimColor
import com.ashstride.app.ui.components.tierColor
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

/** One showcase badge on the card. */
data class CardBadge(val glyph: String, val kind: BadgeKind, val rim: BadgeRim, val stitched: Boolean)

/** Everything printed on the card. Built in ProfileScreen from the same state the rest of the app uses. */
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

private val RAINBOW = listOf(
    Color(0xFFFF5E5E), Color(0xFFFFD25E), Color(0xFF7DFF9E), Color(0xFF5EE6FF),
    Color(0xFF9D7DFF), Color(0xFFFF7DE0), Color(0xFFFF5E5E),
)
private val GILT = listOf(
    Color(0xFF8A6422), Color(0xFFF7DC8A), Color(0xFFC79A38), Color(0xFFFFF3C4),
    Color(0xFFA67C2A), Color(0xFFF2CF76), Color(0xFF8A6422),
)

private fun flavor(t: Tier): String = when (t) {
    Tier.WANDERER -> "Every road starts as a single ember."
    Tier.PATHFINDER -> "The trail remembers those who keep walking."
    Tier.RANGER -> "Wind, rain, weather: all of it is just distance."
    Tier.TRAILBLAZER -> "Where you walked, others will follow."
    Tier.VANGUARD -> "The fire does not ask if you are ready."
    Tier.WARDEN -> "Keeper of the long watch and the longer road."
    Tier.PARAGON -> "The flame learned your name long ago."
    Tier.IMMORTAL -> "Even the ashes walk beside you now."
}

private fun compact(v: Long): String = when {
    v >= 1_000_000L -> String.format(java.util.Locale.US, "%.2fM", v / 1_000_000.0)
    v >= 10_000L -> String.format(java.util.Locale.US, "%.0fk", v / 1_000.0)
    else -> Fmt.n(v)
}

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
) {
    drawIntoCanvas { c ->
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val style = if (bold && italic) Typeface.BOLD_ITALIC else if (bold) Typeface.BOLD else if (italic) Typeface.ITALIC else Typeface.NORMAL
        p.typeface = Typeface.create(if (serif) Typeface.SERIF else Typeface.SANS_SERIF, style)
        p.textSize = size
        p.color = color.toArgb()
        p.textAlign = align
        if (maxW > 0f) {
            val w = p.measureText(s)
            if (w > maxW) p.textSize = size * maxW / w
        }
        c.nativeCanvas.drawText(s, x, y, p)
    }
}

private fun roundPath(l: Float, t: Float, r: Float, b: Float, rad: Float): Path {
    val p = Path()
    p.addRoundRect(RoundRect(l, t, r, b, CornerRadius(rad)))
    return p
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
        val x = cx + rr * kotlin.math.cos(a).toFloat()
        val y = cy + rr * sin(a).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    drawPath(p, color)
}

/**
 * The profile card: a gilded trading card with a live flame in its art window.
 *
 * [tiltX] and [tiltY] (-1..1) are where a finger rests or the idle sway currently points; they slide the
 * gold, the rainbow foil and the white sheen so the card looks like it catches the light. [t] (0..1, a nine
 * second loop) drives the flame, the sparkles and the foil shimmer. The higher your tier, the stronger the foil;
 * Immortal gets a full prismatic border.
 */
fun DrawScope.drawAshCard(d: CardData, tiltX: Float, tiltY: Float, t: Float) {
    val W = size.width
    val H = size.height
    val u = W / 1000f
    val tc = tierColor(d.tier)
    val rarity = d.tier.ordinal / 7f
    val foil = 0.35f + 0.65f * rarity
    val fc = flameColors(d.flame)
    val shift = (tiltX + 1f) / 2f
    val tau = (2.0 * PI).toFloat()
    val parchment = Color(0xFFFBF4EC)
    val soft = Color(0xFFD1C5B9)
    val ink = Color(0xFF1A1410)

    // 1. Gilded frame.
    drawRoundRect(
        brush = Brush.linearGradient(GILT, start = Offset(W * (shift - 0.7f), 0f), end = Offset(W * (shift + 0.7f), H), tileMode = TileMode.Mirror),
        cornerRadius = CornerRadius(48f * u),
    )
    drawRoundRect(Color.Black.copy(alpha = 0.35f), Offset(11f * u, 11f * u), Size(W - 22f * u, H - 22f * u), CornerRadius(40f * u), style = Stroke(2f * u))
    if (d.tier == Tier.IMMORTAL) {
        drawRoundRect(
            brush = Brush.linearGradient(RAINBOW, start = Offset(W * (shift - 0.5f), 0f), end = Offset(W * (shift + 0.5f), H), tileMode = TileMode.Mirror),
            topLeft = Offset(5f * u, 5f * u), size = Size(W - 10f * u, H - 10f * u), cornerRadius = CornerRadius(44f * u), style = Stroke(7f * u),
        )
    }

    // 2. Card body, tinted by the flame.
    val bodyTop = lerp(Color(0xFF1B1620), fc.outer, 0.22f)
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(bodyTop, Color(0xFF120F15)), startY = 24f * u, endY = H - 24f * u),
        topLeft = Offset(24f * u, 24f * u), size = Size(W - 48f * u, H - 48f * u), cornerRadius = CornerRadius(30f * u),
    )

    // 3. Header: name, rank line, level.
    txt(d.name, 60f * u, 106f * u, 62f * u, parchment, bold = true, maxW = 560f * u)
    txt(d.rankTitle + "  |  " + d.title, 62f * u, 150f * u, 29f * u, lerp(tc, Color.White, 0.25f), serif = false, bold = true, maxW = 600f * u)
    txt("LV", 944f * u, 78f * u, 26f * u, soft, serif = false, bold = true, align = Paint.Align.RIGHT)
    txt("${d.level}", 944f * u, 138f * u, 66f * u, Color(0xFFFFD98A), bold = true, align = Paint.Align.RIGHT)

    // 4. Art window.
    val wl = 56f * u
    val wt = 172f * u
    val wr = 944f * u
    val wb = 712f * u
    val glow = Brush.radialGradient(
        colors = listOf(lerp(Color.Black, fc.outer, 0.50f), lerp(Color.Black, fc.deep, 0.25f), Color(0xFF0C090E)),
        center = Offset(W / 2f, 520f * u), radius = 520f * u,
    )
    clipPath(roundPath(wl, wt, wr, wb, 26f * u)) {
        drawRect(glow, Offset(wl, wt), Size(wr - wl, wb - wt))
        // Deep-field specks that drift with the tilt.
        for (i in 0 until 46) {
            val fx = ((i * 7919) % 1000) / 1000f
            val fy = ((i * 104729) % 1000) / 1000f
            val x = wl + fx * (wr - wl) + tiltX * (6f + (i % 4) * 4f) * u
            val y = wt + fy * (wb - wt) + tiltY * (4f + (i % 3) * 3f) * u
            drawCircle(Color.White.copy(alpha = 0.10f + 0.25f * ((i * 13) % 10) / 10f), radius = (0.8f + (i % 3) * 0.7f) * u, center = Offset(x, y))
        }
        val sw = 0.5f + 0.5f * sin(tau * 4f * t)
        val dn = 0.5f + 0.5f * sin(tau * 7f * t)
        val fl = 0.5f + 0.5f * sin(tau * 17f * t)
        inset(256f * u, 182f * u, 256f * u, H - wb + 8f * u) {
            drawFlame(d.flame, sw, dn, fl, (t * 3f) % 1f, t, true)
        }
        // Rainbow foil, stronger with rank.
        drawRect(
            Brush.linearGradient(RAINBOW, start = Offset(W * (shift - 0.45f), wt), end = Offset(W * (shift + 0.45f), wb), tileMode = TileMode.Mirror),
            Offset(wl, wt), Size(wr - wl, wb - wt), alpha = 0.22f * foil, blendMode = BlendMode.Screen,
        )
        // Fine diagonal foil lines.
        var lx = wl - (wb - wt)
        while (lx < wr) {
            drawLine(Color.White.copy(alpha = 0.045f * foil + 0.01f), Offset(lx + tiltX * 20f * u, wb), Offset(lx + (wb - wt) + tiltX * 20f * u, wt), strokeWidth = 1.2f * u)
            lx += 16f * u
        }
        // Twinkling sparkles.
        for (i in 0 until 16) {
            val fx = ((i * 6007 + 211) % 1000) / 1000f
            val fy = ((i * 3001 + 97) % 1000) / 1000f
            val tw = max(0f, sin(tau * (t * (2 + i % 3) + i * 0.173f) + tiltX * 3f))
            val a = tw * tw * tw * (0.45f + 0.5f * foil)
            sparkle(wl + fx * (wr - wl), wt + fy * (wb - wt), (7f + (i % 4) * 3f) * u, a, Color(0xFFFFF3C4), 1.6f * u)
        }
    }
    // Window frame in gold, with a shadow line inside.
    drawPath(roundPath(wl, wt, wr, wb, 26f * u), Brush.linearGradient(GILT, Offset(W * (shift - 0.5f), wt), Offset(W * (shift + 0.5f), wb), tileMode = TileMode.Mirror), style = Stroke(6f * u))
    drawPath(roundPath(wl + 5f * u, wt + 5f * u, wr - 5f * u, wb - 5f * u, 22f * u), Color.Black.copy(alpha = 0.45f), style = Stroke(2f * u))

    // 5. Ribbon under the art.
    drawRoundRect(Color(0x44FFD98A), Offset(wl, 724f * u), Size(wr - wl, 46f * u), CornerRadius(12f * u))
    txt("${d.flameName}   |   ${Fmt.n(d.streakTotal)} day streak", W / 2f, 757f * u, 27f * u, parchment, serif = false, bold = true, align = Paint.Align.CENTER, maxW = 840f * u)

    // 6. Moves.
    val mt = 784f * u
    drawRoundRect(Color(0x33000000), Offset(wl, mt), Size(wr - wl, 306f * u), CornerRadius(20f * u))
    drawRoundRect(Color(0x33FFD98A), Offset(wl, mt), Size(wr - wl, 306f * u), CornerRadius(20f * u), style = Stroke(1.5f * u))
    val rows = listOf(
        Triple("Unbroken Streak", "Pure ${Fmt.n(d.streakPure)}  |  Best ${Fmt.n(d.streakBest)}", "${Fmt.n(d.streakTotal)}"),
        Triple("Long Road", "${compact(d.totalSteps)} steps  |  ${Fmt.n(Math.round(d.totalKm))} km  |  ${d.chapter}", "${d.journeyPct}%"),
        Triple("Shrine Trials", "Legendary ${d.legendaryWon}  |  Menders used ${d.mendersUsed}", "${d.trialsWon}"),
    )
    for ((i, r) in rows.withIndex()) {
        val ry = mt + i * 102f * u
        if (i > 0) drawLine(Color(0x33FFD98A), Offset(wl + 20f * u, ry), Offset(wr - 20f * u, ry), strokeWidth = 1.2f * u)
        for (k in 0..i) drawCircle(lerp(fc.mid, Color.White, 0.2f), radius = 11f * u, center = Offset(wl + 40f * u + k * 30f * u, ry + 52f * u))
        val tx = wl + 40f * u + (i + 1) * 30f * u + 18f * u
        txt(r.first, tx, ry + 46f * u, 38f * u, parchment, bold = true, maxW = 560f * u - (i * 30f * u))
        txt(r.second, tx, ry + 82f * u, 24f * u, soft, serif = false, maxW = 560f * u - (i * 30f * u))
        txt(r.third, wr - 28f * u, ry + 68f * u, 54f * u, Color(0xFFFFD98A), bold = true, align = Paint.Align.RIGHT)
    }

    // 7. XP bar and showcase.
    val bx = 64f * u
    val bw = 520f * u
    txt("XP  ${Fmt.n(d.totalXp)}", bx, 1136f * u, 28f * u, parchment, serif = false, bold = true)
    drawRoundRect(Color(0x55000000), Offset(bx, 1152f * u), Size(bw, 24f * u), CornerRadius(12f * u))
    val frac = if (d.xpFor <= 0L) 0f else (d.xpInto.toFloat() / d.xpFor.toFloat()).coerceIn(0f, 1f)
    if (frac > 0f) {
        drawRoundRect(
            Brush.horizontalGradient(listOf(Color(0xFFFF6B2C), Color(0xFFFFC85A)), startX = bx, endX = bx + bw),
            Offset(bx, 1152f * u), Size(bw * frac, 24f * u), CornerRadius(12f * u),
        )
    }
    txt("${Fmt.n(d.xpInto)} / ${Fmt.n(d.xpFor)} to level ${d.level + 1}", bx, 1212f * u, 24f * u, soft, serif = false)
    for (i in 0 until 3) {
        val cx = 700f * u + i * 100f * u
        val cy = 1164f * u
        val b = d.showcase.getOrNull(i)
        if (b == null) {
            drawCircle(Color(0x66FFD98A), 38f * u, Offset(cx, cy), style = Stroke(2f * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f * u, 7f * u))))
        } else {
            val rc = rimColor(b.rim)
            drawCircle(Color(0xFF2A2128), 40f * u, Offset(cx, cy))
            drawCircle(rc, 40f * u, Offset(cx, cy), style = Stroke(5f * u))
            if (b.stitched) drawCircle(Color.White.copy(alpha = 0.7f), 30f * u, Offset(cx, cy), style = Stroke(1.5f * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f * u, 5f * u))))
            if (b.kind == BadgeKind.CHALLENGE) starShape(cx, cy + 2f * u, 24f * u, rc) else txt(b.glyph, cx, cy + 11f * u, 30f * u, rc, bold = true, align = Paint.Align.CENTER, maxW = 56f * u)
        }
    }
    txt("${d.badgesEarned} badges earned", 790f * u, 1224f * u, 24f * u, soft, serif = false, align = Paint.Align.CENTER)

    // 8. Footer: the fun print.
    drawLine(Color(0x44FFD98A), Offset(wl, 1246f * u), Offset(wr, 1246f * u), strokeWidth = 1.5f * u)
    val colW = (wr - wl) / 3f
    val foot = listOf(
        Pair("weakness", "Rest day  x2"),
        Pair("resistance", "Rain  -10"),
        Pair("retreat", "${Fmt.n(d.goal)} steps"),
    )
    for ((i, f) in foot.withIndex()) {
        val cx = wl + colW * (i + 0.5f)
        txt(f.first, cx, 1280f * u, 22f * u, soft, serif = false, align = Paint.Align.CENTER)
        txt(f.second, cx, 1312f * u, 27f * u, parchment, serif = false, bold = true, align = Paint.Align.CENTER, maxW = colW - 12f * u)
    }
    txt(flavor(d.tier), W / 2f, 1348f * u, 25f * u, soft, italic = true, align = Paint.Align.CENTER, maxW = 880f * u)

    // 9. Light over everything: a sheen band that follows the tilt, plus a glint on the gold.
    clipPath(roundPath(0f, 0f, W, H, 48f * u)) {
        val c = shift
        drawRect(
            Brush.linearGradient(
                listOf(Color.Transparent, Color.White.copy(alpha = 0.10f + 0.12f * foil), Color.Transparent),
                start = Offset(W * (c - 0.45f), H * (0.5f - tiltY * 0.35f - 0.5f)),
                end = Offset(W * (c + 0.25f), H * (0.5f - tiltY * 0.35f + 0.6f)),
            ),
            blendMode = BlendMode.Screen,
        )
        for (i in 0 until 7) {
            val fx = ((i * 4001 + 33) % 1000) / 1000f
            val fy = ((i * 2003 + 71) % 1000) / 1000f
            val edge = i % 2 == 0
            val x = if (edge) (if (fx < 0.5f) 14f * u else W - 14f * u) else fx * W
            val y = if (edge) fy * H else (if (fy < 0.5f) 14f * u else H - 14f * u)
            val tw = max(0f, sin(tau * (t * (1 + i % 3) + i * 0.29f) + tiltX * 4f))
            sparkle(x, y, (6f + (i % 3) * 3f) * u, tw * tw * tw * 0.9f, Color(0xFFFFFFFF), 1.5f * u)
        }
    }
}

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
            .aspectRatio(5f / 7f)
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
        drawAshCard(d, x, y, t)
    }
}

/** Saves the card as a PNG in Pictures/Ashstride. Nothing leaves the device unless you share it. */
object ProfileCard {
    private const val PX_W = 1250
    private const val PX_H = 1750

    fun render(d: CardData): Bitmap {
        val image = ImageBitmap(PX_W, PX_H)
        val canvas = ComposeCanvas(image)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(PX_W.toFloat(), PX_H.toFloat())) {
            drawAshCard(d, 0.35f, -0.2f, 0.37f)
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
