package com.notebook.notes.checklist.ui.colorpicker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

/**
 * Horizontal hue slider (0° → 360°) with a clearly visible circular thumb.
 */
class HueSliderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    fun interface OnHueChangeListener {
        fun onHueChanged(hue: Float, fromUser: Boolean)
    }

    var listener: OnHueChangeListener? = null

    private var hue: Float = 0f
    private val trackRect = RectF()
    private val thumb = ColorSliderThumbStyle(resources)

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        // Required for Paint shadowLayer on hardware-accelerated views pre-P in some cases;
        // software layer keeps the soft thumb shadow crisp.
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun setHue(hueDegrees: Float, notify: Boolean = false) {
        hue = hueDegrees.coerceIn(0f, 360f)
        invalidate()
        if (notify) listener?.onHueChanged(hue, false)
    }

    fun getHue(): Float = hue

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minHeight = thumb.preferredHeight()
        val h = resolveSize(minHeight, heightMeasureSpec)
        val w = resolveSize((200 * thumb.density).toInt(), widthMeasureSpec)
        setMeasuredDimension(w, max(h, minHeight))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val cy = h / 2f
        val inset = thumb.thumbRadius
        trackRect.set(
            inset,
            cy - thumb.trackHeight / 2f,
            w - inset,
            cy + thumb.trackHeight / 2f
        )
        trackPaint.shader = LinearGradient(
            trackRect.left,
            0f,
            trackRect.right,
            0f,
            HUE_COLORS,
            null,
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRoundRect(
            trackRect,
            thumb.trackCornerRadius,
            thumb.trackCornerRadius,
            trackPaint
        )
        val cx = trackRect.left + (hue / 360f) * trackRect.width()
        val cy = height / 2f
        val preview = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
        thumb.draw(canvas, cx, cy, preview)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                updateFromTouch(event.x)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                updateFromTouch(event.x)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateFromTouch(x: Float) {
        val fraction = ((x - trackRect.left) / trackRect.width()).coerceIn(0f, 1f)
        hue = fraction * 360f
        invalidate()
        listener?.onHueChanged(hue, true)
    }

    companion object {
        private val HUE_COLORS = intArrayOf(
            Color.RED,
            Color.YELLOW,
            Color.GREEN,
            Color.CYAN,
            Color.BLUE,
            Color.MAGENTA,
            Color.RED
        )
    }
}
