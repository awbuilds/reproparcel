// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.compose

import android.app.Activity
import android.content.Intent
import android.os.Looper
import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.awbuilds.reproparcel.android.ReproParcel
import com.awbuilds.reproparcel.android.capture.CaptureController
import com.awbuilds.reproparcel.android.capture.CaptureOutcome
import com.awbuilds.reproparcel.android.capture.HandoffCapture
import com.awbuilds.reproparcel.android.capture.ReporterHandoff
import com.awbuilds.reproparcel.core.model.OmissionReason
import com.awbuilds.reproparcel.core.result.ErrorCode
import com.awbuilds.reproparcel.core.result.SdkResult
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

public object ReproParcelCompose {
    /**
     * Captures eligible host content before the reporter window appears, then
     * opens the Material 3 reporter. The bitmap is handed off in process memory,
     * never through a Bundle, Intent extra or navigation argument.
     */
    private val launching = AtomicBoolean(false)

    @MainThread
    public fun showReporter(
        activity: Activity,
        surfaceId: String = activity.javaClass.name,
    ): SdkResult {
        if (Looper.myLooper() != Looper.getMainLooper()) return SdkResult.Failure(ErrorCode.WRONG_THREAD)
        if (!ReproParcel.isInitialized()) return SdkResult.NotInitialized
        if (!ReproParcel.isEnabled()) return SdkResult.Disabled
        if (activity.isFinishing || activity.isDestroyed) return SdkResult.HostUnavailable
        if (!launching.compareAndSet(false, true)) return SdkResult.Failure(ErrorCode.REPORTER_BUSY)

        val policy = ReproParcel.currentCapturePolicy()
        val lifecycleOwner = activity as? LifecycleOwner
        if (lifecycleOwner == null) {
            launching.set(false)
            return SdkResult.HostUnavailable
        }
        lifecycleOwner.lifecycleScope.launch {
            try {
            val capture = if (policy?.screenshotEnabled == true && ReproParcel.isScreenshotCollectionEnabled()) {
                when (
                    val outcome = CaptureController.capture(
                        activity = activity,
                        policy = policy,
                        surfaceId = surfaceId,
                    )
                ) {
                    is CaptureOutcome.Captured -> HandoffCapture.Captured(outcome.bitmap)
                    is CaptureOutcome.Omitted -> HandoffCapture.Omitted(outcome.reason)
                    is CaptureOutcome.Failed -> HandoffCapture.Omitted(
                        if (outcome.code == ErrorCode.INTERNAL_SAFE_FAILURE) {
                            OmissionReason.CAPABILITY_UNSUPPORTED
                        } else {
                            OmissionReason.PROVIDER_FAILED
                        },
                    )
                }
            } else {
                HandoffCapture.Omitted(OmissionReason.POLICY_DISABLED)
            }
            ReporterHandoff.store(capture)
            if (!activity.isFinishing && !activity.isDestroyed) {
                activity.startActivity(
                    Intent(activity, ReporterActivity::class.java)
                        .putExtra("reproparcel_surface_id", surfaceId),
                )
            }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                ReporterHandoff.clear()
            } finally {
                launching.set(false)
            }
        }
        return SdkResult.Success
    }
}
