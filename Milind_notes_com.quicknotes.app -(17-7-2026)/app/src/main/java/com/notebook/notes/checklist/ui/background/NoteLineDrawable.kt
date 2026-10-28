package com.notebook.notes.checklist.ui.background

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import androidx.core.graphics.withSave

/**
 * Draws ruled / grid lines from JSON `line` config (lineSpacing / lineWidth in dp).
 */
class NoteLineDrawable(
    private val lineColor: Int,
    private val lineWidthPx: Float,
    private val spacingPx: Float,
    private val style: String?
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = lineColor
        strokeWidth = lineWidthPx.coerceAtLeast(1f)
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        if (bounds.isEmpty || spacingPx <= 0f) return
        canvas.withSave {
            val left = bounds.left.toFloat()
            val top = bounds.top.toFloat()
            val right = bounds.right.toFloat()
            val bottom = bounds.bottom.toFloat()

            var y = top + spacingPx
            while (y < bottom) {
                drawLine(left, y, right, y, paint)
                y += spacingPx
            }

            if (style.equals("grid", ignoreCase = true)) {
                var x = left + spacingPx
                while (x < right) {
                    drawLine(x, top, x, bottom, paint)
                    x += spacingPx
                }
            }
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
