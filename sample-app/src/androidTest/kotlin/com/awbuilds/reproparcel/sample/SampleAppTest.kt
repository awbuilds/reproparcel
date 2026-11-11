// SPDX-License-Identifier: Apache-2.0
package com.awbuilds.reproparcel.sample

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SampleAppTest {
    private val device: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun sampleStartsDisabledAndEnablesExplicitly() {
        ActivityScenario.launch(MainActivity::class.java).use {
            assertTrue(device.wait(Until.hasObject(By.text("Initialized disabled by default")), 5_000))
            assertTrue(device.wait(Until.hasObject(By.text("Enable SDK")), 5_000))
            device.findObject(By.text("Enable SDK")).click()
            assertTrue(device.wait(Until.hasObject(By.text("Enabled; no collectors start implicitly")), 5_000))
        }
    }

    @Test
    fun sampleRecordsOnlySafeSyntheticEvidence() {
        ActivityScenario.launch(MainActivity::class.java).use {
            assertTrue(device.wait(Until.hasObject(By.text("Enable SDK")), 5_000))
            device.findObject(By.text("Enable SDK")).click()
            assertTrue(device.wait(Until.hasObject(By.text("Record synthetic breadcrumb")), 5_000))
            device.findObject(By.text("Record synthetic breadcrumb")).click()
            assertTrue(device.wait(Until.hasObject(By.text("Safe breadcrumb recorded")), 5_000))
        }
    }
}
