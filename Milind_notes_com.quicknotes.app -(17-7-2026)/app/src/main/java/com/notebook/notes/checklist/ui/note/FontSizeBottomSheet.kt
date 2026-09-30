package com.notebook.notes.checklist.ui.note

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.PanelFontSizeBinding
import com.notebook.notes.checklist.ui.richtext.RichTextFormats
import com.notebook.notes.checklist.util.BottomSheetHelper
import com.notebook.notes.checklist.util.SystemBars
import com.notebook.notes.checklist.utils.Constant

class FontSizeBottomSheet : BottomSheetDialogFragment() {

    private var _binding: PanelFontSizeBinding? = null
    private val binding get() = _binding!!

    private var selectedFontSize: Int = Constant.DEFAULT_FONT_SIZE
    private var onFontSizeSelected: ((Int) -> Unit)? = null

    private val fontSizes = RichTextFormats.fontSizes

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PanelFontSizeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedFontSize = arguments?.getInt(ARG_FONT_SIZE, Constant.DEFAULT_FONT_SIZE)
            ?: Constant.DEFAULT_FONT_SIZE

        updateFontSizeText(selectedFontSize)

        val initialIndex = fontSizes.indexOfFirst { it == selectedFontSize }
            .takeIf { it >= 0 } ?: fontSizes.indexOfFirst { it >= selectedFontSize }.coerceAtLeast(0)

        binding.sliderFontSize.max = fontSizes.size - 1
        binding.sliderFontSize.progress = initialIndex

        binding.sliderFontSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedFontSize = fontSizes[progress]
                updateFontSizeText(selectedFontSize)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.btnDone.setOnClickListener {
            onFontSizeSelected?.invoke(selectedFontSize)
            dismiss()
        }
    }

    private fun updateFontSizeText(size: Int) {
        binding.tvFontSizeValue.text = getString(R.string.font_size_px, size)
    }

    override fun onStart() {
        super.onStart()
        // Theme disables system-bar padding; hide nav bar so Done stays visible.
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
        private const val TAG = "FontSizeBottomSheet"
        private const val ARG_FONT_SIZE = "arg_font_size"

        fun show(
            fragmentManager: FragmentManager,
            currentFontSize: Int,
            onSelected: (Int) -> Unit
        ) {
            val fragment = FontSizeBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt(ARG_FONT_SIZE, currentFontSize)
                }
                onFontSizeSelected = onSelected
            }
            fragment.show(fragmentManager, TAG)
        }
    }
}
