// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.android.capture

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.awbuilds.reproparcel.core.config.CapturePolicy
import com.awbuilds.reproparcel.core.config.MaskRegion
import com.awbuilds.reproparcel.core.model.OmissionReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaptureControllerTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun policy(
        enabled: Boolean = true,
        surfaces: Set<String> = setOf("capture_test"),
        masks: List<MaskRegion> = emptyList(),
    ) = CapturePolicy(
        policyVersion = "synthetic-v1",
        screenshotEnabled = enabled,
        eligibleSurfaces = surfaces,
        maskRegions = masks,
    )

    @Test
    fun eligibleWindowCapturesAndAppliesOpaqueMask() {
        ActivityScenario.launch(CaptureTestActivity::class.java).use { scenario ->
            var activity: CaptureTestActivity? = null
            scenario.onActivity { activity = it }
            instrumentation.waitForIdleSync()
            val result = runBlocking {
                withContext(Dispatchers.Main) {
                    CaptureController.capture(
                        activity!!,
                        policy(masks = listOf(MaskRegion(10, 10, 80, 80))),
                        "capture_test",
                    )
                }
            }
            assertTrue(result is CaptureOutcome.Captured)
            val bitmap = (result as CaptureOutcome.Captured).bitmap
            assertTrue(bitmap.width > 0 && bitmap.height > 0)
            assertEquals(Color.BLACK, bitmap.getPixel(20, 20))
            bitmap.recycle()
        }
    }

    @Test
    fun screenshotDisabledIsOmitted() {
        ActivityScenario.launch(CaptureTestActivity::class.java).use { scenario ->
            var activity: CaptureTestActivity? = null
            scenario.onActivity { activity = it }
            val result = runBlocking {
                withContext(Dispatchers.Main) {
                    CaptureController.capture(activity!!, policy(enabled = false), "capture_test")
                }
            }
            assertEquals(CaptureOutcome.Omitted(OmissionReason.POLICY_DISABLED), result)
        }
    }

    @Test
    fun unsupportedSurfaceIsOmitted() {
        ActivityScenario.launch(CaptureTestActivity::class.java).use { scenario ->
            var activity: CaptureTestActivity? = null
            scenario.onActivity { activity = it }
            val result = runBlocking {
                withContext(Dispatchers.Main) {
                    CaptureController.capture(activity!!, policy(surfaces = setOf("other")), "capture_test")
                }
            }
            assertEquals(CaptureOutcome.Omitted(OmissionReason.UNSUPPORTED_SURFACE), result)
        }
    }

    @Test
    fun secureWindowIsOmitted() {
        ActivityScenario.launch(CaptureTestActivity::class.java).use { scenario ->
            var activity: CaptureTestActivity? = null
            scenario.onActivity {
                activity = it
                it.markSecure()
            }
            instrumentation.waitForIdleSync()
            val result = runBlocking {
                withContext(Dispatchers.Main) {
                    CaptureController.capture(activity!!, policy(), "capture_test")
                }
            }
            assertEquals(CaptureOutcome.Omitted(OmissionReason.SECURE_WINDOW), result)
        }
    }

    @Test
    fun maskRejectsNothingAndCoversFullRegion() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        CaptureController.applyMasks(
            bitmap,
            policy(masks = listOf(MaskRegion(20, 20, 60, 60))),
        )
        assertEquals(Color.BLACK, bitmap.getPixel(30, 30))
        assertEquals(Color.WHITE, bitmap.getPixel(10, 10))
        bitmap.recycle()
    }
}
