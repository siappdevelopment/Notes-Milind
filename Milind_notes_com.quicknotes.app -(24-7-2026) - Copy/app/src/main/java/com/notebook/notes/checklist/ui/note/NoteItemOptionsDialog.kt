package com.notebook.notes.checklist.ui.note

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.core.view.isVisible
import com.notebook.notes.checklist.databinding.DialogNoteItemOptionsBinding

data class NoteItemOptionsDialogConfig(
    val showShare: Boolean = true,
    val showDelete: Boolean = true
)

data class NoteItemOptionsDialogListener(
    val onShare: (() -> Unit)? = null,
    val onDelete: (() -> Unit)? = null
)

object NoteItemOptionsDialog {

    fun show(
        context: Context,
        config: NoteItemOptionsDialogConfig = NoteItemOptionsDialogConfig(),
        listener: NoteItemOptionsDialogListener
    ) {
        val binding = DialogNoteItemOptionsBinding.inflate(LayoutInflater.from(context))
        val dialog = Dialog(context).apply {
            setContentView(binding.root)
            window?.apply {
                setBackgroundDrawableResource(android.R.color.transparent)
                val margin = (24 * context.resources.displayMetrics.density).toInt()
                setLayout(
                    context.resources.displayMetrics.widthPixels - margin * 2,
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
            }
        }

        binding.optionShare.isVisible = config.showShare
        binding.dividerShare.isVisible = config.showShare && config.showDelete
        binding.optionDelete.isVisible = config.showDelete

        binding.optionShare.setOnClickListener {
            dialog.dismiss()
            listener.onShare?.invoke()
        }
        binding.optionDelete.setOnClickListener {
            dialog.dismiss()
            listener.onDelete?.invoke()
        }

        dialog.show()
    }
}
