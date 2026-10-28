package com.notebook.notes.checklist.ui.drawing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.notebook.notes.checklist.model.drawing.CanvasObject

object CanvasObjectRenderer {

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
    }

    fun imageBounds(obj: CanvasObject): RectF {
        return RectF(0f, 0f, obj.width, obj.height)
    }

    fun draw(
        canvas: Canvas,
        obj: CanvasObject,
        bitmap: Bitmap?
    ) {
        if (bitmap == null) return
        canvas.save()
        canvas.concat(CanvasObjectTransforms.buildMatrix(obj))
        canvas.drawBitmap(bitmap, null, imageBounds(obj), bitmapPaint)
        canvas.restore()
    }
}
