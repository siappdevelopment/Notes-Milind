package com.notebook.notes.checklist.ui.drawing

import android.graphics.Color
import com.notebook.notes.checklist.model.drawing.BrushStyle
import com.notebook.notes.checklist.model.drawing.DrawingShapeMode

data class DrawingToolSettingsState(
    val brushStyle: BrushStyle = BrushStyle.WATERCOLOR,
    val selectedColor: Int = Color.parseColor("#9C27B0"),
    val brushSizeDp: Float = 20f,
    val shapeMode: DrawingShapeMode = DrawingShapeMode.FREEHAND
)
