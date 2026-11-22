package com.ashstride.app.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ashstride.app.AshstrideApp
import com.ashstride.app.data.BackupRepository
import com.ashstride.app.data.NotifyPrefs
import com.ashstride.app.domain.BackupCodec
import com.ashstride.app.domain.BackupData
import com.ashstride.app.domain.CsvResult
import com.ashstride.app.domain.NotificationRules
import com.ashstride.app.domain.NotifySettings
import com.ashstride.app.notify.NotificationEngine
import com.ashstride.app.ui.Fmt
import com.ashstride.app.ui.components.BackHeader
import com.ashstride.app.ui.components.Panel
import com.ashstride.app.ui.components.Pill
import com.ashstride.app.ui.components.SectionTitle
import com.ashstride.app.ui.theme.Gold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private fun toast(context: Context, text: String) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()

private fun tryStart(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        toast(context, "This phone does not offer that screen. Open Settings > Apps > Ashstride by hand.")
    }
}

@Composable
fun SettingsScreen(app: AshstrideApp, contentPadding: PaddingValues, onGoalClick: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val goal by app.model.goal.collectAsStateWithLifecycle()
    val np = remember { NotifyPrefs(app) }
    val repo = remember { BackupRepository(app) }
    val settings by np.settingsFlow.collectAsStateWithLifecycle(initialValue = NotifySettings())
    fun save(s: NotifySettings) { scope.launch { np.save(s) } }

    var notifOk by remember { mutableStateOf(NotificationEngine.canPost(context)) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifOk = NotificationEngine.canPost(context) }

    var pendingText by remember { mutableStateOf("") }
    val saveJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) {
                try { context.contentResolver.openOutputStream(uri)?.use { it.write(pendingText.toByteArray()) }; true } catch (e: Exception) { false }
            }
            toast(context, if (ok) "Backup saved" else "Could not save the file")
        }
    }
    val saveCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) {
                try { context.contentResolver.openOutputStream(uri)?.use { it.write(pendingText.toByteArray()) }; true } catch (e: Exception) { false }
            }
            toast(context, if (ok) "Steps exported" else "Could not save the file")
        }
    }
    var restoreJson by remember { mutableStateOf<BackupData?>(null) }
    var importCsv by remember { mutableStateOf<CsvResult?>(null) }
    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) scope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
                }
                if (text.trimStart().startsWith("{")) restoreJson = BackupCodec.decode(text)
                else importCsv = BackupCodec.parseCsv(text, goal)
            } catch (e: Exception) {
                toast(context, e.message ?: "That file could not be read")
            }
        }
    }

    val pm = context.getSystemService(PowerManager::class.java)
    var unrestricted by remember { mutableStateOf(pm?.isIgnoringBatteryOptimizations(context.packageName) == true) }
    val version = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "" } catch (e: Exception) { "" }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BackHeader("Settings", goal, onGoalClick, onBack)

        Panel {
            SectionTitle("Daily goal")
            Text(
                "Currently ${Fmt.n(goal)} steps. It applies from today; past days keep the goal they had.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onGoalClick) { Text("Change goal") }
        }

        Panel {
            SectionTitle("Alerts")
            if (!notifOk) {
                Text("Notifications are off for Ashstride, so no alert can appear.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = {
                    if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else tryStart(context, Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                }) { Text("Allow notifications") }
            } else {
                OutlinedButton(onClick = {
                    toast(context, if (NotificationEngine.sendTest(context)) "Test alert sent" else "Notifications are blocked")
                }) { Text("Send a test alert") }
            }
            ToggleRow("Streak at risk", "One evening nudge if today is still below goal.", settings.atRiskEnabled) { save(settings.copy(atRiskEnabled = it)) }
            if (settings.atRiskEnabled) TimeRow("Send after", settings.atRiskMinutes) { save(settings.copy(atRiskMinutes = it)) }
            ToggleRow("The shrine opens", "Saturday: this week's three cards are ready.", settings.shrineEnabled) { save(settings.copy(shrineEnabled = it)) }
            if (settings.shrineEnabled) TimeRow("Send after", settings.shrineMinutes) { save(settings.copy(shrineMinutes = it)) }
            ToggleRow("Milestones", "New streak and lifetime-step badges.", settings.milestonesEnabled) { save(settings.copy(milestonesEnabled = it)) }
            ToggleRow("Quiet hours", "No alerts between these times.", settings.quietEnabled) { save(settings.copy(quietEnabled = it)) }
            if (settings.quietEnabled) {
                TimeRow("From", settings.quietStartMinutes) { save(settings.copy(quietStartMinutes = it)) }
                TimeRow("Until", settings.quietEndMinutes) { save(settings.copy(quietEndMinutes = it)) }
                if (settings.atRiskEnabled && NotificationRules.inQuietHours(settings, settings.atRiskMinutes)) {
                    Text(
                        "The streak alert time falls inside quiet hours, so it will never fire. Move one of them.",
                        color = Gold, style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        Panel {
            SectionTitle("Home-screen widget")
            Text(
                "Press and hold an empty spot on the home screen, choose Widgets, then Ashstride. It shows today's steps, the goal ring and your flame, and refreshes with every sync.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Panel {
            SectionTitle("Backup and export")
            Text(
                "Everything stays on this phone. A backup file holds your steps, XP, trials, Menders and badges, so you can move to a new phone or recover from a bad day.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = {
                scope.launch {
                    pendingText = withContext(Dispatchers.IO) { repo.exportJson() }
                    saveJson.launch("ashstride-backup-" + LocalDate.now() + ".json")
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Save backup file") }
            OutlinedButton(onClick = {
                scope.launch {
                    pendingText = withContext(Dispatchers.IO) { repo.exportCsv() }
                    saveCsv.launch("ashstride-steps-" + LocalDate.now() + ".csv")
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Export steps as CSV") }
            OutlinedButton(onClick = { openFile.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Restore or import a file") }
            Text(
                "Restoring a backup replaces everything. Importing a CSV only adds or replaces the days it lists. Either way a safety copy is kept inside the app first.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Panel {
            SectionTitle("Keep syncing in the background")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(if (unrestricted) "Unrestricted" else "Battery limits apply", if (unrestricted) Gold else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "OxygenOS stops background apps aggressively. If your steps only appear when you open Ashstride, or alerts arrive late, work through these (wording differs a little between OxygenOS versions):",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val steps = listOf(
                "Tap Open app info below, then Battery usage, and choose Allow background activity (Unrestricted).",
                "In the same app info page, turn on Auto-launch if you see it.",
                "Open the recent-apps screen and lock Ashstride (pull its card down, or tap the lock icon) so clearing recents does not kill it.",
                "In Health Connect, open App permissions, then Ashstride, and allow Access data in the background.",
            )
            for ((i, s) in steps.withIndex()) {
                Text("${i + 1}.  $s", style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = {
                tryStart(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName)))
            }, modifier = Modifier.fillMaxWidth()) { Text("Open app info") }
            OutlinedButton(onClick = {
                tryStart(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                unrestricted = pm?.isIgnoringBatteryOptimizations(context.packageName) == true
            }, modifier = Modifier.fillMaxWidth()) { Text("Open battery optimisation list") }
            TextButton(onClick = { unrestricted = pm?.isIgnoringBatteryOptimizations(context.packageName) == true }) { Text("Check again") }
        }

        Panel {
            SectionTitle("Health Connect")
            Text(
                "Write-back of imported history is off and stays off: writing old steps back would be counted twice by other apps.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            "Ashstride $version  |  no account, no ads, no cloud",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
    }

    val rj = restoreJson
    if (rj != null) {
        AlertDialog(
            onDismissRequest = { restoreJson = null },
            title = { Text("Replace everything?") },
            text = {
                Text(
                    "This backup holds ${Fmt.n(rj.days.size)} days, ${rj.xp.size} XP events and ${rj.weeks.size} shrine weeks. " +
                        "Restoring replaces all data on this phone. A safety copy is saved first.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    restoreJson = null
                    scope.launch {
                        val n = withContext(Dispatchers.IO) { repo.restore(rj) }
                        toast(context, "Restored $n days")
                    }
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { restoreJson = null }) { Text("Cancel") } },
        )
    }
    val ic = importCsv
    if (ic != null) {
        AlertDialog(
            onDismissRequest = { importCsv = null },
            title = { Text("Import steps?") },
            text = {
                Text(
                    "${Fmt.n(ic.days.size)} days found" + (if (ic.skipped > 0) ", ${ic.skipped} unreadable rows skipped" else "") +
                        ". Days already stored with the same date are replaced. A safety copy is saved first.",
                )
            },
            confirmButton = {
                TextButton(enabled = ic.days.isNotEmpty(), onClick = {
                    importCsv = null
                    scope.launch {
                        val n = withContext(Dispatchers.IO) { repo.importCsv(ic) }
                        toast(context, "Imported $n days")
                    }
                }) { Text("Import") }
            },
            dismissButton = { TextButton(onClick = { importCsv = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ToggleRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun TimeRow(label: String, minutes: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = { onChange(NotificationRules.shift(minutes, -30)) }) { Text("-30") }
        Text(NotificationRules.clockText(minutes), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 12.dp))
        OutlinedButton(onClick = { onChange(NotificationRules.shift(minutes, 30)) }) { Text("+30") }
    }
}
