package com.notebook.notes.checklist.ui.colorpicker

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/**
 * Shared thumb drawing for hue / alpha sliders — large white ring,
 * soft shadow, and solid color preview (matches reference UI).
 */
internal class ColorSliderThumbStyle(resources: Resources) {

    val density = resources.displayMetrics.density

    /** Thin capsule track (~1/3–1/4 of thumb height). */
    val trackHeight = 10f * density

    /** Outer thumb radius including white ring. */
    val thumbRadius = 14f * density

    /** White ring thickness. */
    val ringStroke = 4.5f * density

    val trackCornerRadius = trackHeight / 2f

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#40000000")
        setShadowLayer(4f * density, 0f, 1.5f * density, Color.parseColor("#55000000"))
    }

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    fun draw(canvas: Canvas, cx: Float, cy: Float, previewColor: Int) {
        // Soft drop shadow under the white ring
        canvas.drawCircle(cx, cy + 0.5f * density, thumbRadius, shadowPaint)

        // White outer disc (creates the thick ring look)
        canvas.drawCircle(cx, cy, thumbRadius, ringPaint)

        // Inner color preview
        fillPaint.color = previewColor
        val innerRadius = thumbRadius - ringStroke
        canvas.drawCircle(cx, cy, innerRadius.coerceAtLeast(thumbRadius * 0.45f), fillPaint)
    }

    /** Vertical space needed so thumb + shadow are not clipped. */
    fun preferredHeight(): Int = (thumbRadius * 2f + 10f * density).toInt()
}
