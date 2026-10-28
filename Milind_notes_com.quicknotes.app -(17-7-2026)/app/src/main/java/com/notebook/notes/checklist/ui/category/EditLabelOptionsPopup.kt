package com.notebook.notes.checklist.ui.category

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.PopupEditLabelOptionsBinding
import com.notebook.notes.checklist.model.CategoryWithCount
import com.notebook.notes.checklist.util.keepNavigationBarHidden

object EditLabelOptionsPopup {

    fun show(
        context: Context,
        anchor: View,
        category: CategoryWithCount,
        areNotesLocked: Boolean,
        onRename: (CategoryWithCount) -> Unit,
        onDelete: (CategoryWithCount) -> Unit,
        onLockToggle: (CategoryWithCount, locked: Boolean) -> Unit
    ) {
        val binding = PopupEditLabelOptionsBinding.inflate(LayoutInflater.from(context))

        // Non-focusable keeps the activity immersive so the system nav bar stays hidden.
        val popup = PopupWindow(
            binding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false
        ).apply {
            elevation = 16f
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
            isFocusable = false
            keepNavigationBarHidden(context)
        }

        if (areNotesLocked) {
            binding.tvLock.setText(R.string.card_menu_unlock)
            binding.ivLock.setImageResource(R.drawable.ic_unlock)
        } else {
            binding.tvLock.setText(R.string.card_menu_lock)
            binding.ivLock.setImageResource(R.drawable.ic_lock)
        }

        binding.btnRename.setOnClickListener {
            popup.dismiss()
            onRename(category)
        }

        binding.btnDelete.setOnClickListener {
            popup.dismiss()
            onDelete(category)
        }

        binding.btnLock.setOnClickListener {
            popup.dismiss()
            onLockToggle(category, areNotesLocked)
        }

        val density = context.resources.displayMetrics.density
        val margin = (16 * density).toInt()
        val gap = (4 * density).toInt()

        anchor.post {
            binding.root.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )

            val measuredPopupWidth = binding.root.measuredWidth
            val measuredPopupHeight = binding.root.measuredHeight
            val location = IntArray(2)
            anchor.getLocationOnScreen(location)
            val screenWidth = context.resources.displayMetrics.widthPixels
            val screenHeight = context.resources.displayMetrics.heightPixels

            var xOff = anchor.width - measuredPopupWidth
            if (location[0] + xOff + measuredPopupWidth > screenWidth - margin) {
                xOff = screenWidth - margin - location[0] - measuredPopupWidth
            }
            if (location[0] + xOff < margin) {
                xOff = margin - location[0]
            }

            val spaceBelow = screenHeight - (location[1] + anchor.height) - margin
            val spaceAbove = location[1] - margin
            val yOff = if (spaceBelow < measuredPopupHeight && spaceAbove >= measuredPopupHeight) {
                // Open above the anchor so the full menu stays on screen.
                -(anchor.height + measuredPopupHeight + gap)
            } else {
                gap
            }

            popup.showAsDropDown(anchor, xOff, yOff)
        }
    }
}
