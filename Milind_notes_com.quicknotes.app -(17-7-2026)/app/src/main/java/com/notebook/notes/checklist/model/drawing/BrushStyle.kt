package com.notebook.notes.checklist.model.drawing

import androidx.annotation.StringRes
import com.notebook.notes.checklist.R

enum class BrushStyle(
    @StringRes val labelRes: Int,
    val engineTool: ToolType,
    @androidx.annotation.DrawableRes val iconRes: Int,
    val minSizeDp: Float,
    val maxSizeDp: Float,
    val defaultSizeDp: Float
) {
    WATERCOLOR(
        R.string.brush_watercolor,
        ToolType.BRUSH,
        R.drawable.ic_drawing,
        minSizeDp = 6f,
        maxSizeDp = 36f,
        defaultSizeDp = 16f
    ),
    HIGHLIGHTER(
        R.string.highlighter,
        ToolType.MARKER,
        R.drawable.ic_highligher,
        minSizeDp = 10f,
        maxSizeDp = 48f,
        defaultSizeDp = 24f
    ),
    PENCIL(
        R.string.brush_pencil,
        ToolType.PEN,
        R.drawable.ic_pancil,
        minSizeDp = 2f,
        maxSizeDp = 14f,
        defaultSizeDp = 6f
    ),
    DOTTED(
        R.string.brush_dotted,
        ToolType.PEN,
        R.drawable.ic_dotted,
        minSizeDp = 3f,
        maxSizeDp = 18f,
        defaultSizeDp = 8f
    ),
    OUTLINE(
        R.string.brush_outline,
        ToolType.PEN,
        R.drawable.ic_outline,
        minSizeDp = 4f,
        maxSizeDp = 22f,
        defaultSizeDp = 10f
    );

    fun clampSize(sizeDp: Float): Float = sizeDp.coerceIn(minSizeDp, maxSizeDp)

    fun sliderProgressFor(sizeDp: Float): Int {
        return (clampSize(sizeDp) - minSizeDp).toInt().coerceIn(0, sliderMax)
    }

    fun sizeForSliderProgress(progress: Int): Float {
        return clampSize(minSizeDp + progress.coerceIn(0, sliderMax))
    }

    val sliderMax: Int
        get() = (maxSizeDp - minSizeDp).toInt().coerceAtLeast(1)

    companion object {
        fun defaultSizes(): Map<BrushStyle, Float> {
            return entries.associateWith { it.defaultSizeDp }
        }
    }
}
