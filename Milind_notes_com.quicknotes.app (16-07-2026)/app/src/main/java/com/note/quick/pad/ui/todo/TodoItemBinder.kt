package com.note.quick.pad.ui.todo

import android.text.Spannable
import android.text.Spanned
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.RecyclerView
import com.note.quick.pad.R
import com.note.quick.pad.databinding.ItemTodoBinding
import com.note.quick.pad.model.TodoItem
import com.note.quick.pad.ui.list.EmptyListItemDeleteHelper

class TodoItemBinder(
    private val callbacks: Callbacks
) {
    interface Callbacks {
        fun onItemToggled(itemId: Long)
        fun onItemDeleted(itemId: Long)
        fun onAddNextAfter(itemId: Long)
        fun onDeleteIfEmpty(itemId: Long)
        fun onStartDrag(viewHolder: RecyclerView.ViewHolder)
    }

    var pendingFocusItemId: Long? = null
    var isReadingMode: Boolean = false
    var adaptiveInkColor: Int? = null
    var adaptiveMutedColor: Int? = null

    fun bindActions(
        binding: ItemTodoBinding,
        item: TodoItem,
        viewHolder: RecyclerView.ViewHolder
    ) {
        setCompletedState(binding, item.isCompleted)

        binding.ivTodoCheck.isEnabled = !isReadingMode
        binding.ivTodoCheck.setOnClickListener {
            if (isReadingMode) return@setOnClickListener
            val newCompleted = !isItemCompleted(binding)
            setCompletedState(binding, newCompleted)
            applyCompletedStyle(binding, newCompleted)
            applyAdaptiveChrome(binding, newCompleted)
            callbacks.onItemToggled(item.id)
        }

        binding.ivDelete.isEnabled = !isReadingMode
        binding.ivDelete.setOnClickListener {
            if (isReadingMode) return@setOnClickListener
            callbacks.onItemDeleted(item.id)
        }

        binding.ivDragHandle.isEnabled = !isReadingMode
        binding.ivDragHandle.setOnLongClickListener {
            if (isReadingMode) return@setOnLongClickListener false
            callbacks.onStartDrag(viewHolder)
            true
        }

        val editText = binding.etTodoText
        editText.isFocusable = !isReadingMode
        editText.isFocusableInTouchMode = !isReadingMode
        editText.isCursorVisible = !isReadingMode

        editText.imeOptions = EditorInfo.IME_ACTION_NEXT
        EmptyListItemDeleteHelper.attachSoftKeyboardDelete(
            editText = editText,
            isEmpty = { editText.text.isNullOrBlank() },
            onDeleteWhenEmpty = { callbacks.onDeleteIfEmpty(item.id) }
        )
        editText.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
            when (keyCode) {
                KeyEvent.KEYCODE_DEL -> {
                    if (editText.text.isNullOrBlank()) {
                        callbacks.onDeleteIfEmpty(item.id)
                        true
                    } else {
                        false
                    }
                }
                KeyEvent.KEYCODE_ENTER -> {
                    callbacks.onAddNextAfter(item.id)
                    true
                }
                else -> false
            }
        }

        editText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                callbacks.onAddNextAfter(item.id)
                true
            } else {
                false
            }
        }

        applyCompletedStyle(binding, item.isCompleted)
        applyAdaptiveChrome(binding, item.isCompleted)

        if (item.id == pendingFocusItemId) {
            pendingFocusItemId = null
        }
    }

    fun isItemCompleted(binding: ItemTodoBinding): Boolean =
        binding.ivTodoCheck.tag as? Boolean ?: false

    private fun setCompletedState(binding: ItemTodoBinding, completed: Boolean) {
        binding.ivTodoCheck.tag = completed
        binding.ivTodoCheck.setImageResource(
            if (completed) R.drawable.ic_todo_checked else R.drawable.ic_todo_unchecked
        )
    }

    fun requestFocus(binding: ItemTodoBinding, onFocused: (() -> Unit)? = null) {
        val editText = binding.etTodoText
        editText.requestFocus()
        editText.setSelection(editText.text?.length ?: 0)
        onFocused?.invoke()
        val imm = binding.root.context.getSystemService(InputMethodManager::class.java)
        imm?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    fun applyCompletedStyle(binding: ItemTodoBinding, completed: Boolean) {
        val editText = binding.etTodoText
        val spannable = editText.text
        if (spannable is Spannable) {
            spannable.getSpans(0, spannable.length, TodoCompletedStrikeSpan::class.java)
                .forEach { spannable.removeSpan(it) }
            if (completed && spannable.isNotEmpty()) {
                spannable.setSpan(
                    TodoCompletedStrikeSpan(),
                    0,
                    spannable.length,
                    Spanned.SPAN_INCLUSIVE_INCLUSIVE
                )
            }
        }
        editText.alpha = 1f
        applyAdaptiveChrome(binding, completed)
    }

    fun applyAdaptiveChrome(binding: ItemTodoBinding, completed: Boolean) {
        val muted = adaptiveMutedColor
        val ink = adaptiveInkColor
        if (muted != null) {
            binding.ivDragHandle.setColorFilter(muted)
            binding.ivDelete.setColorFilter(muted)
        }
        if (ink == null) return
        if (completed) {
            binding.ivTodoCheck.clearColorFilter()
        } else {
            binding.ivTodoCheck.setColorFilter(ink)
        }
    }
}
