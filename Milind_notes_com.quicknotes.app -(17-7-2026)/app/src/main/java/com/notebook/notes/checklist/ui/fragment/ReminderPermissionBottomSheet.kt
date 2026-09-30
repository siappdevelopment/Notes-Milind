package com.notebook.notes.checklist.ui.fragment

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.notebook.notes.checklist.databinding.DialogReminderPermissionBinding
import com.notebook.notes.checklist.util.BottomSheetHelper
import com.notebook.notes.checklist.util.SystemBars
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReminderPermissionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogReminderPermissionBinding? = null
    private val binding get() = _binding!!

    var onAllowClicked: (() -> Unit)? = null
    var onMaybeLaterClicked: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogReminderPermissionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAllow.setOnClickListener {
            onAllowClicked?.invoke()
            dismiss()
        }

        binding.btnMaybeLater.setOnClickListener {
            onMaybeLaterClicked?.invoke()
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        BottomSheetHelper.prepare(dialog)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        SystemBars.onDialogFragmentDismissed(activity?.window)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "ReminderPermissionBottomSheet"

        fun show(
            fragmentManager: FragmentManager,
            onAllow: () -> Unit,
            onMaybeLater: (() -> Unit)? = null
        ) {
            if (fragmentManager.findFragmentByTag(TAG) != null) return
            ReminderPermissionBottomSheet().apply {
                onAllowClicked = onAllow
                onMaybeLaterClicked = onMaybeLater
            }.show(fragmentManager, TAG)
        }
    }
}
