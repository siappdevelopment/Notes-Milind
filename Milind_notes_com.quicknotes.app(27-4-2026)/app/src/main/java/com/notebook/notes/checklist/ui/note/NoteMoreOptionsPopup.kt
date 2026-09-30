package com.notebook.notes.checklist.ui.note

import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.PopupNoteMoreOptionsBinding
import com.notebook.notes.checklist.util.keepNavigationBarHidden

data class NoteMoreOptionsListener(
    val onAddReminder: (() -> Unit)? = null,
    val onArchive: (() -> Unit)? = null,
    val onLock: (() -> Unit)? = null,
    val onBookmarkNote: (() -> Unit)? = null,
    val onReadingMode: (() -> Unit)? = null,
    val onSearch: (() -> Unit)? = null,
    val onCopy: (() -> Unit)? = null,
    val onExport: (() -> Unit)? = null,
    val onNoteDetails: (() -> Unit)? = null,
    val onPin: (() -> Unit)? = null,
    val onShare: (() -> Unit)? = null,
    val onDelete: (() -> Unit)? = null
)

data class NoteMoreOptionsConfig(
    val isArchived: Boolean = false,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isReadingMode: Boolean = false,
    val isLocked: Boolean = false
)

object NoteMoreOptionsPopup {

    fun show(
        anchor: View,
        listener: NoteMoreOptionsListener,
        config: NoteMoreOptionsConfig = NoteMoreOptionsConfig()
    ) {
        val context = anchor.context
        val binding = PopupNoteMoreOptionsBinding.inflate(LayoutInflater.from(context))
        val density = context.resources.displayMetrics.density
        val minWidth = (200 * density).toInt()
        val maxHeight = (420 * density).toInt()

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

        bindOption(
            root = binding.optionAddReminder.root,
            iconRes = R.drawable.ic_op_reminder,
            labelRes = R.string.add_reminder,
            onClick = listener.onAddReminder,
            popup = popup
        )
        bindOption(
            root = binding.optionArchive.root,
            iconRes = if (config.isArchived) R.drawable.ic_un_arhive else R.drawable.ic_op_archive,
            labelRes = if (config.isArchived) R.string.unarchive_note else R.string.archive_note,
            onClick = listener.onArchive,
            popup = popup
        )
        bindOption(
            root = binding.optionLock.root,
            iconRes = if (config.isLocked) R.drawable.ic_unlock else R.drawable.ic_lock,
            labelRes = if (config.isLocked) R.string.unlock_note else R.string.lock_note,
            onClick = listener.onLock,
            popup = popup
        )
        bindOption(
            root = binding.optionBookmarkNote.root,
            iconRes = if (config.isFavorite) R.drawable.ic_unbookmark else R.drawable.ic_op_bookmark,
            labelRes = if (config.isFavorite) R.string.remove_bookmark else R.string.bookmark_note,
            onClick = listener.onBookmarkNote,
            popup = popup
        )
        bindOption(
            root = binding.optionReadingMode.root,
            iconRes = if (config.isReadingMode) R.drawable.ic_edit_only   else R.drawable.ic_reading_mode,
            labelRes = if (config.isReadingMode) R.string.edit_note else R.string.reading_mode,
            onClick = listener.onReadingMode,
            popup = popup
        )
        bindOption(
            root = binding.optionSearch.root,
            iconRes = R.drawable.ic_op_search,
            labelRes = R.string.search,
            onClick = listener.onSearch,
            popup = popup
        )
        bindOption(
            root = binding.optionCopy.root,
            iconRes = R.drawable.ic_lock,
            labelRes = R.string.copy,
            onClick = listener.onCopy,
            popup = popup
        )
        bindOption(
            root = binding.optionExport.root,
            iconRes = R.drawable.ic_export,
            labelRes = R.string.export,
            onClick = listener.onExport,
            popup = popup
        )
        bindOption(
            root = binding.optionNoteDetails.root,
            iconRes = R.drawable.ic_note_details,
            labelRes = R.string.note_details,
            onClick = listener.onNoteDetails,
            popup = popup
        )
        bindOption(
            root = binding.optionPin.root,
            iconRes = if (config.isPinned) R.drawable.ic_unpin else R.drawable.ic_op_pin,
            labelRes = if (config.isPinned) R.string.unpin_note else R.string.pin_note,
            onClick = listener.onPin,
            popup = popup
        )
        bindOption(
            root = binding.optionShare.root,
            iconRes = R.drawable.ic_share,
            labelRes = R.string.share,
            onClick = listener.onShare,
            popup = popup
        )
        bindOption(
            root = binding.optionDelete.root,
            iconRes = R.drawable.ic_delete,
            labelRes = R.string.delete,
            onClick = listener.onDelete,
            popup = popup,
            isDestructive = true
        )

        val yOffset = (6 * density).toInt()
        anchor.post {
            binding.root.measure(
                View.MeasureSpec.makeMeasureSpec(minWidth, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST)
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

    private fun bindOption(
        root: View,
        @DrawableRes iconRes: Int,
        @StringRes labelRes: Int,
        onClick: (() -> Unit)?,
        popup: PopupWindow,
        isDestructive: Boolean = false
    ) {
        root.findViewById<ImageView>(R.id.ivIcon).setImageResource(iconRes)
        val label = root.findViewById<TextView>(R.id.tvLabel)
        label.setText(labelRes)
        if (isDestructive) {
            val destructiveColor = ContextCompat.getColor(root.context, R.color.audio_cancel_icon)
            label.setTextColor(destructiveColor)
            root.findViewById<ImageView>(R.id.ivIcon).setColorFilter(destructiveColor)
        }
        root.setOnClickListener {
            popup.dismiss()
            onClick?.invoke()
        }
    }
}
