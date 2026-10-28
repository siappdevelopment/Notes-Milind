package com.notebook.notes.checklist.ui.adapter

import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.ItemCategoryPopupRowBinding
import com.notebook.notes.checklist.model.Category
import com.notebook.notes.checklist.util.CategoryIconResolver

class CategoryPopupAdapter(
    private val onCategorySelected: (Category) -> Unit
) : ListAdapter<Category, CategoryPopupAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryPopupRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemCategoryPopupRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category) {
            binding.tvTitle.text = category.name
            binding.ivIcon.setImageResource(
                CategoryIconResolver.resolve(category.icon, category.name)
            )
            val black = ContextCompat.getColor(binding.root.context, R.color.app_txt_black)
            binding.ivIcon.clearColorFilter()
            ImageViewCompat.setImageTintMode(binding.ivIcon, PorterDuff.Mode.SRC_IN)
            ImageViewCompat.setImageTintList(binding.ivIcon, ColorStateList.valueOf(black))
            binding.root.setOnClickListener { onCategorySelected(category) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean =
            oldItem == newItem
    }
}
