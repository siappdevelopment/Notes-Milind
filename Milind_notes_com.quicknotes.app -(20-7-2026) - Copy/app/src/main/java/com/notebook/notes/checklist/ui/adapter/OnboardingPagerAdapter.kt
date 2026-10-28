package com.notebook.notes.checklist.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.notebook.notes.checklist.databinding.ItemOnboardingPageBinding

data class OnboardingPage(
    @DrawableRes val imageRes: Int = 0,
    @StringRes val titlePrefixRes: Int,
    @StringRes val titleSuffixRes: Int,
    @StringRes val subtitleRes: Int
)

class OnboardingPagerAdapter(
    private val pages: List<OnboardingPage>
) : RecyclerView.Adapter<OnboardingPagerAdapter.PageViewHolder>() {

    override fun getItemCount(): Int = pages.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemOnboardingPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(pages[position])
    }

    class PageViewHolder(
        private val binding: ItemOnboardingPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(page: OnboardingPage) {
            if (page.imageRes != 0) {
                binding.ivOnboardingImage.setImageResource(page.imageRes)
            } else {
                binding.ivOnboardingImage.setImageDrawable(null)
            }
        }
    }
}
