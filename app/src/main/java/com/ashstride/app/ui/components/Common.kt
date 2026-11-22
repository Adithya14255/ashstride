package com.ashstride.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashstride.app.ui.Fmt
import com.ashstride.app.ui.theme.AshHigh
import com.ashstride.app.ui.theme.AshRaised
import com.ashstride.app.ui.theme.Ember
import com.ashstride.app.ui.theme.ChamferShape
import com.ashstride.app.ui.theme.Gilt
import com.ashstride.app.ui.theme.giltBorder

/** Title row used on every screen. The goal is always visible and tappable. */
@Composable
fun ScreenHeader(title: String, goal: Int, onGoalClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                letterSpacing = 0.5.sp,
            ),
            color = Gilt.Cream,
        )
        Text(
            "Goal ${Fmt.n(goal)}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .clip(ChamferShape(10.dp))
                .clickable(onClick = onGoalClick)
                .background(Gilt.NightLift)
                .giltBorder(ChamferShape(10.dp), 1.dp)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
    GiltDivider()
    }
}

/** Header for pages opened from Profile (Stats, Badges): back chip, title, and the goal chip like every other screen. */
@Composable
fun BackHeader(title: String, goal: Int, onGoalClick: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "< Back",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .clip(ChamferShape(10.dp))
                .clickable(onClick = onBack)
                .background(Gilt.NightLift)
                .giltBorder(ChamferShape(10.dp), 1.dp)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
        ScreenHeader(title, goal, onGoalClick)
    }
}

/** A raised card. [tint] washes a little colour over the top, used for the flame hero and the shrine. */
@Composable
fun Panel(modifier: Modifier = Modifier, tint: Color? = null, content: @Composable ColumnScope.() -> Unit) {
    val top = if (tint == null) Gilt.NightLift else androidx.compose.ui.graphics.lerp(Gilt.NightLift, tint, 0.16f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(ChamferShape(18.dp))
            .background(Brush.verticalGradient(listOf(top, AshRaised)))
            .giltBorder(ChamferShape(18.dp), 1.5.dp, 0.8f)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, letterSpacing = 0.4.sp),
        color = Gilt.Cream,
        modifier = modifier,
    )
}

@Composable
fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

/** A small rounded label. */
@Composable
fun Pill(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** A hairline of gold with a diamond in the middle, under screen titles. */
@Composable
fun GiltDivider(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(10.dp)) {
        val y = size.height / 2f
        val mid = size.width / 2f
        val gap = 14.dp.toPx()
        drawLine(Gilt.Line.copy(alpha = 0.8f), androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(mid - gap, y), strokeWidth = 1.5.dp.toPx())
        drawLine(Gilt.Line.copy(alpha = 0.8f), androidx.compose.ui.geometry.Offset(mid + gap, y), androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1.5.dp.toPx())
        val r = 4.dp.toPx()
        val p = Path()
        p.moveTo(mid, y - r * 1.25f)
        p.lineTo(mid + r, y)
        p.lineTo(mid, y + r * 1.25f)
        p.lineTo(mid - r, y)
        p.close()
        drawPath(p, Gilt.GoldMid)
    }
}

/** Rounded progress bar with no Material dependencies, so it looks the same everywhere. */
@Composable
fun SoftBar(fraction: Float, modifier: Modifier = Modifier, color: Color = Ember, height: Int = 8) {
    val f = fraction.coerceIn(0f, 1f)
    Box(
        modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(50))
            .background(AshHigh)
            .border(1.dp, Gilt.Line.copy(alpha = 0.5f), RoundedCornerShape(50)),
    ) {
        if (f > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(f)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.75f), color))),
            )
        }
    }
}

private val GOAL_CHOICES = listOf(5_000, 7_500, 10_000, 12_500, 15_000, 20_000)

@Composable
fun GoalDialog(current: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily goal") },
        text = {
            Column {
                Text(
                    "Applies from today. Past days keep the goal they had. XP and challenge targets scale with it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                GOAL_CHOICES.forEach { g ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(g) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = current == g, onClick = null)
                        Text(Fmt.n(g), modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}
