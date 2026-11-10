// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.android.capture

import android.graphics.Bitmap
import com.awbuilds.reproparcel.core.model.OmissionReason

public sealed interface HandoffCapture {
    public data class Captured(public val bitmap: Bitmap) : HandoffCapture
    public data class Omitted(public val reason: OmissionReason) : HandoffCapture
    public data object Unavailable : HandoffCapture
}

/**
 * Process-local handoff from the host capture to the reporter window. It
 * deliberately avoids Bundles, Intent extras, navigation arguments, files and
 * saved state. The reporter consumes the bitmap once; process death safely
 * loses it and the reporter starts text-only.
 */
public object ReporterHandoff {
    private var pending: HandoffCapture? = null

    @Synchronized
    public fun store(capture: HandoffCapture) {
        (pending as? HandoffCapture.Captured)?.bitmap?.recycle()
        pending = capture
    }

    @Synchronized
    public fun take(): HandoffCapture {
        val value = pending ?: HandoffCapture.Unavailable
        pending = null
        return value
    }

    @Synchronized
    public fun clear() {
        (pending as? HandoffCapture.Captured)?.bitmap?.recycle()
        pending = null
    }
}
