package com.notebook.notes.checklist.ui.drawing

import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.ItemDrawingColorSwatchBinding

class DrawingColorSwatchAdapter(
    private val onColorSelected: (Int) -> Unit
) : ListAdapter<Int, DrawingColorSwatchAdapter.ViewHolder>(DiffCallback) {

    private var selectedColor: Int? = null

    fun setSelectedColor(color: Int) {
        if (selectedColor == color) return
        val previousColor = selectedColor
        selectedColor = color
        previousColor?.let { previous ->
            val previousIndex = currentList.indexOf(previous)
            if (previousIndex >= 0) notifyItemChanged(previousIndex)
        }
        val newIndex = currentList.indexOf(color)
        if (newIndex >= 0) notifyItemChanged(newIndex)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDrawingColorSwatchBinding.inflate(
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
        private val binding: ItemDrawingColorSwatchBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(color: Int) {
            val isSelected = color == selectedColor
            binding.colorFill.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
            }
            binding.root.background = createSwatchRing(binding.root, color, isSelected)
            binding.root.isSelected = isSelected
            binding.root.setOnClickListener { onColorSelected(color) }
        }

        private fun createSwatchRing(view: View, color: Int, isSelected: Boolean): GradientDrawable {
            val density = view.context.resources.displayMetrics.density
            val strokeWidthPx = ((if (isSelected) 2 else 1) * density).toInt().coerceAtLeast(1)
            val strokeColor = if (isSelected) {
                color
            } else {
                ContextCompat.getColor(view.context, R.color.bg_stroke)
            }
            return GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.TRANSPARENT)
                setStroke(strokeWidthPx, strokeColor)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Int>() {
        override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean = oldItem == newItem

        override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean = oldItem == newItem
    }
}

class DrawingColorSpacingDecoration(
    private val spacingPx: Int
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position != RecyclerView.NO_POSITION && position < state.itemCount - 1) {
            outRect.right = spacingPx
        }
    }
}
