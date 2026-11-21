package com.ashstride.app.ui.home

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ashstride.app.AshstrideApp
import com.ashstride.app.ui.Fmt
import com.ashstride.app.ui.components.FlameView
import com.ashstride.app.ui.components.Panel
import com.ashstride.app.ui.components.ScreenHeader
import com.ashstride.app.ui.components.StepRing
import com.ashstride.app.ui.components.TierEmblem
import com.ashstride.app.ui.health.HealthConnectCard

@Composable
fun HomeScreen(app: AshstrideApp, contentPadding: PaddingValues, onGoalClick: () -> Unit) {
    val model = app.model
    val ledger by model.ledger.collectAsStateWithLifecycle()
    val snap by model.snapshot.collectAsStateWithLifecycle()
    val today by model.today.collectAsStateWithLifecycle()
    val goal by model.goal.collectAsStateWithLifecycle()

    val todayRow = ledger.firstOrNull { it.date == today }
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
            StepRing(steps = steps, goal = todayGoal, modifier = Modifier.size(220.dp).align(Alignment.CenterHorizontally)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Fmt.n(steps), style = MaterialTheme.typography.displayMedium)
                    Text("of ${Fmt.n(todayGoal)} steps", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Fmt.km(km), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                FlameView(s.flame, Modifier.width(112.dp).height(140.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(Fmt.n(s.streak.total), style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
                    Text("day streak", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Pure ${Fmt.n(s.streak.pure)}  |  Best ${Fmt.n(s.streak.bestTotal)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (s.streak.atRisk) {
                val left = (todayGoal - steps).coerceAtLeast(0)
                Panel {
                    Text("Keep the fire alive", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                    Text(
                        "${Fmt.n(left)} more steps today keeps your ${Fmt.n(s.streak.total)}-day streak going.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val lv = s.level
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TierEmblem(lv)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.fillMaxWidth()) {
                        Text(lv.rankTitle, style = MaterialTheme.typography.titleLarge)
                        Text("Level ${lv.level}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { lv.fractionIntoLevel.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
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
