// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.sample

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.awbuilds.reproparcel.android.Breadcrumb
import com.awbuilds.reproparcel.android.LogEvent
import com.awbuilds.reproparcel.android.ReproParcel
import com.awbuilds.reproparcel.core.config.CapturePolicy
import com.awbuilds.reproparcel.core.config.EvidenceConfig
import com.awbuilds.reproparcel.core.config.ReproParcelConfig
import com.awbuilds.reproparcel.core.model.BuildInfo
import com.awbuilds.reproparcel.core.result.SdkResult

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val config = ReproParcelConfig(
            enabled = false,
            buildInfo = BuildInfo(
                packageName = "com.awbuilds.reproparcel.sample",
                versionName = "0.0.1-SNAPSHOT",
                versionCode = 1,
                variant = "debug",
                gitCommit = null,
            ),
            capturePolicy = CapturePolicy(
                policyVersion = "sample-policy-v1",
                screenshotEnabled = false,
                semanticTextEnabled = false,
            ),
            evidence = EvidenceConfig(
                screenshotEnabled = false,
                semanticTextEnabled = false,
                structuredLogsEnabled = false,
                networkMetadataEnabled = false,
            ),
        )
        ReproParcel.initialize(applicationContext, config)
        setContent {
            MaterialTheme {
                SampleScreen()
            }
        }
    }
}

@Composable
private fun SampleScreen() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val sessionState by ReproParcel.sessionState.collectAsState()
    var status by remember { mutableStateOf("Initialized disabled by default") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("ReproParcel sample", style = MaterialTheme.typography.headlineSmall)
        Text("Synthetic demo. No real reports, accounts or user data.")
        Text("Session: $sessionState")
        Text(status, style = MaterialTheme.typography.bodyMedium)

        Button(onClick = {
            status = when (val result = ReproParcel.setEnabled(true)) {
                SdkResult.Success -> "Enabled; no collectors start implicitly"
                else -> "Enable failed"
            }
        }) { Text("Enable SDK") }

        Button(onClick = {
            status = when (val result = ReproParcel.setEnabled(false)) {
                SdkResult.Success -> "Disabled; jobs cancelled"
                else -> "Disable failed"
            }
        }) { Text("Disable SDK") }

        Button(onClick = {
            val result = ReproParcel.recordBreadcrumb(
                Breadcrumb(
                    category = "navigation",
                    actionId = "open_sample_screen",
                    screenId = "sample_home",
                    attributes = mapOf(
                        "screen_id" to "sample_home",
                        "route_template" to "/sample/{id}",
                    ),
                ),
            )
            status = if (result == SdkResult.Success) "Safe breadcrumb recorded" else "Breadcrumb: $result"
        }) { Text("Record synthetic breadcrumb") }

        Button(onClick = {
            val result = ReproParcel.recordLog(
                LogEvent(
                    level = "info",
                    code = "sample_event",
                    attributes = mapOf("component" to "sample_app"),
                ),
            )
            status = if (result == SdkResult.Success) "Safe log recorded" else "Log: $result"
        }) { Text("Record synthetic log") }

        Button(onClick = {
            val result = activity?.let(ReproParcel::showReporter) ?: SdkResult.HostUnavailable
            status = "Reporter: $result (Phase 2)"
        }) { Text("Show reporter (stub)") }

        Button(onClick = {
            status = when (ReproParcel.deleteLocalReports()) {
                SdkResult.Success -> "Local draft deletion completed (0 drafts)"
                else -> "Deletion unavailable"
            }
        }) { Text("Delete local reports") }

        Text(
            "Screenshot and semantic collection are disabled in this sample. " +
                "The reporter UI is implemented in Phase 2.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
