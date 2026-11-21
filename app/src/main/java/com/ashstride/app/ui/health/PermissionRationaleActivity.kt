package com.ashstride.app.ui.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ashstride.app.ui.theme.AshstrideTheme

/** Shown by Health Connect when it asks how Ashstride uses your data. */
class PermissionRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AshstrideTheme {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .safeDrawingPadding()
                        .padding(24.dp),
                ) {
                    Text("How Ashstride uses your data", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Ashstride reads your daily step count and walking distance from Health Connect so every step " +
                            "feeds your flame, streak and challenges.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Older history permission lets it read data from more than 30 days back. Background permission " +
                            "lets it sync while the app is closed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Everything stays on this phone. There is no account, no cloud and no sharing. Manually entered " +
                            "steps are ignored. Ashstride never writes to Health Connect. You can revoke access any time " +
                            "in Health Connect settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
