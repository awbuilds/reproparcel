// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.compose

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessibilityTest {
    private val device: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @After
    fun tearDown() {
        device.executeShellCommand("settings put system font_scale 1.0")
        device.executeShellCommand("settings put system accelerometer_rotation 1")
        device.executeShellCommand("settings put system user_rotation 0")
    }

    @Test
    fun formRemainsUsableAtTwoHundredPercentFontScale() {
        device.executeShellCommand("settings put system font_scale 2.0")
        ActivityScenario.launch(ReporterActivity::class.java).use {
            assertTrue(device.wait(Until.hasObject(By.text("Report a problem")), 5_000))
            assertTrue(device.wait(Until.hasObject(By.text("What happened?")), 5_000))
            device.swipe(540, 1500, 540, 500, 20)
            assertTrue(device.wait(Until.hasObject(By.text("Review text-only report")), 5_000))
        }
    }

    @Test
    fun formRemainsUsableInLandscape() {
        device.executeShellCommand("settings put system accelerometer_rotation 0")
        device.executeShellCommand("settings put system user_rotation 1")
        ActivityScenario.launch(ReporterActivity::class.java).use {
            assertTrue(device.wait(Until.hasObject(By.text("Report a problem")), 5_000))
            assertTrue(device.wait(Until.hasObject(By.text("What happened?")), 5_000))
        }
    }
}
