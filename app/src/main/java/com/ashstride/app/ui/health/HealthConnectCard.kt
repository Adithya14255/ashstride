package com.ashstride.app.ui.health

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import com.ashstride.app.AshstrideApp
import com.ashstride.app.sync.HealthPermissions
import com.ashstride.app.sync.SyncResult
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun HealthConnectCard(app: AshstrideApp) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<SyncResult?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun runSync() {
        if (busy) return
        busy = true
        scope.launch {
            result = app.healthSync.sync()
            busy = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { runSync() }

    // Sync once whenever Home opens.
    LaunchedEffect(Unit) { runSync() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Health Connect", style = MaterialTheme.typography.titleMedium)
        val r = result
        Text(
            when {
                busy -> "Syncing..."
                r == null -> "Checking..."
                r is SyncResult.Synced -> {
                    val t = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(r.atEpochMs))
                    "Synced at $t. Checked ${r.daysChecked} day(s), updated ${r.daysWritten}."
                }
                r is SyncResult.NeedsPermission -> "Allow Ashstride to read steps and distance so new days count."
                r is SyncResult.Unavailable ->
                    if (r.updateRequired) "Health Connect needs an update before it can be used."
                    else "Health Connect is not available on this device."
                r is SyncResult.Skipped -> "Background sync is waiting for permission."
                r is SyncResult.Failed -> "Sync failed: ${r.message}"
                else -> ""
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        when (r) {
            is SyncResult.NeedsPermission ->
                Button(onClick = { permissionLauncher.launch(HealthPermissions.all) }) { Text("Allow access") }
            is SyncResult.Unavailable ->
                if (r.updateRequired) {
                    Button(onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata")),
                        )
                    }) { Text("Update Health Connect") }
                }
            else -> OutlinedButton(onClick = { runSync() }, enabled = !busy) { Text("Sync now") }
        }
    }
}
