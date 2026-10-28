package com.notebook.notes.checklist.ui.drawing

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.notebook.notes.checklist.databinding.ItemDoodleThumbnailBinding
import com.notebook.notes.checklist.model.BgResource
import kotlinx.coroutines.CoroutineScope

class BgThumbnailAdapter(
    private val scope: CoroutineScope,
    private val thumbResolver: (BgResource) -> String,
    private val onBackgroundSelected: (BgResource) -> Unit
) : ListAdapter<BgResource, BgThumbnailAdapter.ThumbnailViewHolder>(DiffCallback) {

    private var selectedResourceId: Int? = null

    fun setSelectedResource(resourceId: Int?) {
        val previousId = selectedResourceId
        if (previousId == resourceId) return
        selectedResourceId = resourceId
        currentList.forEachIndexed { index, resource ->
            if (resource.id == previousId || resource.id == resourceId) {
                notifyItemChanged(index, PAYLOAD_SELECTION)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbnailViewHolder {
        val binding = ItemDoodleThumbnailBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ThumbnailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ThumbnailViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: ThumbnailViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_SELECTION)) {
            holder.bindSelection(getItem(position))
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class ThumbnailViewHolder(
        private val binding: ItemDoodleThumbnailBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(resource: BgResource) {
            binding.ivDoodle.loadBackgroundAsset(thumbResolver(resource), scope)
            bindSelection(resource)
            binding.root.setOnClickListener {
                onBackgroundSelected(resource)
            }
        }

        fun bindSelection(resource: BgResource) {
            binding.container.isSelected = resource.id == selectedResourceId
        }
    }

    private companion object {
        const val PAYLOAD_SELECTION = "selection"
    }

    private object DiffCallback : DiffUtil.ItemCallback<BgResource>() {
        override fun areItemsTheSame(oldItem: BgResource, newItem: BgResource): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: BgResource, newItem: BgResource): Boolean =
            oldItem == newItem
    }
}
