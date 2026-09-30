package com.note.quick.pad.ui.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.note.quick.pad.R
import com.note.quick.pad.databinding.ItemLanguageBinding
import com.note.quick.pad.model.Language

class LanguageAdapter(
    private val languages: List<Language>,
    private val onLanguageSelected: (Language) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    private var selectedPosition = languages.indexOfFirst { it.isSelected }.takeIf { it != -1 } ?: 0

    fun getSelectedLanguage(): Language = languages[selectedPosition]

    inner class LanguageViewHolder(private val binding: ItemLanguageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(language: Language, position: Int) {
            binding.ivLanguageIcon.setImageResource(language.iconRes)
            binding.tvLanguageName.text = "${language.nativeName} - (${language.name})"
            binding.tvNativeName.text = language.name
            
            // Applying the background color tint to the icon container
            binding.iconContainer.backgroundTintList = ColorStateList.valueOf(
                Color.parseColor(language.backgroundColor)
            )

            val isSelected = position == selectedPosition
            binding.ivSelector.setImageResource(
                if (isSelected) R.drawable.ic_radio_checked
                else R.drawable.ic_radio_unchecked
            )

            binding.root.setOnClickListener {
                val currentPos = bindingAdapterPosition
                if (currentPos == RecyclerView.NO_POSITION || selectedPosition == currentPos) return@setOnClickListener
                
                val oldPosition = selectedPosition
                selectedPosition = currentPos
                notifyItemChanged(oldPosition)
                notifyItemChanged(selectedPosition)
                onLanguageSelected(language)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding = ItemLanguageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LanguageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(languages[position], position)
    }

    override fun getItemCount(): Int = languages.size
}
