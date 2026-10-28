package com.notebook.notes.checklist.ui.richtext

import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import com.notebook.notes.checklist.databinding.PanelFontStyleBinding
import com.notebook.notes.checklist.ui.adapter.FontStyleAdapter

class FontStylePanel(
    private val binding: PanelFontStyleBinding,
    private val onStateChanged: () -> Unit,
    private val onVisibilityChanged: (Boolean) -> Unit
) {
    private var boundController: RichTextEditorController? = null

    private val adapter = FontStyleAdapter { option ->
        val controller = boundController ?: return@FontStyleAdapter
        controller.setFontFamily(option.id)
        refresh()
        onStateChanged()
    }

    val isVisible: Boolean
        get() = binding.root.isVisible

    init {
        binding.rvFontStyles.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = this@FontStylePanel.adapter
            itemAnimator = null
        }
        binding.btnClose.setOnClickListener { hide() }
        binding.root.visibility = View.GONE
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
        val currentFamily = controller.getCurrentFontFamily()
        adapter.submitList(
            RichTextFormats.fontStyles.map { option ->
                FontStyleAdapter.StyleItem(
                    option = option,
                    isSelected = option.id == currentFamily
                )
            }
        )
    }
}
