package com.note.quick.pad.ui.emoji

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.note.quick.pad.databinding.ItemEmojiBinding

class EmojiAdapter(
    private val onEmojiSelected: (EmojiItem) -> Unit
) : ListAdapter<EmojiItem, EmojiAdapter.EmojiViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder {
        val binding = ItemEmojiBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EmojiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EmojiViewHolder(
        private val binding: ItemEmojiBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: EmojiItem) {
            binding.tvEmoji.text = item.emoji
            binding.root.setOnClickListener {
                binding.root.animate()
                    .scaleX(0.88f)
                    .scaleY(0.88f)
                    .setDuration(80)
                    .withEndAction {
                        binding.root.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                        onEmojiSelected(item)
                    }
                    .start()
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<EmojiItem>() {
        override fun areItemsTheSame(oldItem: EmojiItem, newItem: EmojiItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: EmojiItem, newItem: EmojiItem): Boolean =
            oldItem == newItem
    }
}
