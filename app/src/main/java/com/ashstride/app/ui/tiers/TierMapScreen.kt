package com.ashstride.app.ui.tiers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ashstride.app.AshstrideApp
import com.ashstride.app.domain.FlameInfo
import com.ashstride.app.domain.LevelCurve
import com.ashstride.app.domain.LevelInfo
import com.ashstride.app.domain.ProgressSnapshot
import com.ashstride.app.domain.Tier
import com.ashstride.app.ui.Fmt
import com.ashstride.app.ui.components.BackHeader
import com.ashstride.app.ui.components.FLAME_TIER_DESCRIPTIONS
import com.ashstride.app.ui.components.FLAME_TIER_NAMES
import com.ashstride.app.ui.components.FlameView
import com.ashstride.app.ui.components.Pill
import com.ashstride.app.ui.components.SingularityStrip
import com.ashstride.app.ui.components.SoftBar
import com.ashstride.app.ui.components.TierEmblem
import com.ashstride.app.ui.components.crystalPalette
import com.ashstride.app.ui.components.flameDaysToNextTier
import com.ashstride.app.ui.components.flamePreviewInfo
import com.ashstride.app.ui.components.flameRoman
import com.ashstride.app.ui.components.flameStyle
import com.ashstride.app.ui.components.flameTierRange
import com.ashstride.app.ui.components.flameTotalDays
import com.ashstride.app.ui.components.tierColor
import com.ashstride.app.ui.theme.AshRaised
import com.ashstride.app.ui.theme.ChamferShape
import com.ashstride.app.ui.theme.Cinder
import com.ashstride.app.ui.theme.Dim
import com.ashstride.app.ui.theme.Gold
import com.ashstride.app.ui.theme.Smoke

/**
 * Two scrolling ladders in the style of a league map: the ten flame tiers and the rank tiers, highest at the top,
 * each in its own colour, with a marker where you stand now. Opens scrolled to your current step.
 */
@Composable
fun TierMapScreen(app: AshstrideApp, contentPadding: PaddingValues, initialTab: Int, onGoalClick: () -> Unit, onBack: () -> Unit) {
    val snap by app.model.snapshot.collectAsStateWithLifecycle()
    val goal by app.model.goal.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BackHeader("Tier map", goal, onGoalClick, onBack)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(AshRaised)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SegButton("Flame", tab == 0, Modifier.weight(1f)) { tab = 0 }
            SegButton("Rank", tab == 1, Modifier.weight(1f)) { tab = 1 }
        }
        val s = snap
        if (s == null) {
            Text("Loading...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (tab == 0) {
            FlameMap(s, Modifier.weight(1f))
        } else {
            RankMap(s.level, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SegButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Gold else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color(0xFF0A0C13) else Smoke,
        )
    }
}

/** The vertical rail with its node. [litAbove] lights the segment towards the higher step, [litBelow] the one towards the lower step. */
@Composable
private fun Rail(accent: Color, reached: Boolean, current: Boolean, first: Boolean, last: Boolean, litAbove: Boolean, litBelow: Boolean) {
    Canvas(Modifier.width(40.dp).fillMaxHeight()) {
        val x = size.width / 2f
        val y = size.height / 2f
        val stroke = 3.dp.toPx()
        if (!first) drawLine(if (litAbove) accent else Cinder, Offset(x, 0f), Offset(x, y), stroke)
        if (!last) drawLine(if (litBelow) accent else Cinder, Offset(x, y), Offset(x, size.height), stroke)
        val r = (if (current) 11.dp else 8.dp).toPx()
        if (current) drawCircle(accent.copy(alpha = 0.30f), r * 1.9f, Offset(x, y))
        drawCircle(if (reached) accent else Cinder, r, Offset(x, y))
        if (current) drawCircle(Color.White, 4.dp.toPx(), Offset(x, y))
    }
}

// ---- Flame --------------------------------------------------------------------------------------

@Composable
private fun FlameMap(s: ProgressSnapshot, modifier: Modifier) {
    val total = flameTotalDays(s.flame)
    val curTier = flameStyle(s.flame).tier
    val state = rememberLazyListState()
    // The list runs from tier X (index 0) down to tier I (index 9); open with the current tier near the top of the screen.
    LaunchedEffect(Unit) { state.scrollToItem((9 - curTier - 1).coerceAtLeast(0)) }
    LazyColumn(state = state, modifier = modifier, contentPadding = PaddingValues(bottom = 16.dp)) {
        items((9 downTo 0).toList()) { tier ->
            FlameTierRow(tier, curTier, total, s.flame)
        }
    }
}

@Composable
private fun FlameTierRow(tier: Int, curTier: Int, total: Int, live: FlameInfo) {
    val reached = tier <= curTier
    val current = tier == curTier
    val info = if (current) live else flamePreviewInfo(tier)
    val pal = crystalPalette(info)
    val accent = pal.glow
    val shape = ChamferShape(14.dp)
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Rail(accent, reached, current, first = tier == 9, last = tier == 0, litAbove = tier + 1 <= curTier, litBelow = reached)
        Row(
            Modifier
                .weight(1f)
                .padding(bottom = 12.dp)
                .clip(shape)
                .background(Brush.horizontalGradient(listOf(lerp(AshRaised, pal.deep, if (reached) 0.55f else 0.22f), AshRaised)), shape)
                .border(if (current) 2.dp else 1.dp, if (reached) accent else Cinder, shape)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(width = 64.dp, height = 86.dp).alpha(if (reached) 1f else 0.45f)) {
                FlameView(info, Modifier.fillMaxSize(), halo = reached)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("TIER " + flameRoman(tier), style = MaterialTheme.typography.labelMedium, color = if (reached) accent else Dim)
                    if (current) Pill("YOU ARE HERE", Gold)
                }
                Text(FLAME_TIER_NAMES[tier], style = MaterialTheme.typography.titleMedium, color = if (reached) MaterialTheme.colorScheme.onSurface else Smoke)
                Text(flameTierRange(tier), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(FLAME_TIER_DESCRIPTIONS[tier], style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                when {
                    current -> {
                        val toNext = flameDaysToNextTier(live)
                        if (toNext != null) {
                            SoftBar((total - tier * 10) / 10f, color = accent)
                            Text(
                                "Day ${total - tier * 10 + 1} of 10  |  $toNext day(s) to ${FLAME_TIER_NAMES[tier + 1]}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Smoke,
                            )
                        } else {
                            Text("The last tier. Day ${Fmt.n(total)} and still burning.", style = MaterialTheme.typography.labelSmall, color = Smoke)
                        }
                    }
                    reached -> Text("Reached", style = MaterialTheme.typography.labelSmall, color = accent)
                    else -> Text("${tier * 10 - total} day(s) to go", style = MaterialTheme.typography.labelSmall, color = Dim)
                }
                if (tier == 9) SingularityStrip(total)
            }
        }
    }
}

// ---- Rank ---------------------------------------------------------------------------------------

private class RankNode(
    val title: String,
    val tier: Tier,
    val startLevel: Int,
    val endLevel: Int?,
    val color: Color,
)

private val IMMORTAL_COLORS = listOf(
    Color(0xFFFF6E9C), Color(0xFFE040FB), Color(0xFF7C8CFF), Color(0xFF40C4FF), Color(0xFF69F0AE), Color(0xFFFFD740),
)

private fun rankNodes(level: LevelInfo): List<RankNode> {
    val nodes = ArrayList<RankNode>()
    val tiers = Tier.values().filter { it != Tier.IMMORTAL }
    for ((i, t) in tiers.withIndex()) {
        val end = (if (i + 1 < tiers.size) tiers[i + 1].firstLevel else Tier.IMMORTAL.firstLevel) - 1
        nodes.add(RankNode(t.title, t, t.firstLevel, end, tierColor(t)))
    }
    val shown = maxOf(3, level.immortalRank + 2)
    for (r in 1..shown) {
        val start = LevelCurve.IMMORTAL_START + (r - 1) * LevelCurve.LEVELS_PER_IMMORTAL_RANK
        nodes.add(
            RankNode(
                "Immortal " + LevelCurve.roman(r), Tier.IMMORTAL, start,
                start + LevelCurve.LEVELS_PER_IMMORTAL_RANK - 1,
                IMMORTAL_COLORS[(r - 1) % IMMORTAL_COLORS.size],
            ),
        )
    }
    return nodes.reversed()
}

@Composable
private fun RankMap(level: LevelInfo, modifier: Modifier) {
    val nodes = rankNodes(level)
    val curIndex = nodes.indexOfFirst { n -> level.level >= n.startLevel && (n.endLevel == null || level.level <= n.endLevel) }
    val state = rememberLazyListState()
    LaunchedEffect(Unit) { state.scrollToItem((curIndex - 1).coerceAtLeast(0)) }
    LazyColumn(state = state, modifier = modifier, contentPadding = PaddingValues(bottom = 16.dp)) {
        items(nodes.size) { i ->
            RankRow(nodes[i], level, current = i == curIndex, first = i == 0, last = i == nodes.size - 1, aboveReached = i - 1 >= 0 && level.level >= nodes[i - 1].startLevel, isNext = i == curIndex - 1)
        }
    }
}

@Composable
private fun RankRow(n: RankNode, level: LevelInfo, current: Boolean, first: Boolean, last: Boolean, aboveReached: Boolean, isNext: Boolean) {
    val reached = level.level >= n.startLevel
    val accent = n.color
    val shape = ChamferShape(14.dp)
    // The emblem as it looks at the first level of this step.
    val emblemInfo = LevelCurve.infoFor(LevelCurve.xpToReach(n.startLevel))
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Rail(accent, reached, current, first, last, litAbove = aboveReached, litBelow = reached)
        Row(
            Modifier
                .weight(1f)
                .padding(bottom = 12.dp)
                .clip(shape)
                .background(Brush.horizontalGradient(listOf(lerp(AshRaised, accent, if (reached) 0.30f else 0.10f), AshRaised)), shape)
                .border(if (current) 2.dp else 1.dp, if (reached) accent else Cinder, shape)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.alpha(if (reached) 1f else 0.5f)) { TierEmblem(emblemInfo, emblemSize = 64.dp) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (n.endLevel == null) "LEVEL ${n.startLevel}+" else "LEVELS ${n.startLevel} TO ${n.endLevel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (reached) accent else Dim,
                    )
                    if (current) Pill("YOU ARE HERE", Gold)
                }
                Text(n.title, style = MaterialTheme.typography.titleMedium, color = if (reached) MaterialTheme.colorScheme.onSurface else Smoke)
                val need = LevelCurve.xpToReach(n.startLevel)
                Text(
                    if (n.tier == Tier.IMMORTAL) "From ${Fmt.n(need)} XP. A new rank every ${LevelCurve.LEVELS_PER_IMMORTAL_RANK} levels, with no end."
                    else "From ${Fmt.n(need)} XP. A new gem on the emblem every 5 levels.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when {
                    current -> {
                        Text(
                            "Level ${level.level}  |  ${Fmt.n(level.xpIntoLevel)} / ${Fmt.n(level.xpForLevel)} XP to the next level",
                            style = MaterialTheme.typography.labelSmall,
                            color = Smoke,
                        )
                        SoftBar(level.fractionIntoLevel, color = accent)
                    }
                    reached -> Text("Reached", style = MaterialTheme.typography.labelSmall, color = accent)
                    else -> {
                        val toGo = need - level.totalXp
                        Text(
                            (if (isNext) "Next: " else "") + "${Fmt.n(toGo)} XP to go",
                            style = MaterialTheme.typography.labelSmall,
                            color = Dim,
                        )
                    }
                }
            }
        }
    }
}
