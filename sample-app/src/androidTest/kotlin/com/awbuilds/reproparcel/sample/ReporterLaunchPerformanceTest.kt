// SPDX-License-Identifier: Apache-2.0
package com.awbuilds.reproparcel.sample

import android.content.Intent
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.awbuilds.reproparcel.android.ReproParcel
import com.awbuilds.reproparcel.compose.ReproParcelCompose
import kotlin.math.ceil
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Trigger-to-composer p95 with capture disabled. The PRD excludes slow image
 * collection from this budget.
 */
@RunWith(AndroidJUnit4::class)
class ReporterLaunchPerformanceTest {
    private val device: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun triggerToComposerP95IsWithinBudget() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, MainActivity::class.java)
            .putExtra("reproparcel_benchmark_no_capture", true)
        val latencies = mutableListOf<Long>()
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            assertTrue(ReproParcel.setEnabled(true) == com.awbuilds.reproparcel.core.result.SdkResult.Success)
            // Untimed warm-up: the cold first launch includes reporter class loading
            // and is covered by startup, not steady-state trigger-to-composer.
            scenario.onActivity { activity ->
                ReproParcelCompose.showReporter(activity, "benchmark_home")
            }
            assertTrue(device.wait(Until.hasObject(By.text("Report a problem")), 5_000))
            device.pressBack()
            assertTrue(device.wait(Until.hasObject(By.text("Initialized disabled by default")), 5_000))
            Thread.sleep(500)
            repeat(10) {
                scenario.onActivity { activity ->
                    ReproParcelCompose.showReporter(activity, "benchmark_home")
                }
                assertTrue(
                    "reporter composer did not appear",
                    device.wait(Until.hasObject(By.text("Report a problem")), 5_000),
                )
                val inAppDuration = com.awbuilds.reproparcel.compose.ReporterLaunchTiming.lastDurationMs
                    ?: error("in-app reporter launch duration missing")
                latencies += inAppDuration
                device.pressBack()
                assertTrue(
                    "host did not resume",
                    device.wait(Until.hasObject(By.text("Initialized disabled by default")), 5_000),
                )
                Thread.sleep(250)
            }
        }
        val sorted = latencies.sorted()
        val p95 = sorted[(ceil(sorted.size * 0.95).toInt() - 1).coerceAtLeast(0)]
        Log.i(
            "ReproParcelPerf",
            "triggerToComposer n=${sorted.size} p50=${sorted[sorted.size / 2]}ms " +
                "p95=${p95}ms max=${sorted.last()}ms",
        )
        assertTrue("trigger-to-composer p95 ${p95}ms exceeds 300ms", p95 <= 300)
    }
}
