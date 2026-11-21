package com.ashstride.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ashstride.app.data.db.AshDatabase
import com.ashstride.app.data.db.LedgerSummary
import com.ashstride.app.data.seed.Seeder
import com.ashstride.app.domain.SimpleStreak
import java.text.NumberFormat
import java.util.Locale

private val groupFmt: NumberFormat = NumberFormat.getIntegerInstance(Locale.US)

@Composable
fun HomeScreen(db: AshDatabase) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { Seeder.seedIfEmpty(context, db) }

    val days by db.dayDao().observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val summary by db.dayDao().observeSummary().collectAsStateWithLifecycle(initialValue = LedgerSummary(0, 0L, 0.0))
    val streak = SimpleStreak.currentRun(days)
    val goal = days.firstOrNull()?.goal ?: Seeder.DEFAULT_GOAL

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Ashstride", style = MaterialTheme.typography.titleLarge)
            // The goal is shown on every screen, not just here.
            Text(
                "Goal ${groupFmt.format(goal)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        Spacer(Modifier.height(56.dp))

        Text(
            groupFmt.format(streak),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "day streak",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(48.dp))

        val s = summary
        val loaded = s.days > 0
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Your history", style = MaterialTheme.typography.titleMedium)
            StatRow("Days loaded", if (!loaded) "-" else groupFmt.format(s.days))
            StatRow("Total steps", if (!loaded) "-" else groupFmt.format(s.steps))
            StatRow("Total distance", if (!loaded) "-" else "${groupFmt.format(Math.round(s.km))} km")
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "Build 0.1: your history is loaded. Health Connect sync, levels, badges and challenges arrive in the next builds.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
