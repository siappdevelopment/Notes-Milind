package com.notebook.notes.checklist.ui.drawing

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.notebook.notes.checklist.databinding.ItemDoodleCategoryTabBinding
import com.notebook.notes.checklist.model.BgCategory
import kotlinx.coroutines.CoroutineScope

class DoodleCategoryAdapter(
    private val scope: CoroutineScope,
    private val bgRepository: BgConfigRepository,
    private val isLandscapePreview: () -> Boolean,
    private val onCategorySelected: (BgCategory) -> Unit
) : ListAdapter<BgCategory, DoodleCategoryAdapter.CategoryViewHolder>(DiffCallback) {

    private var selectedCategoryId: String? = null

    fun setSelectedCategory(categoryId: String) {
        if (selectedCategoryId == categoryId) return
        val previousIndex = currentList.indexOfFirst { it.id == selectedCategoryId }
        val newIndex = currentList.indexOfFirst { it.id == categoryId }
        selectedCategoryId = categoryId
        if (previousIndex >= 0) notifyItemChanged(previousIndex)
        if (newIndex >= 0) notifyItemChanged(newIndex)
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

    inner class CategoryViewHolder(
        private val binding: ItemDoodleCategoryTabBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: BgCategory) {
            val selected = category.id == selectedCategoryId
            val coverResource = bgRepository.getResourcesForCategory(category.id).firstOrNull()
            if (coverResource != null) {
                val thumbFile = bgRepository.thumbFileName(coverResource, isLandscapePreview())
                binding.ivCategoryCover.loadBackgroundAsset(thumbFile, scope)
            } else {
                binding.ivCategoryCover.setImageDrawable(null)
            }
            binding.ivCategoryCover.alpha = if (selected) 1f else 0.55f
            binding.indicator.visibility = if (selected) View.VISIBLE else View.INVISIBLE
            binding.root.setOnClickListener { onCategorySelected(category) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<BgCategory>() {
        override fun areItemsTheSame(oldItem: BgCategory, newItem: BgCategory): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: BgCategory, newItem: BgCategory): Boolean =
            oldItem == newItem
    }
}
