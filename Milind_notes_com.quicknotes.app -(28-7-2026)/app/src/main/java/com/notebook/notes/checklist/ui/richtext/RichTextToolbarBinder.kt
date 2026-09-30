package com.notebook.notes.checklist.ui.richtext

import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.ToolbarRichTextBinding
import com.notebook.notes.checklist.ui.background.NoteBackgroundPanel
import com.notebook.notes.checklist.ui.emoji.EmojiPanel

class RichTextToolbarBinder(
  private val activity: AppCompatActivity,
  private val binding: ToolbarRichTextBinding,
  private val textFormattingPanel: TextFormattingPanel,
  private val fontSizePanel: FontSizePanel,
  private val fontStylePanel: FontStylePanel,
  private val checklistPanel: ChecklistPanel,
  private val emojiPanel: EmojiPanel,
  private val noteBackgroundPanel: NoteBackgroundPanel,
  private val bottomEditorPanel: View,
  private val selectedBackgroundKey: () -> String?,
  private val isNumberListModeEnabled: () -> Boolean = { false },
  private val isBulletListModeEnabled: () -> Boolean = { false }
) {
  private var activeController: RichTextEditorController? = null
  /** Survives the brief blur that happens before a toolbar button click is delivered. */
  private var lastController: RichTextEditorController? = null
  var suppressEditorBlurHandling: Boolean = false

  private val toolbarToggleButtons: List<ImageView> by lazy {
    listOf(
      binding.btnfontFormating,
      binding.btnFontFamily,
      binding.btnTextStyle,
      binding.btnUndo,
      binding.btnRedo,
      binding.btnChecklist,
      binding.btnEmoji,
      binding.btnInsertBackground
    )
  }

  fun getActiveController(): RichTextEditorController? = activeController ?: lastController

  fun bind() {
    toolbarToggleButtons.forEach { button ->
      button.background = button.background.mutate()
      // ACTION_DOWN runs before the editor loses focus, so we can keep the
      // active controller (critical for todo / list rows opening font style).
      button.setOnTouchListener { _, event ->
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
          prepareForToolbarInteraction()
        }
        false
      }
    }

    binding.btnfontFormating.setOnClickListener { toggleTextFormattingPanel() }
    binding.btnFontFamily.setOnClickListener { toggleFontSizePanel() }
    binding.btnTextStyle.setOnClickListener { toggleFontStylePanel() }
    binding.btnUndo.setOnClickListener {
      if (resolveController()?.undo() == true) {
        refreshToolbarState()
      }
    }
    binding.btnRedo.setOnClickListener {
      if (resolveController()?.redo() == true) {
        refreshToolbarState()
      }
    }
    binding.btnChecklist.setOnClickListener { toggleChecklistPanel() }
    binding.btnEmoji.setOnClickListener { toggleEmojiPanel() }
    binding.btnInsertBackground.setOnClickListener { toggleNoteBackgroundPanel() }
  }

  private fun prepareForToolbarInteraction() {
    suppressEditorBlurHandling = true
    (activeController ?: lastController)?.rememberSelection()
  }

  private fun resolveController(): RichTextEditorController? =
    activeController ?: lastController

  fun setActiveController(controller: RichTextEditorController?) {
    activeController?.onFormatStateChanged = null
    activeController = controller
    if (controller != null) {
      lastController = controller
      controller.onFormatStateChanged = { refreshToolbarState() }
      binding.root.visibility = View.VISIBLE
      refreshToolbarState()
      return
    }
    hideAllPanels(animate = true)
    binding.root.visibility = View.INVISIBLE
    refreshToolbarState()
    syncBottomEditorPanelState()
  }

  fun refreshToolbarState() {
    val controller = resolveController()
    if (controller == null || (activeController == null && !isAnyPanelVisible())) {
      binding.btnUndo.isEnabled = false
      binding.btnRedo.isEnabled = false
      toolbarToggleButtons.forEach { it.isSelected = false }
      if (checklistPanel.isVisible) binding.btnChecklist.isSelected = true
      if (emojiPanel.isVisible) binding.btnEmoji.isSelected = true
      if (noteBackgroundPanel.isVisible) binding.btnInsertBackground.isSelected = true
      syncBottomEditorPanelState()
      return
    }
    val state = controller.getToolbarState()
    binding.btnUndo.isEnabled = state.canUndo
    binding.btnRedo.isEnabled = state.canRedo

    toolbarToggleButtons.forEach { it.isSelected = false }

    when {
      textFormattingPanel.isVisible -> binding.btnfontFormating.isSelected = true
      fontSizePanel.isVisible -> binding.btnFontFamily.isSelected = true
      fontStylePanel.isVisible -> binding.btnTextStyle.isSelected = true
      checklistPanel.isVisible -> binding.btnChecklist.isSelected = true
      emojiPanel.isVisible -> binding.btnEmoji.isSelected = true
      noteBackgroundPanel.isVisible -> binding.btnInsertBackground.isSelected = true
    }

    if (fontSizePanel.isVisible) fontSizePanel.refresh()
    if (fontStylePanel.isVisible) fontStylePanel.refresh()
    syncBottomEditorPanelState()
  }

  fun syncBottomEditorPanelState() {
    val anyPanelVisible = isAnyPanelVisible()
    suppressEditorBlurHandling = anyPanelVisible
    bottomEditorPanel.isSelected =
      binding.root.visibility == View.VISIBLE && anyPanelVisible
  }

  fun isAnyPanelVisible(): Boolean =
    textFormattingPanel.isVisible ||
      fontSizePanel.isVisible ||
      fontStylePanel.isVisible ||
      checklistPanel.isVisible ||
      emojiPanel.isVisible ||
      noteBackgroundPanel.isVisible

  /**
   * Close formatting/checklist/emoji/background panels before showing the IME.
   * Leaving a panel open while the keyboard opens causes the keyboard to bounce closed.
   */
  fun dismissOpenPanelsThenShowKeyboard(controller: RichTextEditorController) {
    if (isAnyPanelVisible()) {
      hideAllPanels(animate = false)
      refreshToolbarState()
      syncBottomEditorPanelState()
    }
    controller.focusEditor()
  }

  private fun hideAllPanels(animate: Boolean = false) {
    // Instant when switching panels so only the newly opened panel animates in.
    textFormattingPanel.hide(animate = animate, refocusEditor = false)
    fontSizePanel.hide(animate = animate, refocusEditor = false)
    fontStylePanel.hide(animate = animate, refocusEditor = false)
    checklistPanel.hide(animate = animate)
    emojiPanel.hide(animate = animate, refocusEditor = false)
    noteBackgroundPanel.hide(animate = animate)
  }

  private fun toggleNoteBackgroundPanel() {
    if (noteBackgroundPanel.isVisible) {
      noteBackgroundPanel.hide(animate = true)
      refreshToolbarState()
    } else {
      hideAllPanels(animate = false)
      suppressEditorBlurHandling = true
      noteBackgroundPanel.show(selectedBackgroundKey(), animate = true)
      binding.btnInsertBackground.isSelected = true
    }
  }

  private fun toggleChecklistPanel() {
    if (checklistPanel.isVisible) {
      checklistPanel.hide(animate = true)
      refreshToolbarState()
    } else {
      hideAllPanels(animate = false)
      suppressEditorBlurHandling = true
      checklistPanel.show(resolveController(), animate = true)
      binding.btnChecklist.isSelected = true
    }
  }

  private fun toggleEmojiPanel() {
    if (emojiPanel.isVisible) {
      emojiPanel.hide(animate = true, refocusEditor = true)
      refreshToolbarState()
    } else {
      resolveController()?.rememberSelection()
      hideAllPanels(animate = false)
      suppressEditorBlurHandling = true
      emojiPanel.show(resolveController(), animate = true)
      binding.btnEmoji.isSelected = true
    }
  }

  private fun toggleTextFormattingPanel() {
    val controller = resolveController() ?: return
    controller.rememberSelection()
    if (textFormattingPanel.isVisible) {
      textFormattingPanel.hide(animate = true, refocusEditor = true)
      refreshToolbarState()
    } else {
      hideAllPanels(animate = false)
      suppressEditorBlurHandling = true
      activeController = controller
      textFormattingPanel.show(controller, activity as? RichTextEditorHost, animate = true)
      binding.btnfontFormating.isSelected = true
    }
  }

  private fun toggleFontSizePanel() {
    val controller = resolveController() ?: return
    controller.rememberSelection()
    if (fontSizePanel.isVisible) {
      fontSizePanel.hide(animate = true, refocusEditor = true)
      refreshToolbarState()
    } else {
      hideAllPanels(animate = false)
      suppressEditorBlurHandling = true
      activeController = controller
      fontSizePanel.show(controller, animate = true)
      binding.btnFontFamily.isSelected = true
    }
  }

  private fun toggleFontStylePanel() {
    val controller = resolveController() ?: return
    controller.rememberSelection()
    if (fontStylePanel.isVisible) {
      fontStylePanel.hide(animate = true, refocusEditor = true)
      refreshToolbarState()
    } else {
      hideAllPanels(animate = false)
      suppressEditorBlurHandling = true
      activeController = controller
      fontStylePanel.show(controller, animate = true)
      binding.btnTextStyle.isSelected = true
    }
  }

  private fun showMoreToolsSheet() {
    RichTextBottomSheets.showMoreToolsSheet(
      activity = activity,
      isNumberListModeEnabled = isNumberListModeEnabled(),
      isBulletListModeEnabled = isBulletListModeEnabled()
    ) { action ->
      val host = activity as? RichTextEditorHost
      if (host != null) {
        host.onMoreToolAction(action)
        return@showMoreToolsSheet
      }
      Toast.makeText(activity, R.string.more_coming_soon, Toast.LENGTH_SHORT).show()
    }
  }
}
