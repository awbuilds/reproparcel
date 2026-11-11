// SPDX-License-Identifier: Apache-2.0
package com.awbuilds.reproparcel.compose

import android.os.SystemClock
import android.util.Log

/** In-process timing for trigger-to-composer performance evidence. */
public object ReporterLaunchTiming {
    @Volatile
    private var requestedAtMs: Long? = null

    @Volatile
    public var lastDurationMs: Long? = null
        private set

    public fun markRequested() {
        requestedAtMs = SystemClock.elapsedRealtime()
        lastDurationMs = null
    }

    public fun markComposerShown() {
        val started = requestedAtMs ?: return
        val duration = SystemClock.elapsedRealtime() - started
        lastDurationMs = duration
        requestedAtMs = null
        Log.i("ReproParcelPerf", "triggerToComposerInApp=${duration}ms")
    }
}
