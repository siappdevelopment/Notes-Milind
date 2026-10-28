package com.notebook.notes.checklist.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.ItemTextStylePresetBinding
import com.notebook.notes.checklist.ui.richtext.TextStylePreset

class TextStylePresetAdapter(
    private val onPresetSelected: (TextStylePreset) -> Unit
) : ListAdapter<TextStylePresetAdapter.PresetItem, TextStylePresetAdapter.ViewHolder>(DiffCallback) {

    data class PresetItem(
        val preset: TextStylePreset,
        val label: String,
        val isSelected: Boolean
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTextStylePresetBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun submitPresets(presets: List<PresetItem>) {
        submitList(presets)
    }

    inner class ViewHolder(
        private val binding: ItemTextStylePresetBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PresetItem) {
            binding.tvPreset.text = item.label
            binding.tvPreset.textSize = item.preset.previewTextSizeSp

            if (item.isSelected) {
                binding.tvPreset.setBackgroundResource(R.drawable.bg_text_style_preset_selected)
                binding.tvPreset.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.white)
                )
            } else {
                binding.tvPreset.setBackgroundResource(R.drawable.bg_text_style_preset_unselected)
                binding.tvPreset.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.grey_1)
                )
            }

            binding.root.isFocusable = false
            binding.root.setOnClickListener { onPresetSelected(item.preset) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<PresetItem>() {
        override fun areItemsTheSame(oldItem: PresetItem, newItem: PresetItem): Boolean =
            oldItem.preset == newItem.preset

        override fun areContentsTheSame(oldItem: PresetItem, newItem: PresetItem): Boolean =
            oldItem == newItem
    }
}
