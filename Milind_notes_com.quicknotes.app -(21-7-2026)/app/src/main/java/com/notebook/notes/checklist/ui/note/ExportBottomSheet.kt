package com.notebook.notes.checklist.ui.note

import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.notebook.notes.checklist.databinding.BottomSheetExportBinding

object ExportBottomSheet {

    enum class ExportType {
        PDF, TEXT, JPG
    }

    fun show(
        activity: AppCompatActivity,
        onExportSelected: (ExportType) -> Unit
    ) {
        val dialog = BottomSheetDialog(activity)
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
            val bottomSheet = dialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.setBackgroundResource(android.R.color.transparent)
        }
        dialog.show()
    }
}
