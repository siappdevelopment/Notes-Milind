package com.notebook.notes.checklist.ui.home

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.sort.NoteSortOrder
import com.notebook.notes.checklist.util.keepNavigationBarHidden

object HomeSortByPopup {

    fun show(
        context: Context,
        anchor: View,
        selectedSort: NoteSortOrder,
        showReminderOption: Boolean,
        onSortSelected: (NoteSortOrder) -> Unit
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.popup_home_sort_by, null)

        val popup = PopupWindow(
            view,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            // Non-focusable keeps the activity immersive so the system nav bar stays hidden.
            false
        ).apply {
            elevation = 16f
            isOutsideTouchable = true
            isFocusable = false
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            keepNavigationBarHidden(context)
        }

        val rows = linkedMapOf(
            NoteSortOrder.COLOR to view.findViewById<LinearLayout>(R.id.optionSortColor),
            NoteSortOrder.MODIFIED_TIME to view.findViewById(R.id.optionSortModified),
            NoteSortOrder.CREATED_TIME to view.findViewById(R.id.optionSortCreated),
            NoteSortOrder.REMINDER_TIME to view.findViewById(R.id.optionSortReminder),
            NoteSortOrder.ALPHABETICAL to view.findViewById(R.id.optionSortAlphabetical),
            NoteSortOrder.CUSTOM_ORDER to view.findViewById(R.id.optionSortCustom)
        )

        val selectedColor = ContextCompat.getColor(context, R.color.app_primary_color)
        val defaultColor = ContextCompat.getColor(context, R.color.app_txt_black)

        rows.forEach { (order, row) ->
            // The Reminder screen is already reminder-based, so hide that option there.
            if (order == NoteSortOrder.REMINDER_TIME && !showReminderOption) {
                row.visibility = View.GONE
                return@forEach
            }

            val isSelected = order == selectedSort
            (row.getChildAt(0) as? ImageView)?.setColorFilter(
                if (isSelected) selectedColor else defaultColor
            )
            (row.getChildAt(1) as? TextView)?.setTextColor(
                if (isSelected) selectedColor else defaultColor
            )

            row.setOnClickListener {
                popup.dismiss()
                onSortSelected(order)
            }
        }

        val density = context.resources.displayMetrics.density
        val minWidth = (220 * density).toInt()

        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupWidth = view.measuredWidth.coerceAtLeast(minWidth)
        popup.width = popupWidth

        val xOffset = anchor.width - popupWidth
        val yOffset = (6 * density).toInt()
        popup.showAsDropDown(anchor, xOffset, yOffset)
    }
}
