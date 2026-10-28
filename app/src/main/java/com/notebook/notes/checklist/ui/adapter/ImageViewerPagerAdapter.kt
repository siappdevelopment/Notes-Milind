package com.notebook.notes.checklist.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.notebook.notes.checklist.databinding.ItemImageViewerPageBinding
import com.notebook.notes.checklist.model.MediaViewerItem
import java.io.File

class ImageViewerPagerAdapter(
    private val items: List<MediaViewerItem>
) : RecyclerView.Adapter<ImageViewerPagerAdapter.PageViewHolder>() {

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemImageViewerPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(items[position])
    }

    class PageViewHolder(
        private val binding: ItemImageViewerPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MediaViewerItem) {
            val file = File(item.path)
            if (file.exists()) {
                binding.zoomableImage.load(file) {
                    crossfade(true)
                    target(binding.zoomableImage)
                }
            } else {
                binding.zoomableImage.setImageDrawable(null)
            }
        }
    }
}
