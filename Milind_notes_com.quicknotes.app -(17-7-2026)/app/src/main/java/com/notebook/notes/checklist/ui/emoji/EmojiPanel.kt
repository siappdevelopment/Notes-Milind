package com.notebook.notes.checklist.ui.emoji

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.notebook.notes.checklist.databinding.BottomSheetEmojiBinding
import com.notebook.notes.checklist.ui.richtext.EditorPanelAnimator
import com.notebook.notes.checklist.ui.richtext.RichTextEditorController

/**
 * In-layout emoji picker (same shell as formatting panels) so RecyclerView
 * resizes via [bottomEditorPanel] constraints instead of a dialog overlay.
 */
class EmojiPanel(
    private val activity: AppCompatActivity,
    private val binding: BottomSheetEmojiBinding,
    private val onEmojiSelected: (String) -> Unit,
    private val onVisibilityChanged: (Boolean) -> Unit
) {
    private var boundController: RichTextEditorController? = null

    private val emojiAdapter = EmojiAdapter { item ->
        onEmojiSelected(item.emoji)
    }

    private val categoryAdapter = EmojiCategoryAdapter { category ->
        onCategorySelected?.invoke(category.id)
    }

    var onCategorySelected: ((String) -> Unit)? = null

    val isVisible: Boolean
        get() = binding.root.isVisible

    init {
        binding.rvEmoji.apply {
            layoutManager = GridLayoutManager(activity, GRID_SPAN_COUNT)
            adapter = emojiAdapter
            itemAnimator = null
            setHasFixedSize(true)
            isNestedScrollingEnabled = false
        }
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
            itemAnimator = null
        }
        categoryAdapter.submitList(EmojiCategory.displayCategories)
        binding.btnClose.setOnClickListener { hide(refocusEditor = true) }
        binding.root.visibility = View.GONE
        binding.root.isFocusable = false
        binding.root.isFocusableInTouchMode = false
    }

    fun render(state: EmojiUiState) {
        categoryAdapter.setSelectedCategory(state.selectedCategoryId)
        emojiAdapter.submitList(state.emojis)
    }

    fun show(controller: RichTextEditorController?, animate: Boolean = true) {
        boundController = controller
        EditorPanelAnimator.hideKeyboardThen(activity, binding.root) {
            if (boundController != controller) return@hideKeyboardThen
            EditorPanelAnimator.show(binding.root, animate = animate) {
                onVisibilityChanged(true)
            }
        }
    }

    fun hide(animate: Boolean = true, refocusEditor: Boolean = false) {
        if (!isVisible && boundController == null) return
        val controller = boundController
        EditorPanelAnimator.hide(binding.root, animate = animate) {
            onVisibilityChanged(false)
            boundController = null
            if (refocusEditor) controller?.focusEditor()
        }
    }

    companion object {
        private const val GRID_SPAN_COUNT = 8
    }
}
