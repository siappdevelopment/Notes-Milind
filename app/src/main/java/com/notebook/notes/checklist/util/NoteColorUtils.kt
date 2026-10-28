package com.notebook.notes.checklist.util

import android.graphics.Color
import androidx.core.graphics.ColorUtils

/**
 * Derives note foreground (ink) colors from the note background.
 *
 * Design reference: bg `#FFDAC7` → ink `#692F1A` (same hue family, darkened).
 * Only the background is persisted; generated ink colors are never stored.
 */
object NoteColorUtils {

    const val DEFAULT_TEXT_HEX = "#000000"
    const val LIGHT_TEXT_HEX = "#FFFFFF"

    /** Reference pair used to calibrate light-surface ink. */
    private const val REF_BG = 0xFFFFDAC7.toInt()
    private const val REF_INK = 0xFF692F1A.toInt()

    private const val LIGHT_INK_LIGHTNESS = 0.26f
    private const val DARK_SURFACE_LIGHTNESS = 0.45f

    /**
     * Primary ink for text and icons on [backgroundColor].
     * Light surfaces → dark same-hue ink; dark surfaces → near-white tinted ink.
     */
    fun inkColorFromBackground(backgroundColor: Int): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(backgroundColor, hsl)
        val luminance = ColorUtils.calculateLuminance(backgroundColor)

        return if (hsl[2] < DARK_SURFACE_LIGHTNESS || luminance < 0.42) {
            hsl[1] = (hsl[1] * 0.28f).coerceIn(0f, 0.35f)
            hsl[2] = 0.96f
            ColorUtils.HSLToColor(hsl)
        } else {
            // Match #FFDAC7 → #692F1A: keep hue, moderate saturation, dark lightness.
            hsl[1] = (hsl[1] * 1.15f).coerceIn(0.35f, 0.75f)
            hsl[2] = LIGHT_INK_LIGHTNESS
            ColorUtils.HSLToColor(hsl)
        }
    }

    fun mutedInkColor(backgroundColor: Int): Int {
        val ink = inkColorFromBackground(backgroundColor)
        return ColorUtils.setAlphaComponent(ink, 0xA6)
    }

    fun hintInkColor(backgroundColor: Int): Int {
        val ink = inkColorFromBackground(backgroundColor)
        return ColorUtils.setAlphaComponent(ink, 0x99)
    }

    fun dividerInkColor(backgroundColor: Int): Int {
        val ink = inkColorFromBackground(backgroundColor)
        return ColorUtils.setAlphaComponent(ink, 0x33)
    }

    fun toHex(color: Int): String = String.format("#%06X", 0xFFFFFF and color)

    /** @deprecated Use [inkColorFromBackground]. Kept for call-site compatibility. */
    fun readableTextColor(backgroundColor: Int): Int = inkColorFromBackground(backgroundColor)

    fun readableTextColorHex(backgroundColor: Int): String = toHex(inkColorFromBackground(backgroundColor))

    fun readableHintColor(backgroundColor: Int): Int = hintInkColor(backgroundColor)

    fun parseColorOrNull(value: String?): Int? {
        if (value.isNullOrBlank()) return null
        return runCatching { Color.parseColor(value.trim()) }.getOrNull()
    }

    /** True for pure / near-white fills that would disappear on a white home screen. */
    fun isTotallyWhite(color: Int): Boolean {
        val rgb = color and 0xFFFFFF
        if (rgb == 0xFFFFFF) return true
        return ColorUtils.calculateLuminance(color) >= 0.96
    }

    fun isDefaultTextColor(hex: String?): Boolean {
        if (hex.isNullOrBlank()) return true
        val normalized = hex.trim().uppercase()
        return normalized == DEFAULT_TEXT_HEX ||
            normalized == "#FF000000" ||
            normalized == "BLACK"
    }

    /**
     * Display-time override for default (#000000) rich-text spans.
     * Does not change stored document metadata.
     */
    @Volatile
    var defaultTextColorOverrideHex: String? = null

    /** Calibration helpers for tests / debugging. */
    fun referenceBackground(): Int = REF_BG
    fun referenceInk(): Int = REF_INK
}
