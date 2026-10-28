package com.notebook.notes.checklist.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.ItemFontStyleBinding
import com.notebook.notes.checklist.ui.richtext.FontStyleOption
import com.notebook.notes.checklist.ui.richtext.RichTextFormats

class FontStyleAdapter(
    private val onStyleSelected: (FontStyleOption) -> Unit
) : ListAdapter<FontStyleAdapter.StyleItem, FontStyleAdapter.ViewHolder>(DiffCallback) {

    data class StyleItem(
        val option: FontStyleOption,
        val isSelected: Boolean
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFontStyleBinding.inflate(
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
        private val binding: ItemFontStyleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: StyleItem) {
            binding.tvFontStyle.text = item.option.displayName
            binding.tvFontStyle.typeface = RichTextFormats.previewTypeface(
              item.option.id,
              binding.root.context
            )

            if (item.isSelected) {
                binding.tvFontStyle.setBackgroundResource(R.drawable.bg_text_style_preset_selected)
                binding.tvFontStyle.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.white)
                )
            } else {
                binding.tvFontStyle.setBackgroundResource(R.drawable.bg_font_style_item_unselected)
                binding.tvFontStyle.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.grey_1)
                )
            }

            binding.root.isFocusable = false
            binding.root.setOnClickListener { onStyleSelected(item.option) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<StyleItem>() {
        override fun areItemsTheSame(oldItem: StyleItem, newItem: StyleItem): Boolean =
            oldItem.option.id == newItem.option.id

        override fun areContentsTheSame(oldItem: StyleItem, newItem: StyleItem): Boolean =
            oldItem == newItem
    }
}
