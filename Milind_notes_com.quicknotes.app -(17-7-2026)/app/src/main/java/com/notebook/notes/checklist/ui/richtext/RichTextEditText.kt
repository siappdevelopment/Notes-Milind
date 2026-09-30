package com.notebook.notes.checklist.ui.richtext

import android.content.Context
import android.graphics.Rect
import android.text.Editable
import android.text.TextWatcher
import android.text.method.KeyListener
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.appcompat.widget.AppCompatEditText
import androidx.emoji2.viewsintegration.EmojiEditTextHelper
import com.notebook.notes.checklist.R

class RichTextEditText @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

  var controller: RichTextEditorController? = null
  var onFocusChangedListener: ((RichTextEditText, Boolean) -> Unit)? = null

  /**
   * When true, focus changes must not dismiss bottom panels / force the IME
   * (e.g. restoring caret after emoji insert while the emoji panel stays open).
   */
  var suppressTypingKeyboardRequest: Boolean = false

  private val centerTextVertically: Boolean
  private var applyingCenterPadding = false
  private var viewReady = false
  private val emojiEditTextHelper = EmojiEditTextHelper(this)

  private val endTypingBatchRunnable = Runnable {
    controller?.onTypingBatchEnded()
  }

  private val changeWatcher = object : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
      val ctrl = controller ?: return
      if (ctrl.isRestoringState) return
      if (count > 0 || after > 0) {
        ctrl.onTypingAboutToStart()
      }
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
      if (count > 0) {
        controller?.applyStyleToNewText(start, count)
      }
    }

    override fun afterTextChanged(s: Editable?) {
      removeCallbacks(endTypingBatchRunnable)
      postDelayed(endTypingBatchRunnable, TYPING_BATCH_MS)
      if (centerTextVertically) {
        updateVerticalCenterPadding()
      }
      notifyCaretMoved()
    }
  }

  private val caretMovedRunnable = Runnable {
    val ctrl = controller ?: return@Runnable
    (context as? RichTextEditorHost)?.onRichTextEditorCaretMoved(ctrl)
  }

  private fun notifyCaretMoved() {
    if (!viewReady || !hasFocus()) return
    removeCallbacks(caretMovedRunnable)
    // Coalesce caret scroll across keystrokes / taps so tall todos don't thrash.
    postDelayed(caretMovedRunnable, CARET_SCROLL_COALESCE_MS)
  }

  init {
    val typedArray = context.obtainStyledAttributes(attrs, R.styleable.RichTextEditText)
    centerTextVertically = typedArray.getBoolean(R.styleable.RichTextEditText_centerTextVertically, false)
    typedArray.recycle()

    addTextChangedListener(changeWatcher)
    isFocusableInTouchMode = true
    isFocusable = true
    includeFontPadding = false
    setHorizontallyScrolling(false)

    if (centerTextVertically) {
      gravity = Gravity.CENTER_VERTICAL or Gravity.START
    } else {
      // Prefer XML padding when provided (list rows); otherwise match text-block defaults.
      if (paddingTop == 0 && paddingBottom == 0) {
        setPadding(paddingLeft, 4, paddingRight, 4)
      }
      val minHeightFallback = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        40f,
        resources.displayMetrics
      ).toInt()
      if (minHeight < minHeightFallback) {
        minHeight = minHeightFallback
      }
      // Allow wrapping for all growing editors. List rows keep single-line
      // inputType so Enter still fires the IME "next item" action, but long
      // text wraps to new lines instead of scrolling/hiding.
      maxLines = Integer.MAX_VALUE
    }
    emojiEditTextHelper.setEnabled(true)
    viewReady = true
  }

  override fun setKeyListener(keyListener: KeyListener?) {
    super.setKeyListener(emojiEditTextHelper.getKeyListener(keyListener))
  }

  override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
    val connection = super.onCreateInputConnection(outAttrs)
    return emojiEditTextHelper.onCreateInputConnection(connection, outAttrs)
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    if (centerTextVertically) {
      updateVerticalCenterPadding()
    }
    // Only nudge scroll when this editor grew from wrapped text — not for every
    // parent padding/layout pass (those caused tall-todo focus/scroll thrash).
    if (h > oldh && hasFocus()) notifyCaretMoved()
  }

  override fun setTextSize(unit: Int, size: Float) {
    super.setTextSize(unit, size)
    if (centerTextVertically) {
      updateVerticalCenterPadding()
    }
  }

  /**
   * Centers single-line content inside a fixed-height editor.
   * Prefer the laid-out line height so AbsoluteSizeSpan / Heading styles
   * are measured correctly instead of the widget's base 16sp paint.
   */
  private fun updateVerticalCenterPadding() {
    if (!centerTextVertically || applyingCenterPadding) return
    val viewHeight = height
    if (viewHeight <= 0) return
    val layoutLineHeight = layout?.let { laidOut ->
      if (laidOut.lineCount > 0) laidOut.getLineBottom(0) - laidOut.getLineTop(0) else null
    }
    val paintLineHeight = paint.fontMetricsInt.let { it.descent - it.ascent }
    val lineHeight = (layoutLineHeight ?: paintLineHeight).coerceAtLeast(paintLineHeight)
    val verticalPad = ((viewHeight - lineHeight) / 2).coerceAtLeast(0)
    if (paddingTop == verticalPad && paddingBottom == verticalPad) return
    applyingCenterPadding = true
    setPadding(paddingLeft, verticalPad, paddingRight, verticalPad)
    applyingCenterPadding = false
  }

  override fun onSelectionChanged(selStart: Int, selEnd: Int) {
    super.onSelectionChanged(selStart, selEnd)
    // Called from the EditText super-constructor before this class is initialized.
    if (!viewReady) return
    if (selStart != selEnd) {
      controller?.rememberSelection(selStart, selEnd)
    } else if (hasFocus()) {
      // New caret position while editing — don't keep applying to an old range.
      controller?.clearRememberedSelection()
    }
    controller?.notifyFormatStateChanged()
    if (hasFocus()) notifyCaretMoved()
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    val handled = super.onTouchEvent(event)
    // Panel open + tap: dismiss panels then show IME (already-focused case).
    if (event.actionMasked == MotionEvent.ACTION_UP && isEnabled && hasFocus()) {
      requestTypingKeyboard()
    }
    return handled
  }

  override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
    if (viewReady && !focused) {
      // Capture selection before focus loss clears it (e.g. opening format panel).
      controller?.rememberSelection()
    }
    super.onFocusChanged(focused, direction, previouslyFocusedRect)
    if (!viewReady) return
    onFocusChangedListener?.invoke(this, focused)
    if (focused) {
      requestTypingKeyboard()
      notifyCaretMoved()
    } else {
      removeCallbacks(endTypingBatchRunnable)
      removeCallbacks(caretMovedRunnable)
      controller?.onTypingBatchEnded()
      controller?.flushContent()
    }
  }

  private fun requestTypingKeyboard() {
    if (suppressTypingKeyboardRequest) return
    val ctrl = controller ?: return
    val host = context as? RichTextEditorHost
    if (host != null) {
      host.onRichTextEditorTypingRequested(ctrl)
    } else {
      ctrl.showSoftInputIfNeeded()
    }
  }

  companion object {
    private const val TYPING_BATCH_MS = 800L
    private const val CARET_SCROLL_COALESCE_MS = 90L
  }
}
