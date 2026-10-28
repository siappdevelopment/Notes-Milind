package com.note.quick.pad.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.note.quick.pad.R
import com.note.quick.pad.databinding.ItemNavDrawerRowBinding
import com.note.quick.pad.model.Category
import com.note.quick.pad.util.CategoryIconResolver

class NavDrawerCategoryAdapter(
    private val onCategorySelected: (Category) -> Unit
) : ListAdapter<NavDrawerCategoryAdapter.DrawerCategoryItem, NavDrawerCategoryAdapter.ViewHolder>(DiffCallback) {

    data class DrawerCategoryItem(
        val category: Category,
        val isSelected: Boolean
    )

    private var selectedId: Long? = null

    fun submitCategories(categories: List<Category>, selectedCategoryId: Long?) {
        selectedId = selectedCategoryId
        submitList(
            categories.map { category ->
                DrawerCategoryItem(category, category.id == selectedCategoryId)
            }
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNavDrawerRowBinding.inflate(
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
        private val binding: ItemNavDrawerRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DrawerCategoryItem) {
            val category = item.category

            binding.tvTitle.text = category.name
            binding.ivIcon.setImageResource(
                CategoryIconResolver.resolve(category.icon, category.name)
            )
            
            binding.rowContainer.isSelected = item.isSelected
            binding.viewSelectedIndicator.visibility = if (item.isSelected) android.view.View.VISIBLE else android.view.View.GONE
            binding.rowContainer.elevation = if (item.isSelected) 30f else 0f
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                // Intense purple shadow to match the "outer shadow" request
                binding.rowContainer.outlineSpotShadowColor = android.graphics.Color.parseColor("#A03525CD")
                binding.rowContainer.outlineAmbientShadowColor = android.graphics.Color.parseColor("#A03525CD")
            }
            
            // Placeholder for count
            binding.tvCount.visibility = if (bindingAdapterPosition == 0 || bindingAdapterPosition == 1) android.view.View.VISIBLE else android.view.View.GONE
            binding.tvCount.text = if (bindingAdapterPosition == 0) "128" else "24"
            
            if (item.isSelected) {
                binding.tvCount.setBackgroundResource(R.drawable.bg_nav_drawer_count_badge)
                binding.tvCount.setTextColor(android.graphics.Color.WHITE)
            } else {
                binding.tvCount.setBackgroundResource(R.drawable.bg_nav_drawer_count_badge_unselected)
                binding.tvCount.setTextColor(binding.root.context.getColor(R.color.app_primary_color))
            }

            binding.root.setOnClickListener { onCategorySelected(category) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<DrawerCategoryItem>() {
        override fun areItemsTheSame(oldItem: DrawerCategoryItem, newItem: DrawerCategoryItem): Boolean =
            oldItem.category.id == newItem.category.id

        override fun areContentsTheSame(oldItem: DrawerCategoryItem, newItem: DrawerCategoryItem): Boolean =
            oldItem == newItem
    }
}
