// SPDX-License-Identifier: Apache-2.0
package com.awbuilds.reproparcel.core

import com.awbuilds.reproparcel.core.limits.ImageBudget
import com.awbuilds.reproparcel.core.limits.ImageDimensions
import com.awbuilds.reproparcel.core.limits.Limits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageBudgetTest {
    @Test
    fun decodesRgbaBytesWithoutOverflow() {
        assertEquals(16L, ImageBudget.decodedBytes(2, 2))
        assertEquals(64_000_000L, ImageBudget.decodedBytes(4000, 4000))
    }

    @Test
    fun rejectsOversizedImagesBeforeDecode() {
        assertFalse(ImageBudget.isWithinDecodedBudget(4000, 4000))
        assertTrue(ImageBudget.isWithinDecodedBudget(1600, 1600))
        assertFalse(ImageBudget.isWithinDecodedBudget(0, 100))
    }

    @Test
    fun scalesToLongestEdge() {
        assertEquals(ImageDimensions(1600, 800), ImageBudget.scaledToLongestEdge(3200, 1600))
        assertEquals(ImageDimensions(800, 1600), ImageBudget.scaledToLongestEdge(800, 1600))
        assertEquals(ImageDimensions(800, 600), ImageBudget.scaledToLongestEdge(800, 600))
    }

    @Test
    fun limitsAreThePrdBudgets() {
        assertEquals(1600, Limits.MAX_IMAGE_LONG_EDGE_PX)
        assertEquals(12L * 1024 * 1024, Limits.MAX_DECODED_IMAGE_BYTES)
        assertEquals(24L * 1024 * 1024, Limits.MAX_CAPTURE_PEAK_BYTES)
        assertEquals(5L * 1024 * 1024, Limits.MAX_PARCEL_COMPRESSED_BYTES)
    }
}
