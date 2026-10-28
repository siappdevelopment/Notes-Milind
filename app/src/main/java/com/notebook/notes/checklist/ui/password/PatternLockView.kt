package com.notebook.notes.checklist.ui.password

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.notebook.notes.checklist.R
import kotlin.math.hypot
import kotlin.math.min

class PatternLockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface Listener {
        fun onPatternComplete(pattern: List<Int>)
    }

    var listener: Listener? = null

    private val dots = Array(9) { Dot(0f, 0f) }
    private val selected = mutableListOf<Int>()
    private val path = Path()

    private var currentX = 0f
    private var currentY = 0f
    private var isDrawing = false
    private var showError = false

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.password_dot)
    }
    private val selectedDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.password_accent)
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
        color = ContextCompat.getColor(context, R.color.password_accent)
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = ContextCompat.getColor(context, R.color.password_line)
    }
    private val errorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = ContextCompat.getColor(context, R.color.password_error)
    }

    private var cellSize = 0f
    private var dotRadius = 0f
    private var hitRadius = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val size = min(w, h).toFloat()
        val left = (w - size) / 2f
        val top = (h - size) / 2f
        cellSize = size / 3f
        dotRadius = cellSize * 0.07f
        hitRadius = cellSize * 0.32f
        for (i in 0 until 9) {
            val row = i / 3
            val col = i % 3
            dots[i].x = left + col * cellSize + cellSize / 2f
            dots[i].y = top + row * cellSize + cellSize / 2f
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val activeLinePaint = if (showError) errorPaint else linePaint
        val activeDotPaint = if (showError) {
            Paint(selectedDotPaint).apply {
                color = ContextCompat.getColor(context, R.color.password_error)
            }
        } else {
            selectedDotPaint
        }
        val activeRingPaint = if (showError) {
            Paint(ringPaint).apply {
                color = ContextCompat.getColor(context, R.color.password_error)
            }
        } else {
            ringPaint
        }

        if (selected.isNotEmpty()) {
            path.reset()
            val first = dots[selected[0]]
            path.moveTo(first.x, first.y)
            for (i in 1 until selected.size) {
                val d = dots[selected[i]]
                path.lineTo(d.x, d.y)
            }
            if (isDrawing) {
                path.lineTo(currentX, currentY)
            }
            canvas.drawPath(path, activeLinePaint)
        }

        for (i in 0 until 9) {
            val d = dots[i]
            if (selected.contains(i)) {
                canvas.drawCircle(d.x, d.y, hitRadius * 0.55f, activeRingPaint)
                canvas.drawCircle(d.x, d.y, dotRadius, activeDotPaint)
            } else {
                canvas.drawCircle(d.x, d.y, dotRadius, dotPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                showError = false
                selected.clear()
                isDrawing = true
                currentX = event.x
                currentY = event.y
                hitTest(event.x, event.y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isDrawing) return false
                currentX = event.x
                currentY = event.y
                hitTest(event.x, event.y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!isDrawing) return false
                isDrawing = false
                invalidate()
                if (selected.isNotEmpty()) {
                    listener?.onPatternComplete(selected.toList())
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun clearPattern(withError: Boolean = false) {
        showError = withError
        selected.clear()
        isDrawing = false
        invalidate()
        if (withError) {
            postDelayed({
                showError = false
                invalidate()
            }, 600)
        }
    }

    private fun hitTest(x: Float, y: Float) {
        for (i in 0 until 9) {
            if (selected.contains(i)) continue
            val d = dots[i]
            if (hypot(x - d.x, y - d.y) <= hitRadius) {
                // Fill in skipped middle dot when jumping over one
                if (selected.isNotEmpty()) {
                    val last = selected.last()
                    val mid = middleDot(last, i)
                    if (mid != null && !selected.contains(mid)) {
                        selected.add(mid)
                    }
                }
                selected.add(i)
                break
            }
        }
    }

    private fun middleDot(a: Int, b: Int): Int? {
        val ax = a % 3
        val ay = a / 3
        val bx = b % 3
        val by = b / 3
        if ((ax + bx) % 2 != 0 || (ay + by) % 2 != 0) return null
        val mx = (ax + bx) / 2
        val my = (ay + by) / 2
        val mid = my * 3 + mx
        return if (mid != a && mid != b) mid else null
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private data class Dot(var x: Float, var y: Float)
}
