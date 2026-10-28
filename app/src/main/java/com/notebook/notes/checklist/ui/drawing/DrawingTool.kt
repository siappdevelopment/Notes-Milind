package com.notebook.notes.checklist.ui.drawing

import com.notebook.notes.checklist.model.drawing.ToolType

enum class DrawingTool {
    PEN,
    DRAWING,
    SHAPE,
    STICKER,
    PHOTO,
    ERASER;

    fun toEngineTool(): ToolType? = when (this) {
        PEN -> ToolType.PEN
        DRAWING -> null
        ERASER -> ToolType.ERASER
        SHAPE, STICKER, PHOTO -> null
    }

    /** True for tools that draw on the canvas (brush / eraser). PEN opens the doodle panel. */
    val isDrawingTool: Boolean
        get() = this == DRAWING || this == ERASER
}
