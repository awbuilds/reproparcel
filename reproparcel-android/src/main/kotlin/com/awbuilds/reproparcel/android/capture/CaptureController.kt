// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.android.capture

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.PixelCopy
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager
import androidx.annotation.MainThread
import com.awbuilds.reproparcel.core.config.CapturePolicy
import com.awbuilds.reproparcel.core.limits.ImageBudget
import com.awbuilds.reproparcel.core.limits.Limits
import com.awbuilds.reproparcel.core.model.OmissionReason
import com.awbuilds.reproparcel.core.result.ErrorCode
import com.awbuilds.reproparcel.android.ReproParcel
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume

public sealed interface CaptureOutcome {
    public data class Captured(
        public val bitmap: Bitmap,
        public val elapsedMs: Long,
        public val generation: Long,
        public val attemptToken: String,
    ) : CaptureOutcome

    public data class Omitted(public val reason: OmissionReason) : CaptureOutcome
    public data class Failed(public val code: ErrorCode) : CaptureOutcome
}

public data class CaptureAttempt(
    public val token: String,
    public val windowToken: String?,
    public val policyVersion: String,
    public val generation: Long,
    public val enabled: Boolean,
)

public fun isCaptureAttemptValid(
    attempt: CaptureAttempt,
    windowToken: String?,
    policyVersion: String?,
    generation: Long,
    enabled: Boolean,
): Boolean =
    attempt.enabled && enabled &&
        attempt.windowToken == windowToken &&
        attempt.policyVersion == policyVersion &&
        attempt.generation == generation

public object CaptureController {
    private const val TIMEOUT_MS = 2_000L
    private const val MASK_PADDING_PX = 8
    private val generations: MutableMap<View, AtomicLong> =
        Collections.synchronizedMap(WeakHashMap())

    internal suspend fun <T> awaitWithTimeout(timeoutMs: Long, block: suspend () -> T): T? =
        try {
            withTimeout(timeoutMs) { block() }
        } catch (_: TimeoutCancellationException) {
            null
        }

    @MainThread
    public suspend fun capture(
        activity: Activity,
        policy: CapturePolicy,
        surfaceId: String,
    ): CaptureOutcome {
        if (!policy.screenshotEnabled) return CaptureOutcome.Omitted(OmissionReason.POLICY_DISABLED)
        val authoritative = ReproParcel.currentCapturePolicy()
            ?: return CaptureOutcome.Omitted(OmissionReason.POLICY_DISABLED)
        if (!ReproParcel.isScreenshotCollectionEnabled() ||
            authoritative.policyVersion != policy.policyVersion ||
            authoritative.screenshotEnabled != policy.screenshotEnabled ||
            authoritative.maskRegions != policy.maskRegions
        ) {
            return CaptureOutcome.Omitted(OmissionReason.POLICY_CHANGED)
        }
        if (surfaceId !in authoritative.eligibleSurfaces) {
            return CaptureOutcome.Omitted(OmissionReason.UNSUPPORTED_SURFACE)
        }
        if (surfaceId in policy.sensitiveSurfaces || surfaceId in policy.excludedSurfaces) {
            return CaptureOutcome.Omitted(OmissionReason.UNSUPPORTED_SURFACE)
        }
        if (activity.isFinishing || activity.isDestroyed) {
            return CaptureOutcome.Omitted(OmissionReason.HOST_UNAVAILABLE)
        }
        val window = activity.window
        if (isSecure(window)) return CaptureOutcome.Omitted(OmissionReason.SECURE_WINDOW)

        // Give a just-triggered host one short, bounded settle window so the
        // trigger-state draw does not immediately invalidate the frame token.
        delay(100)

        val decor = window.decorView
        val width = decor.width
        val height = decor.height
        if (width <= 0 || height <= 0) {
            return CaptureOutcome.Omitted(OmissionReason.UNSUPPORTED_SURFACE)
        }
        if (ImageBudget.decodedBytes(width, height) > Limits.MAX_DECODED_IMAGE_BYTES ||
            maxOf(width, height) > Limits.MAX_IMAGE_LONG_EDGE_PX * 4
        ) {
            return CaptureOutcome.Omitted(OmissionReason.SIZE_LIMIT)
        }
        if (policy.maskRegions.any {
                it.left < 0 || it.top < 0 || it.right > width || it.bottom > height
            }
        ) {
            return CaptureOutcome.Omitted(OmissionReason.UNSUPPORTED_SURFACE)
        }

        val generation = generationOf(decor)
        val attemptToken = java.util.UUID.randomUUID().toString()
        val windowToken = decor.windowToken?.toString()
        val policyVersion = policy.policyVersion
        val maskDigest = policy.maskRegions.toString()
        val attempt = CaptureAttempt(
            token = attemptToken,
            windowToken = windowToken,
            policyVersion = policyVersion,
            generation = generation,
            enabled = ReproParcel.isEnabled(),
        )
        val started = SystemClock.elapsedRealtime()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val handler = Handler(Looper.getMainLooper())
        var callbackCompleted = false

        val result = try {
            awaitWithTimeout(TIMEOUT_MS) {
                suspendCancellableCoroutine<Int> { continuation ->
                    PixelCopy.request(
                        window,
                        bitmap,
                        { copyResult ->
                            callbackCompleted = true
                            if (continuation.isActive) {
                                continuation.resume(copyResult)
                            } else {
                                bitmap.recycle()
                            }
                        },
                        handler,
                    )
                }
            }
        } catch (_: IllegalArgumentException) {
            bitmap.recycle()
            return CaptureOutcome.Omitted(OmissionReason.UNSUPPORTED_SURFACE)
        } catch (_: SecurityException) {
            bitmap.recycle()
            return CaptureOutcome.Omitted(OmissionReason.SECURE_WINDOW)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            if (callbackCompleted) bitmap.recycle()
            throw cancelled
        }

        val elapsed = SystemClock.elapsedRealtime() - started
        if (result == null) {
            // The callback (if it arrives late) owns the bitmap and recycles it.
            return CaptureOutcome.Omitted(OmissionReason.CAPTURE_TIMEOUT)
        }
        if (result != PixelCopy.SUCCESS) {
            bitmap.recycle()
            return CaptureOutcome.Failed(ErrorCode.INTERNAL_SAFE_FAILURE)
        }
        if (!isCaptureAttemptValid(
                attempt,
                decor.windowToken?.toString(),
                ReproParcel.currentCapturePolicy()?.policyVersion,
                generationOf(decor),
                ReproParcel.isEnabled(),
            ) ||
            ReproParcel.currentCapturePolicy()?.maskRegions.toString() != maskDigest ||
            isSecure(window) ||
            activity.isFinishing ||
            activity.isDestroyed
        ) {
            bitmap.recycle()
            return CaptureOutcome.Omitted(
                if (!ReproParcel.isEnabled()) OmissionReason.POLICY_DISABLED else OmissionReason.STALE_FRAME,
            )
        }

        applyMasks(bitmap, policy)
        val bounded = boundImage(bitmap)
        return CaptureOutcome.Captured(
            bitmap = bounded,
            elapsedMs = elapsed,
            generation = generation,
            attemptToken = attemptToken,
        )
    }

    public fun isSecure(window: Window): Boolean =
        (window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0

    public fun applyMasks(bitmap: Bitmap, policy: CapturePolicy) {
        if (policy.maskRegions.isEmpty()) return
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = false
        }
        policy.maskRegions.forEach { region ->
            val rect = Rect(
                (region.left - MASK_PADDING_PX).coerceAtLeast(0),
                (region.top - MASK_PADDING_PX).coerceAtLeast(0),
                (region.right + MASK_PADDING_PX).coerceAtMost(bitmap.width),
                (region.bottom + MASK_PADDING_PX).coerceAtMost(bitmap.height),
            )
            if (rect.width() > 0 && rect.height() > 0) {
                canvas.drawRect(rect, paint)
            }
        }
    }

    private fun boundImage(source: Bitmap): Bitmap {
        val target = ImageBudget.scaledToLongestEdge(source.width, source.height)
        if (target.width == source.width && target.height == source.height) return source
        val scaled = Bitmap.createScaledBitmap(source, target.width, target.height, true)
        if (scaled !== source) source.recycle()
        return scaled
    }

    private fun generationOf(view: View): Long {
        synchronized(generations) {
            val counter = generations.getOrPut(view) {
                val value = AtomicLong(0)
                // Layout/scroll generation is intentionally narrower than a
                // per-draw token. It catches geometry/content shifts without
                // invalidating on every unrelated frame. Phase 2's contract
                // still requires a stronger frame-binding proof before stable.
                view.viewTreeObserver.addOnGlobalLayoutListener { value.incrementAndGet() }
                view.viewTreeObserver.addOnScrollChangedListener { value.incrementAndGet() }
                view.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                    value.incrementAndGet()
                }
                value
            }
            return counter.get()
        }
    }
}
