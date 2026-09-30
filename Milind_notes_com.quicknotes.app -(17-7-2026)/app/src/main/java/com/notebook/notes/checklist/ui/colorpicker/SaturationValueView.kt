package com.notebook.notes.checklist.ui.colorpicker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

/**
 * Saturation (X) × Value/Brightness (Y) selector for a fixed hue.
 * Recreates the SV bitmap only when hue or size changes.
 */
class SaturationValueView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnSvChangeListener {
        fun onSvChanged(saturation: Float, value: Float, fromUser: Boolean)
    }

    var listener: OnSvChangeListener? = null

    private var hue: Float = 0f
    private var saturation: Float = 1f
    private var value: Float = 1f

    private var svBitmap: Bitmap? = null
    private var cachedHue: Float = Float.NaN
    private var cachedWidth: Int = 0
    private var cachedHeight: Int = 0

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val selectorFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    private val selectorStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * resources.displayMetrics.density
        color = Color.WHITE
    }
    private val selectorOuter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * resources.displayMetrics.density
        color = Color.BLACK
        alpha = 80
    }

    private val drawRect = RectF()
    private val clipPath = android.graphics.Path()
    private val cornerRadius = 12f * resources.displayMetrics.density
    private val selectorRadius = 10f * resources.displayMetrics.density

    fun setHue(hueDegrees: Float, invalidateBitmap: Boolean = true) {
        val h = hueDegrees.coerceIn(0f, 360f)
        if (hue == h && !invalidateBitmap) return
        hue = h
        if (invalidateBitmap) {
            recycleBitmap()
            ensureBitmap()
        }
        invalidate()
    }

    fun setSaturationValue(sat: Float, `val`: Float, notify: Boolean = false) {
        saturation = sat.coerceIn(0f, 1f)
        value = `val`.coerceIn(0f, 1f)
        invalidate()
        if (notify) {
            listener?.onSvChanged(saturation, value, false)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            recycleBitmap()
            ensureBitmap()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        ensureBitmap()
        val bmp = svBitmap ?: return
        drawRect.set(0f, 0f, width.toFloat(), height.toFloat())
        clipPath.reset()
        clipPath.addRoundRect(drawRect, cornerRadius, cornerRadius, android.graphics.Path.Direction.CW)
        canvas.save()
        canvas.clipPath(clipPath)
        canvas.drawBitmap(bmp, null, drawRect, bitmapPaint)
        canvas.restore()

        val cx = saturation * width
        val cy = (1f - value) * height
        canvas.drawCircle(cx, cy, selectorRadius, selectorOuter)
        canvas.drawCircle(cx, cy, selectorRadius, selectorStroke)
        selectorFill.color = Color.HSVToColor(floatArrayOf(hue, saturation, value))
        canvas.drawCircle(cx, cy, selectorRadius - selectorStroke.strokeWidth, selectorFill)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                updateFromTouch(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                updateFromTouch(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateFromTouch(x: Float, y: Float) {
        val w = max(width, 1)
        val h = max(height, 1)
        saturation = (x / w).coerceIn(0f, 1f)
        value = (1f - y / h).coerceIn(0f, 1f)
        invalidate()
        listener?.onSvChanged(saturation, value, true)
    }

    private fun ensureBitmap() {
        val w = width
        val h = height
        if (w <= 0 || h <= 0) return
        if (svBitmap != null &&
            cachedWidth == w &&
            cachedHeight == h &&
            cachedHue == hue
        ) {
            return
        }
        recycleBitmap()
        // Downsample for performance while keeping smooth look via FILTER_BITMAP.
        val bmpW = min(w, 256)
        val bmpH = min(h, 256)
        val pixels = IntArray(bmpW * bmpH)
        val hsv = floatArrayOf(hue, 0f, 0f)
        for (y in 0 until bmpH) {
            val v = 1f - y / (bmpH - 1f).coerceAtLeast(1f)
            for (x in 0 until bmpW) {
                val s = x / (bmpW - 1f).coerceAtLeast(1f)
                hsv[1] = s
                hsv[2] = v
                pixels[y * bmpW + x] = Color.HSVToColor(hsv)
            }
        }
        svBitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888).also {
            it.setPixels(pixels, 0, bmpW, 0, 0, bmpW, bmpH)
        }
        cachedHue = hue
        cachedWidth = w
        cachedHeight = h
    }

    private fun recycleBitmap() {
        svBitmap?.recycle()
        svBitmap = null
        cachedHue = Float.NaN
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        recycleBitmap()
    }
}
