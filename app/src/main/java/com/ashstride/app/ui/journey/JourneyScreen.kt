package com.ashstride.app.ui.journey

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ashstride.app.AshstrideApp
import com.ashstride.app.domain.JourneyPosition
import com.ashstride.app.domain.Story
import com.ashstride.app.domain.Waypoint
import com.ashstride.app.ui.Fmt
import com.ashstride.app.ui.components.FlameShapes
import com.ashstride.app.ui.components.Panel
import com.ashstride.app.ui.components.Pill
import com.ashstride.app.ui.components.ScreenHeader
import com.ashstride.app.ui.components.SectionTitle
import com.ashstride.app.ui.components.SoftBar
import com.ashstride.app.ui.theme.Cinder
import com.ashstride.app.ui.theme.Ember
import com.ashstride.app.ui.theme.Gold
import com.ashstride.app.ui.theme.Smoke

private enum class WpState { REACHED, NEXT, FAR }

@Composable
fun JourneyScreen(app: AshstrideApp, contentPadding: PaddingValues, onGoalClick: () -> Unit) {
    val model = app.model
    val journey by model.journey.collectAsStateWithLifecycle()
    val goal by model.goal.collectAsStateWithLifecycle()
    val story = model.story

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader("Journey", goal, onGoalClick)
        val j = journey
        if (j == null) {
            Text("Loading your history...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Panel(tint = Ember) {
                Text(story.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                Text(
                    "${Fmt.n(Math.round(j.km))} km",
                    style = MaterialTheme.typography.displayMedium,
                )
                SoftBar(j.fraction, color = Ember, height = 10)
                Text(
                    "${(j.fraction * 100f).toInt()}% of ${Fmt.n(Math.round(j.routeKm))} km to ${story.destination.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Pill(j.chapter.title, Gold)
                if (j.chapter.intro.isNotEmpty()) {
                    Text(j.chapter.intro, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
                val next = j.next
                if (next != null) {
                    Text(
                        "Next: ${next.name}, ${Fmt.n(Math.round(j.kmToNext))} km ahead",
                        style = MaterialTheme.typography.titleMedium,
                    )
                } else {
                    Text("You have reached the end of the road.", style = MaterialTheme.typography.titleMedium)
                }
            }

            SectionTitle("The road")
            Trail(story, j)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Trail(story: Story, j: JourneyPosition) {
    val nextKm = j.next?.km
    var lastChapter = ""
    Column {
        for ((index, w) in story.waypoints.withIndex()) {
            val state = when {
                w.km <= j.km -> WpState.REACHED
                nextKm != null && w.km == nextKm -> WpState.NEXT
                else -> WpState.FAR
            }
            if (w.chapterId != lastChapter) {
                lastChapter = w.chapterId
                val ch = story.chapters.firstOrNull { it.id == w.chapterId }
                if (ch != null) {
                    Text(
                        ch.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (ch.startKm <= j.km) Gold else Smoke,
                        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
                    )
                }
            }
            WaypointRow(w, state, first = index == 0, last = index == story.waypoints.lastIndex)
            // The traveller sits between the last waypoint passed and the next one.
            if (state == WpState.REACHED && nextKm != null && story.waypoints.getOrNull(index + 1)?.km == nextKm) {
                HereRow(j)
            }
        }
    }
}

@Composable
private fun WaypointRow(w: Waypoint, state: WpState, first: Boolean, last: Boolean) {
    val lit = state == WpState.REACHED
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Canvas(Modifier.width(36.dp).fillMaxHeight()) {
            val x = size.width / 2f
            val lineColor = if (lit) Ember else Cinder
            if (!first) drawLine(lineColor, Offset(x, 0f), Offset(x, 14.dp.toPx()), strokeWidth = 3.dp.toPx())
            if (!last) drawLine(if (lit) Ember else Cinder, Offset(x, 14.dp.toPx()), Offset(x, size.height), strokeWidth = 3.dp.toPx())
            val fill = when (state) {
                WpState.REACHED -> Ember
                WpState.NEXT -> Gold
                WpState.FAR -> Cinder
            }
            drawCircle(fill, radius = 7.dp.toPx(), center = Offset(x, 14.dp.toPx()))
            if (state == WpState.REACHED) drawCircle(Color(0xFF211D1A), radius = 2.5.dp.toPx(), center = Offset(x, 14.dp.toPx()))
        }
        Column(Modifier.weight(1f).padding(bottom = 14.dp)) {
            Text(
                if (state == WpState.FAR) "Unknown place" else w.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (state == WpState.FAR) Smoke else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "${Fmt.n(Math.round(w.km))} km",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (lit && w.text.isNotEmpty()) {
                Text(w.text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HereRow(j: JourneyPosition) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Canvas(Modifier.width(36.dp).fillMaxHeight()) {
            val x = size.width / 2f
            val split = size.height * j.legFraction
            drawLine(Ember, Offset(x, 0f), Offset(x, split), strokeWidth = 3.dp.toPx())
            drawLine(Cinder, Offset(x, split), Offset(x, size.height), strokeWidth = 3.dp.toPx())
        }
        Row(Modifier.padding(vertical = 10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Canvas(Modifier.width(20.dp).height(26.dp)) {
                drawPath(FlameShapes.path(FlameShapes.OUTER, 0f, 0f, size.width, size.height), Ember)
                drawPath(FlameShapes.path(FlameShapes.MID, 0f, 0f, size.width, size.height), Gold)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "You are here  |  ${Fmt.n(Math.round(j.kmToNext))} km to go",
                style = MaterialTheme.typography.labelLarge,
                color = Gold,
            )
        }
    }
}
