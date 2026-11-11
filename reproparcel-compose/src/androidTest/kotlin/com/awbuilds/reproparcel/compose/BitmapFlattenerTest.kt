// SPDX-License-Identifier: Apache-2.0
package com.awbuilds.reproparcel.compose

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BitmapFlattenerTest {
    @Test
    fun maskIsBakedIntoSavedPixels() {
        val source = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.WHITE)
        val png = BitmapFlattener.flatten(
            source,
            listOf(Annotation(AnnotationTool.MASK, listOf(Offset(0.5f, 0.5f)))),
        )
        assertNotNull(png)
        val decoded = BitmapFactory.decodeByteArray(png, 0, png!!.size)
        assertEquals(Color.BLACK, decoded.getPixel(50, 50))
        source.recycle()
        decoded.recycle()
    }
}
