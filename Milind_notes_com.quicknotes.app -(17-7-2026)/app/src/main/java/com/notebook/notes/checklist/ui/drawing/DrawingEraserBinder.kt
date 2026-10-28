package com.notebook.notes.checklist.ui.drawing

import android.content.Context
import android.widget.SeekBar
import com.notebook.notes.checklist.databinding.PanelEraserOptionsBinding
import com.notebook.notes.checklist.viewmodel.DrawingUiState
import com.notebook.notes.checklist.viewmodel.DrawingViewModel

class DrawingEraserBinder(
    private val context: Context,
    private val binding: PanelEraserOptionsBinding,
    private val viewModel: DrawingViewModel,
    private val onClose: () -> Unit
) {
    private var isTrackingSlider = false

    fun bind() {
        binding.btnClose.setOnClickListener { onClose() }
        binding.btnEraserAll.setOnClickListener {
            viewModel.clearStrokes()
            onClose()
        }
        binding.seekEraserSize.max = 100
        setupSizeSlider()
    }

    fun render(state: DrawingUiState) {
        if (!isTrackingSlider) {
            binding.seekEraserSize.progress = DrawingViewModel.eraserSizeToProgress(state.eraserSizeDp)
        }
        updatePreview(state.eraserSizeDp)
    }

    private fun setupSizeSlider() {
        binding.seekEraserSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val sizeDp = DrawingViewModel.progressToEraserSize(progress)
                updatePreview(sizeDp)
                if (fromUser) {
                    viewModel.selectEraserSize(sizeDp)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isTrackingSlider = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isTrackingSlider = false
            }
        })
    }

    private fun updatePreview(sizeDp: Float) {
        val density = context.resources.displayMetrics.density
        val maxPreviewPx = (24f * density).toInt()
        val previewPx = (sizeDp * density * 0.65f).toInt()
            .coerceIn((4f * density).toInt(), maxPreviewPx)
        val params = binding.eraserPreview.layoutParams
        params.width = previewPx
        params.height = previewPx
        binding.eraserPreview.layoutParams = params
    }
}
