package com.notebook.notes.checklist.ui.drawing

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import com.notebook.notes.checklist.model.drawing.DrawingShapeMode
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object OutlineBrushRenderer {

    private data class GlowLayer(
        val widthMultiplier: Float,
        val blurMultiplier: Float,
        val alpha: Int
    )

    private val spreadLayers = listOf(
        GlowLayer(widthMultiplier = 5.2f, blurMultiplier = 1.55f, alpha = 55),
        GlowLayer(widthMultiplier = 3.8f, blurMultiplier = 1.05f, alpha = 110),
        GlowLayer(widthMultiplier = 2.6f, blurMultiplier = 0.55f, alpha = 185),
        GlowLayer(widthMultiplier = 1.7f, blurMultiplier = 0.22f, alpha = 245)
    )

    fun drawPath(canvas: Canvas, path: Path, color: Int, strokeWidthPx: Float) {
        drawSpreadGlow(canvas, color, strokeWidthPx) { paint ->
            canvas.drawPath(path, paint)
        }
        canvas.drawPath(path, createCorePaint(strokeWidthPx))
    }

    fun drawShape(
        canvas: Canvas,
        shapeMode: DrawingShapeMode,
        start: PointF,
        end: PointF,
        color: Int,
        strokeWidthPx: Float
    ) {
        when (shapeMode) {
            DrawingShapeMode.FREEHAND -> Unit
            DrawingShapeMode.LINE -> drawLine(canvas, start.x, start.y, end.x, end.y, color, strokeWidthPx)
            DrawingShapeMode.SQUARE -> drawRect(
                canvas,
                min(start.x, end.x),
                min(start.y, end.y),
                max(start.x, end.x),
                max(start.y, end.y),
                color,
                strokeWidthPx
            )
            DrawingShapeMode.CIRCLE -> drawOval(
                canvas,
                min(start.x, end.x),
                min(start.y, end.y),
                max(start.x, end.x),
                max(start.y, end.y),
                color,
                strokeWidthPx
            )
            DrawingShapeMode.ARROW -> {
                drawLine(canvas, start.x, start.y, end.x, end.y, color, strokeWidthPx)
                drawArrowHead(canvas, start, end, color, strokeWidthPx)
            }
        }
    }

    private fun drawLine(
        canvas: Canvas,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        color: Int,
        strokeWidthPx: Float
    ) {
        drawSpreadGlow(canvas, color, strokeWidthPx) { paint ->
            canvas.drawLine(x1, y1, x2, y2, paint)
        }
        canvas.drawLine(x1, y1, x2, y2, createCorePaint(strokeWidthPx))
    }

    private fun drawRect(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        color: Int,
        strokeWidthPx: Float
    ) {
        drawSpreadGlow(canvas, color, strokeWidthPx) { paint ->
            canvas.drawRect(left, top, right, bottom, paint)
        }
        canvas.drawRect(left, top, right, bottom, createCorePaint(strokeWidthPx))
    }

    private fun drawOval(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        color: Int,
        strokeWidthPx: Float
    ) {
        drawSpreadGlow(canvas, color, strokeWidthPx) { paint ->
            canvas.drawOval(left, top, right, bottom, paint)
        }
        canvas.drawOval(left, top, right, bottom, createCorePaint(strokeWidthPx))
    }

    private fun drawArrowHead(
        canvas: Canvas,
        start: PointF,
        end: PointF,
        color: Int,
        strokeWidthPx: Float
    ) {
        val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
        val headLength = min(strokeWidthPx * 3f, 36f)
        val wingAngle = Math.toRadians(28.0)
        val x1 = end.x - headLength * cos(angle - wingAngle).toFloat()
        val y1 = end.y - headLength * sin(angle - wingAngle).toFloat()
        val x2 = end.x - headLength * cos(angle + wingAngle).toFloat()
        val y2 = end.y - headLength * sin(angle + wingAngle).toFloat()
        drawLine(canvas, end.x, end.y, x1, y1, color, strokeWidthPx)
        drawLine(canvas, end.x, end.y, x2, y2, color, strokeWidthPx)
    }

    private fun drawSpreadGlow(
        canvas: Canvas,
        color: Int,
        strokeWidthPx: Float,
        draw: (Paint) -> Unit
    ) {
        spreadLayers.forEach { layer ->
            draw(createSpreadPaint(color, strokeWidthPx, layer))
        }
    }

    private fun createSpreadPaint(color: Int, strokeWidthPx: Float, layer: GlowLayer): Paint {
        val blurRadius = (strokeWidthPx * layer.blurMultiplier).coerceIn(3f, 72f)
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.color = color
            strokeWidth = max(strokeWidthPx * layer.widthMultiplier, 4f)
            alpha = layer.alpha
            maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        }
    }

    private fun createCorePaint(strokeWidthPx: Float): Paint {
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = Color.WHITE
            strokeWidth = max(strokeWidthPx * 0.42f, 2.5f)
        }
    }
}
