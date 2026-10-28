package com.notebook.notes.checklist.ui.richtext

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Typeface
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.FontRes
import androidx.core.content.res.ResourcesCompat
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.model.RichTextDocument
import com.notebook.notes.checklist.model.RichTextSpan
import com.notebook.notes.checklist.util.SearchHighlightHelper
import com.notebook.notes.checklist.util.SearchHighlightPalette
import kotlin.math.max
import kotlin.math.min

class RichTextEditorController(private val editText: EditText) {

  /** Exposed for host auto-scroll / insets helpers. */
  val editorView: EditText
    get() = editText

  var blockId: Long? = null
  private val undoManager = RichTextUndoManager()
  private var alignment: String = RichTextDocument.ALIGN_LEFT
  private var isRestoring = false
  private var typingBatchActive = false
  private var pendingFormatNotify = false
  var typingStyle: RichTextSpan = RichTextSpan()

  /** Survives focus loss when opening the formatting panel / hiding the keyboard. */
  private var rememberedSelectionStart: Int = -1
  private var rememberedSelectionEnd: Int = -1

  private var searchQuery: String = ""
  private var searchHighlightPalette: SearchHighlightPalette? = null
  private var isApplyingSearchHighlight = false

  private val searchHighlightWatcher = object : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
    override fun afterTextChanged(s: Editable?) {
      if (isRestoring || isApplyingSearchHighlight || searchQuery.isEmpty()) return
      refreshSearchHighlight()
    }
  }

  init {
    editText.addTextChangedListener(searchHighlightWatcher)
  }

  val isRestoringState: Boolean
    get() = isRestoring

  var onContentChanged: ((RichTextDocument) -> Unit)? = null
  var onFormatStateChanged: (() -> Unit)? = null
  /**
   * Fired with [onFormatStateChanged] but not cleared by the toolbar binder.
   * Used by list rows to resize bullet/number/checkbox chrome with font size.
   */
  var onStyleMetricsChanged: (() -> Unit)? = null

  fun notifyFormatStateChanged() {
    // Avoid toolbar/panel refresh on every keystroke — flush once the typing batch ends.
    if (typingBatchActive) {
      pendingFormatNotify = true
      return
    }
    pendingFormatNotify = false
    onFormatStateChanged?.invoke()
  }

  /** Prefix/checkbox chrome should only resize when font metrics actually change. */
  fun notifyStyleMetricsChanged() {
    onStyleMetricsChanged?.invoke()
  }

  fun rememberSelection(start: Int = editText.selectionStart, end: Int = editText.selectionEnd) {
    val length = editText.text?.length ?: 0
    val safeStart = min(start, end).coerceIn(0, length)
    val safeEnd = max(start, end).coerceIn(0, length)
    if (safeStart < safeEnd) {
      rememberedSelectionStart = safeStart
      rememberedSelectionEnd = safeEnd
    }
  }

  fun clearRememberedSelection() {
    rememberedSelectionStart = -1
    rememberedSelectionEnd = -1
  }

  /** Live selection if expanded; otherwise the last remembered range. */
  fun getActiveSelection(): Pair<Int, Int> {
    val length = editText.text?.length ?: 0
    val liveStart = min(editText.selectionStart, editText.selectionEnd).coerceAtLeast(0)
    val liveEnd = max(editText.selectionStart, editText.selectionEnd).coerceAtLeast(0)
    if (liveStart < liveEnd) {
      return liveStart.coerceAtMost(length) to liveEnd.coerceAtMost(length)
    }
    if (rememberedSelectionStart in 0 until rememberedSelectionEnd &&
      rememberedSelectionEnd <= length
    ) {
      return rememberedSelectionStart to rememberedSelectionEnd
    }
    return liveStart.coerceAtMost(length) to liveEnd.coerceAtMost(length)
  }

  fun onTypingAboutToStart() {
    if (isRestoring || typingBatchActive) return
    undoManager.push(captureSnapshot())
    typingBatchActive = true
  }

  fun onTypingBatchEnded() {
    typingBatchActive = false
    if (pendingFormatNotify) {
      notifyFormatStateChanged()
    }
  }

  fun getToolbarState(): RichTextToolbarState {
    val spannable = editText.text as? Spannable
    val (start, end) = getActiveSelection()
    val alignActive = alignment != RichTextDocument.ALIGN_LEFT

    if (spannable == null || spannable.isEmpty()) {
      return RichTextToolbarState(
        bold = typingStyle.bold,
        italic = typingStyle.italic,
        underline = typingStyle.underline,
        strikethrough = typingStyle.strikethrough,
        highlight = !typingStyle.highlightColor.isNullOrBlank(),
        alignActive = alignActive,
        canUndo = undoManager.canUndo(),
        canRedo = undoManager.canRedo()
      )
    }

    if (start >= end) {
      val index = min(start, spannable.length - 1).coerceAtLeast(0)
      val atCursor = RichTextConverter.readStyleAt(spannable, index)
      return RichTextToolbarState(
        bold = typingStyle.bold,
        italic = typingStyle.italic,
        underline = typingStyle.underline,
        strikethrough = typingStyle.strikethrough,
        highlight = !typingStyle.highlightColor.isNullOrBlank(),
        link = !atCursor.linkUrl.isNullOrBlank(),
        bullet = atCursor.isBullet,
        alignActive = alignActive,
        canUndo = undoManager.canUndo(),
        canRedo = undoManager.canRedo()
      )
    }

    return RichTextToolbarState(
      bold = allInRange(spannable, start, end) { it.bold },
      italic = allInRange(spannable, start, end) { it.italic },
      underline = allInRange(spannable, start, end) { it.underline },
      strikethrough = allInRange(spannable, start, end) { it.strikethrough },
      highlight = allInRange(spannable, start, end) { !it.highlightColor.isNullOrBlank() },
      link = allInRange(spannable, start, end) { !it.linkUrl.isNullOrBlank() },
      bullet = allInRange(spannable, start, end) { it.isBullet },
      alignActive = alignActive,
      canUndo = undoManager.canUndo(),
      canRedo = undoManager.canRedo()
    )
  }

  private fun allInRange(
    spannable: Spannable,
    start: Int,
    end: Int,
    predicate: (RichTextSpan) -> Boolean
  ): Boolean {
    for (index in start until end) {
      if (!predicate(RichTextConverter.readStyleAt(spannable, index))) return false
    }
    return true
  }

  fun loadDocument(document: RichTextDocument) {
    val spannable = RichTextConverter.toSpannable(document)
    val initialTypingStyle = document.spans.lastOrNull()?.copy(text = "") ?: RichTextSpan()
    applySnapshot(
      RichTextEditorSnapshot(
        document = document,
        typingStyle = initialTypingStyle,
        selectionStart = spannable.length,
        selectionEnd = spannable.length
      ),
      notify = false
    )
    undoManager.clear()
    refreshSearchHighlight()
    notifyFormatStateChanged()
    notifyStyleMetricsChanged()
  }

  fun setSearchHighlight(query: String, palette: SearchHighlightPalette?) {
    searchQuery = query.trim()
    searchHighlightPalette = palette
    refreshSearchHighlight()
  }

  fun applyAdaptivePaintColors(textColor: Int, hintColor: Int) {
    editText.setTextColor(textColor)
    editText.setHintTextColor(hintColor)
    editText.invalidate()
  }

  fun hasSearchMatch(): Boolean =
    SearchHighlightHelper.firstMatchIndex(editText.text, searchQuery) >= 0

  fun firstSearchMatchIndex(): Int =
    SearchHighlightHelper.firstMatchIndex(editText.text, searchQuery)

  fun searchMatchIndexFrom(fromIndex: Int): Int =
    SearchHighlightHelper.matchIndexFrom(editText.text, searchQuery, fromIndex)

  fun allSearchMatchIndices(): List<Int> =
    SearchHighlightHelper.allMatchIndices(editText.text, searchQuery)

  /**
   * Focuses the editor and places the caret/selection on a search match.
   * Uses [suppressTypingKeyboardRequest] so search navigation does not steal the IME
   * from the search field / force the formatting toolbar keyboard path.
   */
  fun focusSearchMatch(
    matchStart: Int = firstSearchMatchIndex(),
    showKeyboard: Boolean = false
  ): Boolean {
    if (matchStart < 0 || searchQuery.isEmpty()) return false
    val length = editText.text?.length ?: 0
    if (length == 0) return false
    val start = matchStart.coerceIn(0, length)
    val end = (start + searchQuery.length).coerceIn(start, length)
    val richEdit = editText as? RichTextEditText
    editText.isCursorVisible = true
    editText.isFocusable = true
    editText.isFocusableInTouchMode = true
    if (!showKeyboard) {
      richEdit?.suppressTypingKeyboardRequest = true
    }
    try {
      if (!editText.requestFocus()) {
        editText.requestFocusFromTouch()
      }
      editText.setSelection(start, end)
    } finally {
      if (!showKeyboard) {
        richEdit?.post { richEdit.suppressTypingKeyboardRequest = false }
      }
    }
    editText.post {
      val latestLength = editText.text?.length ?: 0
      val safeStart = start.coerceIn(0, latestLength)
      val safeEnd = end.coerceIn(safeStart, latestLength)
      if (!editText.hasFocus()) {
        editText.requestFocus()
      }
      editText.setSelection(safeStart, safeEnd)
      editText.bringPointIntoView(safeStart)
    }
    return true
  }

  private fun refreshSearchHighlight() {
    val spannable = editText.text as? Spannable ?: return
    val palette = searchHighlightPalette
    isApplyingSearchHighlight = true
    try {
      if (searchQuery.isEmpty() || palette == null) {
        SearchHighlightHelper.clearFromSpannable(spannable)
      } else {
        SearchHighlightHelper.applyToSpannable(spannable, searchQuery, palette)
      }
    } finally {
      isApplyingSearchHighlight = false
    }
  }

  fun getDocument(): RichTextDocument {
    val doc = RichTextConverter.fromSpannable(editText.text ?: "")
    return doc.copy(alignment = alignment)
  }

  fun flushContent() {
    onContentChanged?.invoke(getDocument())
  }

  private fun captureSnapshot(): RichTextEditorSnapshot {
    return RichTextEditorSnapshot(
      document = getDocument(),
      typingStyle = typingStyle.copy(),
      selectionStart = editText.selectionStart.coerceAtLeast(0),
      selectionEnd = editText.selectionEnd.coerceAtLeast(0)
    )
  }

  private fun commitFormattingUndo() {
    typingBatchActive = false
    pendingFormatNotify = false
    if (isRestoring) return
    undoManager.push(captureSnapshot())
  }

  fun undo(): Boolean {
    val previous = undoManager.undo(captureSnapshot()) ?: return false
    applySnapshot(previous, notify = false)
    notifyFormatStateChanged()
    notifyStyleMetricsChanged()
    return true
  }

  fun redo(): Boolean {
    val next = undoManager.redo(captureSnapshot()) ?: return false
    applySnapshot(next, notify = false)
    notifyFormatStateChanged()
    notifyStyleMetricsChanged()
    return true
  }

  fun toggleBold() = toggleStyleFlag(isEnabled = { it.bold }) { enabled -> copy(bold = enabled) }
  fun toggleItalic() = toggleStyleFlag(isEnabled = { it.italic }) { enabled -> copy(italic = enabled) }
  fun toggleUnderline() =
    toggleStyleFlag(isEnabled = { it.underline }) { enabled -> copy(underline = enabled) }
  fun toggleStrikethrough() =
    toggleStyleFlag(isEnabled = { it.strikethrough }) { enabled -> copy(strikethrough = enabled) }

  private fun toggleStyleFlag(
    isEnabled: (RichTextSpan) -> Boolean,
    apply: RichTextSpan.(Boolean) -> RichTextSpan
  ) {
    val (start, end) = getActiveSelection()
    val spannable = editText.text as? Spannable
    val enable = if (spannable != null && start < end) {
      !allInRange(spannable, start, end, isEnabled)
    } else {
      !isEnabled(typingStyle)
    }
    transformSelection { style -> style.apply(enable) }
  }

  fun setFontSize(size: Int) = transformSelection(metricsChanged = true) {
    it.copy(fontSize = size)
  }

  fun setFontFamily(family: String) = transformSelection(metricsChanged = true) {
    it.copy(fontFamily = family)
  }

  fun increaseFontSize() = transformSelection(metricsChanged = true) {
    it.copy(fontSize = RichTextFormats.nextFontSize(it.fontSize))
  }

  fun decreaseFontSize() = transformSelection(metricsChanged = true) {
    it.copy(fontSize = RichTextFormats.previousFontSize(it.fontSize))
  }

  fun getCurrentFontSize(): Int {
    val spannable = editText.text as? Spannable
    val (start, end) = getActiveSelection()

    if (spannable == null || spannable.isEmpty() || start >= end) {
      return typingStyle.fontSize.takeIf { it > 0 } ?: 16
    }

    val firstSize = RichTextConverter.readStyleAt(spannable, start).fontSize.takeIf { it > 0 } ?: 16
    for (index in start until end) {
      val size = RichTextConverter.readStyleAt(spannable, index).fontSize.takeIf { it > 0 } ?: 16
      if (size != firstSize) {
        return typingStyle.fontSize.takeIf { it > 0 } ?: 16
      }
    }
    return firstSize
  }

  fun getCurrentFontFamily(): String {
    val spannable = editText.text as? Spannable
    val (start, end) = getActiveSelection()

    if (spannable == null || spannable.isEmpty() || start >= end) {
      return typingStyle.fontFamily.ifBlank { "default" }
    }

    val firstFamily = RichTextConverter.readStyleAt(spannable, start).fontFamily.ifBlank { "default" }
    for (index in start until end) {
      val family = RichTextConverter.readStyleAt(spannable, index).fontFamily.ifBlank { "default" }
      if (family != firstFamily) {
        return typingStyle.fontFamily.ifBlank { "default" }
      }
    }
    return firstFamily
  }

  fun focusEditor() {
    restoreEditorFocus(showKeyboard = true)
  }

  /**
   * After formatting, keep selection and regain editor focus if lost.
   * Does not force the IME open — format panels may still be visible; typing
   * should dismiss panels via [RichTextEditorHost.onRichTextEditorTypingRequested].
   */
  fun ensureFocusAfterFormatting() {
    if (editText.hasFocus()) {
      restoreActiveSelection()
      return
    }
    restoreEditorFocus(showKeyboard = false)
  }

  /** Reopen IME after [EditorPanelAnimator.hideKeyboardAnimated] suppressed it. */
  fun showSoftInputIfNeeded() {
    if (!editText.hasFocus()) return
    showSoftInputForEditor()
  }

  private fun restoreActiveSelection() {
    val length = editText.text?.length ?: 0
    val (selStart, selEnd) = getActiveSelection()
    editText.setSelection(selStart.coerceIn(0, length), selEnd.coerceIn(0, length))
  }

  private fun restoreEditorFocus(showKeyboard: Boolean) {
    val length = editText.text?.length ?: 0
    val (selStart, selEnd) = getActiveSelection()
    val start = selStart.coerceIn(0, length)
    val end = selEnd.coerceIn(0, length)
    val richEdit = editText as? RichTextEditText

    editText.isCursorVisible = true
    editText.isFocusable = true
    editText.isFocusableInTouchMode = true
    if (!showKeyboard) {
      richEdit?.suppressTypingKeyboardRequest = true
    }
    try {
      if (!editText.requestFocus()) {
        editText.requestFocusFromTouch()
      }
      editText.setSelection(start, end)
    } finally {
      if (!showKeyboard) {
        richEdit?.post { richEdit.suppressTypingKeyboardRequest = false }
      }
    }

    if (!showKeyboard) return
    showSoftInputForEditor()
    editText.post {
      val latestLength = editText.text?.length ?: 0
      if (!editText.hasFocus()) {
        editText.requestFocus()
      }
      editText.setSelection(start.coerceIn(0, latestLength), end.coerceIn(0, latestLength))
      showSoftInputForEditor()
    }
  }

  private fun showSoftInputForEditor() {
    val activity = editText.context.findActivity()
    if (activity != null) {
      EditorPanelAnimator.showKeyboard(activity, editText)
      return
    }
    val imm = editText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
  }

  private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
      if (ctx is Activity) return ctx
      ctx = ctx.baseContext
    }
    return null
  }

  fun getCurrentAlignment(): String = alignment

  fun getCurrentTextColor(): String {
    val spannable = editText.text as? Spannable
    val (start, end) = getActiveSelection()

    if (spannable == null || spannable.isEmpty() || start >= end) {
      return typingStyle.textColor.ifBlank { "#000000" }
    }

    val firstColor = RichTextConverter.readStyleAt(spannable, start).textColor.ifBlank { "#000000" }
    for (index in start until end) {
      val color = RichTextConverter.readStyleAt(spannable, index).textColor.ifBlank { "#000000" }
      if (color != firstColor) return "#000000"
    }
    return firstColor
  }

  fun getCurrentHighlightColor(): String? {
    val spannable = editText.text as? Spannable
    val (start, end) = getActiveSelection()

    if (spannable == null || spannable.isEmpty() || start >= end) {
      return typingStyle.highlightColor.takeIf { !it.isNullOrBlank() }
    }

    val firstColor = RichTextConverter.readStyleAt(spannable, start).highlightColor
    for (index in start until end) {
      val color = RichTextConverter.readStyleAt(spannable, index).highlightColor
      if (color != firstColor) return null
    }
    return firstColor
  }

  fun setTextColor(color: String) = transformSelection { it.copy(textColor = color) }
  fun setHighlightColor(color: String?) = transformSelection { it.copy(highlightColor = color) }

  fun setAlignment(align: String) {
    commitFormattingUndo()
    alignment = align
    val editable = editText.text as? Spannable ?: return
    RichTextConverter.applyAlignment(editable, align)
    notifyFormatStateChanged()
    flushContent()
    ensureFocusAfterFormatting()
  }

  fun insertLink(displayText: String, url: String) {
    val cursor = editText.selectionStart.coerceAtLeast(0)
    val span = RichTextSpan(
      text = displayText.ifBlank { url },
      linkUrl = url,
      textColor = "#1976D2",
      underline = true
    )
    insertSpanAt(cursor, span)
  }

  fun getSelectedPlainText(): String {
    val text = editText.text?.toString().orEmpty()
    val (start, end) = getActiveSelection()
    if (start >= end) return ""
    return text.substring(start, end)
  }

  fun removeSelectedText() {
    val (start, end) = getActiveSelection()
    if (start >= end) return
    commitFormattingUndo()
    val editable = editText.text ?: return
    editable.delete(start, end)
    clearRememberedSelection()
    editText.setSelection(start)
    flushContent()
    notifyFormatStateChanged()
  }

  fun insertBulletItem() {
    insertSpanAt(editText.selectionStart.coerceAtLeast(0), RichTextSpan(text = "", isBullet = true))
  }

  fun insertEmoji(emoji: String) {
    if (emoji.isBlank()) return
    val cursor = editText.selectionStart.coerceAtLeast(0)
    val span = typingStyle.copy(text = emoji)
    insertSpanAt(cursor, span)
    // Keep caret/focus without forcing IME — emoji panel may still be open.
    if (!editText.hasFocus()) {
      restoreEditorFocus(showKeyboard = false)
    }
    flushContent()
  }

  fun applyStyleToNewText(start: Int, count: Int) {
    if (isRestoring || count <= 0) return
    val editable = editText.text as? Spannable ?: return
    val end = (start + count).coerceAtMost(editable.length)
    if (start >= end) return
    // Prefer extending adjacent same-style spans over allocating per keystroke.
    RichTextConverter.applyOrExtendTypingStyles(editable, typingStyle, start, end)
    if (alignment != RichTextDocument.ALIGN_LEFT) {
      RichTextConverter.applyAlignment(editable, alignment)
    }
    notifyFormatStateChanged()
  }

  private fun insertSpanAt(cursor: Int, insert: RichTextSpan) {
    commitFormattingUndo()
    val builder = RichTextConverter.toSpannable(getDocument())
    val insertText = RichTextConverter.toSpannable(RichTextDocument(listOf(insert)))
    val safeCursor = cursor.coerceIn(0, builder.length)
    builder.insert(safeCursor, insertText)
    val updated = RichTextConverter.fromSpannable(builder).copy(alignment = alignment)
    val newCursor = (safeCursor + insertText.length).coerceAtMost(builder.length)
    applySnapshot(
      RichTextEditorSnapshot(updated, typingStyle.copy(), newCursor, newCursor),
      notify = false
    )
    notifyFormatStateChanged()
  }

  private fun transformSelection(
    metricsChanged: Boolean = false,
    transform: (RichTextSpan) -> RichTextSpan
  ) {
    val (start, end) = getActiveSelection()

    if (start >= end) {
      commitFormattingUndo()
      typingStyle = transform(typingStyle)
      notifyFormatStateChanged()
      if (metricsChanged) notifyStyleMetricsChanged()
      ensureFocusAfterFormatting()
      return
    }

    commitFormattingUndo()
    val editable = editText.text as? Spannable ?: return
    RichTextConverter.transformStylesInRange(editable, start, end, transform)
    // Re-add search spans after the fresh style spans so the match text color stays on top.
    refreshSearchHighlight()
    rememberedSelectionStart = start
    rememberedSelectionEnd = end
    // Keep typing style in sync with the formatted selection for continued typing.
    typingStyle = RichTextConverter.readStyleAt(editable, start).copy(text = "")
    editText.invalidate()
    // Only remasure for font size / family — bold/italic/color must not relayout.
    if (metricsChanged) {
      editText.requestLayout()
    }
    flushContent()
    editText.post {
      ensureFocusAfterFormatting()
      notifyFormatStateChanged()
      if (metricsChanged) notifyStyleMetricsChanged()
    }
  }

  private fun applySnapshot(snapshot: RichTextEditorSnapshot, notify: Boolean) {
    isRestoring = true
    typingBatchActive = false
    pendingFormatNotify = false
    alignment = snapshot.document.alignment
    typingStyle = snapshot.typingStyle.copy()
    val spannable = RichTextConverter.toSpannable(snapshot.document)
    editText.setText(spannable)
    val safeStart = snapshot.selectionStart.coerceIn(0, spannable.length)
    val safeEnd = snapshot.selectionEnd.coerceIn(0, spannable.length)
    editText.setSelection(safeStart, safeEnd)
    isRestoring = false
    refreshSearchHighlight()
    if (notify) onContentChanged?.invoke(snapshot.document)
  }
}

object RichTextFormats {
  private var appContext: Context? = null
  private val typefaceCache = mutableMapOf<Int, Typeface>()

  val fontSizes = listOf(12, 14, 16, 18, 20, 24, 28, 32, 40)
  val fontStyles = listOf(
    FontStyleOption("default", "Normal"),
    FontStyleOption("abhaya_libre", "Abhaya Libre", R.font.abhaya),
    FontStyleOption("gilroy", "Gilroy", R.font.gilroy),
    FontStyleOption("corinthia", "Corinthia", R.font.corinthia),
    FontStyleOption("allura", "Allura", R.font.allura),
    FontStyleOption("oswald", "Oswald", R.font.oswald),
    FontStyleOption("federo", "Federo", R.font.federo),
    FontStyleOption("cinzel", "Cinzel", R.font.cinzel),
    FontStyleOption("teachers", "Teachers", R.font.teachers),
    FontStyleOption("josefin_sans", "Josefin Sans", R.font.josefin_sans),
    FontStyleOption("orbitron", "Orbitron", R.font.orbitron),
    FontStyleOption("style_script", "Style Script", R.font.style_script),
    FontStyleOption("dynapuff", "DynaPuff", R.font.dynapuff),
    FontStyleOption("lexend_tera", "Lexend Tera", R.font.lexend_tera)
  )
  val fontFamilies = fontStyles.map { it.id }
  val textColors = listOf(
    "#B2DFDB", "#BBDEFB", "#F8BBD0", "#D1C4E9", "#FFCCBC",
    "#C8E6C9", "#FF8A80", "#FFF9C4", "#FFAB91", "#9575CD", "#FFB74D",
    "#F06292", "#4CAF50", "#64B5F6", "#BA68C8", "#FFCC80"
  )
  val highlightColors = textColors

  private val fontResById: Map<String, Int> by lazy {
    fontStyles.mapNotNull { option -> option.fontResId?.let { option.id to it } }.toMap()
  }

  fun init(context: Context) {
    appContext = context.applicationContext
  }

  fun previewTypeface(familyId: String, context: Context? = null): Typeface =
    resolveTypeface(familyId, bold = false, italic = false, context)

  fun resolveTypeface(
    family: String,
    bold: Boolean,
    italic: Boolean,
    context: Context? = null
  ): Typeface {
    context?.let { if (appContext == null) init(it) }
    val customFont = loadFontFamily(family)
    if (customFont != null) {
      if (!bold && !italic) return customFont
      val style = when {
        bold && italic -> Typeface.BOLD_ITALIC
        bold -> Typeface.BOLD
        else -> Typeface.ITALIC
      }
      return Typeface.create(customFont, style)
    }
    val style = when {
      bold && italic -> Typeface.BOLD_ITALIC
      bold -> Typeface.BOLD
      italic -> Typeface.ITALIC
      else -> Typeface.NORMAL
    }
    return Typeface.create(Typeface.DEFAULT, style)
  }

  private fun loadFontFamily(familyId: String): Typeface? {
    if (familyId.isBlank() || familyId == "default") return null
    val resId = fontResById[familyId.lowercase()] ?: return null
    val context = appContext ?: return null
    typefaceCache[resId]?.let { return it }
    val loaded = runCatching { ResourcesCompat.getFont(context, resId) }.getOrNull() ?: return null
    typefaceCache[resId] = loaded
    return loaded
  }

  fun nextFontSize(current: Int): Int {
    val index = fontSizes.indexOfFirst { it > current }
    return fontSizes.getOrElse(if (index == -1) fontSizes.lastIndex else index) { fontSizes.last() }
  }

  fun previousFontSize(current: Int): Int {
    val index = fontSizes.indexOfLast { it < current }
    return fontSizes.getOrElse(if (index == -1) 0 else index) { fontSizes.first() }
  }
}

data class FontStyleOption(
  val id: String,
  val displayName: String,
  @FontRes val fontResId: Int? = null
)
