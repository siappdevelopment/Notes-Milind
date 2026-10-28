package com.note.quick.pad.ui.richtext

interface RichTextEditorHost {
  fun onRichTextEditorFocused(controller: RichTextEditorController)
  fun onRichTextEditorBlurred() {}
  /** Editor was tapped / focused for typing — dismiss open panels then show the keyboard. */
  fun onRichTextEditorTypingRequested(controller: RichTextEditorController) {
    controller.focusEditor()
  }
  /** Caret moved or text grew — keep the active editor visible above keyboard/toolbar. */
  fun onRichTextEditorCaretMoved(controller: RichTextEditorController) {}
  fun onNumberListAction(controller: RichTextEditorController) {}
  fun onBulletListAction(controller: RichTextEditorController) {}
  fun onChecklistFromToolbar() {}
  fun onChecklistOption(option: ChecklistOption, controller: RichTextEditorController?) {}
  fun onDrawingFromToolbar() {}
  fun onBackgroundFromToolbar() {}
  fun onEmojiFromToolbar() {}
  fun onMoreToolAction(action: MoreToolAction) {}
}
