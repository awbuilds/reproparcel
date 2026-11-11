// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.android.sensor

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShakeDetectorTest {
    @Test
    fun startAndStopAreExplicitAndIdempotent() {
        val detector = ShakeDetector(ApplicationProvider.getApplicationContext()) {}
        assertTrue(detector.start())
        assertTrue(detector.isStarted)
        detector.stop()
        assertFalse(detector.isStarted)
        detector.stop()
        assertFalse(detector.isStarted)
    }
}
