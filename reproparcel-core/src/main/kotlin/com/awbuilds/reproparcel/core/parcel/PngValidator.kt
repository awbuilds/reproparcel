// SPDX-License-Identifier: Apache-2.0
package com.awbuilds.reproparcel.core.parcel

import com.awbuilds.reproparcel.core.limits.ImageBudget
import com.awbuilds.reproparcel.core.limits.Limits
import java.nio.ByteBuffer

public object PngValidator {
    private val signature = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
    )

    public fun validate(bytes: ByteArray): Boolean {
        if (bytes.size < 24) return false
        if (!bytes.copyOfRange(0, 8).contentEquals(signature)) return false
        if (String(bytes, 12, 4, Charsets.US_ASCII) != "IHDR") return false
        val buffer = ByteBuffer.wrap(bytes, 16, 8)
        val width = buffer.int
        val height = buffer.int
        if (width <= 0 || height <= 0) return false
        if (maxOf(width, height) > Limits.MAX_IMAGE_LONG_EDGE_PX) return false
        return ImageBudget.decodedBytes(width, height) <= Limits.MAX_DECODED_IMAGE_BYTES
    }
}
