package com.notebook.notes.checklist.ui.drawing

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.notebook.notes.checklist.model.drawing.BrushStyle
import kotlin.math.max
import kotlin.math.min

class StrokePreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val path = Path()

    var strokeColor: Int = Color.BLACK
        set(value) {
            field = value
            invalidate()
        }

    var strokeSizeDp: Float = 6f
        set(value) {
            field = value
            invalidate()
        }

    var brushStyle: BrushStyle = BrushStyle.WATERCOLOR
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = max(resources.displayMetrics.density, 1f)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        buildWavePath(w, h)
        if (brushStyle == BrushStyle.OUTLINE) {
            val previewWidth = min(strokeSizeDp * density * 0.32f, h * 0.2f).coerceAtLeast(2f)
            OutlineBrushRenderer.drawPath(canvas, path, strokeColor, previewWidth)
            return
        }
        if (brushStyle == BrushStyle.PENCIL) {
            val previewWidth = min(strokeSizeDp * density * 0.5f, h * 0.34f).coerceAtLeast(2f)
            PencilBrushRenderer.drawPath(canvas, path, strokeColor, previewWidth)
            return
        }
        if (brushStyle == BrushStyle.DOTTED) {
            val previewWidth = min(strokeSizeDp * density * 0.55f, h * 0.34f).coerceAtLeast(3f)
            DottedBrushRenderer.drawPath(canvas, path, strokeColor, previewWidth)
            return
        }

        applyPreviewPaint(density, h)
        canvas.drawPath(path, paint)
    }

    private fun applyPreviewPaint(density: Float, viewHeight: Float) {
        DrawingPaintStyle.apply(paint, brushStyle, strokeColor, strokeSizeDp, density)
        val previewWidth = min(strokeSizeDp * density * 0.55f, viewHeight * 0.38f)
            .coerceAtLeast(2.5f)
        paint.strokeWidth = when (brushStyle) {
            BrushStyle.HIGHLIGHTER -> previewWidth * 1.35f
            BrushStyle.WATERCOLOR -> previewWidth * 1.15f
            else -> previewWidth
        }
        paint.pathEffect = null
    }

    private fun buildWavePath(w: Float, h: Float) {
        val centerY = h * 0.5f
        val amplitude = h * 0.24f
        val startX = w * 0.1f
        val endX = w * 0.9f
        val midX = w * 0.5f

        path.reset()
        path.moveTo(startX, centerY)
        path.cubicTo(
            startX + w * 0.14f,
            centerY - amplitude,
            midX - w * 0.08f,
            centerY - amplitude,
            midX,
            centerY
        )
        path.cubicTo(
            midX + w * 0.08f,
            centerY + amplitude,
            endX - w * 0.14f,
            centerY + amplitude,
            endX,
            centerY
        )
    }
}
