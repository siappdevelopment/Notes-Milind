package com.notebook.notes.checklist.ui.richtext

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.notebook.notes.checklist.databinding.DialogColorPickerBinding
import com.notebook.notes.checklist.ui.adapter.ColorPickerAdapter
import com.notebook.notes.checklist.ui.colorpicker.AdvancedColorPickerDialogFragment
import com.notebook.notes.checklist.ui.colorpicker.SelectedColor

class ColorPickerDialogFragment : DialogFragment() {

    private var _binding: DialogColorPickerBinding? = null
    private val binding get() = _binding!!

    private var onColorSelected: ((String?) -> Unit)? = null
    private var titleText: String? = null
    private var colorList: List<String> = emptyList()
    private var initialColor: String? = null

    companion object {
        fun newInstance(
            title: String,
            colors: List<String>,
            selectedColor: String?,
            onColorSelected: (String?) -> Unit
        ): ColorPickerDialogFragment {
            return ColorPickerDialogFragment().apply {
                this.titleText = title
                this.colorList = colors
                this.initialColor = selectedColor
                this.onColorSelected = onColorSelected
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogColorPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvTitle.text = titleText
        binding.btnClose.setOnClickListener { dismiss() }

        val adapter = ColorPickerAdapter(
            colors = colorList,
            selectedColor = initialColor,
            onColorClick = { color ->
                onColorSelected?.invoke(color)
                dismiss()
            },
            onCustomClick = { openAdvancedColorPicker() }
        )
        binding.rvColors.adapter = adapter
    }

    private fun openAdvancedColorPicker() {
        AdvancedColorPickerDialogFragment.show(
            fragmentManager = parentFragmentManager,
            initialColorHex = initialColor
        ) { selected: SelectedColor ->
            // Prefer opaque RGB hex for text/highlight compatibility with presets.
            val hex = SelectedColor.toRgbHex(selected.colorInt)
            onColorSelected?.invoke(hex)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
