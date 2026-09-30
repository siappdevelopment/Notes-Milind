package com.note.quick.pad.ui.drawing

import com.note.quick.pad.model.drawing.CanvasObject

data class CanvasTransformState(
    val objects: List<CanvasObject> = emptyList(),
    val selectedObjectId: String? = null
) {
    val selectedObject: CanvasObject?
        get() = objects.firstOrNull { it.id == selectedObjectId }
}
