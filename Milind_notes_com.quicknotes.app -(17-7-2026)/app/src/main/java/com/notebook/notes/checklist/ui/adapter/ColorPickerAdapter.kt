package com.notebook.notes.checklist.ui.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.ItemColorPickerBinding

class ColorPickerAdapter(
    private val colors: List<String>,
    private val selectedColor: String?,
    private val onColorClick: (String?) -> Unit,
    private val onCustomClick: (() -> Unit)? = null
) : RecyclerView.Adapter<ColorPickerAdapter.ViewHolder>() {

    private val items: List<ColorItem> = buildList {
        add(ColorItem.None)
        addAll(colors.map { ColorItem.Color(it) })
        add(ColorItem.Custom)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemColorPickerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemColorPickerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ColorItem) {
            when (item) {
                is ColorItem.None -> {
                    binding.vColor.visibility = View.GONE
                    binding.ivIcon.visibility = View.VISIBLE
                    binding.ivIcon.setImageResource(R.drawable.ic_no_color)
                    binding.ivSelected.visibility =
                        if (selectedColor == null) View.VISIBLE else View.GONE
                    binding.ivSelected.setColorFilter(Color.parseColor("#7C69D3"))
                    binding.root.setOnClickListener { onColorClick(null) }
                }

                is ColorItem.Color -> {
                    binding.vColor.visibility = View.VISIBLE
                    binding.ivIcon.visibility = View.GONE
                    val color = Color.parseColor(item.hex)
                    val bg = binding.vColor.background as? GradientDrawable
                        ?: GradientDrawable().apply { shape = GradientDrawable.OVAL }
                    bg.setColor(color)
                    binding.vColor.background = bg

                    val isSelected = selectedColor?.lowercase() == item.hex.lowercase()
                    binding.ivSelected.visibility = if (isSelected) View.VISIBLE else View.GONE

                    val contrast = ColorUtils.calculateLuminance(color)
                    binding.ivSelected.setColorFilter(if (contrast > 0.5) Color.BLACK else Color.WHITE)

                    binding.root.setOnClickListener { onColorClick(item.hex) }
                }

                is ColorItem.Custom -> {
                    binding.vColor.visibility = View.GONE
                    binding.ivIcon.visibility = View.VISIBLE
                    binding.ivIcon.setImageResource(R.drawable.ic_cutom_color_selection)
                    binding.ivSelected.visibility = View.GONE
                    binding.ivIcon.contentDescription =
                        binding.root.context.getString(R.string.custom_color)
                    binding.root.setOnClickListener {
                        onCustomClick?.invoke()
                    }
                }
            }
        }
    }

    sealed class ColorItem {
        object None : ColorItem()
        data class Color(val hex: String) : ColorItem()
        object Custom : ColorItem()
    }
}
