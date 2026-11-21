package com.ashstride.app.ui.badges

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ashstride.app.AshstrideApp
import com.ashstride.app.domain.BadgeEvaluator
import com.ashstride.app.domain.BadgeVariant
import com.ashstride.app.ui.Fmt
import com.ashstride.app.ui.components.Panel
import com.ashstride.app.ui.components.ScreenHeader
import com.ashstride.app.ui.theme.AshHigh
import com.ashstride.app.ui.theme.Cinder
import com.ashstride.app.ui.theme.Gold
import com.ashstride.app.ui.theme.Smoke
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Silver = Color(0xFFC7CDD4)
private val Bronze = Color(0xFFB87333)

private sealed interface Row2 {
    data class Header(val text: String) : Row2
    data class Earned(val title: String, val sub: String, val variant: BadgeVariant) : Row2
    data class Locked(val title: String, val sub: String) : Row2
}

private fun titleFor(badgeId: String): String = when {
    badgeId.startsWith("streak_") -> "${badgeId.removePrefix("streak_")}-day streak"
    badgeId.startsWith("steps_") -> "${Fmt.n(badgeId.removePrefix("steps_").toLong())} lifetime steps"
    else -> badgeId
}

@Composable
fun BadgesScreen(app: AshstrideApp, contentPadding: PaddingValues, onGoalClick: () -> Unit) {
    val model = app.model
    val snap by model.snapshot.collectAsStateWithLifecycle()
    val stats by model.stats.collectAsStateWithLifecycle()
    val goal by model.goal.collectAsStateWithLifecycle()
    val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

    val s = snap
    val rows = ArrayList<Row2>()
    if (s != null) {
        val earned = s.badges.sortedByDescending { it.date }
        rows += Row2.Header("Earned (${earned.size})")
        for (b in earned) {
            val variantLabel = when (b.variant) {
                BadgeVariant.PURE -> "Pure"
                BadgeVariant.MENDED -> "Mended"
                BadgeVariant.STANDARD -> "Lifetime"
            }
            rows += Row2.Earned(titleFor(b.badgeId), "$variantLabel  |  ${dateFmt.format(b.date)}  |  +${Fmt.n(b.xp)} XP", b.variant)
        }
        val have = s.badges.map { it.key }.toSet()
        val locked = ArrayList<Row2>()
        for (m in BadgeEvaluator.STREAK_MILESTONES) {
            val id = BadgeEvaluator.streakBadgeId(m)
            val got = com.ashstride.app.domain.BadgeKey(id, BadgeVariant.PURE) in have ||
                com.ashstride.app.domain.BadgeKey(id, BadgeVariant.MENDED) in have
            if (!got) locked += Row2.Locked(titleFor(id), "Streak now ${Fmt.n(s.streak.total)} / $m  |  +${Fmt.n(BadgeEvaluator.streakXp(m, BadgeVariant.PURE))} XP")
        }
        val total = stats?.totalSteps ?: 0L
        for (m in BadgeEvaluator.STEP_MILESTONES) {
            val id = BadgeEvaluator.stepsBadgeId(m)
            if (com.ashstride.app.domain.BadgeKey(id, BadgeVariant.STANDARD) !in have) {
                locked += Row2.Locked(titleFor(id), "${Fmt.n(total)} / ${Fmt.n(m)}  |  +${Fmt.n(BadgeEvaluator.stepsXp(m))} XP")
            }
        }
        rows += Row2.Header("Locked (${locked.size})")
        rows.addAll(locked)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp + contentPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
            end = 20.dp,
            top = 12.dp + contentPadding.calculateTopPadding(),
            bottom = 12.dp + contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { ScreenHeader("Badges", goal, onGoalClick) }
        if (s == null) item { Text("Loading your history...", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(rows) { r ->
            when (r) {
                is Row2.Header -> Text(r.text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                is Row2.Earned -> BadgeRow(r.title, r.sub, rimFor(r.variant), stitched = r.variant == BadgeVariant.MENDED, dim = false)
                is Row2.Locked -> BadgeRow(r.title, r.sub, Cinder, stitched = false, dim = true)
            }
        }
    }
}

private fun rimFor(v: BadgeVariant): Color = when (v) {
    BadgeVariant.PURE -> Gold
    BadgeVariant.MENDED -> Silver
    BadgeVariant.STANDARD -> Bronze
}

@Composable
private fun BadgeRow(title: String, sub: String, rim: Color, stitched: Boolean, dim: Boolean) {
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(44.dp)) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val r = size.minDimension / 2f - 4.dp.toPx()
                drawCircle(AshHigh, radius = r, center = c)
                drawCircle(
                    color = rim,
                    radius = r,
                    center = c,
                    style = Stroke(
                        width = 4.dp.toPx(),
                        pathEffect = if (stitched) PathEffect.dashPathEffect(floatArrayOf(10f, 7f)) else null,
                    ),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = if (dim) Smoke else MaterialTheme.colorScheme.onSurface)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
