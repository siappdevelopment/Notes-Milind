package com.notebook.notes.checklist.ui.colorpicker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.ColorUtils
import kotlin.math.max

/**
 * Alpha (transparency) slider with checkerboard track and clear circular thumb.
 */
class AlphaSliderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    fun interface OnAlphaChangeListener {
        fun onAlphaChanged(alpha: Int, fromUser: Boolean)
    }

    var listener: OnAlphaChangeListener? = null

    private var alphaValue: Int = 255
    private var solidColor: Int = Color.RED

    private val trackRect = RectF()
    private val thumb = ColorSliderThumbStyle(resources)

    private val checkerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var checkerShader: BitmapShader? = null

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun setAlphaValue(alpha: Int, notify: Boolean = false) {
        alphaValue = alpha.coerceIn(0, 255)
        invalidate()
        if (notify) listener?.onAlphaChanged(alphaValue, false)
    }

    fun setSolidColor(color: Int) {
        solidColor = ColorUtils.setAlphaComponent(color, 255)
        updateGradient()
        invalidate()
    }

    fun getAlphaValue(): Int = alphaValue

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
        ensureCheckerShader()
        updateGradient()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRoundRect(
            trackRect,
            thumb.trackCornerRadius,
            thumb.trackCornerRadius,
            checkerPaint
        )
        canvas.drawRoundRect(
            trackRect,
            thumb.trackCornerRadius,
            thumb.trackCornerRadius,
            gradientPaint
        )

        val cx = trackRect.left + (alphaValue / 255f) * trackRect.width()
        val cy = height / 2f
        val preview = ColorUtils.setAlphaComponent(solidColor, alphaValue)
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
        alphaValue = (fraction * 255f).toInt().coerceIn(0, 255)
        invalidate()
        listener?.onAlphaChanged(alphaValue, true)
    }

    private fun updateGradient() {
        if (trackRect.width() <= 0f) return
        gradientPaint.shader = LinearGradient(
            trackRect.left,
            0f,
            trackRect.right,
            0f,
            ColorUtils.setAlphaComponent(solidColor, 0),
            ColorUtils.setAlphaComponent(solidColor, 255),
            Shader.TileMode.CLAMP
        )
    }

    private fun ensureCheckerShader() {
        if (checkerShader != null) return
        val size = (8 * thumb.density).toInt().coerceAtLeast(4)
        val bmp = Bitmap.createBitmap(size * 2, size * 2, Bitmap.Config.ARGB_8888)
        val light = Color.parseColor("#FFE0E0E0")
        val dark = Color.parseColor("#FFFFFFFF")
        for (y in 0 until bmp.height) {
            for (x in 0 until bmp.width) {
                val lightCell = ((x / size) + (y / size)) % 2 == 0
                bmp.setPixel(x, y, if (lightCell) light else dark)
            }
        }
        checkerShader = BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        checkerPaint.shader = checkerShader
    }
}
