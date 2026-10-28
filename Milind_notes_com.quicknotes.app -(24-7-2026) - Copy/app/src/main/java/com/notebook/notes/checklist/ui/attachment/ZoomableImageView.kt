package com.notebook.notes.checklist.ui.attachment

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    private val matrix = Matrix()
    private val lastPoint = PointF()
    private var mode = Mode.NONE
    private var minScale = 1f
    private var maxScale = 4f

    private enum class Mode { NONE, DRAG, ZOOM }

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scale = currentScale() * detector.scaleFactor
            val clamped = scale.coerceIn(minScale, maxScale)
            matrix.postScale(clamped / currentScale(), clamped / currentScale(), detector.focusX, detector.focusY)
            imageMatrix = matrix
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            matrix.reset()
            imageMatrix = matrix
            return true
        }
    })

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastPoint.set(event.x, event.y)
                mode = Mode.DRAG
                parent?.requestDisallowInterceptTouchEvent(currentScale() > minScale)
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == Mode.DRAG && !scaleDetector.isInProgress) {
                    parent?.requestDisallowInterceptTouchEvent(currentScale() > minScale)
                    matrix.postTranslate(event.x - lastPoint.x, event.y - lastPoint.y)
                    lastPoint.set(event.x, event.y)
                    imageMatrix = matrix
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = Mode.NONE
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    fun resetTransform() {
        matrix.reset()
        imageMatrix = matrix
    }

    private fun currentScale(): Float {
        val values = FloatArray(9)
        matrix.getValues(values)
        return values[Matrix.MSCALE_X]
    }
}
