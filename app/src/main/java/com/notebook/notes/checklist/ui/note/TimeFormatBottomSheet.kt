package com.notebook.notes.checklist.ui.note

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.PanelTimeFormatBinding
import com.notebook.notes.checklist.util.BottomSheetHelper
import com.notebook.notes.checklist.util.SystemBars
import com.notebook.notes.checklist.utils.Constant

class TimeFormatBottomSheet : BottomSheetDialogFragment() {

    private var _binding: PanelTimeFormatBinding? = null
    private val binding get() = _binding!!

    private var selectedFormat: String = Constant.TIME_FORMAT_12
    private var onFormatSelected: ((String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PanelTimeFormatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedFormat = arguments?.getString(ARG_TIME_FORMAT, Constant.TIME_FORMAT_12)
            ?: Constant.TIME_FORMAT_12

        updateUI()

        binding.layout12Hours.setOnClickListener {
            selectedFormat = Constant.TIME_FORMAT_12
            onFormatSelected?.invoke(selectedFormat)
            dismiss()
        }

        binding.layout24Hours.setOnClickListener {
            selectedFormat = Constant.TIME_FORMAT_24
            onFormatSelected?.invoke(selectedFormat)
            dismiss()
        }

        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun updateUI() {
        if (selectedFormat == Constant.TIME_FORMAT_24) {
            binding.iv24Hours.setImageResource(R.drawable.ic_radio_checked)
            binding.iv12Hours.setImageResource(R.drawable.ic_time_redio)
        } else {
            binding.iv24Hours.setImageResource(R.drawable.ic_radio_unchecked)
            binding.iv12Hours.setImageResource(R.drawable.ic_time_redio)
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
        private const val TAG = "TimeFormatBottomSheet"
        private const val ARG_TIME_FORMAT = "arg_time_format"

        fun show(
            fragmentManager: FragmentManager,
            currentTimeFormat: String,
            onSelected: (String) -> Unit
        ) {
            val fragment = TimeFormatBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_TIME_FORMAT, currentTimeFormat)
                }
                onFormatSelected = onSelected
            }
            fragment.show(fragmentManager, TAG)
        }
    }
}
