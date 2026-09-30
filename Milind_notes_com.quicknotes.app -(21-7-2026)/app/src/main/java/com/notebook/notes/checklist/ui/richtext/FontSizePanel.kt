package com.notebook.notes.checklist.ui.richtext

import android.view.View
import android.widget.SeekBar
import androidx.core.view.isVisible
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.PanelFontSizeBinding

class FontSizePanel(
    private val binding: PanelFontSizeBinding,
    private val onStateChanged: () -> Unit,
    private val onVisibilityChanged: (Boolean) -> Unit
) {
    private var boundController: RichTextEditorController? = null
    private var isUpdatingSlider = false

    val isVisible: Boolean
        get() = binding.root.isVisible

    init {
        val maxIndex = (RichTextFormats.fontSizes.size - 1).coerceAtLeast(0)
        binding.sliderFontSize.apply {
            max = maxIndex
            isFocusable = false
            isFocusableInTouchMode = false
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (!fromUser || isUpdatingSlider) return
                    applyFontSize(RichTextFormats.fontSizes[progress])
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        binding.btnClose.setOnClickListener { hide() }
        // Sizes are applied live as the slider moves; Done just dismisses the panel.
        binding.btnDone.setOnClickListener { hide() }
        binding.root.visibility = View.GONE
    }

    fun toggle(controller: RichTextEditorController) {
        if (isVisible && boundController == controller) {
            hide()
            return
        }
        show(controller)
    }

    fun show(controller: RichTextEditorController, animate: Boolean = true) {
        controller.rememberSelection()
        boundController = controller
        refresh()
        val activity = binding.root.context as? android.app.Activity ?: run {
            EditorPanelAnimator.show(binding.root, animate = animate) {
                onVisibilityChanged(true)
            }
            return
        }
        EditorPanelAnimator.hideKeyboardThen(activity, binding.root) {
            if (boundController != controller) return@hideKeyboardThen
            EditorPanelAnimator.show(binding.root, animate = animate) {
                onVisibilityChanged(true)
                refresh()
            }
        }
    }

    fun hide(animate: Boolean = true, refocusEditor: Boolean = true) {
        if (!isVisible && boundController == null) return
        val controller = boundController
        EditorPanelAnimator.hide(binding.root, animate = animate) {
            onVisibilityChanged(false)
            boundController = null
            if (refocusEditor) controller?.focusEditor()
        }
    }

    fun refresh() {
        val controller = boundController ?: return
        val currentSize = controller.getCurrentFontSize()
        val index = RichTextFormats.fontSizes
            .indexOfFirst { it == currentSize }
            .takeIf { it >= 0 }
            ?: RichTextFormats.fontSizes.indexOfFirst { it >= currentSize }.coerceAtLeast(0)

        isUpdatingSlider = true
        binding.sliderFontSize.progress = index
        isUpdatingSlider = false
        updateValueLabel(currentSize)
    }

    private fun applyFontSize(size: Int) {
        val controller = boundController ?: return
        controller.setFontSize(size)
        updateValueLabel(size)
        onStateChanged()
    }

    private fun updateValueLabel(size: Int) {
        binding.tvFontSizeValue.text =
            binding.root.context.getString(R.string.font_size_px, size)
    }
}
