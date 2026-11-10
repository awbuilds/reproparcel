// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.compose

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.geometry.Offset
import java.io.ByteArrayOutputStream
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

enum class AnnotationTool {
    PEN,
    ARROW,
    MASK,
}

data class Annotation(
    val tool: AnnotationTool,
    val points: List<Offset>,
)

object BitmapFlattener {
    fun flatten(bitmap: Bitmap, annotations: List<Annotation>): ByteArray? {
        val copy = bitmap.copy(Bitmap.Config.ARGB_8888, true) ?: return null
        return try {
            val canvas = Canvas(copy)
            val minSide = minOf(copy.width, copy.height).toFloat()
            annotations.forEach { annotation ->
                when (annotation.tool) {
                    AnnotationTool.PEN -> drawStroke(
                        canvas,
                        copy,
                        annotation.points,
                        Color.RED,
                        maxOf(3f, minSide / 160f),
                    )
                    AnnotationTool.ARROW -> drawArrow(canvas, copy, annotation.points, Color.RED)
                    AnnotationTool.MASK -> drawStroke(
                        canvas,
                        copy,
                        annotation.points,
                        Color.BLACK,
                        maxOf(24f, minSide / 10f),
                    )
                }
            }
            val output = ByteArrayOutputStream()
            val compressed = copy.compress(Bitmap.CompressFormat.PNG, 100, output)
            if (!compressed) null else output.toByteArray()
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: IllegalStateException) {
            null
        } finally {
            copy.recycle()
        }
    }

    private fun drawStroke(
        canvas: Canvas,
        bitmap: Bitmap,
        points: List<Offset>,
        color: Int,
        width: Float,
    ) {
        if (points.isEmpty()) return
        val paint = Paint().apply {
            this.color = color
            this.strokeWidth = width
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
        if (points.size == 1) {
            val p = points.first()
            canvas.drawCircle(p.x * bitmap.width, p.y * bitmap.height, width / 2f, paint)
            return
        }
        val path = Path()
        points.forEachIndexed { index, point ->
            val x = point.x * bitmap.width
            val y = point.y * bitmap.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, paint)
    }

    private fun drawArrow(
        canvas: Canvas,
        bitmap: Bitmap,
        points: List<Offset>,
        color: Int,
    ) {
        if (points.size < 2) {
            drawStroke(canvas, bitmap, points, color, 4f)
            return
        }
        val start = points.first()
        val end = points.last()
        val sx = start.x * bitmap.width
        val sy = start.y * bitmap.height
        val ex = end.x * bitmap.width
        val ey = end.y * bitmap.height
        val width = maxOf(4f, minOf(bitmap.width, bitmap.height) / 120f)
        val paint = Paint().apply {
            this.color = color
            strokeWidth = width
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
        canvas.drawLine(sx, sy, ex, ey, paint)
        val angle = atan2((ey - sy).toDouble(), (ex - sx).toDouble())
        val head = maxOf(20f, width * 5f)
        canvas.drawLine(ex, ey, (ex - head * cos(angle - 0.5)).toFloat(), (ey - head * sin(angle - 0.5)).toFloat(), paint)
        canvas.drawLine(ex, ey, (ex - head * cos(angle + 0.5)).toFloat(), (ey - head * sin(angle + 0.5)).toFloat(), paint)
    }
}
