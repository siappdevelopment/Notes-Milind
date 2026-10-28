package com.notebook.notes.checklist.ui.drawing

import com.notebook.notes.checklist.databinding.PanelPhotoOptionsBinding

class PhotoPanelBinder(
    private val binding: PanelPhotoOptionsBinding,
    private val onClose: () -> Unit,
    private val onTakePhoto: () -> Unit,
    private val onChooseFromGallery: () -> Unit
) {

    fun bind() {
        binding.btnClose.setOnClickListener { onClose() }
        binding.btnTakePhoto.setOnClickListener {
            onTakePhoto()
        }
        binding.btnChooseGallery.setOnClickListener {
            onChooseFromGallery()
        }
    }
}
