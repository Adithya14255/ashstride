package com.ashstride.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ashstride.app.domain.FlameEngine
import com.ashstride.app.ui.theme.Dim
import com.ashstride.app.ui.theme.Gold
import com.ashstride.app.ui.theme.Smoke

/**
 * The colour stages of Eternal Singularity: one chip per stage in its real flame colour, the day range under it, the
 * stage you are on outlined and the ones still ahead dimmed.
 */
@Composable
fun SingularityStrip(total: Int, modifier: Modifier = Modifier) {
    val current = FlameEngine.singularityStageIndex(total)
    val colors = remember { SINGULARITY_STAGES.map { singularityStageColor(it) } }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (rowStart in 0 until SINGULARITY_STAGES.size step 4) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in rowStart until minOf(rowStart + 4, SINGULARITY_STAGES.size)) {
                    val s = SINGULARITY_STAGES[i]
                    val reached = i <= current && current >= 0
                    val here = i == current
                    Column(Modifier.weight(1f).alpha(if (reached) 1f else 0.5f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        androidx.compose.foundation.layout.Box(
                            Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors[i])
                                .border(if (here) 2.dp else 1.dp, if (here) Color.White else Dim, RoundedCornerShape(6.dp)),
                        )
                        Text(s.name, style = MaterialTheme.typography.labelSmall, color = if (here) Gold else Smoke, maxLines = 2)
                        Text("Day " + s.range, style = MaterialTheme.typography.labelSmall, color = Dim, maxLines = 1)
                    }
                }
            }
        }
        val toNext = FlameEngine.singularityDaysToNext(total)
        val note = buildString {
            if (current >= 0) append("You are on ${SINGULARITY_STAGES[current].name}.")
            if (toNext != null) append(" The colour changes in $toNext day(s).")
            append(" From day 320 the six colours repeat, every 120 days.")
        }
        Text(note.trim(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
