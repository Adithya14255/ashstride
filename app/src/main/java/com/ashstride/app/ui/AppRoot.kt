package com.ashstride.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ashstride.app.AshstrideApp
import com.ashstride.app.ui.badges.BadgesScreen
import com.ashstride.app.ui.celebrate.CelebrationOverlay
import com.ashstride.app.ui.components.GoalDialog
import com.ashstride.app.ui.components.NavGlyph
import com.ashstride.app.ui.components.NavIcon
import com.ashstride.app.ui.home.HomeScreen
import com.ashstride.app.ui.journey.JourneyScreen
import com.ashstride.app.ui.profile.ProfileScreen
import com.ashstride.app.ui.settings.SettingsScreen
import com.ashstride.app.ui.shrine.ShrineScreen
import com.ashstride.app.ui.stats.StatsScreen
import com.ashstride.app.ui.streak.StreakScreen
import com.ashstride.app.ui.theme.AshDeep
import com.ashstride.app.ui.theme.Ember
import com.ashstride.app.ui.theme.EmberLight
import com.ashstride.app.ui.theme.Smoke
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp

enum class Tab(val label: String, val glyph: NavGlyph) {
    HOME("Home", NavGlyph.HOME),
    STREAK("Streak", NavGlyph.STREAK),
    SHRINE("Shrine", NavGlyph.SHRINE),
    JOURNEY("Journey", NavGlyph.JOURNEY),
    PROFILE("Profile", NavGlyph.PROFILE),
}

/** Pages opened from Profile. NONE means the tab itself is showing. */
enum class SubScreen { NONE, STATS, BADGES, SETTINGS }

@Composable
fun AppRoot(app: AshstrideApp) {
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var sub by rememberSaveable { mutableStateOf(SubScreen.NONE) }
    var showGoal by remember { mutableStateOf(false) }
    val goal by app.model.goal.collectAsStateWithLifecycle()
    val celebrations by app.model.celebrations.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val openGoal = { showGoal = true }
    val go: (Tab) -> Unit = { t -> tab = t; sub = SubScreen.NONE }

    BackHandler(enabled = sub != SubScreen.NONE) { sub = SubScreen.NONE }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF161A29), Color(0xFF0C0F18), Color(0xFF08090E)))),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(
                    containerColor = AshDeep,
                    modifier = Modifier.drawBehind {
                        drawLine(com.ashstride.app.ui.theme.Gilt.GoldMid.copy(alpha = 0.7f), androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, 0f), strokeWidth = 1.5.dp.toPx())
                    },
                ) {
                    Tab.values().forEach { t ->
                        val selected = tab == t
                        NavigationBarItem(
                            selected = selected,
                            onClick = { go(t) },
                            icon = { NavIcon(t.glyph, if (selected) com.ashstride.app.ui.theme.Gilt.GoldBright else Smoke, filled = selected) },
                            label = { Text(t.label) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = com.ashstride.app.ui.theme.Gilt.NightLift,
                                selectedTextColor = com.ashstride.app.ui.theme.Gilt.GoldBright,
                                unselectedTextColor = Smoke,
                            ),
                        )
                    }
                }
            },
        ) { inner ->
            when (sub) {
                SubScreen.STATS -> StatsScreen(app, inner, openGoal) { sub = SubScreen.NONE }
                SubScreen.BADGES -> BadgesScreen(app, inner, openGoal) { sub = SubScreen.NONE }
                SubScreen.SETTINGS -> SettingsScreen(app, inner, openGoal) { sub = SubScreen.NONE }
                SubScreen.NONE -> when (tab) {
                    Tab.HOME -> HomeScreen(app, inner, openGoal, go)
                    Tab.STREAK -> StreakScreen(app, inner, openGoal)
                    Tab.SHRINE -> ShrineScreen(app, inner, openGoal)
                    Tab.JOURNEY -> JourneyScreen(app, inner, openGoal)
                    Tab.PROFILE -> ProfileScreen(app, inner, openGoal) { sub = it }
                }
            }
        }
    }

    if (showGoal) {
        GoalDialog(
            current = goal,
            onPick = { g -> scope.launch { app.model.setGoal(g) } },
            onDismiss = { showGoal = false },
        )
    }

    val head = celebrations.firstOrNull()
    if (head != null) {
        CelebrationOverlay(head) { app.model.dismissCelebration() }
    }
}
