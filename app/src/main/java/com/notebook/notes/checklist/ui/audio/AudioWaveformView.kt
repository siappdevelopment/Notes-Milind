package com.notebook.notes.checklist.ui.audio

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.ContextCompat
import com.notebook.notes.checklist.R
import kotlin.math.abs

class AudioWaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.app_primary_color)
        style = Paint.Style.FILL
    }

    private val unplayedBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.app_primary_color)
        alpha = 56
        style = Paint.Style.FILL
    }

    private val barCount = 48
    private val barHeights = FloatArray(barCount)
    private var isRecording = false
    private var showPlaybackProgress = false
    private var playbackProgress = 0f

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var isDraggingSeek = false
    private var suppressClick = false

    /** Fired while user drags (0f..1f). */
    var onSeekProgress: ((Float) -> Unit)? = null

    /** Fired when user finishes a drag seek (0f..1f). */
    var onSeekFinished: ((Float) -> Unit)? = null

    /** Fired on a tap (not drag). */
    var onWaveformClick: (() -> Unit)? = null

    init {
        isClickable = true
        resetIdleWaveform()
    }

    fun setAmplitude(amplitude: Int, recording: Boolean) {
        isRecording = recording
        if (!recording) {
            resetIdleWaveform()
            invalidate()
            return
        }

        val normalized = VoiceWaveformHelper.normalizeAmplitude(amplitude)
        for (index in barCount - 1 downTo 1) {
            barHeights[index] = barHeights[index - 1]
        }
        barHeights[0] = normalized
        invalidate()
    }

    fun setWaveform(samples: List<Float>) {
        isRecording = false
        if (samples.isEmpty()) {
            resetIdleWaveform()
        } else {
            val normalized = VoiceWaveformHelper.downsample(samples, barCount)
            for (index in 0 until barCount) {
                barHeights[index] = normalized[index]
            }
        }
        invalidate()
    }

    fun setPlaybackProgress(progress: Float) {
        if (isDraggingSeek) return
        val clamped = progress.coerceIn(0f, 1f)
        showPlaybackProgress = true
        if (clamped != playbackProgress) {
            playbackProgress = clamped
            invalidate()
        }
    }

    fun clearPlaybackProgress() {
        if (isDraggingSeek) return
        if (!showPlaybackProgress && playbackProgress == 0f) return
        showPlaybackProgress = false
        playbackProgress = 0f
        invalidate()
    }

    private fun resetIdleWaveform() {
        barHeights.fill(0.08f)
    }

    private fun fractionForX(x: Float): Float {
        val contentLeft = paddingLeft.toFloat()
        val contentWidth = (width - paddingLeft - paddingRight).toFloat().coerceAtLeast(1f)
        return ((x - contentLeft) / contentWidth).coerceIn(0f, 1f)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isRecording || !isEnabled) return super.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                isDraggingSeek = false
                suppressClick = false
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isDraggingSeek && abs(event.x - downX) > touchSlop) {
                    isDraggingSeek = true
                    suppressClick = true
                }
                if (isDraggingSeek) {
                    val fraction = fractionForX(event.x)
                    showPlaybackProgress = true
                    playbackProgress = fraction
                    invalidate()
                    onSeekProgress?.invoke(fraction)
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                if (isDraggingSeek) {
                    val fraction = fractionForX(event.x)
                    playbackProgress = fraction
                    invalidate()
                    onSeekFinished?.invoke(fraction)
                } else if (event.actionMasked == MotionEvent.ACTION_UP && !suppressClick) {
                    onWaveformClick?.invoke()
                }
                isDraggingSeek = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val contentLeft = paddingLeft.toFloat()
        val contentTop = paddingTop.toFloat()
        val contentRight = (width - paddingRight).toFloat()
        val contentBottom = (height - paddingBottom).toFloat()
        val contentWidth = contentRight - contentLeft
        val contentHeight = contentBottom - contentTop
        if (contentWidth <= 0f || contentHeight <= 0f) return

        val barWidth = contentWidth / (barCount * 2f)
        val gap = barWidth
        val totalBarWidth = barCount * (barWidth + gap) - gap
        var startX = contentLeft + (contentWidth - totalBarWidth) / 2f
        val cornerRadius = barWidth / 2f

        for (index in 0 until barCount) {
            val barHeight = barHeights[index] * contentHeight
            val top = contentTop + (contentHeight - barHeight) / 2f
            val rect = RectF(startX, top, startX + barWidth, top + barHeight)
            val barCenterProgress = (index + 0.5f) / barCount
            val paint = if (showPlaybackProgress && barCenterProgress > playbackProgress) {
                unplayedBarPaint
            } else {
                barPaint
            }
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
            startX += barWidth + gap
        }
    }
}
