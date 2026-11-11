// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.compose

import android.graphics.Rect
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.awbuilds.reproparcel.android.capture.ReporterHandoff
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReporterSemanticsTest {
    private val device: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun setUp() {
        ReporterHandoff.clear()
    }

    private fun clickableAncestor(text: String): UiObject2? {
        var current: UiObject2? = device.findObject(By.text(text))
        repeat(5) {
            if (current?.isClickable == true) return current
            current = current?.parent
        }
        return null
    }

    @Test
    fun textOnlyReviewRequiresDescriptionAndAnExplicitApproval() {
        ActivityScenario.launch(ReporterActivity::class.java).use {
            assertTrue(device.wait(Until.hasObject(By.text("Report a problem")), 5_000))
            val review = clickableAncestor("Review text-only report")
            assertTrue(review != null)
            assertEquals(false, review!!.isEnabled)

            val fields = device.findObjects(By.clazz("android.widget.EditText"))
            assertTrue(fields.size >= 5)
            fields[1].setText("Synthetic description")
            device.waitForIdle()
            assertTrue(review.wait(Until.enabled(true), 5_000))
            review.click()

            assertTrue(device.wait(Until.hasObject(By.text("Exact final content")), 5_000))
            assertTrue(device.wait(Until.hasObject(By.text("I reviewed the exact content above.")), 5_000))
            val save = clickableAncestor("Save file")
            assertTrue(save != null)
            assertEquals(false, save!!.isEnabled)
            val share = clickableAncestor("Share")
            assertTrue(share != null)
            assertEquals(false, share!!.isEnabled)
        }
    }

    @Test
    fun primaryControlsMeetMinimumTouchTargets() {
        ActivityScenario.launch(ReporterActivity::class.java).use {
            assertTrue(device.wait(Until.hasObject(By.text("Review text-only report")), 5_000))
            val review = clickableAncestor("Review text-only report")
            assertTrue(review != null)
            val bounds = review!!.visibleBounds
            val density = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
            val minimumPx = (48 * density).toInt()
            assertTrue(
                "review button height ${bounds.height()}px should be >= ${minimumPx}px",
                bounds.height() >= minimumPx,
            )
            for (label in listOf("What happened?", "Expected behavior", "Actual behavior", "Steps to reproduce (optional)")) {
                assertTrue("missing label $label", device.wait(Until.hasObject(By.text(label)), 5_000))
            }
        }
    }
}
