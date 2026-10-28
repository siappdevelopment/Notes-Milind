package com.notebook.notes.checklist.iap

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.notebook.notes.checklist.databinding.DialogPurchaseBinding
import com.notebook.notes.checklist.util.SystemBars

/**
 * Purchase-only dialog that reuses dialog_purchase.xml.
 * The Watch Ad section (OR + button + footer) is always hidden here.
 */
object PurchaseDialog {

    fun show(
        context: Context,
        layoutInflater: LayoutInflater,
        onContinue: () -> Unit,
        onCancel: (() -> Unit)? = null,
        @StringRes titleRes: Int? = null,
        @StringRes messageRes: Int? = null,
        @StringRes continueRes: Int? = null,
    ) {
        val binding = DialogPurchaseBinding.inflate(layoutInflater)

        titleRes?.let { binding.tvTitle.setText(it) }
        messageRes?.let { binding.tvMessage.setText(it) }

        // Reuse layout; hide Watch Ad block for purchase-only flow
        binding.llWatchAdSection.visibility = View.GONE

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(binding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        binding.btnCancel.setOnClickListener {
            onCancel?.invoke()
            dialog.dismiss()
        }
        binding.btnUpgrade.setOnClickListener {
            onContinue()
            dialog.dismiss()
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
