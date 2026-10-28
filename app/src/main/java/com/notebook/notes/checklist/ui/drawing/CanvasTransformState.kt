package com.notebook.notes.checklist.ui.drawing

import com.notebook.notes.checklist.model.drawing.CanvasObject

data class CanvasTransformState(
    val objects: List<CanvasObject> = emptyList(),
    val selectedObjectId: String? = null
) {
    val selectedObject: CanvasObject?
        get() = objects.firstOrNull { it.id == selectedObjectId }
}
