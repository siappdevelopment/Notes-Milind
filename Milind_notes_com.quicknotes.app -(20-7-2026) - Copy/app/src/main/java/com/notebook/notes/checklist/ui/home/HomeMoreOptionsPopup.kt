package com.notebook.notes.checklist.ui.home

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.PopupHomeMoreOptionsBinding
import com.notebook.notes.checklist.sort.NoteSortOrder

object HomeMoreOptionsPopup {

    fun show(
        context: Context,
        anchor: View,
        isGridView: Boolean,
        selectedSort: NoteSortOrder,
        showReminderSort: Boolean,
        onViewModeClick: () -> Unit,
        onSelectClick: () -> Unit,
        onSortSelected: (NoteSortOrder) -> Unit
    ) {
        val binding = PopupHomeMoreOptionsBinding.inflate(LayoutInflater.from(context))

        val popup = PopupWindow(
            binding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 16f
            isOutsideTouchable = true
            isFocusable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        if (isGridView) {
            binding.tvViewMode.setText(R.string.view_list)
            binding.ivViewMode.setImageResource(R.drawable.ic_view_list)
        } else {
            binding.tvViewMode.setText(R.string.view_grid)
            binding.ivViewMode.setImageResource(R.drawable.ic_view_grid)
        }

        binding.optionViewMode.setOnClickListener {
            popup.dismiss()
            onViewModeClick()
        }
        binding.optionSelect.setOnClickListener {
            popup.dismiss()
            onSelectClick()
        }
        binding.optionSortBy.setOnClickListener {
            popup.dismiss()
            anchor.post {
                HomeSortByPopup.show(
                    context = context,
                    anchor = anchor,
                    selectedSort = selectedSort,
                    showReminderOption = showReminderSort,
                    onSortSelected = onSortSelected
                )
            }
        }
        binding.optionSync.setOnClickListener {
            popup.dismiss()
            Toast.makeText(context, R.string.drawer_coming_soon, Toast.LENGTH_SHORT).show()
        }

        val density = context.resources.displayMetrics.density
        val minWidth = (200 * density).toInt()

        binding.root.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupWidth = binding.root.measuredWidth.coerceAtLeast(minWidth)
        popup.width = popupWidth
        val xOffset = anchor.width - popupWidth
        val yOffset = (6 * density).toInt()
        popup.showAsDropDown(anchor, xOffset, yOffset)
    }
}
