package com.notebook.notes.checklist.ui.colorpicker

import android.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red

/**
 * Result returned by [AdvancedColorPickerDialogFragment].
 */
data class SelectedColor(
    val colorInt: Int,
    val alpha: Int,
    val hex: String,
    val red: Int,
    val green: Int,
    val blue: Int
) {
    companion object {
        fun fromColorInt(color: Int): SelectedColor {
            val a = color.alpha
            val r = color.red
            val g = color.green
            val b = color.blue
            return SelectedColor(
                colorInt = color,
                alpha = a,
                hex = toHex(color),
                red = r,
                green = g,
                blue = b
            )
        }

        fun fromRgba(red: Int, green: Int, blue: Int, alpha: Int): SelectedColor {
            val color = Color.argb(
                alpha.coerceIn(0, 255),
                red.coerceIn(0, 255),
                green.coerceIn(0, 255),
                blue.coerceIn(0, 255)
            )
            return fromColorInt(color)
        }

        fun fromHsv(hue: Float, saturation: Float, value: Float, alpha: Int): SelectedColor {
            val rgb = Color.HSVToColor(
                alpha.coerceIn(0, 255),
                floatArrayOf(
                    hue.coerceIn(0f, 360f),
                    saturation.coerceIn(0f, 1f),
                    value.coerceIn(0f, 1f)
                )
            )
            return fromColorInt(rgb)
        }

        fun parse(hexOrNull: String?, fallback: Int = Color.RED): SelectedColor {
            val parsed = hexOrNull
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { runCatching { Color.parseColor(it) }.getOrNull() }
            return fromColorInt(parsed ?: fallback)
        }

        fun toHex(color: Int): String {
            val a = color.alpha
            return if (a >= 255) {
                String.format("#%02X%02X%02X", color.red, color.green, color.blue)
            } else {
                String.format("#%02X%02X%02X%02X", a, color.red, color.green, color.blue)
            }
        }

        fun toRgbHex(color: Int): String =
            String.format("#%02X%02X%02X", color.red, color.green, color.blue)
    }
}

data class HsvColorState(
    val hue: Float = 0f,
    val saturation: Float = 1f,
    val value: Float = 1f,
    val alpha: Int = 255
) {
    fun toSelectedColor(): SelectedColor =
        SelectedColor.fromHsv(hue, saturation, value, alpha)

    fun withRgb(red: Int, green: Int, blue: Int): HsvColorState {
        val hsv = FloatArray(3)
        Color.colorToHSV(
            Color.rgb(red.coerceIn(0, 255), green.coerceIn(0, 255), blue.coerceIn(0, 255)),
            hsv
        )
        return copy(hue = hsv[0], saturation = hsv[1], value = hsv[2])
    }

    companion object {
        fun fromColorInt(color: Int): HsvColorState {
            val hsv = FloatArray(3)
            Color.colorToHSV(ColorUtils.setAlphaComponent(color, 255), hsv)
            return HsvColorState(
                hue = hsv[0],
                saturation = hsv[1],
                value = hsv[2],
                alpha = color.alpha
            )
        }
    }
}
