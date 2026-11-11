// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.awbuilds.reproparcel.android.ReproParcel
import com.awbuilds.reproparcel.core.config.CapturePolicy
import com.awbuilds.reproparcel.core.config.EvidenceConfig
import com.awbuilds.reproparcel.core.config.ReproParcelConfig
import com.awbuilds.reproparcel.core.model.BuildInfo

/**
 * Benchmark-only long list. It initializes and enables the SDK only when the
 * benchmark intent asks for the enabled condition, so both runs use the same UI.
 */
class ScrollActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (intent.getBooleanExtra("reproparcel_benchmark_sdk_enabled", false)) {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            ReproParcel.initialize(
                applicationContext,
                ReproParcelConfig(
                    enabled = true,
                    buildInfo = BuildInfo(
                        packageName = packageName,
                        versionName = packageInfo.versionName ?: "0.0.1-SNAPSHOT",
                        versionCode = androidx.core.content.pm.PackageInfoCompat
                            .getLongVersionCode(packageInfo),
                        variant = "benchmark",
                    ),
                    capturePolicy = CapturePolicy(
                        policyVersion = "benchmark-no-capture",
                        screenshotEnabled = false,
                        semanticTextEnabled = false,
                    ),
                    evidence = EvidenceConfig(),
                ),
            )
        }
        setContent {
            MaterialTheme {
                ScrollScreen()
            }
        }
    }
}

@Composable
private fun ScrollScreen() {
    LazyColumn(modifier = Modifier.fillMaxWidth().testTag("synthetic_scroll")) {
        items((1..1000).map { "Synthetic row $it" }) { label ->
            Text(
                text = label,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            )
        }
    }
}
