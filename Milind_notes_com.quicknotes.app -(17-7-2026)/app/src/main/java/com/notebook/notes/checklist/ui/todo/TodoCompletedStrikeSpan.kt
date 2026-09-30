package com.notebook.notes.checklist.ui.todo

import android.text.TextPaint
import android.text.style.MetricAffectingSpan

/** Visual-only strikethrough for completed todos; not persisted as rich-text formatting. */
class TodoCompletedStrikeSpan : MetricAffectingSpan() {
    override fun updateDrawState(textPaint: TextPaint) {
        textPaint.isStrikeThruText = true
    }

    override fun updateMeasureState(textPaint: TextPaint) {
        textPaint.isStrikeThruText = true
    }
}
