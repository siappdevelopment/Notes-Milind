package com.notebook.notes.checklist.ui.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.ItemFormattingToolBinding
import com.notebook.notes.checklist.ui.richtext.FormattingToolId
import com.notebook.notes.checklist.ui.richtext.FormattingToolItem

class FormattingToolAdapter(
    private val onToolClick: (FormattingToolId) -> Unit
) : ListAdapter<FormattingToolItem, FormattingToolAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFormattingToolBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun submitTools(tools: List<FormattingToolItem>) {
        submitList(tools.toList())
    }

    inner class ViewHolder(
        private val binding: ItemFormattingToolBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FormattingToolItem) {
            val context = binding.root.context
            val toolColor = ContextCompat.getColor(
                context,
                if (item.isSelected) R.color.app_primary_color else R.color.app_txt_black
            )
            val toolColorStateList = ColorStateList.valueOf(toolColor)

            if (item.label != null) {
                binding.tvToolLabel.isVisible = true
                binding.ivToolIcon.isVisible = false
                binding.flSpecialIcon.isVisible = false
                binding.tvToolLabel.text = item.label
                binding.tvToolLabel.setTextColor(toolColor)
                binding.tvToolLabel.typeface = when (item.id) {
                    FormattingToolId.BOLD -> Typeface.DEFAULT_BOLD
                    FormattingToolId.ITALIC -> Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                    else -> Typeface.DEFAULT
                }
                binding.tvToolLabel.paint.isUnderlineText = item.id == FormattingToolId.UNDERLINE
                binding.tvToolLabel.paint.isStrikeThruText = item.id == FormattingToolId.STRIKETHROUGH
            } else if (item.id == FormattingToolId.TEXT_COLOR || item.id == FormattingToolId.HIGHLIGHT) {
                binding.tvToolLabel.isVisible = false
                binding.ivToolIcon.isVisible = false
                binding.flSpecialIcon.isVisible = true

                val displayColor = item.color
                    ?: if (item.id == FormattingToolId.HIGHLIGHT) Color.TRANSPARENT else Color.BLACK

                if (item.id == FormattingToolId.HIGHLIGHT) {
                    binding.tvSpecialText.isVisible = false
                    binding.ivSpecialIcon.isVisible = true
                    binding.ivSpecialIcon.setImageResource(R.drawable.ic_text_bg)
                    binding.ivSpecialBg.isVisible = false
                    binding.ivSpecialIcon.imageTintList = if (displayColor == Color.TRANSPARENT) {
                        toolColorStateList
                    } else {
                        ColorStateList.valueOf(displayColor)
                    }
                } else {
                    binding.tvSpecialText.isVisible = true
                    binding.ivSpecialIcon.isVisible = false
                    binding.ivSpecialBg.isVisible = true
                    binding.tvSpecialText.text = "A"
                    binding.ivSpecialBg.imageTintList = ColorStateList.valueOf(displayColor)
                    binding.tvSpecialText.setTextColor(Color.WHITE)
                }
            } else {
                binding.tvToolLabel.isVisible = false
                binding.ivToolIcon.isVisible = true
                binding.flSpecialIcon.isVisible = false
                binding.ivToolIcon.setImageResource(item.iconRes ?: 0)
                binding.ivToolIcon.imageTintList = toolColorStateList
            }

            val description = if (item.contentDescriptionRes != 0) {
                context.getString(item.contentDescriptionRes)
            } else {
                item.label
            }
            binding.root.contentDescription = description
            binding.root.isFocusable = false
            binding.root.setOnClickListener { onToolClick(item.id) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<FormattingToolItem>() {
        override fun areItemsTheSame(oldItem: FormattingToolItem, newItem: FormattingToolItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: FormattingToolItem, newItem: FormattingToolItem): Boolean =
            oldItem == newItem
    }
}
