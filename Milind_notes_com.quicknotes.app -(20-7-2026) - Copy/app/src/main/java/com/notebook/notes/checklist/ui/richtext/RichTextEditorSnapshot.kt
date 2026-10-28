package com.notebook.notes.checklist.ui.richtext

import com.notebook.notes.checklist.model.RichTextDocument
import com.notebook.notes.checklist.model.RichTextSpan

data class RichTextEditorSnapshot(
  val document: RichTextDocument,
  val typingStyle: RichTextSpan = RichTextSpan(),
  val selectionStart: Int = 0,
  val selectionEnd: Int = 0
) {
  fun deepCopy(): RichTextEditorSnapshot = copy(
    document = document.copy(spans = document.spans.map { it.copy() }),
    typingStyle = typingStyle.copy()
  )
}

class RichTextUndoManager(private val maxSize: Int = 50) {
  private val undoStack = ArrayDeque<RichTextEditorSnapshot>()
  private val redoStack = ArrayDeque<RichTextEditorSnapshot>()

  fun push(snapshot: RichTextEditorSnapshot) {
    val copy = snapshot.deepCopy()
    if (undoStack.lastOrNull() == copy) return
    undoStack.addLast(copy)
    if (undoStack.size > maxSize) undoStack.removeFirst()
    redoStack.clear()
  }

  fun undo(current: RichTextEditorSnapshot): RichTextEditorSnapshot? {
    if (undoStack.isEmpty()) return null
    redoStack.addLast(current.deepCopy())
    return undoStack.removeLast()
  }

  fun redo(current: RichTextEditorSnapshot): RichTextEditorSnapshot? {
    if (redoStack.isEmpty()) return null
    undoStack.addLast(current.deepCopy())
    return redoStack.removeLast()
  }

  fun canUndo(): Boolean = undoStack.isNotEmpty()
  fun canRedo(): Boolean = redoStack.isNotEmpty()

  fun clear() {
    undoStack.clear()
    redoStack.clear()
  }
}
