package com.notebook.notes.checklist.model.drawing

import android.graphics.PointF
import com.notebook.notes.checklist.model.drawing.BrushStyle
import com.notebook.notes.checklist.model.drawing.DrawingShapeMode

data class Stroke(
    val points: List<PointF>,
    val color: Int,
    val brushSize: Float,
    val toolType: ToolType,
    val brushStyle: BrushStyle = BrushStyle.PENCIL,
    val shapeMode: DrawingShapeMode = DrawingShapeMode.FREEHAND,
    /** Shares one ordering sequence with [com.notebook.notes.checklist.model.drawing.CanvasObject.zIndex]. */
    val zIndex: Int = 0
)
