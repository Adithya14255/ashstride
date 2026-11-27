package com.ashstride.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ashstride.app.domain.FlameEngine
import com.ashstride.app.domain.FlameInfo
import com.ashstride.app.ui.Fmt
import com.ashstride.app.ui.theme.Gilt
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private const val TAU = 6.2831855f

private class Mote(val x: Float, val cycles: Int, val offset: Float, val size: Float, val sway: Float)

/**
 * Full-screen showcase of the streak flame: slow light rays, rings of light spreading from the flame, embers drifting
 * up, and a burst of sparks when the flame is tapped. Everything is tinted by the tier's own palette.
 */
@Composable
fun FlameShowcase(info: FlameInfo, onOpenTierMap: () -> Unit, onDismiss: () -> Unit) {
    val pal = crystalPalette(info)
    val style = flameStyle(info)
    val total = flameTotalDays(info)
    val tint = pal.glow
    val dark = Color(0xFF05060A)

    val inf = rememberInfiniteTransition(label = "showcase")
    val t by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "t")
    var burstCount by remember { mutableIntStateOf(0) }
    val burst = remember { Animatable(1f) }
    LaunchedEffect(burstCount) {
        if (burstCount > 0) {
            burst.snapTo(0f)
            burst.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))
        }
    }
    val motes = remember {
        List(44) { i ->
            val h1 = ((i * 7919) % 1000) / 1000f
            val h2 = ((i * 104729) % 1000) / 1000f
            Mote(x = h1 - 0.5f, cycles = 1 + i % 3, offset = h2, size = 1.4f + (i % 4) * 0.8f, sway = 6f + (i % 5) * 3f)
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(dark, lerp(dark, pal.deep, 0.55f), dark))),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height * 0.29f
                val reach = hypot(size.width, size.height)
                val md = size.minDimension

                // Soft glow behind the flame.
                drawCircle(
                    Brush.radialGradient(listOf(tint.copy(alpha = 0.30f), Color.Transparent), Offset(cx, cy), md * 0.75f),
                    radius = md * 0.75f, center = Offset(cx, cy),
                )

                // Slow rotating rays.
                val rot = t * 360f
                for (i in 0 until 12) {
                    val a0 = Math.toRadians((i * 30f + rot - 5f).toDouble())
                    val a1 = Math.toRadians((i * 30f + rot + 5f).toDouble())
                    val p = Path()
                    p.moveTo(cx, cy)
                    p.lineTo(cx + reach * cos(a0).toFloat(), cy + reach * sin(a0).toFloat())
                    p.lineTo(cx + reach * cos(a1).toFloat(), cy + reach * sin(a1).toFloat())
                    p.close()
                    drawPath(p, Brush.radialGradient(listOf(tint.copy(alpha = 0.16f), Color.Transparent), Offset(cx, cy), reach * 0.55f))
                }

                // Rings of light spreading outward.
                for (k in 0 until 3) {
                    val ph = (t * 3f + k / 3f) % 1f
                    drawCircle(
                        color = pal.rim.copy(alpha = (1f - ph) * 0.42f),
                        radius = md * 0.14f + ph * md * 0.62f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }

                // Embers drifting up the whole screen.
                for (m in motes) {
                    val p = (t * m.cycles + m.offset) % 1f
                    val x = cx + m.x * size.width + sin(p * TAU * 2f) * m.sway.dp.toPx()
                    val y = size.height * 0.86f - p * size.height * 0.80f
                    val a = sin(p * PI.toFloat()).coerceIn(0f, 1f)
                    drawCircle(lerp(tint, pal.hot, 0.5f).copy(alpha = a * 0.85f), m.size.dp.toPx(), Offset(x, y))
                }

                // Burst when the flame is tapped.
                val b = burst.value
                if (burstCount > 0 && b < 1f) {
                    val fade = 1f - b
                    drawCircle(pal.hot.copy(alpha = fade * 0.5f), md * 0.10f + b * md * 0.55f, Offset(cx, cy), style = Stroke(3.dp.toPx()))
                    for (i in 0 until 36) {
                        val a = Math.toRadians(i * 10.0 + (i % 3) * 4.0)
                        val d = md * (0.12f + b * (0.30f + 0.35f * ((i * 5) % 7) / 6f))
                        drawCircle(
                            color = (if (i % 2 == 0) pal.hot else tint).copy(alpha = fade),
                            radius = (if (i % 3 == 0) 4f else 2.5f).dp.toPx() * (1f - b * 0.5f),
                            center = Offset(cx + d * cos(a).toFloat(), cy + d * sin(a).toFloat()),
                        )
                    }
                }
            }

            Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().weight(0.58f), contentAlignment = Alignment.Center) {
                    FlameView(
                        info,
                        Modifier
                            .fillMaxHeight(0.88f)
                            .aspectRatio(0.78f)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { burstCount += 1 },
                    )
                }
                Column(
                    Modifier.fillMaxWidth().weight(0.42f).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(flameTierLabel(info), style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp), color = Gilt.GoldBright)
                    Text(
                        flameName(info),
                        style = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.Serif),
                        color = tint,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "${Fmt.n(total)} day streak",
                        style = MaterialTheme.typography.titleMedium,
                        color = Gilt.Ink,
                    )
                    Text(
                        FLAME_TIER_DESCRIPTIONS[style.tier],
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gilt.Muted,
                        textAlign = TextAlign.Center,
                    )
                    val toNext = flameDaysToNextTier(info)
                    if (toNext != null) {
                        SoftBar(((total - style.tier * 10) / 10f), color = tint)
                        Text(
                            "$toNext day(s) to ${FLAME_TIER_NAMES[style.tier + 1]}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gilt.Muted,
                        )
                    } else {
                        Text("The last tier. The fire only grows from here.", style = MaterialTheme.typography.bodySmall, color = Gilt.Muted)
                    }
                    if (style.tier == 9) {
                        Spacer(Modifier.height(4.dp))
                        SingularityStrip(total)
                    }
                    if (info.hasMendSeam) {
                        Text(
                            "The gold thread running through the flame is a Mender stitch: this streak was repaired once.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gilt.GoldBright,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onOpenTierMap) { Text("Tier map") }
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = tint, contentColor = Color(0xFF0A0C13)),
                        ) { Text("Close") }
                    }
                    Text("Tap the flame", style = MaterialTheme.typography.labelSmall, color = Gilt.Muted.copy(alpha = 0.7f))
                }
            }
        }
    }
}
