package com.notebook.notes.checklist.ui.drawing

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.notebook.notes.checklist.databinding.ItemDoodleCategoryTabBinding
import com.notebook.notes.checklist.model.StickerCategory
import kotlinx.coroutines.CoroutineScope

class StickerCategoryAdapter(
    private val scope: CoroutineScope,
    private val repository: StickerConfigRepository,
    private val onCategorySelected: (StickerCategory) -> Unit
) : ListAdapter<StickerCategory, StickerCategoryAdapter.CategoryViewHolder>(DiffCallback) {

    private var selectedCategoryId: String? = null

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).id.hashCode().toLong()

    fun setSelectedCategory(categoryId: String) {
        if (selectedCategoryId == categoryId) return
        val previousIndex = currentList.indexOfFirst { it.id == selectedCategoryId }
        val newIndex = currentList.indexOfFirst { it.id == categoryId }
        selectedCategoryId = categoryId
        if (previousIndex >= 0) notifyItemChanged(previousIndex, PAYLOAD_SELECTION)
        if (newIndex >= 0) notifyItemChanged(newIndex, PAYLOAD_SELECTION)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemDoodleCategoryTabBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_SELECTION)) {
            holder.bindSelection(getItem(position))
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class CategoryViewHolder(
        private val binding: ItemDoodleCategoryTabBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var boundCategoryId: String? = null

        fun bind(category: StickerCategory) {
            boundCategoryId = category.id
            val thumb = repository.categoryCoverFileName(binding.root.context, category)
            binding.ivCategoryCover.loadStickerAsset(
                fileName = thumb,
                scope = scope,
                fallbacks = listOf(category.cover)
            )
            bindSelection(category)
            binding.root.setOnClickListener { onCategorySelected(category) }
        }

        fun bindSelection(category: StickerCategory) {
            val selected = category.id == selectedCategoryId
            binding.ivCategoryCover.alpha = if (selected) 1f else 0.55f
            binding.indicator.visibility = if (selected) View.VISIBLE else View.INVISIBLE
        }
    }

    private companion object {
        const val PAYLOAD_SELECTION = "selection"
    }

    private object DiffCallback : DiffUtil.ItemCallback<StickerCategory>() {
        override fun areItemsTheSame(oldItem: StickerCategory, newItem: StickerCategory): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: StickerCategory, newItem: StickerCategory): Boolean =
            oldItem == newItem
    }
}
