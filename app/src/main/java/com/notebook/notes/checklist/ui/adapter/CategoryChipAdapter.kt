package com.notebook.notes.checklist.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.model.Category

class CategoryChipAdapter(
    private val onCategorySelected: (Category) -> Unit,
    private val onCategoryLongClick: (() -> Unit)? = null
) : ListAdapter<CategoryChipAdapter.CategoryChipItem, CategoryChipAdapter.ViewHolder>(DiffCallback) {

    data class CategoryChipItem(
        val category: Category,
        val isSelected: Boolean
    )

    private var selectedId: Long? = null

    fun submitCategories(categories: List<Category>, selectedCategoryId: Long?, onCommitted: () -> Unit = {}) {
        selectedId = selectedCategoryId
        submitList(
            categories.map { category ->
                CategoryChipItem(category, category.id == selectedCategoryId)
            },
            onCommitted
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val textView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_chip, parent, false) as TextView
        return ViewHolder(textView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val textView: TextView) : RecyclerView.ViewHolder(textView) {
        fun bind(item: CategoryChipItem) {
            val category = item.category
            textView.text = category.name
            
            if (item.isSelected) {
                textView.setBackgroundResource(R.drawable.bg_category_chip_selected)
                textView.setTextColor(ContextCompat.getColor(textView.context, R.color.white))
            } else {
                textView.setBackgroundResource(R.drawable.bg_category_chip_unselected)
                textView.setTextColor(ContextCompat.getColor(textView.context, R.color.app_txt_black))
            }
            
            textView.setOnClickListener { onCategorySelected(category) }
            textView.setOnLongClickListener {
                onCategoryLongClick?.invoke()
                onCategoryLongClick != null
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<CategoryChipItem>() {
        override fun areItemsTheSame(oldItem: CategoryChipItem, newItem: CategoryChipItem): Boolean =
            oldItem.category.id == newItem.category.id

        override fun areContentsTheSame(oldItem: CategoryChipItem, newItem: CategoryChipItem): Boolean =
            oldItem == newItem
    }
}
