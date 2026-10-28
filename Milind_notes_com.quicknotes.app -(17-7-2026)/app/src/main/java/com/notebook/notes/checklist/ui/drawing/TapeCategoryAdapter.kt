package com.notebook.notes.checklist.ui.drawing

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.notebook.notes.checklist.databinding.ItemDoodleCategoryTabBinding
import com.notebook.notes.checklist.model.TapeCategory
import kotlinx.coroutines.CoroutineScope

class TapeCategoryAdapter(
    private val scope: CoroutineScope,
    private val onCategorySelected: (TapeCategory) -> Unit
) : ListAdapter<TapeCategory, TapeCategoryAdapter.CategoryViewHolder>(DiffCallback) {

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

        fun bind(category: TapeCategory) {
            binding.ivCategoryCover.loadTapeAsset(category.cover, scope)
            bindSelection(category)
            binding.root.setOnClickListener { onCategorySelected(category) }
        }

        fun bindSelection(category: TapeCategory) {
            val selected = category.id == selectedCategoryId
            binding.ivCategoryCover.alpha = if (selected) 1f else 0.55f
            binding.indicator.visibility = if (selected) View.VISIBLE else View.INVISIBLE
        }
    }

    private companion object {
        const val PAYLOAD_SELECTION = "selection"
    }

    private object DiffCallback : DiffUtil.ItemCallback<TapeCategory>() {
        override fun areItemsTheSame(oldItem: TapeCategory, newItem: TapeCategory): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: TapeCategory, newItem: TapeCategory): Boolean =
            oldItem == newItem
    }
}
