// SPDX-License-Identifier: Apache-2.0

package com.awbuilds.reproparcel.spike.capturelab

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
import android.view.Window
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.MainThread
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume

enum class CaptureStatus {
    SUCCESS,
    SECURE_WINDOW,
    TIMEOUT,
    FAILED,
}

data class CaptureResult(
    val status: CaptureStatus,
    val width: Int = 0,
    val height: Int = 0,
    val elapsedMs: Long = 0,
    val detail: String = "",
    val bitmap: Bitmap? = null,
)

data class SemanticsNode(
    val className: String,
    val text: String?,
    val contentDescription: String?,
    val bounds: Rect,
    val depth: Int,
    val editable: Boolean,
    val password: Boolean,
    val childCount: Int,
)

data class SemanticsSnapshot(
    val nodes: List<SemanticsNode>,
    val truncated: Boolean,
    val elapsedMs: Long,
    val approximateBytes: Int,
    val limitation: String = "",
)

object CaptureLab {
    const val DEFAULT_TIMEOUT_MS = 2_000L
    private const val MAX_NODES = 500
    private const val MAX_DEPTH = 32

    @MainThread
    suspend fun captureWindow(
        window: Window,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        respectSecureFlag: Boolean = true,
    ): CaptureResult {
        if (respectSecureFlag && isSecure(window)) {
            return CaptureResult(
                status = CaptureStatus.SECURE_WINDOW,
                detail = "FLAG_SECURE is set; capture omitted",
            )
        }

        val decor = window.decorView
        val width = decor.width
        val height = decor.height
        if (width <= 0 || height <= 0) {
            return CaptureResult(
                status = CaptureStatus.FAILED,
                detail = "window has no measurable content",
            )
        }

        val started = SystemClock.elapsedRealtime()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val handler = Handler(Looper.getMainLooper())
        var callbackCompleted = false

        val result = try {
            withTimeout(timeoutMs) {
                suspendCancellableCoroutine<Int> { continuation ->
                    PixelCopy.request(
                        window,
                        bitmap,
                        { copyResult ->
                            callbackCompleted = true
                            if (continuation.isActive) {
                                continuation.resume(copyResult)
                            } else {
                                // The timeout/cancellation won the race. The
                                // callback owns the destination now; never
                                // recycle underneath an in-flight native copy.
                                bitmap.recycle()
                            }
                        },
                        handler,
                    )
                }
            }
        } catch (_: TimeoutCancellationException) {
            // Do not recycle here: PixelCopy can still write to this bitmap.
            // The callback recycles it, or it becomes garbage after the
            // request is abandoned. A later Phase 2 implementation should use
            // a request-owned buffer with an explicit abandon protocol.
            return CaptureResult(
                status = CaptureStatus.TIMEOUT,
                elapsedMs = SystemClock.elapsedRealtime() - started,
                detail = "PixelCopy exceeded ${timeoutMs}ms",
            )
        } catch (unavailable: IllegalArgumentException) {
            if (callbackCompleted) bitmap.recycle()
            return CaptureResult(
                status = CaptureStatus.FAILED,
                elapsedMs = SystemClock.elapsedRealtime() - started,
                detail = "PixelCopy source unavailable: ${unavailable.javaClass.simpleName}",
            )
        } catch (denied: SecurityException) {
            if (callbackCompleted) bitmap.recycle()
            return CaptureResult(
                status = CaptureStatus.FAILED,
                elapsedMs = SystemClock.elapsedRealtime() - started,
                detail = "PixelCopy denied: ${denied.javaClass.simpleName}",
            )
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            if (callbackCompleted) bitmap.recycle()
            throw cancelled
        }

        val elapsed = SystemClock.elapsedRealtime() - started
        if (result != PixelCopy.SUCCESS) {
            bitmap.recycle()
            return CaptureResult(
                status = CaptureStatus.FAILED,
                elapsedMs = elapsed,
                detail = "PixelCopy result=$result",
            )
        }

        return CaptureResult(
            status = CaptureStatus.SUCCESS,
            width = width,
            height = height,
            elapsedMs = elapsed,
            detail = "bytes=${bitmap.byteCount}",
            bitmap = bitmap,
        )
    }

    fun isSecure(window: Window): Boolean {
        return (window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
    }

    fun applyOpaqueMask(
        source: Bitmap,
        region: Rect,
        color: Int = Color.BLACK,
    ): Bitmap {
        require(Color.alpha(color) == 255) { "privacy mask must be opaque" }
        require(region.width() > 0 && region.height() > 0) { "mask region is empty" }
        val intersection = Rect(region)
        require(intersection.intersect(0, 0, source.width, source.height)) {
            "mask region does not intersect the bitmap"
        }
        val copy = source.copy(Bitmap.Config.ARGB_8888, true)
            ?: error("bitmap copy failed")
        val canvas = Canvas(copy)
        val paint = Paint().apply {
            this.color = color
            style = Paint.Style.FILL
            isAntiAlias = false
        }
        canvas.drawRect(region, paint)
        return copy
    }

    fun samplePixel(bitmap: Bitmap, x: Int, y: Int): Int {
        val safeX = x.coerceIn(0, bitmap.width - 1)
        val safeY = y.coerceIn(0, bitmap.height - 1)
        return bitmap.getPixel(safeX, safeY)
    }

    suspend fun savePng(bitmap: Bitmap, file: File): Boolean = withContext(Dispatchers.IO) {
        val compressed = file.outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        if (!compressed || !file.isFile || file.length() <= 0L) {
            file.delete()
            false
        } else {
            true
        }
    }

    fun dumpSemantics(
        root: View,
        maxNodes: Int = MAX_NODES,
        maxDepth: Int = MAX_DEPTH,
    ): SemanticsSnapshot {
        return dumpSemantics(root.createAccessibilityNodeInfo(), maxNodes, maxDepth)
    }

    fun dumpSemantics(
        root: AccessibilityNodeInfo?,
        maxNodes: Int = MAX_NODES,
        maxDepth: Int = MAX_DEPTH,
    ): SemanticsSnapshot {
        val started = SystemClock.elapsedRealtime()
        val nodes = mutableListOf<SemanticsNode>()
        var truncated = false
        var limitation = ""

        fun visit(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null) return
            if (nodes.size >= maxNodes || depth > maxDepth) {
                truncated = true
                return
            }
            val bounds = Rect().also(node::getBoundsInScreen)
            val forbidden = node.isEditable || node.isPassword
            nodes += SemanticsNode(
                className = node.className?.toString().orEmpty(),
                text = if (forbidden) null else node.text?.toString()?.take(120),
                contentDescription = if (forbidden) null else node.contentDescription?.toString()?.take(120),
                bounds = bounds,
                depth = depth,
                editable = node.isEditable,
                password = node.isPassword,
                childCount = node.childCount,
            )
            if (forbidden) {
                // Never descend into an editable/password subtree.
                return
            }
            for (index in 0 until node.childCount) {
                try {
                    visit(node.getChild(index), depth + 1)
                } catch (unsealed: IllegalStateException) {
                    truncated = true
                    limitation = "node is not sealed; app-level child traversal unavailable"
                    return
                }
            }
        }

        visit(root, 0)
        val approximateBytes = nodes.sumOf {
            it.className.length + (it.text?.length ?: 0) + (it.contentDescription?.length ?: 0)
        }
        return SemanticsSnapshot(
            nodes = nodes,
            truncated = truncated,
            elapsedMs = SystemClock.elapsedRealtime() - started,
            approximateBytes = approximateBytes,
            limitation = limitation,
        )
    }
}
