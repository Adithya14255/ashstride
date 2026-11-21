package com.ashstride.app.ui.profile

import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ashstride.app.AshstrideApp
import com.ashstride.app.domain.BadgeEntry
import com.ashstride.app.domain.TitleEngine
import com.ashstride.app.ui.Fmt
import com.ashstride.app.ui.SubScreen
import com.ashstride.app.ui.components.BadgeArt
import com.ashstride.app.ui.components.FlameShapes
import com.ashstride.app.ui.components.Panel
import com.ashstride.app.ui.components.Pill
import com.ashstride.app.ui.components.ScreenHeader
import com.ashstride.app.ui.components.SectionTitle
import com.ashstride.app.ui.components.StatRow
import com.ashstride.app.ui.components.TierEmblem
import com.ashstride.app.ui.components.tierColor
import com.ashstride.app.ui.theme.AshHigh
import com.ashstride.app.ui.theme.Cinder
import com.ashstride.app.ui.theme.Gold
import com.ashstride.app.ui.theme.Smoke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun avatarHash(name: String): Int = (name.hashCode().toLong() and 0x7fffffffL).toInt()

/** A generated avatar: colours come from the name, the initial sits over a faint flame, aura rings for Immortals. */
@Composable
fun AvatarView(name: String, ring: Color, auraRank: Int, modifier: Modifier = Modifier, avatarSize: Dp = 92.dp) {
    val h = avatarHash(name)
    val c1 = Color.hsv((h % 360).toFloat(), 0.45f, 0.62f)
    val c2 = Color.hsv(((h / 7) % 360).toFloat(), 0.55f, 0.30f)
    val sz = avatarSize.value
    Box(modifier.size(avatarSize), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension / 2f - 6.dp.toPx()
            for (i in 1..minOf(auraRank, 3)) {
                drawCircle(ring.copy(alpha = 0.32f - 0.08f * i), radius = r + i * 2.5.dp.toPx(), center = c, style = Stroke(1.5.dp.toPx()))
            }
            drawCircle(Brush.linearGradient(listOf(c1, c2), Offset(0f, 0f), Offset(size.width, size.height)), radius = r, center = c)
            val fh = r * 1.25f
            val fw = fh * FlameShapes.ASPECT
            drawPath(FlameShapes.path(FlameShapes.OUTER, c.x - fw / 2f, c.y - fh * 0.45f, fw, fh), Color.White.copy(alpha = 0.10f))
            drawCircle(ring, radius = r, center = c, style = Stroke(3.dp.toPx()))
        }
        Text(
            name.firstOrNull()?.uppercase() ?: "W",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = (sz * 0.42f).sp, lineHeight = (sz * 0.46f).sp,
            ),
        )
    }
}

@Composable
fun ProfileScreen(app: AshstrideApp, contentPadding: PaddingValues, onGoalClick: () -> Unit, onOpen: (SubScreen) -> Unit) {
    val model = app.model
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val goal by model.goal.collectAsStateWithLifecycle()
    val snap by model.snapshot.collectAsStateWithLifecycle()
    val career by model.career.collectAsStateWithLifecycle()
    val journey by model.journey.collectAsStateWithLifecycle()
    val name by model.profileName.collectAsStateWithLifecycle()
    val titleId by model.profileTitleId.collectAsStateWithLifecycle()
    val equipped by model.equippedBadge.collectAsStateWithLifecycle()
    val showcase by model.showcase.collectAsStateWithLifecycle()
    val badges by model.badgeEntries.collectAsStateWithLifecycle()

    var editName by remember { mutableStateOf(false) }
    var pickEmblem by remember { mutableStateOf(false) }
    var pickShowcase by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader("Profile", goal, onGoalClick)

        val s = snap
        val cs = career
        if (s == null || cs == null) {
            Text("Loading your history...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val tc = tierColor(s.level.tier)
            val title = TitleEngine.resolve(cs, titleId)
            val equippedEntry = badges.firstOrNull { it.key == equipped }

            Panel(tint = tc) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarView(name, tc, s.level.immortalRank)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            name,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.clickable { editName = true },
                        )
                        Text("tap to rename", style = MaterialTheme.typography.labelSmall, color = Smoke)
                        Spacer(Modifier.height(6.dp))
                        Pill(title.name, tc)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${s.level.rankTitle}  |  Level ${s.level.level}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box(Modifier.clickable { pickEmblem = true }, contentAlignment = Alignment.Center) {
                        if (equippedEntry != null) {
                            BadgeArt(equippedEntry.kind, equippedEntry.rim, equippedEntry.glyph, equippedEntry.stitched, badgeSize = 72.dp)
                        } else {
                            TierEmblem(s.level, emblemSize = 80.dp)
                        }
                    }
                }
                Text(
                    "Tap the emblem to equip any badge you have earned.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Panel {
                SectionTitle("Showcase")
                Row(Modifier.fillMaxWidth().clickable { pickShowcase = true }, horizontalArrangement = Arrangement.SpaceEvenly) {
                    for (i in 0 until 3) {
                        val e = badges.firstOrNull { it.key == showcase.getOrNull(i) }
                        if (e != null) {
                            BadgeArt(e.kind, e.rim, e.glyph, e.stitched, badgeSize = 72.dp)
                        } else {
                            Box(Modifier.size(72.dp).clip(CircleShape).background(AshHigh).border(2.dp, Cinder, CircleShape), contentAlignment = Alignment.Center) {
                                Text("+", color = Smoke, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
                Text("Tap to choose up to three badges.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Panel {
                SectionTitle("Titles")
                val all = TitleEngine.all(cs)
                for (t in all) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = t.earned) { scope.launch { model.setProfileTitle(t.id) } }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(t.name, color = if (t.earned) MaterialTheme.colorScheme.onSurface else Smoke, style = MaterialTheme.typography.bodyLarge)
                            Text(t.how, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (t.id == title.id) Pill("Worn", Gold) else if (!t.earned) Pill("Locked", Cinder)
                    }
                }
            }

            Panel {
                SectionTitle("Career")
                StatRow("Lifetime steps", Fmt.n(cs.totalSteps))
                StatRow("Distance", "${Fmt.n(Math.round(cs.totalKm))} km")
                StatRow("Best streak", "${Fmt.n(cs.bestTotalStreak)} days (pure ${Fmt.n(cs.bestPureStreak)})")
                StatRow("Trials won", "${cs.challengesWon}  (Legendary ${cs.legendaryWon})")
                StatRow("Menders used", "${cs.mendersUsed}")
                StatRow("Road travelled", "${(cs.journeyFraction * 100f).toInt()}%")
                StatRow("Badges earned", "${badges.size}")
            }

            Panel(modifier = Modifier.clickable { onOpen(SubScreen.BADGES) }) {
                SectionTitle("Badges  >")
                Text("${badges.size} earned. See them all and what is still locked.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Panel(modifier = Modifier.clickable { onOpen(SubScreen.STATS) }) {
                SectionTitle("Stats  >")
                Text("Day-of-week averages, months, records.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Button(
                onClick = {
                    val d = CardData(
                        name = name,
                        title = title.name,
                        rank = s.level.rankTitle,
                        level = s.level.level,
                        streak = s.streak.total,
                        steps = Fmt.n(cs.totalSteps),
                        km = "${Fmt.n(Math.round(cs.totalKm))} km",
                        journey = journey?.let { "${(it.fraction * 100f).toInt()}% of the road  |  ${it.chapter.title}" } ?: "",
                        tierColor = tc.toArgb(),
                        flame = s.flame,
                    )
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) { ProfileCard.save(context, d) }
                        Toast.makeText(context, if (ok) "Saved to Pictures/Ashstride" else "Could not save the card", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Export profile card") }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (editName) {
        var draft by remember { mutableStateOf(name) }
        AlertDialog(
            onDismissRequest = { editName = false },
            title = { Text("Your name") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(20) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    editName = false
                    scope.launch { model.setProfileName(draft) }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editName = false }) { Text("Cancel") } },
        )
    }
    if (pickEmblem) {
        BadgePickerDialog(
            title = "Equip an emblem",
            entries = badges,
            initial = listOfNotNull(equipped),
            max = 1,
            onDone = { keys ->
                pickEmblem = false
                scope.launch { model.setEquipped(keys.firstOrNull()) }
            },
            onDismiss = { pickEmblem = false },
        )
    }
    if (pickShowcase) {
        BadgePickerDialog(
            title = "Choose your showcase",
            entries = badges,
            initial = showcase,
            max = 3,
            onDone = { keys ->
                pickShowcase = false
                scope.launch { model.setShowcase(keys) }
            },
            onDismiss = { pickShowcase = false },
        )
    }
}

@Composable
private fun BadgePickerDialog(
    title: String,
    entries: List<BadgeEntry>,
    initial: List<String>,
    max: Int,
    onDone: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var chosen by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (entries.isEmpty()) Text("No badges earned yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                for (e in entries) {
                    val on = e.key in chosen
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (on) AshHigh else Color.Transparent)
                            .clickable {
                                chosen = when {
                                    on -> chosen - e.key
                                    max == 1 -> listOf(e.key)
                                    chosen.size < max -> chosen + e.key
                                    else -> chosen
                                }
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BadgeArt(e.kind, e.rim, e.glyph, e.stitched, badgeSize = 44.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(e.title, style = MaterialTheme.typography.bodyLarge)
                            Text(e.variantLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (on) Pill("Chosen", Gold)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onDone(chosen) }) { Text("Done") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
