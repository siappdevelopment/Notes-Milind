package com.notebook.notes.checklist.ui.category

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.PopupCategorySelectorBinding
import com.notebook.notes.checklist.model.Category
import com.notebook.notes.checklist.ui.activity.EditLabelActivity
import com.notebook.notes.checklist.ui.adapter.CategoryPopupAdapter
import com.notebook.notes.checklist.util.keepNavigationBarHidden

object CategorySelectorPopup {

    fun show(
        activity: AppCompatActivity,
        anchor: View,
        categories: List<Category>,
        onCategorySelected: (Category) -> Unit,
        onCategoryCreated: ()  -> Unit
    ) {
        val binding = PopupCategorySelectorBinding.inflate(LayoutInflater.from(activity))

        val density = activity.resources.displayMetrics.density
        val minWidth = (220 * density).toInt()
        val popupWidth = maxOf(anchor.width, minWidth)

        lateinit var popup: PopupWindow
        popup = PopupWindow(
            binding.root,
            popupWidth,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            false
        ).apply {
            elevation = 12f
            isOutsideTouchable = true
            isFocusable = false
            setBackgroundDrawable(
                ContextCompat.getDrawable(activity, android.R.color.transparent)
            )
            keepNavigationBarHidden(activity)
        }

        val adapter = CategoryPopupAdapter { category ->
            popup.dismiss()
            onCategorySelected(category)
        }

        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(activity)
            this.adapter = adapter
            itemAnimator = null
        }
        adapter.submitList(categories)

        binding.btnAddCategory.setOnClickListener {
            // Dismiss first so the create-label sheet can attach cleanly.
            popup.dismiss()
            onCategoryCreated.invoke()
        }



        val yOffset = (4 * density).toInt()

        anchor.post {
            val widthSpec = View.MeasureSpec.makeMeasureSpec(popupWidth, View.MeasureSpec.EXACTLY)
            val unspecifiedHeightSpec =
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            binding.root.measure(widthSpec, unspecifiedHeightSpec)

            // Cap the popup at 70% of the screen so the "Add Category" footer
            // (which sits outside the RecyclerView) always stays on screen.
            // Only the category list scrolls.
            val screenHeight = activity.resources.displayMetrics.heightPixels
            val maxPopupHeight = (screenHeight * 0.7f).toInt()

            if (binding.root.measuredHeight > maxPopupHeight) {
                val fixedChromeHeight =
                    binding.root.measuredHeight - binding.rvCategories.measuredHeight
                val minListHeight = (88 * density).toInt()
                binding.rvCategories.layoutParams = binding.rvCategories.layoutParams.apply {
                    height = (maxPopupHeight - fixedChromeHeight).coerceAtLeast(minListHeight)
                }
                binding.root.measure(widthSpec, unspecifiedHeightSpec)
            }

            val measuredPopupWidth = binding.root.measuredWidth
            val location = IntArray(2)
            anchor.getLocationOnScreen(location)
            val screenWidth = activity.resources.displayMetrics.widthPixels

            // Open directly below the clicked button, aligned to its left edge.
            val marginEnd = (10 * density).toInt()
            var xOff = 0
            if (location[0] + measuredPopupWidth + marginEnd > screenWidth) {
                xOff = screenWidth - location[0] - measuredPopupWidth - marginEnd
            }

            popup.showAsDropDown(anchor, xOff, yOffset)
        }
    }
}
