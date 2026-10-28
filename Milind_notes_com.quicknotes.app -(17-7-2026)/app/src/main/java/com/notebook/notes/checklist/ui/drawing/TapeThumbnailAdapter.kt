package com.notebook.notes.checklist.ui.drawing

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.notebook.notes.checklist.databinding.ItemDoodleThumbnailBinding
import com.notebook.notes.checklist.model.TapeResource
import kotlinx.coroutines.CoroutineScope

class TapeThumbnailAdapter(
    private val scope: CoroutineScope,
    private val onTapeFailed: (Int) -> Unit,
    private val onTapeSelected: (TapeResource) -> Unit
) : ListAdapter<TapeResource, TapeThumbnailAdapter.ThumbnailViewHolder>(DiffCallback) {

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

        fun bind(resource: TapeResource) {
            binding.ivDoodle.loadTapeAsset(
                fileName = resource.thumb,
                scope = scope,
                onFailure = { onTapeFailed(resource.id) }
            )
            bindSelection(resource)
            binding.root.setOnClickListener {
                val previousId = selectedResourceId
                selectedResourceId = resource.id
                val previousIndex = currentList.indexOfFirst { it.id == previousId }
                val newIndex = bindingAdapterPosition
                if (previousIndex >= 0) notifyItemChanged(previousIndex, PAYLOAD_SELECTION)
                if (newIndex >= 0) notifyItemChanged(newIndex, PAYLOAD_SELECTION)
                onTapeSelected(resource)
            }
        }

        fun bindSelection(resource: TapeResource) {
            binding.container.isSelected = resource.id == selectedResourceId
        }
    }

    private companion object {
        const val PAYLOAD_SELECTION = "selection"
    }

    private object DiffCallback : DiffUtil.ItemCallback<TapeResource>() {
        override fun areItemsTheSame(oldItem: TapeResource, newItem: TapeResource): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: TapeResource, newItem: TapeResource): Boolean =
            oldItem == newItem
    }
}
