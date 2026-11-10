// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.core.limits

public object Limits {
    public const val MAX_EVIDENCE_BUFFER_BYTES: Long = 1L * 1024 * 1024
    public const val MAX_CAPTURE_PEAK_BYTES: Long = 24L * 1024 * 1024
    public const val MAX_IMAGE_LONG_EDGE_PX: Int = 1600
    public const val MAX_DECODED_IMAGE_BYTES: Long = 12L * 1024 * 1024
    public const val MAX_CONCURRENT_DECODED_IMAGE_BYTES: Long = 24L * 1024 * 1024
    public const val MAX_PARCEL_COMPRESSED_BYTES: Long = 5L * 1024 * 1024
    public const val MAX_PARCEL_UNCOMPRESSED_BYTES: Long = 10L * 1024 * 1024
    public const val MAX_PARCEL_ENTRIES: Int = 16
    public const val MAX_COMPRESSION_RATIO: Int = 100
    public const val MAX_JSON_BYTES: Long = 2L * 1024 * 1024
    public const val MAX_JSON_DEPTH: Int = 32
    public const val MAX_JSON_NODES: Int = 10_000
    public const val MAX_EVENTS: Int = 50
    public const val MAX_ACTIONS: Int = 20
    public const val MAX_ACTION_AGE_MS: Long = 5L * 60 * 1000
    public const val MAX_LOGS: Int = 200
    public const val MAX_LOG_BYTES: Long = 256L * 1024
    public const val MAX_NETWORK_ENTRIES: Int = 50
    public const val MAX_NETWORK_BYTES: Long = 128L * 1024
    public const val MAX_SEMANTIC_NODES: Int = 500
    public const val MAX_SEMANTIC_DEPTH: Int = 32
    public const val MAX_SEMANTIC_BYTES: Long = 512L * 1024
    public const val MAX_DRAFTS: Int = 10
    public const val MAX_DRAFT_BYTES: Long = 25L * 1024 * 1024
    public const val DEFAULT_DRAFT_TTL_MS: Long = 72L * 60 * 60 * 1000
    public const val MAX_ENTRY_PATH_LENGTH: Int = 128
    public const val MAX_ATTRIBUTE_LENGTH: Int = 256
    public const val MAX_FREE_TEXT_LENGTH: Int = 20_000
}

public data class ImageDimensions(
    val width: Int,
    val height: Int,
) {
    public val longestEdge: Int get() = maxOf(width, height)
}

public object ImageBudget {
    public fun decodedBytes(width: Int, height: Int): Long {
        require(width >= 0 && height >= 0) { "dimensions must be non-negative" }
        return width.toLong() * height.toLong() * 4L
    }

    public fun isWithinDecodedBudget(
        width: Int,
        height: Int,
        maxBytes: Long = Limits.MAX_DECODED_IMAGE_BYTES,
    ): Boolean {
        if (width <= 0 || height <= 0) return false
        if (maxOf(width, height) > Limits.MAX_IMAGE_LONG_EDGE_PX * 4) return false
        return decodedBytes(width, height) <= maxBytes
    }

    public fun scaledToLongestEdge(
        width: Int,
        height: Int,
        maxLongEdge: Int = Limits.MAX_IMAGE_LONG_EDGE_PX,
    ): ImageDimensions {
        require(width > 0 && height > 0) { "dimensions must be positive" }
        require(maxLongEdge > 0) { "maxLongEdge must be positive" }
        val longest = maxOf(width, height)
        if (longest <= maxLongEdge) return ImageDimensions(width, height)
        val scale = maxLongEdge.toDouble() / longest.toDouble()
        return ImageDimensions(
            width = maxOf(1, (width * scale).toInt()),
            height = maxOf(1, (height * scale).toInt()),
        )
    }
}
