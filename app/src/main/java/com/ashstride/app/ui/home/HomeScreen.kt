package com.ashstride.app.ui.home

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ashstride.app.AshstrideApp
import com.ashstride.app.domain.ChallengeText
import com.ashstride.app.domain.WeekStatus
import com.ashstride.app.ui.Fmt
import com.ashstride.app.ui.Tab
import com.ashstride.app.ui.components.FlameView
import com.ashstride.app.ui.components.Panel
import com.ashstride.app.ui.components.Pill
import com.ashstride.app.ui.components.ScreenHeader
import com.ashstride.app.ui.components.SectionTitle
import com.ashstride.app.ui.components.SoftBar
import com.ashstride.app.ui.components.StepRing
import com.ashstride.app.ui.components.TierEmblem
import com.ashstride.app.ui.components.flameColors
import com.ashstride.app.ui.components.flameName
import com.ashstride.app.ui.health.HealthConnectCard
import com.ashstride.app.ui.theme.AshHigh
import com.ashstride.app.ui.theme.Cinder
import com.ashstride.app.ui.theme.Ember
import com.ashstride.app.ui.theme.Gold
import com.ashstride.app.ui.theme.Parchment
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(app: AshstrideApp, contentPadding: PaddingValues, onGoalClick: () -> Unit, onNavigate: (Tab) -> Unit) {
    val model = app.model
    val ledger by model.ledger.collectAsStateWithLifecycle()
    val snap by model.snapshot.collectAsStateWithLifecycle()
    val today by model.today.collectAsStateWithLifecycle()
    val goal by model.goal.collectAsStateWithLifecycle()
    val shrine by model.shrine.collectAsStateWithLifecycle()
    val journey by model.journey.collectAsStateWithLifecycle()

    val byDate = ledger.associateBy { it.date }
    val todayRow = byDate[today]
    val steps = todayRow?.steps ?: 0
    val km = todayRow?.distanceKm ?: 0.0
    val todayGoal = todayRow?.goal ?: goal

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader("Ashstride", goal, onGoalClick)

        val s = snap
        if (s == null) {
            Text("Loading your history...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            // Hero: the flame and the streak.
            val shownStreak by animateIntAsState(s.streak.total, tween(900), label = "streak")
            Panel(tint = flameColors(s.flame).outer) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FlameView(s.flame, Modifier.width(128.dp).height(168.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            Fmt.n(shownStreak),
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 68.sp, lineHeight = 70.sp),
                            color = flameColors(s.flame).mid,
                        )
                        Text("day streak", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Text(flameName(s.flame), style = MaterialTheme.typography.labelLarge, color = Parchment)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Pill("Pure ${Fmt.n(s.streak.pure)}")
                            Pill("Best ${Fmt.n(s.streak.bestTotal)}", Gold)
                        }
                    }
                }
            }

            // Today: ring, the week strip.
            Panel {
                StepRing(steps = steps, goal = todayGoal, modifier = Modifier.size(220.dp).align(Alignment.CenterHorizontally)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(Fmt.n(steps), style = MaterialTheme.typography.displayMedium)
                        Text("of ${Fmt.n(todayGoal)} steps", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Fmt.km(km), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                val left = (todayGoal - steps).coerceAtLeast(0)
                Text(
                    if (left == 0) "Goal met. The fire is fed." else "${Fmt.n(left)} steps to today's goal",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (left == 0) Gold else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (i in 6 downTo 0) {
                        val d = today.minusDays(i.toLong())
                        val row = byDate[d]
                        val bg: Color
                        val fg: Color
                        when {
                            row == null -> { bg = AshHigh; fg = Cinder }
                            row.met -> { bg = Ember; fg = Color(0xFF211D1A) }
                            row.mended -> { bg = AshHigh; fg = Gold }
                            else -> { bg = AshHigh; fg = MaterialTheme.colorScheme.onSurfaceVariant }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(bg, CircleShape)
                                    .border(if (d == today) 2.dp else 0.dp, if (d == today) Parchment else Color.Transparent, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${d.dayOfMonth}", color = fg, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            if (s.streak.atRisk) {
                Panel(tint = Ember) {
                    Text("Keep the fire alive", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                    Text(
                        "${Fmt.n((todayGoal - steps).coerceAtLeast(0))} more steps today keeps your ${Fmt.n(s.streak.total)}-day streak going.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Shrine teaser.
            val sh = shrine
            if (sh != null) {
                Panel(modifier = Modifier.clickable { onNavigate(Tab.SHRINE) }) {
                    SectionTitle("The Shrine")
                    val p = sh.picked
                    val prog = sh.progress
                    when {
                        sh.status == WeekStatus.OPEN -> Text(
                            "Three sealed cards wait. Choose one trial for this week.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        p != null && prog != null -> {
                            Text(ChallengeText.title(p) + "  |  " + ChallengeText.tierName(p.tier), style = MaterialTheme.typography.titleMedium)
                            SoftBar(prog.fraction.toFloat(), color = Gold)
                            Text(
                                if (sh.status == WeekStatus.WON) "Complete" else ChallengeText.progress(p, prog) + "  |  ${sh.daysLeft} day(s) left",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        else -> Text("The shrine is quiet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Journey teaser.
            val j = journey
            if (j != null) {
                Panel(modifier = Modifier.clickable { onNavigate(Tab.JOURNEY) }) {
                    SectionTitle("The Road")
                    val next = j.next
                    Text(
                        if (next == null) "You have arrived." else "${Fmt.n(Math.round(j.kmToNext))} km to ${next.name}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SoftBar(j.legFraction, color = Ember)
                    Text(
                        "${j.chapter.title}  |  ${Fmt.n(Math.round(j.km))} of ${Fmt.n(Math.round(j.routeKm))} km",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Level.
            val lv = s.level
            Panel(modifier = Modifier.clickable { onNavigate(Tab.PROFILE) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TierEmblem(lv, emblemSize = 88.dp)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.fillMaxWidth()) {
                        Text(lv.rankTitle, style = MaterialTheme.typography.titleLarge)
                        Text("Level ${lv.level}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        SoftBar(lv.fractionIntoLevel, color = Ember)
                        Text(
                            "${Fmt.n(lv.xpIntoLevel)} / ${Fmt.n(lv.xpForLevel)} XP",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    "Total ${Fmt.n(lv.totalXp)} XP",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        HealthConnectCard(app)
        Spacer(Modifier.height(8.dp))
    }
}
