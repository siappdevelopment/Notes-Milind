package com.notebook.notes.checklist.ui.drawing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.widget.ImageView

object DrawingExportHelper {

    fun exportComposite(backgroundView: ImageView, drawingView: DrawingView): Bitmap? {
        val width = drawingView.width.takeIf { it > 0 } ?: return null
        val height = drawingView.height.takeIf { it > 0 } ?: return null
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)

        drawBackgroundLayer(backgroundView, canvas, width, height)

        drawingView.exportContentBitmap()?.let { content ->
            canvas.drawBitmap(content, 0f, 0f, null)
        }
        return result
    }

    private fun drawBackgroundLayer(backgroundView: ImageView, canvas: Canvas, width: Int, height: Int) {
        val drawable = backgroundView.drawable ?: return
        if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap ?: return
            canvas.drawBitmap(
                bitmap,
                null,
                RectF(0f, 0f, width.toFloat(), height.toFloat()),
                null
            )
            return
        }
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
    }
}
