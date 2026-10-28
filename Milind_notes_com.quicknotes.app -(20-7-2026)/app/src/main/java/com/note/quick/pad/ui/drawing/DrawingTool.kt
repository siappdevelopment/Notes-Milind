package com.note.quick.pad.ui.drawing

import com.note.quick.pad.model.drawing.ToolType

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

    val isDrawingTool: Boolean
        get() = this == PEN || this == DRAWING || this == ERASER
}
