package com.ashstride.app.ui.celebrate

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ashstride.app.domain.BadgeKind
import com.ashstride.app.domain.ChallengeText
import com.ashstride.app.ui.Fmt
import com.ashstride.app.ui.components.BadgeArt
import com.ashstride.app.ui.components.FlameView
import com.ashstride.app.ui.components.NavGlyph
import com.ashstride.app.ui.components.NavIcon
import com.ashstride.app.ui.components.TierEmblem
import com.ashstride.app.ui.components.flameColors
import com.ashstride.app.ui.components.flameName
import com.ashstride.app.ui.components.rimColor
import com.ashstride.app.ui.components.tierColor
import com.ashstride.app.ui.theme.Ember
import kotlin.math.cos
import kotlin.math.sin

/** Full-screen moment: a burst of embers behind a big emblem, a line of text and a Continue button. */
@Composable
fun CelebrationOverlay(c: Celebration, onDone: () -> Unit) {
    val t = remember(c) { Animatable(0f) }
    LaunchedEffect(c) { t.animateTo(1f, tween(1600, easing = FastOutSlowInEasing)) }

    val accent: Color = when (c) {
        is Celebration.LevelUp -> tierColor(c.level.tier)
        is Celebration.FlameStage -> flameColors(c.info).mid
        is Celebration.WaypointReached -> Ember
        is Celebration.ChallengeWon -> rimColor(c.challenge.rim)
    }
    val appear = (t.value * 1.8f).coerceIn(0f, 1f)
    val pop = 0.7f + 0.3f * appear

    Dialog(
        onDismissRequest = onDone,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color(0xF2070810)), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val centre = Offset(size.width / 2f, size.height * 0.40f)
                val reach = size.minDimension * 0.55f
                val tt = t.value
                drawCircle(
                    color = accent.copy(alpha = (1f - tt) * 0.5f),
                    radius = reach * tt,
                    center = centre,
                    style = Stroke(width = 3.dp.toPx()),
                )
                for (i in 0 until 28) {
                    val a = Math.toRadians(i * 360.0 / 28.0 + (i % 2) * 6.0)
                    val r = size.minDimension * 0.10f + (reach - size.minDimension * 0.10f) * tt * (0.6f + 0.4f * ((i * 7) % 5) / 4f)
                    drawCircle(
                        color = accent.copy(alpha = ((1f - tt) * 0.95f).coerceIn(0f, 1f)),
                        radius = (if (i % 2 == 0) 3.5f else 2f).dp.toPx(),
                        center = Offset(centre.x + r * cos(a).toFloat(), centre.y + r * sin(a).toFloat()),
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(32.dp)
                    .graphicsLayer { scaleX = pop; scaleY = pop; alpha = appear },
            ) {
                when (c) {
                    is Celebration.LevelUp -> {
                        TierEmblem(c.level, emblemSize = 150.dp)
                        Spacer(Modifier.height(20.dp))
                        Title(if (c.tierChanged) "A new rank" else "Level up")
                        Sub("Level ${Fmt.n(c.level.level)}  |  ${c.level.rankTitle}")
                    }
                    is Celebration.FlameStage -> {
                        FlameView(c.info, Modifier.size(width = 160.dp, height = 200.dp))
                        Spacer(Modifier.height(12.dp))
                        Title("Your flame has changed")
                        Sub("${flameName(c.info)}  |  ${Fmt.n(c.streak)} day streak")
                    }
                    is Celebration.WaypointReached -> {
                        NavIcon(NavGlyph.JOURNEY, accent, filled = true, iconSize = 84.dp)
                        Spacer(Modifier.height(20.dp))
                        Title(c.waypoint.name)
                        Sub(c.chapterTitle)
                        if (c.waypoint.text.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                c.waypoint.text,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    is Celebration.ChallengeWon -> {
                        BadgeArt(
                            kind = BadgeKind.CHALLENGE,
                            rim = c.challenge.rim,
                            glyph = "",
                            stitched = false,
                            badgeSize = 130.dp,
                        )
                        Spacer(Modifier.height(20.dp))
                        Title("Trial complete")
                        Sub(ChallengeText.title(c.challenge))
                        Spacer(Modifier.height(6.dp))
                        Text(
                            ChallengeText.rewardText(c.challenge),
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
                Spacer(Modifier.height(28.dp))
                Button(
                    onClick = onDone,
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF0A0C13)),
                ) { Text("Continue") }
            }
        }
    }
}

@Composable
private fun Title(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun Sub(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}
