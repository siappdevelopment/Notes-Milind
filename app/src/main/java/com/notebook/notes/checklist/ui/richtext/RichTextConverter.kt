package com.notebook.notes.checklist.ui.richtext

import android.graphics.Typeface
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.AlignmentSpan
import android.text.style.MetricAffectingSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import com.notebook.notes.checklist.model.RichTextDocument
import com.notebook.notes.checklist.model.RichTextSpan

object RichTextConverter {

  private const val CHECKBOX_UNCHECKED = "\u2610 "
  private const val CHECKBOX_CHECKED = "\u2611 "
  private const val BULLET_PREFIX = "\u2022 "

  fun toSpannable(document: RichTextDocument): SpannableStringBuilder {
    val builder = SpannableStringBuilder()
    mergeAdjacentSpans(document.spans).forEach { span ->
      val displayText = formatDisplayText(span)
      val start = builder.length
      builder.append(displayText)
      val end = builder.length
      if (start < end) {
        applySpanStyles(builder, span, start, end)
      }
    }
    applyAlignment(builder, document.alignment)
    return builder
  }

  fun fromSpannable(text: CharSequence): RichTextDocument {
    if (text.isEmpty()) return RichTextDocument()
    val spannable = if (text is Spannable) text else SpannableStringBuilder(text)
    val alignment = readAlignment(spannable)
    val spans = mutableListOf<RichTextSpan>()
    var index = 0
    while (index < spannable.length) {
      val style = readStyleAt(spannable, index)
      var end = index + 1
      while (end < spannable.length && stylesEqual(readStyleAt(spannable, end), style)) {
        end++
      }
      val raw = spannable.subSequence(index, end).toString()
      spans.add(parseDisplaySegment(raw, style))
      index = end
    }
    return RichTextDocument(
      spans = mergeAdjacentSpans(spans.ifEmpty { listOf(RichTextSpan()) }),
      alignment = alignment
    )
  }

  fun readStyleAt(spannable: Spannable, index: Int): RichTextSpan {
    val metaSpans = spannable.getSpans(index, index + 1, RichTextStyleSpan::class.java)
    val base = metaSpans.firstOrNull()?.metadata ?: RichTextSpan()
    val styleSpans = spannable.getSpans(index, index + 1, StyleSpan::class.java)
    val bold = base.bold || styleSpans.any { it.style == Typeface.BOLD || it.style == Typeface.BOLD_ITALIC }
    val italic = base.italic || styleSpans.any { it.style == Typeface.ITALIC || it.style == Typeface.BOLD_ITALIC }
    val underline = spannable.getSpans(index, index + 1, UnderlineSpan::class.java).isNotEmpty()
    val strikethrough = spannable.getSpans(index, index + 1, StrikethroughSpan::class.java).isNotEmpty()
    val link = spannable.getSpans(index, index + 1, URLSpan::class.java).firstOrNull()?.url
    val sizeSpan = spannable.getSpans(index, index + 1, AbsoluteSizeSpan::class.java).firstOrNull()
    val fontSize = sizeSpan?.size ?: base.fontSize
    return base.copy(
      fontSize = fontSize,
      bold = bold,
      italic = italic,
      underline = underline,
      strikethrough = strikethrough,
      linkUrl = link
    )
  }

  fun applySpanStyles(target: Spannable, span: RichTextSpan, start: Int, end: Int) {
    target.setSpan(RichTextStyleSpan(span.copy(text = "")), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    if (span.underline) target.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    if (span.strikethrough) target.setSpan(StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    target.setSpan(AbsoluteSizeSpan(span.fontSize, true), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    if (!span.linkUrl.isNullOrBlank()) {
      target.setSpan(URLSpan(span.linkUrl), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
  }

  /**
   * While typing, extend an adjacent run with the same style instead of creating
   * a new MetricAffectingSpan / size span for every character.
   */
  fun applyOrExtendTypingStyles(target: Spannable, span: RichTextSpan, start: Int, end: Int) {
    if (start >= end) return
    if (start > 0 && tryExtendTypingStyles(target, span, start, end)) return
    applySpanStyles(target, span, start, end)
  }

  private fun tryExtendTypingStyles(
    target: Spannable,
    span: RichTextSpan,
    start: Int,
    end: Int
  ): Boolean {
    val prevStyle = readStyleAt(target, start - 1)
    if (!stylesEqual(prevStyle, span)) return false

    val meta = target.getSpans(start - 1, start, RichTextStyleSpan::class.java)
      .firstOrNull { stylesEqual(it.metadata, span) && target.getSpanEnd(it) == start }
      ?: return false
    val size = target.getSpans(start - 1, start, AbsoluteSizeSpan::class.java)
      .firstOrNull { target.getSpanEnd(it) == start }
      ?: return false

    extendSpan(target, meta, target.getSpanStart(meta), end)
    extendSpan(target, size, target.getSpanStart(size), end)

    if (span.underline) {
      val underline = target.getSpans(start - 1, start, UnderlineSpan::class.java)
        .firstOrNull { target.getSpanEnd(it) == start }
      if (underline != null) extendSpan(target, underline, target.getSpanStart(underline), end)
      else target.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    if (span.strikethrough) {
      val strike = target.getSpans(start - 1, start, StrikethroughSpan::class.java)
        .firstOrNull { target.getSpanEnd(it) == start }
      if (strike != null) extendSpan(target, strike, target.getSpanStart(strike), end)
      else target.setSpan(StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    if (!span.linkUrl.isNullOrBlank()) {
      val link = target.getSpans(start - 1, start, URLSpan::class.java)
        .firstOrNull { it.url == span.linkUrl && target.getSpanEnd(it) == start }
      if (link != null) extendSpan(target, link, target.getSpanStart(link), end)
      else target.setSpan(URLSpan(span.linkUrl), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return true
  }

  private fun extendSpan(target: Spannable, span: Any, start: Int, end: Int) {
    if (start >= end) return
    val flags = target.getSpanFlags(span)
    target.removeSpan(span)
    target.setSpan(span, start, end, flags)
  }

  fun removeStyleSpansInRange(target: Spannable, start: Int, end: Int) {
    val spanTypes = arrayOf(
      RichTextStyleSpan::class.java,
      StyleSpan::class.java,
      UnderlineSpan::class.java,
      StrikethroughSpan::class.java,
      AbsoluteSizeSpan::class.java,
      URLSpan::class.java
    )
    spanTypes.forEach { type ->
      target.getSpans(start, end, type).forEach { span ->
        val spanStart = target.getSpanStart(span)
        val spanEnd = target.getSpanEnd(span)
        target.removeSpan(span)
        if (spanStart < start) {
          reapplySpan(target, span, spanStart, start)
        }
        if (spanEnd > end) {
          reapplySpan(target, span, end, spanEnd)
        }
      }
    }
  }

  /**
   * Transforms each style run inside [start, end) so mixed formatting
   * (e.g. partial bold) is preserved when changing color/highlight/size.
   */
  fun transformStylesInRange(
    target: Spannable,
    start: Int,
    end: Int,
    transform: (RichTextSpan) -> RichTextSpan
  ) {
    if (start >= end) return
    val runs = mutableListOf<Pair<Int, Pair<Int, RichTextSpan>>>()
    var index = start
    while (index < end) {
      val style = readStyleAt(target, index)
      var runEnd = index + 1
      while (runEnd < end && stylesEqual(readStyleAt(target, runEnd), style)) {
        runEnd++
      }
      runs.add(index to (runEnd to transform(style)))
      index = runEnd
    }
    removeStyleSpansInRange(target, start, end)
    runs.forEach { (runStart, endAndStyle) ->
      val (runEnd, style) = endAndStyle
      applySpanStyles(target, style, runStart, runEnd)
    }
  }

  private fun reapplySpan(target: Spannable, span: Any, start: Int, end: Int) {
    if (start >= end) return
    target.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
  }

  fun mergeAdjacentSpans(spans: List<RichTextSpan>): List<RichTextSpan> {
    if (spans.isEmpty()) return spans
    val merged = mutableListOf<RichTextSpan>()
    var current = spans.first()
    for (i in 1 until spans.size) {
      val next = spans[i]
      if (canMerge(current, next)) {
        current = current.copy(text = current.text + next.text)
      } else {
        merged.add(current)
        current = next
      }
    }
    merged.add(current)
    return merged
  }

  private fun canMerge(a: RichTextSpan, b: RichTextSpan): Boolean {
    return a.copy(text = "") == b.copy(text = "") &&
      a.isCheckbox == b.isCheckbox &&
      a.checked == b.checked &&
      a.isBullet == b.isBullet &&
      a.isNumbered == b.isNumbered &&
      a.numberIndex == b.numberIndex
  }

  private fun stylesEqual(a: RichTextSpan, b: RichTextSpan): Boolean =
    a.copy(text = "") == b.copy(text = "")

  private fun formatDisplayText(span: RichTextSpan): String {
    val body = span.text
    return when {
      span.isCheckbox -> if (span.checked) CHECKBOX_CHECKED + body else CHECKBOX_UNCHECKED + body
      span.isBullet -> BULLET_PREFIX + body
      span.isNumbered -> "${span.numberIndex}. $body"
      else -> body
    }
  }

  private fun parseDisplaySegment(raw: String, style: RichTextSpan): RichTextSpan {
    return when {
      raw.startsWith(CHECKBOX_CHECKED) -> style.copy(
        text = raw.removePrefix(CHECKBOX_CHECKED),
        isCheckbox = true,
        checked = true
      )
      raw.startsWith(CHECKBOX_UNCHECKED) -> style.copy(
        text = raw.removePrefix(CHECKBOX_UNCHECKED),
        isCheckbox = true,
        checked = false
      )
      raw.startsWith(BULLET_PREFIX) -> style.copy(
        text = raw.removePrefix(BULLET_PREFIX),
        isBullet = true
      )
      style.isNumbered -> {
        val dotIndex = raw.indexOf(". ")
        if (dotIndex > 0) {
          val number = raw.substring(0, dotIndex).toIntOrNull() ?: style.numberIndex
          style.copy(text = raw.substring(dotIndex + 2), numberIndex = number, isNumbered = true)
        } else style.copy(text = raw)
      }
      else -> style.copy(text = raw)
    }
  }

  fun applyAlignment(target: Spannable, alignment: String) {
    target.getSpans(0, target.length, AlignmentSpan::class.java).forEach { target.removeSpan(it) }
    if (target.isEmpty()) return
    val align = when (alignment) {
      RichTextDocument.ALIGN_CENTER -> AlignmentSpan.Standard(android.text.Layout.Alignment.ALIGN_CENTER)
      RichTextDocument.ALIGN_RIGHT -> AlignmentSpan.Standard(android.text.Layout.Alignment.ALIGN_OPPOSITE)
      RichTextDocument.ALIGN_JUSTIFY -> AlignmentSpan.Standard(android.text.Layout.Alignment.ALIGN_NORMAL)
      else -> AlignmentSpan.Standard(android.text.Layout.Alignment.ALIGN_NORMAL)
    }
    target.setSpan(align, 0, target.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
  }

  private fun applyAlignment(builder: SpannableStringBuilder, alignment: String) {
    applyAlignment(builder as Spannable, alignment)
  }

  private fun readAlignment(spannable: Spannable): String {
    val spans = spannable.getSpans(0, spannable.length, AlignmentSpan::class.java)
    val span = spans.firstOrNull() ?: return RichTextDocument.ALIGN_LEFT
    return when (span.alignment) {
      android.text.Layout.Alignment.ALIGN_CENTER -> RichTextDocument.ALIGN_CENTER
      android.text.Layout.Alignment.ALIGN_OPPOSITE -> RichTextDocument.ALIGN_RIGHT
      else -> RichTextDocument.ALIGN_LEFT
    }
  }
}

class RichTextStyleSpan(val metadata: RichTextSpan) : MetricAffectingSpan() {
  override fun updateDrawState(tp: TextPaint) {
    applyStyle(tp)
  }

  override fun updateMeasureState(tp: TextPaint) {
    applyStyle(tp)
  }

  private fun applyStyle(tp: TextPaint) {
    val colorHex = displayTextColor(metadata.textColor)
    parseColorCached(colorHex)?.let { tp.color = it }
    metadata.highlightColor?.let { hex ->
      parseColorCached(hex)?.let { tp.bgColor = it }
    }
    tp.typeface = RichTextFormats.resolveTypeface(metadata.fontFamily, metadata.bold, metadata.italic)
    tp.isUnderlineText = metadata.underline
    tp.isStrikeThruText = metadata.strikethrough
  }

  private fun displayTextColor(stored: String): String {
    val override = com.notebook.notes.checklist.util.NoteColorUtils.defaultTextColorOverrideHex
    return if (
      override != null &&
      com.notebook.notes.checklist.util.NoteColorUtils.isDefaultTextColor(stored)
    ) {
      override
    } else {
      stored.ifBlank { com.notebook.notes.checklist.util.NoteColorUtils.DEFAULT_TEXT_HEX }
    }
  }

  companion object {
    private val colorCache = HashMap<String, Int>(64)

    private fun parseColorCached(hex: String): Int? {
      colorCache[hex]?.let { return it }
      return runCatching { android.graphics.Color.parseColor(hex) }
        .getOrNull()
        ?.also { colorCache[hex] = it }
    }
  }
}
