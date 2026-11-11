// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.benchmark

import android.content.Intent
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Fair paired startup comparison: identical app, UI and custom-intent launch
 * path. Only the SDK initialization is skipped in the uninitialized case.
 */
@RunWith(AndroidJUnit4::class)
class PairedStartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    private fun launchIntent(skipInit: Boolean) = Intent(Intent.ACTION_MAIN).apply {
        setClassName(
            "com.awbuilds.reproparcel.sample",
            "com.awbuilds.reproparcel.sample.MainActivity",
        )
        putExtra("reproparcel_benchmark_skip_init", skipInit)
    }

    @Test
    fun coldStartupInitialized() = benchmarkRule.measureRepeated(
        packageName = "com.awbuilds.reproparcel.sample",
        metrics = listOf(StartupTimingMetric()),
        iterations = 20,
        startupMode = StartupMode.COLD,
    ) {
        pressHome()
        startActivityAndWait(launchIntent(skipInit = false))
    }

    @Test
    fun coldStartupUninitialized() = benchmarkRule.measureRepeated(
        packageName = "com.awbuilds.reproparcel.sample",
        metrics = listOf(StartupTimingMetric()),
        iterations = 20,
        startupMode = StartupMode.COLD,
    ) {
        pressHome()
        startActivityAndWait(launchIntent(skipInit = true))
    }
}
