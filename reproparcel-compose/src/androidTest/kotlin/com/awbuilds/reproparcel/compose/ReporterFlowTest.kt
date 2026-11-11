// SPDX-License-Identifier: Apache-2.0
package com.awbuilds.reproparcel.compose

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.awbuilds.reproparcel.android.ReproParcel
import com.awbuilds.reproparcel.android.capture.ReporterHandoff
import com.awbuilds.reproparcel.core.config.CapturePolicy
import com.awbuilds.reproparcel.core.config.EvidenceConfig
import com.awbuilds.reproparcel.core.config.ReproParcelConfig
import com.awbuilds.reproparcel.core.model.BuildInfo
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReporterFlowTest {
    private lateinit var context: Context
    private val device: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ReproParcel.initialize(
            context,
            ReproParcelConfig(
                enabled = true,
                buildInfo = BuildInfo(
                    packageName = "com.awbuilds.reproparcel.synthetic",
                    versionName = "1.0.0",
                    versionCode = 1,
                    variant = "debug",
                ),
                capturePolicy = CapturePolicy(
                    policyVersion = "synthetic-v1",
                    screenshotEnabled = false,
                    semanticTextEnabled = false,
                ),
                evidence = EvidenceConfig(structuredLogsEnabled = true),
            ),
        )
    }

    @Test
    fun withoutHandoffStartsTextOnly() {
        ReporterHandoff.clear()
        ActivityScenario.launch(ReporterActivity::class.java).use {
            assertTrue(device.wait(Until.hasObject(By.text("Report a problem")), 5_000))
            assertTrue(device.wait(Until.hasObject(By.text("What happened?")), 5_000))
            assertTrue(device.wait(Until.hasObject(By.text("Review text-only report")), 5_000))
            assertTrue(device.findObject(By.text("Edit image")) == null)
        }
    }

    @Test
    fun finishedHostReturnsHostUnavailableFromReporterEntry() {
        ActivityScenario.launch(ReporterActivity::class.java).use { scenario ->
            var result: com.awbuilds.reproparcel.core.result.SdkResult? = null
            scenario.onActivity { activity ->
                activity.finish()
                result = ReproParcelCompose.showReporter(activity, "synthetic_surface")
            }
            assertTrue(
                "finished host should return HostUnavailable, got $result",
                result == com.awbuilds.reproparcel.core.result.SdkResult.HostUnavailable,
            )
        }
    }

    @Test
    fun shakeEntryCanBeEnabledAndDisabledOnARealActivity() {
        ActivityScenario.launch(ReporterActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(ShakeToReport.enable(activity, "synthetic_surface"))
                ShakeToReport.disable()
            }
        }
    }

    @Test
    fun reviewShowsTitleAndReproFields() {
        ActivityScenario.launch(ReporterActivity::class.java).use {
            assertTrue(device.wait(Until.hasObject(By.text("Report a problem")), 5_000))
            val fields = device.findObjects(By.clazz("android.widget.EditText"))
            assertTrue(fields.size >= 5)
            fields[0].setText("Synthetic title")
            fields[1].setText("Synthetic description")
            fields[4].setText("Synthetic steps")
            device.findObject(By.text("Review text-only report")).click()
            assertTrue(device.wait(Until.hasObject(By.text("Title: Synthetic title")), 5_000))
            assertTrue(device.wait(Until.hasObject(By.text("Steps: Synthetic steps")), 5_000))
        }
    }

    @Test
    fun recreationLosesSensitiveStateButReporterRemainsUsable() {
        ActivityScenario.launch(ReporterActivity::class.java).use { scenario ->
            assertTrue(device.wait(Until.hasObject(By.text("Report a problem")), 5_000))
            val fields = device.findObjects(By.clazz("android.widget.EditText"))
            fields[1].setText("Sensitive synthetic state")
            scenario.recreate()
            assertTrue(device.wait(Until.hasObject(By.text("Report a problem")), 5_000))
            assertTrue(device.wait(Until.hasObject(By.text("What happened?")), 5_000))
            assertTrue(
                "recreated reporter must not restore entered text",
                device.findObject(By.text("Sensitive synthetic state")) == null,
            )
            val recreatedFields = device.findObjects(By.clazz("android.widget.EditText"))
            assertTrue(recreatedFields.size >= 2)
            assertTrue(recreatedFields[1].text.isNullOrEmpty())
        }
    }

    @Test
    fun textOnlyFormReviewsAndSavesDraft() {
        ActivityScenario.launch(ReporterActivity::class.java).use {
            assertTrue(device.wait(Until.hasObject(By.text("Report a problem")), 5_000))
            val fields = device.findObjects(By.clazz("android.widget.EditText"))
            assertTrue("expected form fields", fields.size >= 2)
            fields[1].setText("Synthetic description")
            assertTrue(device.wait(Until.hasObject(By.text("Review text-only report")), 5_000))
            device.findObject(By.text("Review text-only report")).click()
            assertTrue(device.wait(Until.hasObject(By.text("Exact final content")), 5_000))
            assertTrue(device.wait(Until.hasObject(By.text("Save encrypted draft")), 5_000))
            device.findObject(By.text("Save encrypted draft")).click()
            assertTrue(
                device.wait(
                    Until.hasObject(By.text("Encrypted draft saved locally.")),
                    5_000,
                ),
            )
        }
    }
}
