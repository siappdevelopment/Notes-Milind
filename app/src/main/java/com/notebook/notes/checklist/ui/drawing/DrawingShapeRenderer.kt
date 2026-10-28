package com.notebook.notes.checklist.ui.drawing

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import com.notebook.notes.checklist.model.drawing.DrawingShapeMode
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object DrawingShapeRenderer {

    fun buildShapePath(
        shapeMode: DrawingShapeMode,
        start: PointF,
        end: PointF
    ): Path {
        val path = Path()
        when (shapeMode) {
            DrawingShapeMode.FREEHAND -> Unit
            DrawingShapeMode.LINE -> {
                path.moveTo(start.x, start.y)
                path.lineTo(end.x, end.y)
            }
            DrawingShapeMode.SQUARE -> {
                path.addRect(
                    min(start.x, end.x),
                    min(start.y, end.y),
                    max(start.x, end.x),
                    max(start.y, end.y),
                    Path.Direction.CW
                )
            }
            DrawingShapeMode.CIRCLE -> {
                path.addOval(
                    RectF(
                        min(start.x, end.x),
                        min(start.y, end.y),
                        max(start.x, end.x),
                        max(start.y, end.y)
                    ),
                    Path.Direction.CW
                )
            }
            DrawingShapeMode.ARROW -> {
                path.moveTo(start.x, start.y)
                path.lineTo(end.x, end.y)
            }
        }
        return path
    }

    fun draw(
        canvas: Canvas,
        shapeMode: DrawingShapeMode,
        start: PointF,
        end: PointF,
        paint: Paint
    ) {
        when (shapeMode) {
            DrawingShapeMode.FREEHAND -> Unit
            DrawingShapeMode.LINE -> {
                canvas.drawLine(start.x, start.y, end.x, end.y, paint)
            }
            DrawingShapeMode.SQUARE -> {
                canvas.drawRect(
                    min(start.x, end.x),
                    min(start.y, end.y),
                    max(start.x, end.x),
                    max(start.y, end.y),
                    paint
                )
            }
            DrawingShapeMode.CIRCLE -> {
                canvas.drawOval(
                    min(start.x, end.x),
                    min(start.y, end.y),
                    max(start.x, end.x),
                    max(start.y, end.y),
                    paint
                )
            }
            DrawingShapeMode.ARROW -> {
                canvas.drawLine(start.x, start.y, end.x, end.y, paint)
                drawArrowHead(canvas, start, end, paint)
            }
        }
    }

    private fun drawArrowHead(canvas: Canvas, start: PointF, end: PointF, paint: Paint) {
        val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
        val headLength = min(paint.strokeWidth * 3f, 36f)
        val wingAngle = Math.toRadians(28.0)
        val x1 = end.x - headLength * cos(angle - wingAngle).toFloat()
        val y1 = end.y - headLength * sin(angle - wingAngle).toFloat()
        val x2 = end.x - headLength * cos(angle + wingAngle).toFloat()
        val y2 = end.y - headLength * sin(angle + wingAngle).toFloat()
        canvas.drawLine(end.x, end.y, x1, y1, paint)
        canvas.drawLine(end.x, end.y, x2, y2, paint)
    }
}
