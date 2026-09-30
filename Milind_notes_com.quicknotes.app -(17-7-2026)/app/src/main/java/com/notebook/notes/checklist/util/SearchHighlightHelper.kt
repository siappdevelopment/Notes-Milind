package com.notebook.notes.checklist.util

import android.graphics.Color
import android.text.Spannable
import android.text.Spanned
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.UpdateAppearance
import androidx.core.graphics.ColorUtils

/** Highlight background plus a contrast-checked text color for search matches. */
data class SearchHighlightPalette(
    val backgroundColor: Int,
    val textColor: Int
)

/**
 * Temporary span used only for in-app search highlighting; never persisted as note formatting.
 * Paints both the match background and a readable text color so the match stays legible
 * regardless of the note ink color underneath (e.g. white ink on dark notes).
 */
class SearchMatchSpan(
    private val palette: SearchHighlightPalette
) : CharacterStyle(), UpdateAppearance {
    override fun updateDrawState(tp: TextPaint) {
        tp.bgColor = palette.backgroundColor
        tp.color = palette.textColor
    }
}

object SearchHighlightHelper {

    /** Strong ambers for light note surfaces, ordered from preferred to strongest. */
    private val LIGHT_SURFACE_HIGHLIGHTS = intArrayOf(
        0xFFFFD54F.toInt(),
        0xFFFFB300.toInt(),
        0xFFF57F17.toInt()
    )

    /** Soft yellows for dark note surfaces, ordered from preferred to lightest. */
    private val DARK_SURFACE_HIGHLIGHTS = intArrayOf(
        0xFFFFF59D.toInt(),
        0xFFFFF9C4.toInt()
    )

    private const val DARK_SURFACE_LUMINANCE = 0.4
    /** Minimum contrast between highlight pill and note surface so the pill itself is visible. */
    private const val MIN_SURFACE_DISTINCTION = 1.5

    private val paletteCache = HashMap<Int, SearchHighlightPalette>(16)

    /**
     * Derives WCAG-friendly highlight colors from the note background:
     * dark surfaces get a light yellow pill, light surfaces a stronger amber,
     * and the match text color is whichever of black/white contrasts most with the pill
     * (>= 4.5:1 for every candidate above).
     */
    fun paletteFor(noteBackgroundColor: Int): SearchHighlightPalette {
        val surface = ColorUtils.setAlphaComponent(noteBackgroundColor, 0xFF)
        paletteCache[surface]?.let { return it }

        val isDarkSurface = ColorUtils.calculateLuminance(surface) < DARK_SURFACE_LUMINANCE
        val candidates = if (isDarkSurface) DARK_SURFACE_HIGHLIGHTS else LIGHT_SURFACE_HIGHLIGHTS
        val highlight = candidates.firstOrNull { candidate ->
            ColorUtils.calculateContrast(candidate, surface) >= MIN_SURFACE_DISTINCTION
        } ?: candidates.last()

        val blackContrast = ColorUtils.calculateContrast(Color.BLACK, highlight)
        val whiteContrast = ColorUtils.calculateContrast(Color.WHITE, highlight)
        val textColor = if (blackContrast >= whiteContrast) Color.BLACK else Color.WHITE

        return SearchHighlightPalette(highlight, textColor).also { paletteCache[surface] = it }
    }

    fun highlight(
        text: CharSequence?,
        query: String?,
        palette: SearchHighlightPalette
    ): CharSequence {
        val source = text ?: return ""
        val trimmedQuery = query?.trim().orEmpty()
        if (trimmedQuery.isEmpty() || source.isEmpty()) return source

        val spannable = android.text.SpannableString(source)
        applyToSpannable(spannable, trimmedQuery, palette)
        return spannable
    }

    fun applyToSpannable(
        spannable: Spannable,
        query: String?,
        palette: SearchHighlightPalette
    ) {
        clearFromSpannable(spannable)
        val trimmedQuery = query?.trim().orEmpty()
        if (trimmedQuery.isEmpty() || spannable.isEmpty()) return

        val lowerSource = spannable.toString().lowercase()
        val lowerQuery = trimmedQuery.lowercase()
        var start = lowerSource.indexOf(lowerQuery)
        while (start >= 0) {
            val end = start + lowerQuery.length
            spannable.setSpan(
                SearchMatchSpan(palette),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            start = lowerSource.indexOf(lowerQuery, end)
        }
    }

    fun clearFromSpannable(spannable: Spannable) {
        spannable.getSpans(0, spannable.length, SearchMatchSpan::class.java)
            .forEach { spannable.removeSpan(it) }
    }

    fun firstMatchIndex(text: CharSequence?, query: String?): Int {
        val source = text?.toString().orEmpty()
        val trimmedQuery = query?.trim().orEmpty()
        if (trimmedQuery.isEmpty() || source.isEmpty()) return -1
        return source.lowercase().indexOf(trimmedQuery.lowercase())
    }

    /** First match at or after [fromIndex], or -1 if none. */
    fun matchIndexFrom(text: CharSequence?, query: String?, fromIndex: Int): Int {
        val source = text?.toString().orEmpty()
        val trimmedQuery = query?.trim().orEmpty()
        if (trimmedQuery.isEmpty() || source.isEmpty()) return -1
        val start = fromIndex.coerceIn(0, source.length)
        return source.lowercase().indexOf(trimmedQuery.lowercase(), start)
    }

    /** All match start indices in [text] for [query] (case-insensitive). */
    fun allMatchIndices(text: CharSequence?, query: String?): List<Int> {
        val source = text?.toString().orEmpty()
        val trimmedQuery = query?.trim().orEmpty()
        if (trimmedQuery.isEmpty() || source.isEmpty()) return emptyList()
        val lowerSource = source.lowercase()
        val lowerQuery = trimmedQuery.lowercase()
        val indices = mutableListOf<Int>()
        var start = lowerSource.indexOf(lowerQuery)
        while (start >= 0) {
            indices.add(start)
            start = lowerSource.indexOf(lowerQuery, start + lowerQuery.length)
        }
        return indices
    }
}
