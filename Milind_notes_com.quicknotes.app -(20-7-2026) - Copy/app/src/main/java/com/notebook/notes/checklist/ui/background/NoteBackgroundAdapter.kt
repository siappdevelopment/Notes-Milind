package com.notebook.notes.checklist.ui.background

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.notebook.notes.checklist.databinding.ItemNoteBackgroundBinding
import com.notebook.notes.checklist.model.NoteBackgroundOption

class NoteBackgroundAdapter(
    private val repository: NoteBackgroundRepository,
    private val onOptionSelected: (NoteBackgroundOption) -> Unit
) : ListAdapter<NoteBackgroundOption, NoteBackgroundAdapter.ViewHolder>(DiffCallback) {

    private var selectedKey: String? = null

    fun setSelectedKey(key: String?) {
        if (selectedKey.equals(key, ignoreCase = true)) return
        val previous = selectedKey
        selectedKey = key
        currentList.forEachIndexed { index, option ->
            val wasSelected = option.key.equals(previous, ignoreCase = true)
            val isSelected = option.key.equals(key, ignoreCase = true)
            if (wasSelected || isSelected) notifyItemChanged(index, PAYLOAD_SELECTION)
        }
    }

    fun indexOfKey(key: String?): Int {
        if (key.isNullOrBlank()) return -1
        return currentList.indexOfFirst { it.key.equals(key, ignoreCase = true) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNoteBackgroundBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_SELECTION)) {
            holder.bindSelection(getItem(position))
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class ViewHolder(
        private val binding: ItemNoteBackgroundBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val selectedStrokePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            2.5f,
            binding.root.resources.displayMetrics
        ).toInt()

        fun bind(option: NoteBackgroundOption) {
            when (option) {
                is NoteBackgroundOption.SolidColor -> bindSolid(option.hex)
                is NoteBackgroundOption.Asset -> bindAsset(option)
            }
            bindSelection(option)
            binding.root.setOnClickListener {
                if (!option.key.equals(selectedKey, ignoreCase = true)) {
                    setSelectedKey(option.key)
                }
                onOptionSelected(option)
            }
        }

        fun bindSelection(option: NoteBackgroundOption) {
            val selected = option.key.equals(selectedKey, ignoreCase = true)
            binding.cardBackground.isChecked = selected
            binding.cardBackground.strokeWidth = if (selected) selectedStrokePx else 0
            binding.vSelectedScrim.isVisible = selected
            binding.ivSelected.isVisible = selected
            if (selected) {
                val sample = sampleColorFor(option)
                val contrast = ColorUtils.calculateLuminance(sample)

                binding.vSelectedScrim.setBackgroundColor(
                    if (contrast > 0.55) 0x22000000 else 0x33000000
                )
            }
        }

        private fun bindSolid(hex: String) {
            binding.ivThumbnail.isVisible = false
            binding.ivThumbnail.setImageDrawable(null)
            binding.vColor.isVisible = true
            val color = runCatching { Color.parseColor(hex) }.getOrDefault(Color.LTGRAY)
            binding.vColor.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(color)
            }
        }

        private fun bindAsset(option: NoteBackgroundOption.Asset) {
            val item = option.item
            val thumbPath = repository.thumbnailAssetPath(item)
                ?: repository.overlayAssetPath(item)
            val colors = item.bg?.gradientColors.orEmpty()
                .mapNotNull { runCatching { Color.parseColor(it) }.getOrNull() }

            if (thumbPath != null) {
                binding.vColor.isVisible = colors.isNotEmpty()
                if (colors.isNotEmpty()) {
                    binding.vColor.background = gradientDrawable(colors)
                }
                binding.ivThumbnail.isVisible = true
                binding.ivThumbnail.load(repository.assetUri(thumbPath)) {
                    crossfade(true)
                }
            } else {
                binding.ivThumbnail.isVisible = false
                binding.ivThumbnail.setImageDrawable(null)
                binding.vColor.isVisible = true
                binding.vColor.background = gradientDrawable(colors)
            }
        }

        private fun sampleColorFor(option: NoteBackgroundOption): Int {
            return when (option) {
                is NoteBackgroundOption.SolidColor ->
                    runCatching { Color.parseColor(option.hex) }.getOrDefault(Color.LTGRAY)
                is NoteBackgroundOption.Asset ->
                    option.item.bg?.gradientColors.orEmpty()
                        .firstOrNull()
                        ?.let { runCatching { Color.parseColor(it) }.getOrNull() }
                        ?: Color.GRAY
            }
        }

        private fun gradientDrawable(colors: List<Int>): GradientDrawable {
            return GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                when {
                    colors.isEmpty() -> setColor(Color.parseColor("#F5F5F5"))
                    colors.size == 1 -> setColor(colors.first())
                    else -> {
                        orientation = GradientDrawable.Orientation.TOP_BOTTOM
                        this.colors = colors.toIntArray()
                    }
                }
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<NoteBackgroundOption>() {
        override fun areItemsTheSame(
            oldItem: NoteBackgroundOption,
            newItem: NoteBackgroundOption
        ): Boolean = oldItem.key == newItem.key

        override fun areContentsTheSame(
            oldItem: NoteBackgroundOption,
            newItem: NoteBackgroundOption
        ): Boolean = oldItem == newItem
    }

    companion object {
        private const val PAYLOAD_SELECTION = "selection"
    }
}
