// SPDX-License-Identifier: Apache-2.0
package com.awbuilds.reproparcel.benchmark

import android.content.Intent
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStartupIntegrated() = benchmarkRule.measureRepeated(
        packageName = "com.awbuilds.reproparcel.sample",
        metrics = listOf(StartupTimingMetric()),
        iterations = 10,
        startupMode = StartupMode.COLD,
    ) {
        pressHome()
        startActivityAndWait()
    }

    @Test
    fun coldStartupSdkUninitializedPaired() = benchmarkRule.measureRepeated(
        packageName = "com.awbuilds.reproparcel.sample",
        metrics = listOf(StartupTimingMetric()),
        iterations = 10,
        startupMode = StartupMode.COLD,
    ) {
        pressHome()
        startActivityAndWait(
            Intent(Intent.ACTION_MAIN).apply {
                setClassName(
                    "com.awbuilds.reproparcel.sample",
                    "com.awbuilds.reproparcel.sample.MainActivity",
                )
                putExtra("reproparcel_benchmark_skip_init", true)
            },
        )
    }

    @Test
    fun coldStartupSdkAbsentBaseline() = benchmarkRule.measureRepeated(
        packageName = "com.awbuilds.reproparcel.baseline",
        metrics = listOf(StartupTimingMetric()),
        iterations = 10,
        startupMode = StartupMode.COLD,
    ) {
        pressHome()
        startActivityAndWait()
    }
}
