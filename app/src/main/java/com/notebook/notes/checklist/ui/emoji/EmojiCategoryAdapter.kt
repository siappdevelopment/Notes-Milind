package com.notebook.notes.checklist.ui.emoji

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.notebook.notes.checklist.databinding.ItemEmojiCategoryTabBinding

class EmojiCategoryAdapter(
    private val onCategorySelected: (EmojiCategory) -> Unit
) : ListAdapter<EmojiCategory, EmojiCategoryAdapter.CategoryViewHolder>(DiffCallback) {

    private var selectedCategoryId: String = EmojiCategory.SMILEYS.id

    fun setSelectedCategory(categoryId: String) {
        if (selectedCategoryId == categoryId) return
        val previousIndex = currentList.indexOfFirst { it.id == selectedCategoryId }
        val newIndex = currentList.indexOfFirst { it.id == categoryId }
        selectedCategoryId = categoryId
        if (previousIndex >= 0) notifyItemChanged(previousIndex)
        if (newIndex >= 0) notifyItemChanged(newIndex)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemEmojiCategoryTabBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(
        private val binding: ItemEmojiCategoryTabBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: EmojiCategory) {
            val selected = category.id == selectedCategoryId
            binding.ivTabIcon.setImageResource(category.tabIconRes)
            binding.ivTabIcon.alpha = if (selected) 1f else 0.55f
            binding.indicator.visibility = if (selected) View.VISIBLE else View.INVISIBLE
            binding.root.setOnClickListener { onCategorySelected(category) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<EmojiCategory>() {
        override fun areItemsTheSame(oldItem: EmojiCategory, newItem: EmojiCategory): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: EmojiCategory, newItem: EmojiCategory): Boolean =
            oldItem == newItem
    }
}
