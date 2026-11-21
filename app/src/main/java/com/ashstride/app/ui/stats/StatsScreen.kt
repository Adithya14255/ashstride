package com.ashstride.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ashstride.app.AshstrideApp
import com.ashstride.app.ui.Fmt
import com.ashstride.app.ui.components.Panel
import com.ashstride.app.ui.components.ScreenHeader
import com.ashstride.app.ui.components.StatRow
import com.ashstride.app.ui.theme.Ember
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun StatsScreen(app: AshstrideApp, contentPadding: PaddingValues, onGoalClick: () -> Unit) {
    val model = app.model
    val stats by model.stats.collectAsStateWithLifecycle()
    val goal by model.goal.collectAsStateWithLifecycle()
    val dayFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    val monthFmt = DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader("Stats", goal, onGoalClick)
        val s = stats
        if (s == null) {
            Text("Loading your history...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Panel {
                Text("Lifetime", style = MaterialTheme.typography.titleMedium)
                StatRow("Steps", Fmt.n(s.totalSteps))
                StatRow("Distance", "${Fmt.n(Math.round(s.totalKm))} km")
                StatRow("Days tracked", Fmt.n(s.days))
                StatRow("Goal days", "${Fmt.n(s.daysMet)} (${if (s.days == 0) 0 else s.daysMet * 100 / s.days}%)")
                StatRow("Average per day", Fmt.n(s.averageSteps))
            }

            Panel {
                Text("Average by day of week", style = MaterialTheme.typography.titleMedium)
                BarChart(
                    values = s.dayOfWeekAverage.map { it.toLong() },
                    labels = listOf("M", "T", "W", "T", "F", "S", "S"),
                )
            }

            Panel {
                Text("Last 12 months", style = MaterialTheme.typography.titleMedium)
                BarChart(
                    values = s.monthly.map { it.steps },
                    labels = s.monthly.map { it.month.month.name.take(1) },
                )
                val cur = s.monthly.lastOrNull()
                if (cur != null) {
                    StatRow("This month", "${Fmt.n(cur.steps)} steps")
                }
            }

            Panel {
                Text("Records", style = MaterialTheme.typography.titleMedium)
                val bd = s.bestDay
                if (bd != null) StatRow("Best day", "${Fmt.n(bd.steps)}  (${dayFmt.format(bd.date)})")
                val bw = s.bestWeek
                if (bw != null) StatRow("Best week", "${Fmt.n(bw.steps)}  (from ${dayFmt.format(bw.start)})")
                val bm = s.bestMonth
                if (bm != null) StatRow("Best month", "${Fmt.n(bm.steps)}  (${monthFmt.format(bm.month)})")
                val ld = s.longestDay
                if (ld != null) StatRow("Longest walk", "${Fmt.km(ld.distanceKm)}  (${dayFmt.format(ld.date)})")
            }
        }
    }
}

/** Simple bar chart built from boxes, so it needs no extra libraries. */
@Composable
private fun BarChart(values: List<Long>, labels: List<String>) {
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    Row(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEach { v ->
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                val frac = (v.toFloat() / max.toFloat()).coerceIn(0f, 1f)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(if (v > 0L) frac.coerceAtLeast(0.02f) else 0.01f)
                        .background(if (v > 0L) Ember else Color.Transparent, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
                )
            }
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        labels.forEach {
            Text(
                it,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
