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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.ashstride.app.domain.FlameInfo
import com.ashstride.app.domain.FlameLook

/** The three colours other screens tint themselves with: crystal body, glow, white-hot heart. */
class FlameColors(val outer: Color, val mid: Color, val core: Color) {
    /** Darker than [outer]. */
    val deep: Color get() = lerp(outer, Color.Black, 0.5f)

    /** Between [mid] and [core]. */
    val inner: Color get() = lerp(mid, core, 0.5f)
}

/**
 * Everything one flame tier is painted with. [deep] is the shadow side of a facet and [light] the lit side,
 * [hot] is the heart of the crystal, [glow] the halo and neon, [rim] the bright edge, [trim] the gold used on
 * the ornaments of the top tiers.
 */
class CrystalPalette(
    val deep: Color,
    val base: Color,
    val light: Color,
    val hot: Color,
    val glow: Color,
    val rim: Color,
    val trim: Color,
)

/**
 * How a streak is drawn. [tier] is 0..9 (the ten tiers of the concept art), [heat] 0..1 rises across the tier
 * (a visible step every 5 days, a little more every day), [layers] is the extra orbit rings and rays from 100
 * days, [prismatic] is the hue-cycling look from 200 days.
 */
class FlameStyle(val tier: Int, val heat: Float, val layers: Int, val prismatic: Boolean)

val FLAME_TIER_NAMES: List<String> = listOf(
    "Dull Spark", "Kindled Ember", "Ignited Flame", "White Heat", "Cobalt Blaze",
    "Wildfire", "Shadowfire", "Dragonfire", "Celestial Flame", "Eternal Singularity",
)

private val ROMAN = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X")

/**
 * Ten tiers, a new one every ten days (two flame stages), the last from day 90 onward. Inside a tier the flame
 * still steps up every 5 days and grows a little every day, so something changes at least that often.
 */
fun flameStyle(info: FlameInfo): FlameStyle {
    val top = info.look == FlameLook.SOLAR || info.look == FlameLook.PRISMATIC
    val tier = if (top) 9 else (info.stage / 2).coerceIn(0, 9)
    val heat = if (top) 1f else (((info.stage % 2) + info.growth) / 2f).coerceIn(0f, 1f)
    return FlameStyle(tier, heat, info.solarLayers, info.look == FlameLook.PRISMATIC)
}

private fun hsv(h: Float, s: Float, v: Float): Color =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)))

private fun cp(deep: Long, base: Long, light: Long, hot: Long, glow: Long, rim: Long, trim: Long = 0xFFE6B84A) =
    CrystalPalette(Color(deep), Color(base), Color(light), Color(hot), Color(glow), Color(rim), Color(trim))

private val PALETTES: List<CrystalPalette> = listOf(
    cp(0xFF26272B, 0xFF55565B, 0xFF8A8B90, 0xFFC9CACF, 0xFF8C8C92, 0xFFA3A4A9),
    cp(0xFF241210, 0xFF7A3A2A, 0xFFB8573A, 0xFFFF7A3C, 0xFFE2572B, 0xFFD27A5A),
    cp(0xFF7A2E08, 0xFFE8741A, 0xFFFFA43D, 0xFFFFE08A, 0xFFFF8A1F, 0xFFFFB066),
    cp(0xFFB5651A, 0xFFFFB02E, 0xFFFFD866, 0xFFFFF6D0, 0xFFFFC23D, 0xFFFFE9A3),
    cp(0xFF0A2A66, 0xFF2C7BE0, 0xFF5FB4FF, 0xFFDDF3FF, 0xFF3DA5FF, 0xFF9BD8FF),
    cp(0xFF0C4A1F, 0xFF2DBE4E, 0xFF72F07A, 0xFFE4FFD6, 0xFF49E36A, 0xFFA6FF9E),
    cp(0xFF2B0D52, 0xFF7B3FE0, 0xFFA878FF, 0xFFEBDDFF, 0xFF8E5BFF, 0xFFC4A3FF),
    cp(0xFF4A0E06, 0xFFD43A12, 0xFFFF7A2E, 0xFFFFD27A, 0xFFFF5A1F, 0xFFFF9A5C),
    cp(0xFF8A7A5A, 0xFFE9E1CC, 0xFFFFFAEA, 0xFFFFFFFF, 0xFFFFE6A0, 0xFFFFE9B0),
    cp(0xFF9A8A6C, 0xFFF3EBD8, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFD98A, 0xFFFFF0C0),
)

fun crystalPalette(info: FlameInfo): CrystalPalette {
    val st = flameStyle(info)
    if (st.prismatic) {
        val h = info.hueShiftDegrees.toFloat()
        return CrystalPalette(
            deep = hsv(h, 0.35f, 0.62f),
            base = hsv(h, 0.26f, 1f),
            light = hsv((h + 25f) % 360f, 0.12f, 1f),
            hot = Color.White,
            glow = hsv((h + 10f) % 360f, 0.55f, 1f),
            rim = hsv((h + 40f) % 360f, 0.25f, 1f),
            trim = Color(0xFFE6B84A),
        )
    }
    return PALETTES[st.tier]
}

fun flameColors(info: FlameInfo): FlameColors {
    val p = crystalPalette(info)
    return FlameColors(outer = p.base, mid = p.glow, core = p.hot)
}

/** Name of the tier, as on the concept art. */
fun flameName(info: FlameInfo): String = FLAME_TIER_NAMES[flameStyle(info).tier]

/** "TIER X" style label. */
fun flameTierLabel(info: FlameInfo): String = "TIER " + ROMAN[flameStyle(info).tier]

/**
 * The streak flame, drawn entirely in code from five crystal shards. Ten tiers (see [FLAME_TIER_NAMES]); each
 * has its own colour and its own ornaments, and every one of them breathes: shards drift and tilt out of step,
 * facets shimmer, a glint sweeps across, embers rise.
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
