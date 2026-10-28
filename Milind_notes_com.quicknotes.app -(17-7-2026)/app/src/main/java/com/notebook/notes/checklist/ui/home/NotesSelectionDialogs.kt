package com.notebook.notes.checklist.ui.home

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.notebook.notes.checklist.databinding.DialogConfirmationBinding
import com.notebook.notes.checklist.util.SystemBars

object NotesSelectionDialogs {

    fun showConfirmation(
        fragment: Fragment,
        titleRes: Int,
        messageRes: Int,
        positiveButtonRes: Int,
        iconRes: Int,
        positiveButtonColorRes: Int? = null,
        onPositiveClick: () -> Unit
    ) {
        showConfirmation(
            context = fragment.requireContext(),
            layoutInflater = fragment.layoutInflater,
            titleRes = titleRes,
            messageRes = messageRes,
            positiveButtonRes = positiveButtonRes,
            iconRes = iconRes,
            positiveButtonColorRes = positiveButtonColorRes,
            onPositiveClick = onPositiveClick
        )
    }

    fun showConfirmation(
        context: Context,
        layoutInflater: LayoutInflater,
        titleRes: Int,
        messageRes: Int,
        positiveButtonRes: Int,
        iconRes: Int,
        positiveButtonColorRes: Int? = null,
        onPositiveClick: () -> Unit
    ) {
        val dialogBinding = DialogConfirmationBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogBinding.root)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogBinding.apply {
            ivIcon.setImageResource(iconRes)
            tvTitle.setText(titleRes)
            tvMessage.setText(messageRes)
            btnPositive.setText(positiveButtonRes)
            btnPositive.maxLines = 2
            btnPositive.isSingleLine = false
            btnPositive.gravity = android.view.Gravity.CENTER

            if (positiveButtonColorRes != null) {
                val color = ContextCompat.getColor(context, positiveButtonColorRes)
                val colorStateList = ColorStateList.valueOf(color)
                ivIcon.imageTintList = colorStateList
                btnPositive.backgroundTintList = colorStateList
                btnNegative.setTextColor(color)

                val alphaColor = ColorUtils.setAlphaComponent(color, 26) // ~10% alpha
                btnNegative.backgroundTintList = ColorStateList.valueOf(alphaColor)
                btnNegative.rippleColor = ColorStateList.valueOf(alphaColor)
            }

            btnNegative.setOnClickListener { dialog.dismiss() }
            btnPositive.setOnClickListener {
                onPositiveClick()
                dialog.dismiss()
            }
        }
        dialog.setOnShowListener {
            SystemBars.hideNavigationBar(dialog)
        }
        dialog.setOnDismissListener {
            SystemBars.hideNavigationBar(context)
        }
        dialog.show()
        SystemBars.hideNavigationBar(dialog)
    }
}
