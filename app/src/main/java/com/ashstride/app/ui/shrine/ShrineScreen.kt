package com.ashstride.app.ui.shrine

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ashstride.app.AshstrideApp
import com.ashstride.app.domain.BadgeKind
import com.ashstride.app.domain.Challenge
import com.ashstride.app.domain.ChallengeOdds
import com.ashstride.app.domain.ChallengeProgress
import com.ashstride.app.domain.ChallengeText
import com.ashstride.app.domain.MenderRules
import com.ashstride.app.domain.WeekStatus
import com.ashstride.app.ui.PastWeek
import com.ashstride.app.ui.components.BadgeArt
import com.ashstride.app.ui.components.FlameShapes
import com.ashstride.app.ui.components.FlameView
import com.ashstride.app.ui.components.Panel
import com.ashstride.app.ui.components.Pill
import com.ashstride.app.ui.components.ScreenHeader
import com.ashstride.app.ui.components.SectionTitle
import com.ashstride.app.ui.components.SoftBar
import com.ashstride.app.ui.components.rimColor
import com.ashstride.app.ui.theme.Cinder
import com.ashstride.app.ui.theme.Dim
import com.ashstride.app.ui.theme.EmberLight
import com.ashstride.app.ui.theme.Ember
import com.ashstride.app.ui.theme.Gold
import com.ashstride.app.ui.theme.Smoke
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class CardMode { SEALED, ACTIVE, DIM }

@Composable
fun ShrineScreen(app: AshstrideApp, contentPadding: PaddingValues, onGoalClick: () -> Unit) {
    val model = app.model
    val shrine by model.shrine.collectAsStateWithLifecycle()
    val inventory by model.inventory.collectAsStateWithLifecycle()
    val journey by model.journey.collectAsStateWithLifecycle()
    val snap by model.snapshot.collectAsStateWithLifecycle()
    val goal by model.goal.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf<Challenge?>(null) }
    val dateFmt = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader("Shrine", goal, onGoalClick)

        val sh = shrine
        if (sh == null) {
            Text("Loading...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            // The scene: a torii with the player's own flame between the pillars.
            val scene = journey?.let { model.story.sceneFor(it.chapter.id) }
            val lastLost = sh.past.firstOrNull()?.status == WeekStatus.LOST && sh.status == WeekStatus.OPEN
            Panel(tint = Ember) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    ToriiArt(Modifier.fillMaxSize())
                    val s = snap
                    if (s != null) FlameView(s.flame, Modifier.size(width = 70.dp, height = 92.dp).padding(top = 8.dp))
                }
                if (scene != null) {
                    Text(
                        when {
                            sh.status == WeekStatus.WON -> scene.won
                            lastLost -> scene.lost
                            else -> scene.arrive
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            // Mender stash.
            Panel {
                SectionTitle("Mender stash")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Shards", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (i in 0 until MenderRules.SHARDS_PER_MENDER) ShardSlot(i < inventory.shards)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Menders", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (i in 0 until MenderRules.MAX_MENDERS) MenderSlot(i < inventory.menders)
                        }
                    }
                }
                Text(
                    "Hard trials give 1 shard, Legendary give 2. Three shards forge a Mender, which fixes one missed day on the Streak page.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // This week.
            val weekLabel = dateFmt.format(sh.weekStart) + " - " + dateFmt.format(sh.weekStart.plusDays(6))
            when {
                sh.status == ShrineStateNone -> {
                    Panel { Text("The shrine is quiet. Cards appear once your history has loaded.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                sh.status == WeekStatus.OPEN -> {
                    SectionTitle("Choose your trial  |  $weekLabel")
                    Text(
                        "Pick one card. It locks the moment you choose, and progress counts from Saturday.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    for (c in sh.offers) {
                        ChallengeCard(c, CardMode.SEALED, null, ChallengeOdds.label(sh.odds[c.id])) { if (sh.canPick) confirm = c }
                    }
                }
                else -> {
                    val picked = sh.picked
                    SectionTitle("This week's trial  |  $weekLabel")
                    if (picked != null) {
                        ChallengeCard(picked, CardMode.ACTIVE, sh.progress, ChallengeOdds.label(sh.odds[picked.id]), onClick = null)
                        val statusLine = when (sh.status) {
                            WeekStatus.WON -> "Complete. The reward is yours."
                            else -> "${sh.daysLeft} day(s) left in the week."
                        }
                        Text(statusLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    for (c in sh.offers) if (c.id != sh.picked?.id) ChallengeCard(c, CardMode.DIM, null, null, onClick = null)
                }
            }

            if (sh.past.isNotEmpty()) {
                Panel {
                    SectionTitle("Past weeks")
                    for (w in sh.past) PastRow(w, dateFmt)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    val c = confirm
    if (c != null) {
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Lock in this trial?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(ChallengeText.tierName(c.tier) + ": " + ChallengeText.title(c), style = MaterialTheme.typography.titleMedium)
                    Text(ChallengeText.describe(c))
                    Text(ChallengeText.rewardText(c), color = MaterialTheme.colorScheme.secondary)
                    Text(
                        "You cannot change your pick afterwards. The other two cards stay sealed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val ws = shrine?.weekStart
                    confirm = null
                    if (ws != null) scope.launch { model.pickChallenge(ws, c.id) }
                }) { Text("Lock it in") }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } },
        )
    }
}

/** Matches ShrineState.NONE without importing the companion into every branch. */
private const val ShrineStateNone = "NONE"

@Composable
private fun ChallengeCard(c: Challenge, mode: CardMode, progress: ChallengeProgress?, odds: String?, onClick: (() -> Unit)?) {
    val rim = rimColor(c.rim)
    val shape = com.ashstride.app.ui.theme.ChamferShape(16.dp)
    val dim = mode == CardMode.DIM
    val borderBrush: Brush = if (dim) Brush.linearGradient(listOf(Cinder, Cinder)) else if (c.rim == com.ashstride.app.domain.BadgeRim.PRISMATIC) {
        Brush.linearGradient(listOf(Color(0xFFFF5252), Color(0xFFFFD740), Color(0xFF69F0AE), Color(0xFF40C4FF), Color(0xFFE040FB)))
    } else {
        Brush.linearGradient(listOf(rim, rim))
    }
    var mod = Modifier
        .fillMaxWidth()
        .clip(shape)
        .background(Brush.verticalGradient(listOf(Color(0xFF1C2132), Color(0xFF141824))), shape)
        .border(if (dim) 1.dp else 2.dp, borderBrush, shape)
    if (onClick != null) mod = mod.clickable(onClick = onClick)
    Column(mod.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BadgeArt(kind = BadgeKind.CHALLENGE, rim = c.rim, glyph = "", stitched = false, locked = dim, badgeSize = 56.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Pill(ChallengeText.tierName(c.tier), if (dim) Smoke else rim)
                    if (mode == CardMode.SEALED) Pill("Sealed", Smoke)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    ChallengeText.title(c),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (dim) Smoke else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        if (!dim) {
            Text(ChallengeText.describe(c), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            Text(ChallengeText.rewardText(c), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelLarge)
            if (odds != null) Text(odds, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        if (progress != null) {
            SoftBar(progress.fraction.toFloat(), color = if (progress.done) Gold else rim)
            Text(
                ChallengeText.progress(c, progress),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (mode == CardMode.SEALED && onClick != null) {
            Text("Tap to choose", style = MaterialTheme.typography.labelMedium, color = EmberLight)
        }
    }
}

@Composable
private fun PastRow(w: PastWeek, fmt: DateTimeFormatter) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                w.picked?.let { ChallengeText.title(it) + "  |  " + ChallengeText.tierName(it.tier) } ?: "No card picked",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text("Week of ${fmt.format(w.weekStart)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val (label, color) = when (w.status) {
            WeekStatus.WON -> "Won" to Gold
            WeekStatus.LOST -> "Lost" to Smoke
            else -> "Skipped" to Dim
        }
        Pill(label, color)
    }
}

@Composable
private fun ToriiArt(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val pillarGap = minOf(w * 0.30f, 130.dp.toPx())
        val top = h * 0.14f
        val col = Color(0xFF3A3250)
        val glow = Color(0xFFD1AE5C)
        drawCircle(
            brush = Brush.radialGradient(listOf(glow.copy(alpha = 0.22f), Color.Transparent), Offset(cx, h * 0.6f), h * 0.9f),
            radius = h * 0.9f, center = Offset(cx, h * 0.6f),
        )
        val beam = Path()
        beam.moveTo(cx - pillarGap - 22.dp.toPx(), top + 8.dp.toPx())
        beam.cubicTo(cx - pillarGap * 0.5f, top + 2.dp.toPx(), cx + pillarGap * 0.5f, top + 2.dp.toPx(), cx + pillarGap + 22.dp.toPx(), top + 8.dp.toPx())
        beam.lineTo(cx + pillarGap + 16.dp.toPx(), top - 4.dp.toPx())
        beam.cubicTo(cx + pillarGap * 0.5f, top - 10.dp.toPx(), cx - pillarGap * 0.5f, top - 10.dp.toPx(), cx - pillarGap - 16.dp.toPx(), top - 4.dp.toPx())
        beam.close()
        drawPath(beam, glow.copy(alpha = 0.9f))
        drawLine(glow.copy(alpha = 0.8f), Offset(cx - pillarGap - 8.dp.toPx(), top + 26.dp.toPx()), Offset(cx + pillarGap + 8.dp.toPx(), top + 26.dp.toPx()), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(col, Offset(cx - pillarGap, top + 6.dp.toPx()), Offset(cx - pillarGap, h), strokeWidth = 9.dp.toPx(), cap = StrokeCap.Butt)
        drawLine(col, Offset(cx + pillarGap, top + 6.dp.toPx()), Offset(cx + pillarGap, h), strokeWidth = 9.dp.toPx(), cap = StrokeCap.Butt)
    }
}

@Composable
private fun ShardSlot(filled: Boolean) {
    Canvas(Modifier.size(26.dp)) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val p = Path()
        p.moveTo(c.x, 1.dp.toPx())
        p.lineTo(size.width - 4.dp.toPx(), c.y)
        p.lineTo(c.x, size.height - 1.dp.toPx())
        p.lineTo(4.dp.toPx(), c.y)
        p.close()
        if (filled) drawPath(p, Brush.verticalGradient(listOf(Color(0xFFFFE0A3), Gold))) else drawPath(p, Cinder, style = Stroke(width = 1.5.dp.toPx()))
    }
}

@Composable
private fun MenderSlot(filled: Boolean) {
    Canvas(Modifier.size(width = 24.dp, height = 31.dp)) {
        val p = FlameShapes.silhouetteIn(1.dp.toPx(), 1.dp.toPx(), size.width - 2.dp.toPx(), size.height - 2.dp.toPx())
        if (filled) drawPath(p, Gold) else drawPath(p, Cinder, style = Stroke(width = 1.5.dp.toPx()))
    }
}
