package com.notebook.notes.checklist.ui.note

import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.PopupNoteItemOptionsBinding
import com.notebook.notes.checklist.util.keepNavigationBarHidden

data class NoteItemOptionsConfig(
    val showView: Boolean = true,
    val showShare: Boolean = true,
    val showDelete: Boolean = true,
    @StringRes val viewLabelRes: Int = R.string.view_item
)

data class NoteItemOptionsListener(
    val onView: (() -> Unit)? = null,
    val onShare: (() -> Unit)? = null,
    val onDelete: (() -> Unit)? = null
)

object NoteItemOptionsPopup {

    fun show(
        anchor: View,
        config: NoteItemOptionsConfig,
        listener: NoteItemOptionsListener
    ) {
        val context = anchor.context
        val binding = PopupNoteItemOptionsBinding.inflate(LayoutInflater.from(context))
        val density = context.resources.displayMetrics.density
        val minWidth = (160 * density).toInt()

        val popup = PopupWindow(
            binding.root,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            false
        ).apply {
            elevation = 16f
            isOutsideTouchable = true
            isFocusable = false
            setBackgroundDrawable(
                ContextCompat.getDrawable(context, android.R.color.transparent)
            )
            keepNavigationBarHidden(context)
        }

        binding.optionView.isVisible = config.showView
        binding.optionShare.isVisible = config.showShare
        binding.optionDelete.isVisible = config.showDelete
        binding.tvViewLabel.setText(config.viewLabelRes)

        if (config.showView) {
            binding.optionView.setOnClickListener {
                popup.dismiss()
                listener.onView?.invoke()
            }
        }
        if (config.showShare) {
            binding.optionShare.setOnClickListener {
                popup.dismiss()
                listener.onShare?.invoke()
            }
        }
        if (config.showDelete) {
            binding.optionDelete.setOnClickListener {
                popup.dismiss()
                listener.onDelete?.invoke()
            }
        }

        val yOffset = (6 * density).toInt()
        anchor.post {
            binding.root.measure(
                View.MeasureSpec.makeMeasureSpec(minWidth, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )

            val measuredPopupWidth = binding.root.measuredWidth.coerceAtLeast(minWidth)
            val location = IntArray(2)
            anchor.getLocationOnScreen(location)
            val screenWidth = context.resources.displayMetrics.widthPixels
            val margin = (12 * density).toInt()

            var xOff = anchor.width - measuredPopupWidth
            if (location[0] + xOff < margin) {
                xOff = margin - location[0]
            }
            if (location[0] + xOff + measuredPopupWidth > screenWidth - margin) {
                xOff = screenWidth - margin - location[0] - measuredPopupWidth
            }

            popup.showAsDropDown(anchor, xOff, yOffset)
        }
    }
}
