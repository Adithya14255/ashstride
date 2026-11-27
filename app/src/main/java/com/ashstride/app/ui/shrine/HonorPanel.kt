package com.ashstride.app.ui.shrine

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ashstride.app.AshstrideApp
import com.ashstride.app.domain.BadgeKind
import com.ashstride.app.domain.FeatEngine
import com.ashstride.app.domain.FeatLadder
import com.ashstride.app.domain.HonorRules
import com.ashstride.app.ui.Fmt
import com.ashstride.app.ui.components.BadgeArt
import com.ashstride.app.ui.components.Panel
import com.ashstride.app.ui.components.SectionTitle
import com.ashstride.app.ui.components.SoftBar
import com.ashstride.app.ui.theme.Gold
import kotlinx.coroutines.launch

/**
 * Great feats at the shrine: honor earned from single big efforts (any day, trial or not), what the next rung of each
 * ladder is, and the redeem button that turns honor into Mender Shards.
 */
@Composable
fun HonorPanel(app: AshstrideApp, onOpenBadges: () -> Unit) {
    val feats by app.model.feats.collectAsStateWithLifecycle()
    val honor by app.model.honor.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Panel(tint = Gold) {
        SectionTitle("Great feats and honor")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column {
                Text("Honor", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                Text(Fmt.n(honor.balance), style = MaterialTheme.typography.headlineMedium, color = Gold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Feats earned", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                Text(feats.records.size.toString(), style = MaterialTheme.typography.headlineMedium)
            }
        }
        SoftBar(honor.balance.coerceAtMost(HonorRules.SHARD_COST) / HonorRules.SHARD_COST.toFloat(), color = Gold)
        Button(
            enabled = honor.canRedeem,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch {
                    val ok = app.model.redeemHonor()
                    Toast.makeText(context, if (ok) "A shard is yours" else "Cannot redeem right now", Toast.LENGTH_SHORT).show()
                }
            },
        ) { Text("Redeem ${HonorRules.SHARD_COST} honor for 1 shard") }
        Text(
            when {
                honor.redeemedThisWeek >= HonorRules.WEEKLY_LIMIT -> "You have redeemed ${HonorRules.WEEKLY_LIMIT} shards this week. Honor keeps until next Monday."
                honor.balance < HonorRules.SHARD_COST -> "${HonorRules.SHARD_COST - honor.balance} more honor for the next shard. At most ${HonorRules.WEEKLY_LIMIT} shards can be redeemed each week."
                else -> "${honor.redeemedThisWeek} of ${HonorRules.WEEKLY_LIMIT} redeemed this week."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        for (ladder in FeatLadder.values()) {
            val next = FeatEngine.nextRung(feats, ladder)
            val best = feats.best(ladder)
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (next == null) {
                    Text("${ladder.label}: every rung reached.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    BadgeArt(kind = BadgeKind.FEAT, rim = next.rim, glyph = next.glyph, stitched = false, locked = true, badgeSize = 44.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(next.title + "  |  " + next.description, style = MaterialTheme.typography.bodyMedium)
                        SoftBar((best / next.threshold).toFloat(), color = Gold, height = 6)
                        Text(
                            "Best so far " + (if (ladder == FeatLadder.DAY_DISTANCE) Fmt.km(best) else Fmt.n(best.toLong()) + " steps") + "  |  +${next.honor} honor",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Text(
            "A feat counts whenever it happens, trial week or not. The first time earns a badge and full honor; repeating it pays half as much each time, down to a fixed minimum.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onOpenBadges) { Text("See all feats and badges") }
    }
}
