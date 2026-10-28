package com.notebook.notes.checklist.ui.note

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.notebook.notes.checklist.databinding.BottomSheetExportBinding
import com.notebook.notes.checklist.util.BottomSheetHelper
import com.notebook.notes.checklist.util.SystemBars

object ExportBottomSheet {

    enum class ExportType {
        PDF, TEXT, JPG
    }

    fun show(
        activity: AppCompatActivity,
        onExportSelected: (ExportType) -> Unit
    ) {
        val dialog = BottomSheetHelper.create(activity)
        val binding = BottomSheetExportBinding.inflate(LayoutInflater.from(activity))

        binding.btnClose.setOnClickListener {
            dialog.dismiss()
        }

        binding.btnExportPdf.setOnClickListener {
            onExportSelected(ExportType.PDF)
            dialog.dismiss()
        }

        binding.btnExportText.setOnClickListener {
            onExportSelected(ExportType.TEXT)
            dialog.dismiss()
        }

        binding.btnExportJpg.setOnClickListener {
            onExportSelected(ExportType.JPG)
            dialog.dismiss()
        }

        dialog.setContentView(binding.root)
        dialog.setOnShowListener {
            BottomSheetHelper.prepare(dialog)
        }
        dialog.setOnDismissListener {
            SystemBars.hideNavigationBar(activity)
        }
        dialog.show()
    }
}
