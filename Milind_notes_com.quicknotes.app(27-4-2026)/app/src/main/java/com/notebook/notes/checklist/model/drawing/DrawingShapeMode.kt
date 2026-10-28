package com.notebook.notes.checklist.model.drawing

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.notebook.notes.checklist.R

enum class DrawingShapeMode(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int
) {
    FREEHAND(R.string.shape_freehand, R.drawable.ic_liner),
    LINE(R.string.shape_line, R.drawable.ic_straigh_liner),
    SQUARE(R.string.shape_rectangle, R.drawable.ic_sequer),
    CIRCLE(R.string.shape_circle, R.drawable.ic_circle),
    ARROW(R.string.shape_arrow, R.drawable.ic_arrow)
}
