package com.ashstride.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ashstride.app.AshstrideApp
import com.ashstride.app.ui.badges.BadgesScreen
import com.ashstride.app.ui.components.GoalDialog
import com.ashstride.app.ui.home.HomeScreen
import com.ashstride.app.ui.stats.StatsScreen
import com.ashstride.app.ui.streak.StreakScreen
import com.ashstride.app.ui.theme.Cinder
import com.ashstride.app.ui.theme.Ember
import com.ashstride.app.ui.theme.Smoke
import kotlinx.coroutines.launch

enum class Tab(val label: String) { HOME("Home"), STREAK("Streak"), STATS("Stats"), BADGES("Badges") }

@Composable
fun AppRoot(app: AshstrideApp) {
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var showGoal by remember { mutableStateOf(false) }
    val goal by app.model.goal.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val openGoal = { showGoal = true }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                Tab.values().forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Box(Modifier.size(8.dp).background(if (tab == t) Ember else Cinder, CircleShape)) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedTextColor = Ember,
                            unselectedTextColor = Smoke,
                        ),
                    )
                }
            }
        },
    ) { inner ->
        when (tab) {
            Tab.HOME -> HomeScreen(app, inner, openGoal)
            Tab.STREAK -> StreakScreen(app, inner, openGoal)
            Tab.STATS -> StatsScreen(app, inner, openGoal)
            Tab.BADGES -> BadgesScreen(app, inner, openGoal)
        }
    }

    if (showGoal) {
        GoalDialog(
            current = goal,
            onPick = { g -> scope.launch { app.model.setGoal(g) } },
            onDismiss = { showGoal = false },
        )
    }
}
