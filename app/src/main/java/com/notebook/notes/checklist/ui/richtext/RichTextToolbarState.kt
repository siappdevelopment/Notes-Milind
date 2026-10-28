package com.notebook.notes.checklist.ui.richtext

import com.notebook.notes.checklist.model.RichTextDocument

data class RichTextToolbarState(
  val bold: Boolean = false,
  val italic: Boolean = false,
  val underline: Boolean = false,
  val strikethrough: Boolean = false,
  val highlight: Boolean = false,
  val link: Boolean = false,
  val bullet: Boolean = false,
  val numberList: Boolean = false,
  val alignActive: Boolean = false,
  val canUndo: Boolean = false,
  val canRedo: Boolean = false
)
