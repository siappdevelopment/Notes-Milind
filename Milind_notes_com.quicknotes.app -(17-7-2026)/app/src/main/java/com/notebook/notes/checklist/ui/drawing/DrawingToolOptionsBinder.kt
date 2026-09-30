package com.notebook.notes.checklist.ui.drawing

import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.ItemBrushStyleTabBinding
import com.notebook.notes.checklist.databinding.PanelDrawingToolOptionsBinding
import com.notebook.notes.checklist.model.drawing.BrushStyle
import com.notebook.notes.checklist.model.drawing.DrawingShapeMode
import com.notebook.notes.checklist.viewmodel.DrawingUiState
import com.notebook.notes.checklist.viewmodel.DrawingViewModel

class DrawingToolOptionsBinder(
    private val context: Context,
    private val binding: PanelDrawingToolOptionsBinding,
    private val viewModel: DrawingViewModel,
    private val onClose: () -> Unit
) {
    private val colorAdapter = DrawingColorSwatchAdapter { color ->
        viewModel.selectToolOptionsColor(color)
    }

    private val shapeButtons: Map<DrawingShapeMode, ImageView> = mapOf(
        DrawingShapeMode.FREEHAND to binding.btnShapeFreehand,
        DrawingShapeMode.LINE to binding.btnShapeLine,
        DrawingShapeMode.SQUARE to binding.btnShapeSquare,
        DrawingShapeMode.CIRCLE to binding.btnShapeCircle,
        DrawingShapeMode.ARROW to binding.btnShapeArrow
    )

    private val brushTabBindings = mutableListOf<ItemBrushStyleTabBinding>()
    private var isTrackingBrushSize = false

    fun bind() {
        setupColorRecyclerView()
        setupBrushStyleTabs()
        setupShapeButtons()
        setupBrushSizeSlider()
        binding.btnClose.setOnClickListener { onClose() }
    }

    fun render(state: DrawingUiState) {
        val style = state.brushStyle
        val size = state.selectedBrushSize
        binding.tvTitle.setText(style.labelRes)
        binding.tvBrushSize.text = context.getString(R.string.brush_size_px, size.toInt())
        binding.seekBrushSize.max = style.sliderMax
        if (!isTrackingBrushSize) {
            binding.seekBrushSize.progress = style.sliderProgressFor(size)
                .coerceIn(0, style.sliderMax)
        }
        binding.strokePreview.strokeColor = state.selectedColor
        binding.strokePreview.strokeSizeDp = size
        binding.strokePreview.brushStyle = style
        colorAdapter.setSelectedColor(state.selectedColor)
        updateBrushStyleSelection(state.brushStyle)
        updateShapeSelection(state.shapeMode)
        binding.tvOutlineHint.isVisible = state.brushStyle == BrushStyle.OUTLINE
        scrollBrushTabIntoView(state.brushStyle)
    }

    private fun setupColorRecyclerView() {
        val spacingPx = (8 * context.resources.displayMetrics.density).toInt()
        binding.rvColors.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = colorAdapter
            itemAnimator = null
            if (itemDecorationCount == 0) {
                addItemDecoration(DrawingColorSpacingDecoration(spacingPx))
            }
        }
        colorAdapter.submitList(DrawingViewModel.TOOL_PALETTE_COLORS)
    }

    private fun setupBrushStyleTabs() {
        val container = binding.brushStyleTabs
        container.removeAllViews()
        brushTabBindings.clear()
        BrushStyle.entries.forEach { style ->
            val tabBinding = ItemBrushStyleTabBinding.inflate(
                LayoutInflater.from(context),
                container,
                false
            )
            tabBinding.ivBrushIcon.setImageResource(style.iconRes)
            tabBinding.tvBrushLabel.setText(style.labelRes)
            tabBinding.root.setOnClickListener {
                viewModel.selectBrushStyle(style)
            }
            brushTabBindings += tabBinding
            container.addView(tabBinding.root)
        }
    }

    private fun setupShapeButtons() {
        shapeButtons.forEach { (mode, button) ->
            button.setImageResource(mode.iconRes)
            button.setOnClickListener {
                viewModel.selectShapeMode(mode)
            }
        }
    }

    private fun setupBrushSizeSlider() {
        binding.seekBrushSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val style = viewModel.uiState.value.brushStyle
                    viewModel.selectToolOptionsSize(style.sizeForSliderProgress(progress))
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isTrackingBrushSize = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isTrackingBrushSize = false
            }
        })
    }

    private fun updateBrushStyleSelection(selectedStyle: BrushStyle) {
        brushTabBindings.forEachIndexed { index, tabBinding ->
            tabBinding.root.isSelected = BrushStyle.entries[index] == selectedStyle
        }
    }

    private fun updateShapeSelection(selectedMode: DrawingShapeMode) {
        shapeButtons.forEach { (mode, button) ->
            button.isSelected = mode == selectedMode
        }
    }

    private fun scrollBrushTabIntoView(selectedStyle: BrushStyle) {
        val index = BrushStyle.entries.indexOf(selectedStyle)
        val tabView = brushTabBindings.getOrNull(index)?.root ?: return
        binding.sectionBrushTypes.post {
            val scrollX = tabView.left - binding.sectionBrushTypes.paddingLeft
            binding.sectionBrushTypes.smoothScrollTo(scrollX.coerceAtLeast(0), 0)
        }
    }
}
