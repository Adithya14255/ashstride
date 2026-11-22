package com.ashstride.app.ui.streak

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ashstride.app.AshstrideApp
import com.ashstride.app.domain.CalendarGrid
import com.ashstride.app.domain.LedgerDay
import com.ashstride.app.domain.MendCheck
import com.ashstride.app.domain.MenderRules
import com.ashstride.app.domain.StreakEngine
import com.ashstride.app.ui.Fmt
import com.ashstride.app.ui.components.Panel
import com.ashstride.app.ui.components.ScreenHeader
import com.ashstride.app.ui.components.StatRow
import com.ashstride.app.ui.theme.AshHigh
import com.ashstride.app.ui.theme.AshRaised
import com.ashstride.app.ui.theme.Cinder
import com.ashstride.app.ui.theme.Dim
import com.ashstride.app.ui.theme.Ember
import com.ashstride.app.ui.theme.Gold
import com.ashstride.app.ui.theme.Parchment
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun StreakScreen(app: AshstrideApp, contentPadding: PaddingValues, onGoalClick: () -> Unit) {
    val model = app.model
    val ledger by model.ledger.collectAsStateWithLifecycle()
    val snap by model.snapshot.collectAsStateWithLifecycle()
    val today by model.today.collectAsStateWithLifecycle()
    val goal by model.goal.collectAsStateWithLifecycle()
    val inventory by model.inventory.collectAsStateWithLifecycle()

    var month by remember { mutableStateOf(YearMonth.from(LocalDate.now())) }
    var selected by remember { mutableStateOf<LocalDate?>(null) }
    val byDate = remember(ledger) { ledger.associateBy { it.date } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader("Streak", goal, onGoalClick)

        val s = snap
        if (s != null) {
            Panel {
                StatRow("Total streak", "${Fmt.n(s.streak.total)} days")
                StatRow("Pure streak", "${Fmt.n(s.streak.pure)} days")
                StatRow("Best total", "${Fmt.n(s.streak.bestTotal)} days")
                StatRow("Best pure", "${Fmt.n(s.streak.bestPure)} days")
                StatRow("Menders", "${inventory.menders} / ${MenderRules.MAX_MENDERS}  (${inventory.shards}/${MenderRules.SHARDS_PER_MENDER} shards)")
            }
        }

        Panel {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = { month = month.minusMonths(1) }) { Text("<") }
                Text(
                    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()).format(month),
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(onClick = { month = month.plusMonths(1) }, enabled = month.isBefore(YearMonth.from(today))) { Text(">") }
            }
            Row(Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                    Text(
                        it,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            CalendarGrid.weeks(month).forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    week.forEach { date ->
                        Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                            if (date != null) DayCell(date, byDate[date], today) { selected = date }
                        }
                    }
                }
            }
            Spacer(Modifier.size(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                LegendDot(Ember, "Met")
                LegendDot(Gold, "Mended")
                LegendDot(AshHigh, "Missed")
            }
            Text(
                "Tap a day to see it or mend it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    val sel = selected
    if (sel != null) {
        DayDialog(
            date = sel,
            day = byDate[sel],
            ledger = ledger,
            today = today,
            menders = inventory.menders,
            onDismiss = { selected = null },
            app = app,
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DayCell(date: LocalDate, day: LedgerDay?, today: LocalDate, onClick: () -> Unit) {
    val future = date.isAfter(today)
    val isToday = date == today
    val bg: Color
    val fg: Color
    var ring: Color? = null
    when {
        day == null || future -> { bg = Color.Transparent; fg = Dim }
        day.met -> { bg = Ember; fg = MaterialTheme.colorScheme.onPrimary }
        day.mended -> { bg = AshHigh; fg = Gold; ring = Gold }
        else -> { bg = AshRaised; fg = MaterialTheme.colorScheme.onSurfaceVariant }
    }
    if (isToday && ring == null) ring = Parchment
    var mod = Modifier.fillMaxSize().padding(2.dp).clip(CircleShape).background(bg, CircleShape)
    if (ring != null) mod = mod.border(2.dp, ring, CircleShape)
    if (day != null && !future) mod = mod.clickable(onClick = onClick)
    Box(mod, contentAlignment = Alignment.Center) {
        Text("${date.dayOfMonth}", color = fg, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun DayDialog(
    date: LocalDate,
    day: LedgerDay?,
    ledger: List<LedgerDay>,
    today: LocalDate,
    menders: Int,
    onDismiss: () -> Unit,
    app: AshstrideApp,
) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    val preview = StreakEngine.previewMend(ledger, date, today)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.getDefault()).format(date)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (day != null) {
                    Text("${Fmt.n(day.steps)} of ${Fmt.n(day.goal)} steps  |  ${Fmt.km(day.distanceKm)}")
                    val status = when {
                        day.met -> "Goal met."
                        day.mended -> "Mended: counts for the streak, no steps or XP."
                        else -> "Goal missed."
                    }
                    Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (preview.allowed) {
                    Text(
                        "Mend this day? Total streak ${Fmt.n(preview.totalBefore)} -> ${Fmt.n(preview.totalAfter)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        "Using a Mender is permanent. A mended day counts for streak and badges only.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (menders <= 0) {
                        Text(
                            "You have no Menders. Win Hard or Legendary trials at the Shrine to earn shards: three shards forge a Mender.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else if (day != null && !day.met && !day.mended) {
                    val reason = when (preview.check) {
                        MendCheck.BELOW_FLOOR -> "Too few steps to mend: needs at least ${Fmt.n(StreakEngine.mendFloor(day.goal))} (25% of that day's goal)."
                        else -> "This day cannot be mended."
                    }
                    Text(reason, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            if (preview.allowed && menders > 0) {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        busy = true
                        scope.launch {
                            app.model.mend(date)
                            busy = false
                            onDismiss()
                        }
                    },
                ) { Text("Use Mender") }
            } else {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        dismissButton = {
            if (preview.allowed && menders > 0) TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
