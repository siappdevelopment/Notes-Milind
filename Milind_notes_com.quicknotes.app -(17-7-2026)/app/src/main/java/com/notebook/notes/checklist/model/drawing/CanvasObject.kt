package com.notebook.notes.checklist.model.drawing

import java.util.UUID

enum class CanvasObjectType {
    IMAGE,
    STICKER,
    EMOJI,
    TEXT,
    SHAPE
}

data class CanvasObject(
    val id: String = UUID.randomUUID().toString(),
    val type: CanvasObjectType,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float = 0f,
    val scale: Float = 1f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val zIndex: Int = 0,
    val imagePath: String? = null,
    val content: String? = null
) {
    val centerX: Float get() = x + width / 2f
    val centerY: Float get() = y + height / 2f
    val scaledWidth: Float get() = width * kotlin.math.abs(scaleX)
    val scaledHeight: Float get() = height * kotlin.math.abs(scaleY)
}
