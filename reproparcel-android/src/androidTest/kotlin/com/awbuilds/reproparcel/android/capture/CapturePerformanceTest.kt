// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.android.capture

import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.awbuilds.reproparcel.android.ReproParcel
import com.awbuilds.reproparcel.core.config.CapturePolicy
import com.awbuilds.reproparcel.core.config.EvidenceConfig
import com.awbuilds.reproparcel.core.config.MaskRegion
import com.awbuilds.reproparcel.core.config.ReproParcelConfig
import com.awbuilds.reproparcel.core.model.BuildInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.ceil

/**
 * Physical-device capture latency/allocation sample. This is a direct
 * controller measurement, not a full trigger-to-composer benchmark.
 */
@RunWith(AndroidJUnit4::class)
class CapturePerformanceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val policy = CapturePolicy(
        policyVersion = "capture-performance",
        screenshotEnabled = true,
        eligibleSurfaces = setOf("capture_test"),
        maskRegions = listOf(MaskRegion(10, 10, 80, 80)),
    )

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
                capturePolicy = policy,
                evidence = EvidenceConfig(screenshotEnabled = true),
            ),
        )
    }

    @After
    fun tearDown() {
        ReproParcel.resetForTest()
    }

    @Test
    fun captureLatencyAndAllocationStayWithinBudget() {
        val latencies = mutableListOf<Long>()
        val allocations = mutableListOf<Long>()
        ActivityScenario.launch(CaptureTestActivity::class.java).use { scenario ->
            var activity: CaptureTestActivity? = null
            scenario.onActivity { activity = it }
            instrumentation.waitForIdleSync()
            repeat(20) {
                val started = SystemClock.elapsedRealtime()
                val result = runBlocking {
                    withContext(Dispatchers.Main) {
                        CaptureController.capture(activity!!, policy, "capture_test")
                    }
                }
                val elapsed = SystemClock.elapsedRealtime() - started
                when (result) {
                    is CaptureOutcome.Captured -> {
                        latencies += elapsed
                        allocations += result.bitmap.byteCount.toLong()
                        result.bitmap.recycle()
                    }
                    else -> error("capture omitted during performance run: $result")
                }
            }
        }
        latencies.sort()
        allocations.sort()
        val p50 = latencies[latencies.size / 2]
        val p95 = latencies[(ceil(latencies.size * 0.95).toInt() - 1).coerceAtLeast(0)]
        val max = latencies.last()
        val maxAllocation = allocations.last()
        Log.i(
            "ReproParcelPerf",
            "capture n=${latencies.size} p50=${p50}ms p95=${p95}ms max=${max}ms " +
                "allocationMaxBytes=$maxAllocation",
        )
        assertTrue("capture p95 ${p95}ms exceeds 2000ms", p95 <= 2_000)
        assertTrue(
            "capture allocation ${maxAllocation}B exceeds 12MiB decoded budget",
            maxAllocation <= 12L * 1024 * 1024,
        )
    }
}
