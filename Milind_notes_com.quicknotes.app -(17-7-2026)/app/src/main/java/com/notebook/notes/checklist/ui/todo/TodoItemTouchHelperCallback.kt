package com.notebook.notes.checklist.ui.todo

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.notebook.notes.checklist.model.NoteContentItem

class TodoItemTouchHelperCallback(
    private val getItem: (Int) -> NoteContentItem?,
    private val onMove: (fromPosition: Int, toPosition: Int) -> Boolean,
    private val onDragFinished: () -> Unit
) : ItemTouchHelper.Callback() {

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = if (viewHolder.itemViewType == VIEW_TYPE_TODO) {
            ItemTouchHelper.UP or ItemTouchHelper.DOWN
        } else {
            0
        }
        return makeMovementFlags(dragFlags, 0)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        if (viewHolder.itemViewType != VIEW_TYPE_TODO ||
            target.itemViewType != VIEW_TYPE_TODO
        ) {
            return false
        }
        return onMove(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

    override fun isLongPressDragEnabled(): Boolean = false

    override fun canDropOver(
        recyclerView: RecyclerView,
        current: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = target.itemViewType == VIEW_TYPE_TODO

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        onDragFinished()
    }

    companion object {
        const val VIEW_TYPE_TODO = 1
    }
}
