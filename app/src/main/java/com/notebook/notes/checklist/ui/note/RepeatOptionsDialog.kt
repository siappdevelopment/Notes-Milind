package com.notebook.notes.checklist.ui.note

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.notebook.notes.checklist.databinding.DialogRepeatOptionsBinding
import com.notebook.notes.checklist.util.SystemBars

class RepeatOptionsDialog : DialogFragment() {

    private var _binding: DialogRepeatOptionsBinding? = null
    private val binding get() = _binding!!

    var onRepeatSelected: ((String) -> Unit)? = null
    private var currentRepeat: String = "Never"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogRepeatOptionsBinding.inflate(inflater, container, false)
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            requestFeature(Window.FEATURE_NO_TITLE)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set initial state
        when (currentRepeat) {
            "Never" -> binding.rbNever.isChecked = true
            "Daily" -> binding.rbDaily.isChecked = true
            "Weekly" -> binding.rbWeekly.isChecked = true
            "Monthly" -> binding.rbMonthly.isChecked = true
        }

        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnSet.setOnClickListener {
            val selectedRepeat = when (binding.rgRepeat.checkedRadioButtonId) {
                binding.rbNever.id -> "Never"
                binding.rbDaily.id -> "Daily"
                binding.rbWeekly.id -> "Weekly"
                binding.rbMonthly.id -> "Monthly"
                else -> "Never"
            }
            onRepeatSelected?.invoke(selectedRepeat)
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        SystemBars.onDialogFragmentStarted(dialog)
        dialog?.window?.apply {
            setLayout(
                (resources.displayMetrics.widthPixels * 0.85).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.CENTER)
        }
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        SystemBars.onDialogFragmentDismissed(activity?.window)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "RepeatOptionsDialog"

        fun show(
            fragmentManager: FragmentManager,
            currentRepeat: String,
            onSelected: (String) -> Unit
        ) {
            RepeatOptionsDialog().apply {
                this.currentRepeat = currentRepeat
                this.onRepeatSelected = onSelected
            }.show(fragmentManager, TAG)
        }
    }
}
