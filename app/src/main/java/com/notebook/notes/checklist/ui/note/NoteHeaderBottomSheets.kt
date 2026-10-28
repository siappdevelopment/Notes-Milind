package com.notebook.notes.checklist.ui.note

import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.notebook.notes.checklist.databinding.BottomSheetAttachmentBinding
import com.notebook.notes.checklist.util.BottomSheetHelper
import com.notebook.notes.checklist.util.SystemBars

enum class AttachmentOption {
    AUDIO,
    IMAGE,
    DOODLE,
    FILE,
    LINK
}

object NoteHeaderBottomSheets {

    private var attachmentSheet: com.google.android.material.bottomsheet.BottomSheetDialog? = null

    fun showAttachmentSheet(
        activity: AppCompatActivity,
        onSheetLaidOut: ((Int) -> Unit)? = null,
        onDismissed: (() -> Unit)? = null,
        onOptionSelected: (AttachmentOption) -> Unit
    ) {
        if (attachmentSheet?.isShowing == true) return

        val dialog = BottomSheetHelper.create(activity)
        attachmentSheet = dialog
        val binding = BottomSheetAttachmentBinding.inflate(LayoutInflater.from(activity))
        dialog.setContentView(binding.root)

        binding.btnClose.setOnClickListener { dialog.dismiss() }

        binding.btnInsertAudio.setOnClickListener {
            onOptionSelected(AttachmentOption.AUDIO)
            dialog.dismiss()
        }

        binding.btnAddAttachment.setOnClickListener {
            onOptionSelected(AttachmentOption.IMAGE)
            dialog.dismiss()
        }

        binding.btnInsertDoodle.setOnClickListener {
            onOptionSelected(AttachmentOption.DOODLE)
            dialog.dismiss()
        }

        binding.btnInsertFiles.setOnClickListener {
            onOptionSelected(AttachmentOption.FILE)
            dialog.dismiss()
        }

        binding.btnInsertLink.setOnClickListener {
            onOptionSelected(AttachmentOption.LINK)
            dialog.dismiss()
        }

        dialog.setOnShowListener {
            BottomSheetHelper.prepare(dialog)
            val bottomSheet = dialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.post {
                val height = bottomSheet.height.takeIf { it > 0 }
                    ?: binding.root.height
                if (height > 0) onSheetLaidOut?.invoke(height)
            }
        }
        dialog.setOnDismissListener {
            SystemBars.hideNavigationBar(activity)
            if (attachmentSheet === dialog) attachmentSheet = null
            onDismissed?.invoke()
        }

        dialog.show()
    }
}
