package com.ashstride.app.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import com.ashstride.app.ui.Fmt
import com.ashstride.app.ui.theme.AshHigh
import com.ashstride.app.ui.theme.AshRaised
import com.ashstride.app.ui.theme.Ember
import com.ashstride.app.ui.theme.Hairline

/** Title row used on every screen. The goal is always visible and tappable. */
@Composable
fun ScreenHeader(title: String, goal: Int, onGoalClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Serif))
        Text(
            "Goal ${Fmt.n(goal)}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onGoalClick)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, Hairline, RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
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
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onBack)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, Hairline, RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
        ScreenHeader(title, goal, onGoalClick)
    }
}

/** A raised card. [tint] washes a little colour over the top, used for the flame hero and the shrine. */
@Composable
fun Panel(modifier: Modifier = Modifier, tint: Color? = null, content: @Composable ColumnScope.() -> Unit) {
    val top = if (tint == null) Color(0xFF453B33) else androidx.compose.ui.graphics.lerp(Color(0xFF453B33), tint, 0.18f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(top, AshRaised)))
            .border(1.dp, Hairline, RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Serif),
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

/** Rounded progress bar with no Material dependencies, so it looks the same everywhere. */
@Composable
fun SoftBar(fraction: Float, modifier: Modifier = Modifier, color: Color = Ember, height: Int = 8) {
    val f = fraction.coerceIn(0f, 1f)
    Box(
        modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(50))
            .background(AshHigh),
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
