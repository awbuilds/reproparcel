// SPDX-License-Identifier: Apache-2.0
package com.awbuilds.reproparcel.benchmark

import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMetricApi::class)
@RunWith(AndroidJUnit4::class)
class StartupTraceBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStartupWithInitTrace() = benchmarkRule.measureRepeated(
        packageName = "com.awbuilds.reproparcel.sample",
        metrics = listOf(
            StartupTimingMetric(),
            TraceSectionMetric("ReproParcelInitialize"),
        ),
        iterations = 20,
        startupMode = StartupMode.COLD,
    ) {
        pressHome()
        startActivityAndWait()
    }
}
