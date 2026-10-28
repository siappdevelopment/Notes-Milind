package com.note.quick.pad.ui.todo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.note.quick.pad.databinding.ItemTodoBinding
import com.note.quick.pad.model.TodoItem

class TodoItemAdapter(
    callbacks: TodoItemBinder.Callbacks
) : ListAdapter<TodoItem, TodoItemAdapter.TodoViewHolder>(TodoDiffCallback()) {

    private val itemBinder = TodoItemBinder(callbacks)
    private var itemTouchHelper: ItemTouchHelper? = null
    private var dragInProgress = false
    var onDragFinished: (List<TodoItem>) -> Unit = {}

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val binding = ItemTodoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TodoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun attachDragHelper(recyclerView: RecyclerView) {
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION || from == to) {
                    return false
                }
                val updated = currentList.toMutableList()
                val moved = updated.removeAt(from)
                updated.add(to, moved)
                dragInProgress = true
                submitList(updated)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

            override fun isLongPressDragEnabled(): Boolean = false

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                if (dragInProgress) {
                    dragInProgress = false
                    onDragFinished(currentList)
                }
            }
        }
        itemTouchHelper = ItemTouchHelper(callback).also { it.attachToRecyclerView(recyclerView) }
    }

    fun focusItem(recyclerView: RecyclerView, itemId: Long) {
        itemBinder.pendingFocusItemId = itemId
        val index = currentList.indexOfFirst { it.id == itemId }
        if (index < 0) return
        recyclerView.scrollToPosition(index)
        recyclerView.post {
            (recyclerView.findViewHolderForAdapterPosition(index) as? TodoViewHolder)?.requestFocus()
        }
    }

    inner class TodoViewHolder(
        private val binding: ItemTodoBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TodoItem) = itemBinder.bindActions(binding, item, this)
        fun requestFocus() = itemBinder.requestFocus(binding)
    }

    private class TodoDiffCallback : DiffUtil.ItemCallback<TodoItem>() {
        override fun areItemsTheSame(oldItem: TodoItem, newItem: TodoItem) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: TodoItem, newItem: TodoItem) =
            oldItem.isCompleted == newItem.isCompleted &&
                oldItem.position == newItem.position &&
                oldItem.text == newItem.text
    }
}
