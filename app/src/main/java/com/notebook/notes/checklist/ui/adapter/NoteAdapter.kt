package com.notebook.notes.checklist.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.ItemNoteBinding
import com.notebook.notes.checklist.model.Note
import com.notebook.notes.checklist.util.DateTimeUtils
import com.notebook.notes.checklist.util.NoteTitleHelper

class NoteAdapter(
    private val onNoteClick: (Note) -> Unit,
    private val onPinClick: (Note) -> Unit,
    private val onFavoriteClick: (Note) -> Unit,
    private val onDeleteClick: (Note) -> Unit
) : ListAdapter<NoteAdapter.NoteItem, NoteAdapter.NoteViewHolder>(NoteDiffCallback()) {

    data class NoteItem(
        val note: Note,
        val preview: String
    )

    fun submitNotes(notes: List<Note>, previews: Map<Long, String>) {
        submitList(notes.map { NoteItem(it, previews[it.id].orEmpty()) })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NoteViewHolder(
        private val binding: ItemNoteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NoteItem) {
            val note = item.note
            val context = binding.root.context
            binding.tvTitle.text = NoteTitleHelper.displayTitle(context, note.title)
            binding.tvPreview.text = item.preview.ifBlank { context.getString(R.string.no_content) }
            binding.tvDate.text = DateTimeUtils.formatListDate(note.updatedAt)
            binding.ivPin.isSelected = note.isPinned
            binding.ivFavorite.isSelected = note.isFavorite

            binding.root.setOnClickListener { onNoteClick(note) }
            binding.ivPin.setOnClickListener { onPinClick(note) }
            binding.ivFavorite.setOnClickListener { onFavoriteClick(note) }
            binding.ivDelete.setOnClickListener { onDeleteClick(note) }
        }
    }

    private class NoteDiffCallback : DiffUtil.ItemCallback<NoteItem>() {
        override fun areItemsTheSame(oldItem: NoteItem, newItem: NoteItem) =
            oldItem.note.id == newItem.note.id

        override fun areContentsTheSame(oldItem: NoteItem, newItem: NoteItem) =
            oldItem == newItem
    }
}
