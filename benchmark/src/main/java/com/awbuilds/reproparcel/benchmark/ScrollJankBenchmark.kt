// SPDX-License-Identifier: Apache-2.0
package com.awbuilds.reproparcel.benchmark

import android.content.Intent
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScrollJankBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    private fun scroll(sdkEnabled: Boolean) = benchmarkRule.measureRepeated(
        packageName = "com.awbuilds.reproparcel.sample",
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.COLD,
    ) {
        pressHome()
        startActivityAndWait(
            Intent(Intent.ACTION_MAIN).apply {
                setClassName(
                    "com.awbuilds.reproparcel.sample",
                    "com.awbuilds.reproparcel.sample.ScrollActivity",
                )
                putExtra("reproparcel_benchmark_sdk_enabled", sdkEnabled)
            },
        )
        device.wait(Until.hasObject(By.text("Synthetic row 1")), 5_000)
        repeat(80) {
            device.swipe(540, 1800, 540, 400, 8)
        }
    }

    @Test
    fun scrollSdkEnabled() = scroll(sdkEnabled = true)

    @Test
    fun scrollSdkDisabled() = scroll(sdkEnabled = false)
}
