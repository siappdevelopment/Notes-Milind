package com.notebook.notes.checklist.ui.drawing

import android.graphics.DashPathEffect
import android.graphics.Paint
import com.notebook.notes.checklist.model.drawing.BrushStyle
import com.notebook.notes.checklist.model.drawing.DrawingShapeMode
import kotlin.math.max

object DrawingPaintStyle {

    fun apply(
        paint: Paint,
        brushStyle: BrushStyle,
        color: Int,
        brushSizeDp: Float,
        density: Float
    ) {
        val sizePx = brushSizeDp * max(density, 1f)
        paint.style = Paint.Style.STROKE
        paint.pathEffect = null
        paint.maskFilter = null
        when (brushStyle) {
            BrushStyle.WATERCOLOR -> {
                paint.color = color
                paint.strokeWidth = sizePx * 2f
                paint.alpha = 180
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeJoin = Paint.Join.ROUND
            }
            BrushStyle.HIGHLIGHTER -> {
                paint.color = color
                paint.strokeWidth = sizePx * 1.5f
                paint.alpha = 128
                paint.strokeCap = Paint.Cap.SQUARE
                paint.strokeJoin = Paint.Join.MITER
            }
            BrushStyle.PENCIL -> {
                paint.color = color
                paint.strokeWidth = sizePx * 0.9f
                paint.alpha = 210
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeJoin = Paint.Join.ROUND
            }
            BrushStyle.DOTTED -> {
                paint.color = color
                paint.strokeWidth = sizePx.coerceAtLeast(2f)
                paint.alpha = 255
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeJoin = Paint.Join.ROUND
                paint.pathEffect = DashPathEffect(DottedBrushRenderer.dashIntervals(sizePx), 0f)
            }
            BrushStyle.OUTLINE -> {
                paint.color = color
                paint.strokeWidth = max(sizePx * 2.35f, 6f)
                paint.alpha = 235
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeJoin = Paint.Join.ROUND
            }
        }
    }

    fun applyForShape(
        paint: Paint,
        brushStyle: BrushStyle,
        color: Int,
        brushSizeDp: Float,
        density: Float,
        shapeMode: DrawingShapeMode
    ) {
        val sizePx = brushSizeDp * max(density, 1f)
        paint.pathEffect = null
        paint.maskFilter = null
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND

        if (brushStyle == BrushStyle.OUTLINE || isLineShape(shapeMode)) {
            paint.style = Paint.Style.STROKE
            paint.color = color
            paint.strokeWidth = sizePx.coerceAtLeast(2f)
            paint.alpha = 255
            return
        }

        when (brushStyle) {
            BrushStyle.WATERCOLOR -> {
                paint.style = Paint.Style.FILL
                paint.color = color
                paint.alpha = 180
            }
            BrushStyle.HIGHLIGHTER -> {
                paint.style = Paint.Style.FILL
                paint.color = color
                paint.alpha = 128
            }
            BrushStyle.PENCIL -> {
                paint.style = Paint.Style.FILL
                paint.color = color
                paint.alpha = 255
            }
            BrushStyle.DOTTED -> {
                paint.style = Paint.Style.STROKE
                paint.color = color
                paint.strokeWidth = sizePx.coerceAtLeast(2f)
                paint.alpha = 255
                paint.pathEffect = DashPathEffect(DottedBrushRenderer.dashIntervals(sizePx), 0f)
            }
            BrushStyle.OUTLINE -> Unit
        }
    }

    private fun isLineShape(shapeMode: DrawingShapeMode): Boolean {
        return shapeMode == DrawingShapeMode.LINE || shapeMode == DrawingShapeMode.ARROW
    }
}
