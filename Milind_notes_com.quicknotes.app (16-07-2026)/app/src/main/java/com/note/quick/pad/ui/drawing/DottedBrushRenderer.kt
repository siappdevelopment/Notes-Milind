package com.note.quick.pad.ui.drawing

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import com.note.quick.pad.model.drawing.DrawingShapeMode
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object DottedBrushRenderer {

    fun dashIntervals(strokeWidthPx: Float): FloatArray {
        val width = strokeWidthPx.coerceAtLeast(2f)
        val dashOn = (width * 2.35f).coerceAtLeast(5f)
        val dashOff = (dashOn * 0.65f).coerceAtLeast(width * 0.85f)
        return floatArrayOf(dashOn, dashOff)
    }

    fun createStrokePaint(color: Int, strokeWidthPx: Float): Paint {
        val width = strokeWidthPx.coerceAtLeast(2f)
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.color = color
            alpha = Color.alpha(color).takeIf { it > 0 } ?: 255
            strokeWidth = width
            pathEffect = DashPathEffect(dashIntervals(width), 0f)
        }
    }

    fun drawFreehand(
        canvas: Canvas,
        points: List<PointF>,
        color: Int,
        strokeWidthPx: Float
    ) {
        if (points.isEmpty()) return
        val paint = createStrokePaint(color, strokeWidthPx)
        if (points.size == 1) {
            val point = points.first()
            val radius = strokeWidthPx.coerceAtLeast(2f) * 0.5f
            canvas.drawCircle(point.x, point.y, radius, paint.apply { style = Paint.Style.FILL })
            return
        }
        canvas.drawPath(buildSmoothPath(points), paint)
    }

    fun drawPath(
        canvas: Canvas,
        path: Path,
        color: Int,
        strokeWidthPx: Float
    ) {
        canvas.drawPath(path, createStrokePaint(color, strokeWidthPx))
    }

    fun drawShape(
        canvas: Canvas,
        shapeMode: DrawingShapeMode,
        start: PointF,
        end: PointF,
        color: Int,
        strokeWidthPx: Float
    ) {
        if (shapeMode == DrawingShapeMode.ARROW) {
            val shaft = Path().apply {
                moveTo(start.x, start.y)
                lineTo(end.x, end.y)
            }
            drawPath(canvas, shaft, color, strokeWidthPx)
            drawArrowHead(canvas, start, end, color, strokeWidthPx)
            return
        }
        val path = DrawingShapeRenderer.buildShapePath(shapeMode, start, end)
        drawPath(canvas, path, color, strokeWidthPx)
    }

    private fun drawArrowHead(
        canvas: Canvas,
        start: PointF,
        end: PointF,
        color: Int,
        strokeWidthPx: Float
    ) {
        val paint = createStrokePaint(color, strokeWidthPx * 0.9f)
        val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
        val headLength = min(strokeWidthPx * 3f, 36f)
        val wingAngle = Math.toRadians(28.0)
        val wingOne = Path().apply {
            moveTo(end.x, end.y)
            lineTo(
                end.x - headLength * cos(angle - wingAngle).toFloat(),
                end.y - headLength * sin(angle - wingAngle).toFloat()
            )
        }
        val wingTwo = Path().apply {
            moveTo(end.x, end.y)
            lineTo(
                end.x - headLength * cos(angle + wingAngle).toFloat(),
                end.y - headLength * sin(angle + wingAngle).toFloat()
            )
        }
        canvas.drawPath(wingOne, paint)
        canvas.drawPath(wingTwo, paint)
    }

    private fun buildSmoothPath(points: List<PointF>): Path {
        val path = Path()
        path.moveTo(points[0].x, points[0].y)
        if (points.size == 1) {
            path.lineTo(points[0].x + 0.1f, points[0].y + 0.1f)
            return path
        }
        var index = 1
        while (index < points.size - 1) {
            val cx = points[index].x
            val cy = points[index].y
            val ex = (cx + points[index + 1].x) / 2f
            val ey = (cy + points[index + 1].y) / 2f
            path.quadTo(cx, cy, ex, ey)
            index++
        }
        path.lineTo(points.last().x, points.last().y)
        return path
    }
}
