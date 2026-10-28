package com.notebook.notes.checklist.ui.note

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.notebook.notes.checklist.databinding.PanelFontStyleBinding
import com.notebook.notes.checklist.ui.adapter.FontStyleAdapter
import com.notebook.notes.checklist.ui.richtext.RichTextFormats
import com.notebook.notes.checklist.util.BottomSheetHelper
import com.notebook.notes.checklist.util.SystemBars
import com.notebook.notes.checklist.utils.Constant

class FontStyleBottomSheet : BottomSheetDialogFragment() {

    private var _binding: PanelFontStyleBinding? = null
    private val binding get() = _binding!!

    private var selectedFontFamily: String = Constant.DEFAULT_FONT_FAMILY
    private var onFontStyleSelected: ((String) -> Unit)? = null

    private val adapter = FontStyleAdapter { option ->
        selectedFontFamily = option.id
        onFontStyleSelected?.invoke(option.id)
        dismiss()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PanelFontStyleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedFontFamily = arguments?.getString(ARG_FONT_FAMILY)
            ?.takeIf { family -> RichTextFormats.fontStyles.any { it.id == family } }
            ?: Constant.DEFAULT_FONT_FAMILY

        binding.rvFontStyles.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = this@FontStyleBottomSheet.adapter
            itemAnimator = null
        }

        refreshList()

        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun refreshList() {
        adapter.submitList(
            RichTextFormats.fontStyles.map { option ->
                FontStyleAdapter.StyleItem(
                    option = option,
                    isSelected = option.id == selectedFontFamily
                )
            }
        )
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
        private const val TAG = "FontStyleBottomSheet"
        private const val ARG_FONT_FAMILY = "arg_font_family"

        fun show(
            fragmentManager: FragmentManager,
            currentFontFamily: String,
            onSelected: (String) -> Unit
        ) {
            val fragment = FontStyleBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_FONT_FAMILY, currentFontFamily)
                }
                onFontStyleSelected = onSelected
            }
            fragment.show(fragmentManager, TAG)
        }
    }
}
