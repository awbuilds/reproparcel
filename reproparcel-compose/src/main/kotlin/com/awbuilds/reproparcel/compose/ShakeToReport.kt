// SPDX-License-Identifier: Apache-2.0
package com.awbuilds.reproparcel.compose

import android.app.Activity
import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.awbuilds.reproparcel.android.sensor.ShakeDetector

/** Opt-in foreground-only shake entry point. Manual reporting remains primary. */
public object ShakeToReport {
    private var detector: ShakeDetector? = null
    private var observer: DefaultLifecycleObserver? = null
    private var activity: Activity? = null
    private var surfaceId: String? = null

    @MainThread
    public fun enable(activity: Activity, surfaceId: String): Boolean {
        disable()
        this.activity = activity
        this.surfaceId = surfaceId
        val owner = activity as? LifecycleOwner ?: return false
        val shake = ShakeDetector(
            context = activity.applicationContext,
            onShake = {
                val target = this.activity ?: return@ShakeDetector
                val surface = this.surfaceId ?: return@ShakeDetector
                ReproParcelCompose.showReporter(target, surface)
            },
        )
        val lifecycleObserver = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                shake.start()
            }

            override fun onStop(owner: LifecycleOwner) {
                shake.stop()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                shake.stop()
                owner.lifecycle.removeObserver(this)
            }
        }
        owner.lifecycle.addObserver(lifecycleObserver)
        detector = shake
        observer = lifecycleObserver
        return true
    }

    @MainThread
    public fun disable() {
        detector?.stop()
        observer?.let { (activity as? LifecycleOwner)?.lifecycle?.removeObserver(it) }
        detector = null
        observer = null
        activity = null
        surfaceId = null
    }
}
