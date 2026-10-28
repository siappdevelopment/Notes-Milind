package com.notebook.notes.checklist.ui.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.ItemLanguageBinding
import com.notebook.notes.checklist.model.Language

class LanguageAdapter(
    private val languages: List<Language>,
    private val onLanguageSelected: (Language) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    private var selectedPosition = languages.indexOfFirst { it.isSelected }

    fun hasSelection(): Boolean = selectedPosition in languages.indices

    fun getSelectedLanguage(): Language? =
        if (hasSelection()) languages[selectedPosition] else null

    inner class LanguageViewHolder(private val binding: ItemLanguageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(language: Language, position: Int) {
            binding.ivLanguageIcon.setImageResource(language.iconRes)
            binding.tvLanguageName.text = "${language.nativeName} - (${language.name})"
            binding.tvNativeName.text = language.displayName()
            
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
                if (oldPosition >= 0) notifyItemChanged(oldPosition)
                notifyItemChanged(selectedPosition)
                onLanguageSelected(languages[currentPos])
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
