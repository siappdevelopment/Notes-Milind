package com.notebook.notes.checklist.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.notebook.notes.checklist.databinding.ItemEditLabelBinding
import com.notebook.notes.checklist.model.CategoryWithCount
import com.notebook.notes.checklist.model.DefaultCategories

class EditLabelAdapter(
    private val onMoreClick: (View, CategoryWithCount) -> Unit
) : ListAdapter<CategoryWithCount, EditLabelAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEditLabelBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemEditLabelBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoryWithCount) {
            binding.tvLabelName.text = "${item.name} ( ${item.count} )"
            
            // "All Notes" is a system filter — no rename/delete/lock options.
            if (item.name.equals(DefaultCategories.ALL_NOTES, ignoreCase = true)) {
                binding.btnMore.visibility = View.GONE
            } else {
                binding.btnMore.visibility = View.VISIBLE
                binding.btnMore.setOnClickListener { onMoreClick(it, item) }
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<CategoryWithCount>() {
        override fun areItemsTheSame(oldItem: CategoryWithCount, newItem: CategoryWithCount): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CategoryWithCount, newItem: CategoryWithCount): Boolean {
            return oldItem == newItem
        }
    }
}
