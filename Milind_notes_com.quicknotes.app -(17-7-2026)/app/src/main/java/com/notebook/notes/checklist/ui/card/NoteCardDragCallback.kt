package com.notebook.notes.checklist.ui.card

import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.notebook.notes.checklist.R

class NoteCardDragCallback(
    private val adapter: NoteCardAdapter,
    private val onDragFinished: () -> Unit
) : ItemTouchHelper.SimpleCallback(
    ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
    0
) {

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        if (!adapter.isDragReorderEnabled() ||
            viewHolder.itemViewType == NoteCardAdapter.VIEW_TYPE_SECTION
        ) {
            return 0
        }
        // In selection mode only the single selected card may be dragged.
        if (adapter.isInSelectionMode()) {
            val item = adapter.getListItem(viewHolder.bindingAdapterPosition) as? HomeListItem.Card
                ?: return 0
            if (!adapter.isNoteSelected(item.model.noteId)) return 0
        }
        return makeMovementFlags(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
            0
        )
    }

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
        return adapter.moveItem(from, to)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

    override fun isLongPressDragEnabled(): Boolean = false

    override fun canDropOver(
        recyclerView: RecyclerView,
        current: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        if (current.itemViewType == NoteCardAdapter.VIEW_TYPE_SECTION ||
            target.itemViewType == NoteCardAdapter.VIEW_TYPE_SECTION
        ) {
            return false
        }
        val fromItem = adapter.getListItem(current.bindingAdapterPosition) as? HomeListItem.Card
            ?: return false
        val toItem = adapter.getListItem(target.bindingAdapterPosition) as? HomeListItem.Card
            ?: return false
        return fromItem.model.isPinned == toItem.model.isPinned
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        val itemView = viewHolder?.itemView ?: return
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            val density = itemView.resources.displayMetrics.density
            val card = itemView as? CardView
            itemView.setTag(R.id.cardRoot, card?.cardElevation ?: itemView.elevation)
            itemView.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .alpha(0.92f)
                .setDuration(150)
                .start()
            if (card != null) {
                card.cardElevation = 8f * density
            } else {
                itemView.elevation = 8f * density
            }
        } else {
            itemView.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(150)
                .start()
            restoreRestingElevation(itemView)
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        viewHolder.itemView.apply {
            scaleX = 1f
            scaleY = 1f
            alpha = 1f
            restoreRestingElevation(this)
        }
        if (adapter.finishDragIfNeeded()) {
            onDragFinished()
        }
    }

    private fun restoreRestingElevation(itemView: android.view.View) {
        val resting = itemView.getTag(R.id.cardRoot) as? Float
            ?: itemView.resources.getDimension(R.dimen.note_card_elevation)
        val card = itemView as? CardView
        if (card != null) {
            card.cardElevation = resting
        } else {
            itemView.elevation = resting
        }
    }
}
