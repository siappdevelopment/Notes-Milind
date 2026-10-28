package com.notebook.notes.checklist.ui.drawing

import android.widget.ImageView
import com.notebook.notes.checklist.databinding.ToolbarDrawingBottomBinding

class DrawingToolbarBinder(
    private val binding: ToolbarDrawingBottomBinding,
    private val onToolSelected: (DrawingTool) -> Unit
) {
    private val toolButtons: Map<DrawingTool, ImageView> by lazy {
        mapOf(
            DrawingTool.PEN to binding.btnPen,
            DrawingTool.DRAWING to binding.btnDrawing,
            DrawingTool.SHAPE to binding.btnShape,
            DrawingTool.STICKER to binding.btnSticker,
            DrawingTool.PHOTO to binding.btnGallery,
            DrawingTool.ERASER to binding.btnEraser
        )
    }

    fun bind() {
        toolButtons.forEach { (_, button) ->
            button.background = button.background.mutate()
        }

        binding.btnPen.setOnClickListener { onToolSelected(DrawingTool.PEN) }
        binding.btnDrawing.setOnClickListener { onToolSelected(DrawingTool.DRAWING) }
        binding.btnShape.setOnClickListener { onToolSelected(DrawingTool.SHAPE) }
        binding.btnSticker.setOnClickListener { onToolSelected(DrawingTool.STICKER) }
        binding.btnGallery.setOnClickListener { onToolSelected(DrawingTool.PHOTO) }
        binding.btnEraser.setOnClickListener { onToolSelected(DrawingTool.ERASER) }
    }

    fun updateSelection(selectedTool: DrawingTool?) {
        toolButtons.forEach { (tool, button) ->
            val selected = selectedTool != null && tool == selectedTool
            if (button.isSelected != selected) {
                button.isSelected = selected
            }
            button.refreshDrawableState()
            button.invalidate()
        }
    }
}
