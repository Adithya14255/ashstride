package com.ashstride.app.ui.badges

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ashstride.app.AshstrideApp
import com.ashstride.app.domain.BadgeCatalog
import com.ashstride.app.domain.BadgeEntry
import com.ashstride.app.domain.BadgeEvaluator
import com.ashstride.app.domain.BadgeKind
import com.ashstride.app.domain.BadgeKey
import com.ashstride.app.domain.BadgeRim
import com.ashstride.app.domain.BadgeVariant
import com.ashstride.app.ui.Fmt
import com.ashstride.app.ui.components.BackHeader
import com.ashstride.app.ui.components.BadgeArt
import com.ashstride.app.ui.components.Panel
import com.ashstride.app.ui.theme.Smoke
import java.time.format.DateTimeFormatter
import java.util.Locale

private sealed interface Row2 {
    data class Header(val text: String) : Row2
    data class Earned(val entry: BadgeEntry) : Row2
    data class Locked(val title: String, val sub: String, val kind: BadgeKind, val glyph: String) : Row2
}

@Composable
fun BadgesScreen(app: AshstrideApp, contentPadding: PaddingValues, onGoalClick: () -> Unit, onBack: () -> Unit) {
    val model = app.model
    val snap by model.snapshot.collectAsStateWithLifecycle()
    val stats by model.stats.collectAsStateWithLifecycle()
    val goal by model.goal.collectAsStateWithLifecycle()
    val entries by model.badgeEntries.collectAsStateWithLifecycle()
    val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    var open by remember { mutableStateOf<BadgeEntry?>(null) }

    val s = snap
    val rows = ArrayList<Row2>()
    if (s != null) {
        rows += Row2.Header("Earned (${entries.size})")
        for (e in entries) rows += Row2.Earned(e)

        val have = s.badges.map { it.key }.toSet()
        val locked = ArrayList<Row2>()
        for (m in BadgeEvaluator.STREAK_MILESTONES) {
            val id = BadgeEvaluator.streakBadgeId(m)
            val got = BadgeKey(id, BadgeVariant.PURE) in have || BadgeKey(id, BadgeVariant.MENDED) in have
            if (!got) {
                locked += Row2.Locked(
                    BadgeCatalog.titleForId(id),
                    "Streak now ${Fmt.n(s.streak.total)} / $m  |  +${Fmt.n(BadgeEvaluator.streakXp(m, BadgeVariant.PURE))} XP",
                    BadgeKind.STREAK, "$m",
                )
            }
        }
        val total = stats?.totalSteps ?: 0L
        for (m in BadgeEvaluator.STEP_MILESTONES) {
            val id = BadgeEvaluator.stepsBadgeId(m)
            if (BadgeKey(id, BadgeVariant.STANDARD) !in have) {
                locked += Row2.Locked(
                    BadgeCatalog.titleForId(id),
                    "${Fmt.n(total)} / ${Fmt.n(m)}  |  +${Fmt.n(BadgeEvaluator.stepsXp(m))} XP",
                    BadgeKind.STEPS, BadgeCatalog.glyphForSteps(m),
                )
            }
        }
        rows += Row2.Header("Locked (${locked.size})")
        rows.addAll(locked)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp + contentPadding.calculateLeftPadding(LayoutDirection.Ltr),
            end = 20.dp,
            top = 12.dp + contentPadding.calculateTopPadding(),
            bottom = 12.dp + contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { BackHeader("Badges", goal, onGoalClick, onBack) }
        if (s == null) item { Text("Loading your history...", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(rows) { r ->
            when (r) {
                is Row2.Header -> Text(r.text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                is Row2.Earned -> {
                    val e = r.entry
                    Panel(modifier = Modifier.clickable { open = e }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BadgeArt(e.kind, e.rim, e.glyph, e.stitched, badgeSize = 60.dp)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.fillMaxWidth()) {
                                Text(e.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${e.variantLabel}  |  ${dateFmt.format(e.date)}  |  +${Fmt.n(e.xp)} XP",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                is Row2.Locked -> Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BadgeArt(r.kind, BadgeRim.BRONZE, r.glyph, stitched = false, locked = true, badgeSize = 60.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.fillMaxWidth()) {
                            Text(r.title, style = MaterialTheme.typography.titleMedium, color = Smoke)
                            Text(r.sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    val shown = open
    if (shown != null) {
        AlertDialog(
            onDismissRequest = { open = null },
            title = { Text(shown.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${shown.variantLabel} badge, earned ${dateFmt.format(shown.date)}.")
                    Text("+${Fmt.n(shown.xp)} XP", color = MaterialTheme.colorScheme.secondary)
                    Text(
                        "Equip it or add it to your showcase from the Profile page.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = { TextButton(onClick = { open = null }) { Text("Close") } },
        )
    }
}
