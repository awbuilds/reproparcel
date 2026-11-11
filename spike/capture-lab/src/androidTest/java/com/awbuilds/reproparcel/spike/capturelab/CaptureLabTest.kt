// SPDX-License-Identifier: Apache-2.0

package com.awbuilds.reproparcel.spike.capturelab

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaptureLabTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun captureWindowReturnsBitmap() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var activity: MainActivity? = null
            scenario.onActivity { activity = it }
            instrumentation.waitForIdleSync()
            val result = runBlocking {
                withContext(Dispatchers.Main) {
                    CaptureLab.captureWindow(activity!!.window)
                }
            }
            assertEquals(CaptureStatus.SUCCESS, result.status)
            assertTrue(result.width > 0)
            assertTrue(result.height > 0)
            assertNotNull(result.bitmap)
            result.bitmap?.recycle()
        }
    }

    @Test
    fun secureWindowIsOmittedByPolicy() {
        ActivityScenario.launch(SecureActivity::class.java).use { scenario ->
            var activity: SecureActivity? = null
            scenario.onActivity { activity = it }
            instrumentation.waitForIdleSync()
            val result = runBlocking {
                withContext(Dispatchers.Main) {
                    CaptureLab.captureWindow(activity!!.window, respectSecureFlag = true)
                }
            }
            assertEquals(CaptureStatus.SECURE_WINDOW, result.status)
        }
    }

    @Test
    fun captureAfterRotationUsesLandscapeWindow() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var activity: MainActivity? = null
            scenario.onActivity {
                activity = it
                it.requestedOrientation =
                    android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            instrumentation.waitForIdleSync()
            Thread.sleep(1_000)
            scenario.onActivity { activity = it }
            instrumentation.waitForIdleSync()
            val result = runBlocking {
                withContext(Dispatchers.Main) {
                    CaptureLab.captureWindow(activity!!.window)
                }
            }
            assertEquals(CaptureStatus.SUCCESS, result.status)
            assertTrue(
                "expected landscape dimensions, got ${result.width}x${result.height}",
                result.width > result.height,
            )
            result.bitmap?.recycle()
        }
    }

    @Test
    fun opaqueMaskCoversEveryRegionPixelAndLeavesOutsidePixels() {
        val source = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.WHITE)
        val masked = CaptureLab.applyOpaqueMask(source, Rect(10, 10, 60, 60))
        for (x in 10 until 60) {
            for (y in 10 until 60) {
                assertEquals("masked pixel ($x,$y)", Color.BLACK, CaptureLab.samplePixel(masked, x, y))
            }
        }
        assertEquals(Color.WHITE, CaptureLab.samplePixel(masked, 9, 9))
        assertEquals(Color.WHITE, CaptureLab.samplePixel(masked, 60, 60))
        source.recycle()
        masked.recycle()
    }

    @Test(expected = IllegalArgumentException::class)
    fun transparentMaskIsRejected() {
        val source = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        CaptureLab.applyOpaqueMask(source, Rect(0, 0, 5, 5), Color.TRANSPARENT)
        source.recycle()
    }

    @Test(expected = IllegalArgumentException::class)
    fun maskOutsideBitmapIsRejected() {
        val source = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        CaptureLab.applyOpaqueMask(source, Rect(20, 20, 30, 30))
        source.recycle()
    }

    @Test
    fun rawSecureCaptureBypassesThePolicyCheck() {
        ActivityScenario.launch(SecureActivity::class.java).use { scenario ->
            var activity: SecureActivity? = null
            scenario.onActivity { activity = it }
            instrumentation.waitForIdleSync()
            val raw = runBlocking {
                withContext(Dispatchers.Main) {
                    CaptureLab.captureWindow(activity!!.window, respectSecureFlag = false)
                }
            }
            assertTrue(
                "raw experiment must not return the policy omission result",
                raw.status != CaptureStatus.SECURE_WINDOW,
            )
            raw.bitmap?.recycle()
        }
    }

    @Test
    fun appLevelSemanticsTraversalIsLimited() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var activity: MainActivity? = null
            scenario.onActivity { activity = it }
            instrumentation.waitForIdleSync()
            val snapshot = CaptureLab.dumpSemantics(activity!!.window.decorView)
            assertTrue("expected at least the root node", snapshot.nodes.isNotEmpty())
            assertTrue(
                "app-level traversal must report the unsealed-node limitation, got: " +
                    snapshot.limitation,
                snapshot.limitation.contains("not sealed"),
            )
        }
    }
}
