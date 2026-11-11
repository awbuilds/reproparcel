// SPDX-License-Identifier: Apache-2.0
package com.awbuilds.reproparcel.android

import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.awbuilds.reproparcel.core.config.CapturePolicy
import com.awbuilds.reproparcel.core.config.EvidenceConfig
import com.awbuilds.reproparcel.core.config.ReproParcelConfig
import com.awbuilds.reproparcel.core.model.BuildInfo
import com.awbuilds.reproparcel.core.model.NetworkMetadata
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.ceil

@RunWith(AndroidJUnit4::class)
class CollectorCostTest {
    @Before
    fun setUp() {
        ReproParcel.resetForTest()
        ReproParcel.initialize(
            ApplicationProvider.getApplicationContext(),
            ReproParcelConfig(
                enabled = true,
                buildInfo = BuildInfo(
                    packageName = "com.awbuilds.reproparcel.synthetic",
                    versionName = "1.0.0",
                    versionCode = 1,
                    variant = "benchmark",
                ),
                capturePolicy = CapturePolicy(
                    policyVersion = "collector-cost",
                    screenshotEnabled = false,
                    semanticTextEnabled = false,
                ),
                evidence = EvidenceConfig(
                    structuredLogsEnabled = true,
                    networkMetadataEnabled = true,
                    semanticTextEnabled = false,
                ),
            ),
        )
    }

    @After
    fun tearDown() {
        ReproParcel.resetForTest()
    }

    private fun p95(values: List<Long>): Long {
        val sorted = values.sorted()
        return sorted[(ceil(sorted.size * 0.95).toInt() - 1).coerceAtLeast(0)]
    }

    @Test
    fun breadcrumbAndLogAddsStayUnderFourMilliseconds() {
        val breadcrumbLatencies = mutableListOf<Long>()
        val logLatencies = mutableListOf<Long>()
        repeat(1_000) { index ->
            val started = System.nanoTime()
            ReproParcel.recordBreadcrumb(
                Breadcrumb(
                    category = "navigation",
                    actionId = "action_$index",
                    screenId = "screen",
                    attributes = mapOf("screen_id" to "screen"),
                ),
            )
            breadcrumbLatencies += (System.nanoTime() - started) / 1_000
            val logStarted = System.nanoTime()
            ReproParcel.recordLog(
                LogEvent("info", "event_$index", mapOf("component" to "synthetic")),
            )
            logLatencies += (System.nanoTime() - logStarted) / 1_000
        }
        val breadcrumbP95 = p95(breadcrumbLatencies)
        val logP95 = p95(logLatencies)
        Log.i(
            "ReproParcelPerf",
            "collector breadcrumbP95Us=$breadcrumbP95 logP95Us=$logP95 (n=1000 each)",
        )
        assertTrue("breadcrumb p95 ${breadcrumbP95}us exceeds 4ms", breadcrumbP95 <= 4_000)
        assertTrue("log p95 ${logP95}us exceeds 4ms", logP95 <= 4_000)
    }

    @Test
    fun networkMetadataAddsStayUnderFourMilliseconds() {
        val latencies = mutableListOf<Long>()
        repeat(1_000) {
            val started = System.nanoTime()
            ReproParcel.recordNetworkMetadata(
                NetworkMetadata("GET", 200, 12, "/orders/{id}"),
            )
            latencies += (System.nanoTime() - started) / 1_000
        }
        val latencyP95 = p95(latencies)
        Log.i("ReproParcelPerf", "collector networkP95Us=$latencyP95 (n=1000)")
        assertTrue("network p95 ${latencyP95}us exceeds 4ms", latencyP95 <= 4_000)
    }
}
