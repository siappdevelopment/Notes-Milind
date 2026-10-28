package com.notebook.notes.checklist.ui.colorpicker

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.DialogAdvancedColorPickerBinding
import kotlinx.coroutines.launch

/**
 * Professional HSV color picker used from the rainbow / custom swatch
 * in [com.notebook.notes.checklist.ui.adapter.ColorPickerAdapter].
 */
class AdvancedColorPickerDialogFragment : DialogFragment() {

    private var _binding: DialogAdvancedColorPickerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdvancedColorPickerViewModel by viewModels()

    private var onColorSelected: ((SelectedColor) -> Unit)? = null
    private var suppressRgbWatchers = false

    companion object {
        const val TAG = "AdvancedColorPicker"
        private const val ARG_INITIAL_COLOR = "initial_color"

        /** Persists last custom pick so reopening shows the same color. */
        @Volatile
        var lastSelectedColorInt: Int = Color.RED
            private set

        fun newInstance(
            initialColorHex: String? = null,
            onColorSelected: (SelectedColor) -> Unit
        ): AdvancedColorPickerDialogFragment {
            val initial = SelectedColor.parse(
                hexOrNull = initialColorHex,
                fallback = lastSelectedColorInt
            ).colorInt
            return AdvancedColorPickerDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_INITIAL_COLOR, initial)
                }
                this.onColorSelected = onColorSelected
            }
        }

        fun show(
            fragmentManager: FragmentManager,
            initialColorHex: String? = null,
            onColorSelected: (SelectedColor) -> Unit
        ) {
            if (fragmentManager.findFragmentByTag(TAG) != null) return
            newInstance(initialColorHex, onColorSelected)
                .show(fragmentManager, TAG)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAdvancedColorPickerBinding.inflate(inflater, container, false)
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

        val initial = arguments?.getInt(ARG_INITIAL_COLOR, lastSelectedColorInt)
            ?: lastSelectedColorInt
        viewModel.initialize(initial)

        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnApply.setOnClickListener { applyAndDismiss() }
        binding.btnEyedropper.setOnClickListener {
            Toast.makeText(
                requireContext(),
                R.string.color_picker_eyedropper_coming_soon,
                Toast.LENGTH_SHORT
            ).show()
        }
        binding.btnColorMode.setOnClickListener {
            // Future: HEX / HSV. RGB only for now.
            Toast.makeText(
                requireContext(),
                R.string.color_picker_mode_rgb_only,
                Toast.LENGTH_SHORT
            ).show()
        }

        setupPickers()
        setupRgbInputs()
        observeState()
    }

    private fun setupPickers() {
        binding.svPicker.listener = object : SaturationValueView.OnSvChangeListener {
            override fun onSvChanged(saturation: Float, value: Float, fromUser: Boolean) {
                if (fromUser) viewModel.setSaturationValue(saturation, value)
            }
        }
        binding.hueSlider.listener = HueSliderView.OnHueChangeListener { hue, fromUser ->
            if (fromUser) viewModel.setHue(hue)
        }
        binding.alphaSlider.listener = AlphaSliderView.OnAlphaChangeListener { alpha, fromUser ->
            if (fromUser) viewModel.setAlpha(alpha)
        }
    }

    private fun setupRgbInputs() {
        binding.etRed.addChannelWatcher { viewModel.setRed(it) }
        binding.etGreen.addChannelWatcher { viewModel.setGreen(it) }
        binding.etBlue.addChannelWatcher { viewModel.setBlue(it) }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    render(state)
                }
            }
        }
    }

    private fun render(state: HsvColorState) {
        val selected = state.toSelectedColor()

        binding.svPicker.setHue(state.hue)
        binding.svPicker.setSaturationValue(state.saturation, state.value)
        binding.hueSlider.setHue(state.hue)
        binding.alphaSlider.setSolidColor(selected.colorInt)
        binding.alphaSlider.setAlphaValue(state.alpha)

        val percent = ((state.alpha / 255f) * 100f).toInt().coerceIn(0, 100)
        binding.tvAlphaPercent.text = getString(R.string.color_picker_alpha_percent, percent)

        suppressRgbWatchers = true
        setTextIfChanged(binding.etRed, selected.red.toString())
        setTextIfChanged(binding.etGreen, selected.green.toString())
        setTextIfChanged(binding.etBlue, selected.blue.toString())
        suppressRgbWatchers = false
    }

    private fun applyAndDismiss() {
        val selected = viewModel.selectedColor()
        lastSelectedColorInt = selected.colorInt
        onColorSelected?.invoke(selected)
        dismiss()
    }

    private fun setTextIfChanged(editText: EditText, value: String) {
        if (editText.text?.toString() != value) {
            editText.setText(value)
            editText.setSelection(editText.text?.length ?: 0)
        }
    }

    private fun EditText.addChannelWatcher(onValue: (Int) -> Unit) {
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (suppressRgbWatchers) return
                val raw = s?.toString()?.trim().orEmpty()
                if (raw.isEmpty()) return
                val value = raw.toIntOrNull()?.coerceIn(0, 255) ?: return
                onValue(value)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
