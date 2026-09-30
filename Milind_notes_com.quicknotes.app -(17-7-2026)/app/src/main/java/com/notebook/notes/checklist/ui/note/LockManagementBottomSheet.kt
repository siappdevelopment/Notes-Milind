package com.notebook.notes.checklist.ui.note

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.notebook.notes.checklist.databinding.BottomSheetLockManagementBinding

class LockManagementBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetLockManagementBinding? = null
    private val binding get() = _binding!!

    private var onChangePassword: (() -> Unit)? = null
    private var onRemovePassword: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetLockManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.btnChangePassword.setOnClickListener {
            onChangePassword?.invoke()
            dismiss()
        }

        binding.btnRemovePassword.setOnClickListener {
            onRemovePassword?.invoke()
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundResource(android.R.color.transparent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "LockManagementBottomSheet"

        fun show(
            fragmentManager: FragmentManager,
            onChangePassword: () -> Unit,
            onRemovePassword: () -> Unit
        ) {
            val fragment = LockManagementBottomSheet().apply {
                this.onChangePassword = onChangePassword
                this.onRemovePassword = onRemovePassword
            }
            fragment.show(fragmentManager, TAG)
        }
    }
}
