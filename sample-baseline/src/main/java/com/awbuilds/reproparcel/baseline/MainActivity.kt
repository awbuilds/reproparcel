// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.baseline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * SDK-absent paired baseline for startup/jank measurement. The screen mirrors
 * the sample app shell without initializing or linking the ReproParcel SDK.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                BaselineScreen()
            }
        }
    }
}

@Composable
private fun BaselineScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("ReproParcel baseline", style = MaterialTheme.typography.headlineSmall)
        Text("Synthetic demo. No real reports, accounts or user data.")
        Text("Session: DISABLED")
        Text("Initialized disabled by default")
        Button(onClick = {}) { Text("Enable SDK") }
        Button(onClick = {}) { Text("Disable SDK") }
        Button(onClick = {}) { Text("Record synthetic breadcrumb") }
        Button(onClick = {}) { Text("Record synthetic log") }
        Button(onClick = {}) { Text("Open reporter") }
        Button(onClick = {}) { Text("Delete local reports") }
        Text(
            "Screenshot and semantic collection are disabled in this sample. " +
                "The reporter UI is implemented in Phase 2.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
