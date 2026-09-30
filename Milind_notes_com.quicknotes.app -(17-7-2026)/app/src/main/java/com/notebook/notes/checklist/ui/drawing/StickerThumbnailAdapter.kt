package com.notebook.notes.checklist.ui.drawing

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.notebook.notes.checklist.databinding.ItemDoodleThumbnailBinding
import com.notebook.notes.checklist.model.StickerResource
import kotlinx.coroutines.CoroutineScope

class StickerThumbnailAdapter(
    private val scope: CoroutineScope,
    private val repository: StickerConfigRepository,
    private val onStickerFailed: (Int) -> Unit,
    private val onStickerSelected: (StickerResource) -> Unit
) : ListAdapter<StickerResource, StickerThumbnailAdapter.ThumbnailViewHolder>(DiffCallback) {

    private var selectedResourceId: Int? = null

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).id.toLong()

    fun setSelectedResource(resourceId: Int?) {
        val previousId = selectedResourceId
        if (previousId == resourceId) return
        selectedResourceId = resourceId
        val previousIndex = currentList.indexOfFirst { it.id == previousId }
        val newIndex = currentList.indexOfFirst { it.id == resourceId }
        if (previousIndex >= 0) notifyItemChanged(previousIndex, PAYLOAD_SELECTION)
        if (newIndex >= 0) notifyItemChanged(newIndex, PAYLOAD_SELECTION)
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

        private var boundResourceId: Int? = null

        fun bind(resource: StickerResource) {
            boundResourceId = resource.id
            val thumb = repository.thumbFileName(binding.root.context, resource)
            binding.ivDoodle.loadStickerAsset(
                fileName = thumb,
                scope = scope,
                fallbacks = listOf(resource.res, resource.thumb),
                onFailure = { onStickerFailed(resource.id) }
            )
            bindSelection(resource)
            binding.root.setOnClickListener {
                val previousId = selectedResourceId
                selectedResourceId = resource.id
                val previousIndex = currentList.indexOfFirst { it.id == previousId }
                val newIndex = bindingAdapterPosition
                if (previousIndex >= 0) notifyItemChanged(previousIndex, PAYLOAD_SELECTION)
                if (newIndex >= 0) notifyItemChanged(newIndex, PAYLOAD_SELECTION)
                onStickerSelected(resource)
            }
        }

        fun bindSelection(resource: StickerResource) {
            binding.container.isSelected = resource.id == selectedResourceId
        }
    }

    private companion object {
        const val PAYLOAD_SELECTION = "selection"
    }

    private object DiffCallback : DiffUtil.ItemCallback<StickerResource>() {
        override fun areItemsTheSame(oldItem: StickerResource, newItem: StickerResource): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: StickerResource, newItem: StickerResource): Boolean =
            oldItem == newItem
    }
}
