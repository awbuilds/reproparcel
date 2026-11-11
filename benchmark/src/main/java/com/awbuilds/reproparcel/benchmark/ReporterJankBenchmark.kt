// SPDX-License-Identifier: Apache-2.0
package com.awbuilds.reproparcel.benchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.benchmark.macro.StartupMode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReporterJankBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun reporterFormScroll() = benchmarkRule.measureRepeated(
        packageName = "com.awbuilds.reproparcel.sample",
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.WARM,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait()
        device.findObject(By.text("Enable SDK"))?.click()
        device.wait(Until.hasObject(By.text("Open reporter")), 5_000)
        device.findObject(By.text("Open reporter"))?.click()
        device.wait(Until.hasObject(By.text("What happened?")), 10_000)
        repeat(4) {
            device.swipe(540, 1700, 540, 500, 12)
            device.swipe(540, 500, 540, 1700, 12)
        }
    }
}
