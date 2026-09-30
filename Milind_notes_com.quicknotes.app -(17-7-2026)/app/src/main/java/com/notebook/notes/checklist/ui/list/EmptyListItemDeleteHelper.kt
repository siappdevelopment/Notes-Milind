package com.notebook.notes.checklist.ui.list

import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.widget.EditText
import com.notebook.notes.checklist.R

object EmptyListItemDeleteHelper {

    fun attach(
        editText: EditText,
        isEmpty: () -> Boolean,
        onDeleteWhenEmpty: () -> Unit
    ) {
        attachSoftKeyboardDelete(editText, isEmpty, onDeleteWhenEmpty)
        attachHardwareBackspaceDelete(editText, isEmpty, onDeleteWhenEmpty)
    }

    fun detach(editText: EditText) {
        (editText.getTag(R.id.tag_empty_delete_watcher) as? TextWatcher)?.let {
            editText.removeTextChangedListener(it)
        }
        editText.setTag(R.id.tag_empty_delete_watcher, null)
        editText.setOnKeyListener(null)
    }

    fun attachSoftKeyboardDelete(
        editText: EditText,
        isEmpty: () -> Boolean,
        onDeleteWhenEmpty: () -> Unit
    ) {
        (editText.getTag(R.id.tag_empty_delete_watcher) as? TextWatcher)?.let {
            editText.removeTextChangedListener(it)
        }

        var deletePending = false
        val triggerDelete = {
            if (!deletePending && isEmpty()) {
                deletePending = true
                onDeleteWhenEmpty()
                editText.post { deletePending = false }
            }
        }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (!editText.hasFocus()) return
                if (s.isNullOrEmpty() && count == 0 && after == 0) {
                    triggerDelete()
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) = Unit
        }
        editText.setTag(R.id.tag_empty_delete_watcher, watcher)
        editText.addTextChangedListener(watcher)
    }

    fun attachHardwareBackspaceDelete(
        editText: EditText,
        isEmpty: () -> Boolean,
        onDeleteWhenEmpty: () -> Unit
    ) {
        var deletePending = false
        editText.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DEL &&
                event.action == KeyEvent.ACTION_DOWN &&
                isEmpty()
            ) {
                if (deletePending) return@setOnKeyListener true
                deletePending = true
                onDeleteWhenEmpty()
                editText.post { deletePending = false }
                true
            } else {
                false
            }
        }
    }
}
