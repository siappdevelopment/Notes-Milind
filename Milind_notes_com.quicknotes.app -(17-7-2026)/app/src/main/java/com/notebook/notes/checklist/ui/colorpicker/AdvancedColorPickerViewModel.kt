package com.notebook.notes.checklist.ui.colorpicker

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Holds synchronized HSV / RGB / Alpha state for the advanced color picker.
 * Avoids bitmap work here — views cache their own drawables.
 */
class AdvancedColorPickerViewModel : ViewModel() {

    private val _state = MutableStateFlow(HsvColorState())
    val state: StateFlow<HsvColorState> = _state.asStateFlow()

    private val _colorMode = MutableStateFlow(ColorMode.RGB)
    val colorMode: StateFlow<ColorMode> = _colorMode.asStateFlow()

    fun initialize(initialColor: Int) {
        _state.value = HsvColorState.fromColorInt(initialColor)
    }

    fun setHue(hue: Float) {
        _state.update { it.copy(hue = hue.coerceIn(0f, 360f)) }
    }

    fun setSaturationValue(saturation: Float, value: Float) {
        _state.update {
            it.copy(
                saturation = saturation.coerceIn(0f, 1f),
                value = value.coerceIn(0f, 1f)
            )
        }
    }

    fun setAlpha(alpha: Int) {
        _state.update { it.copy(alpha = alpha.coerceIn(0, 255)) }
    }

    fun setAlphaPercent(percent: Int) {
        setAlpha(((percent.coerceIn(0, 100) / 100f) * 255f).toInt())
    }

    fun setRgb(red: Int, green: Int, blue: Int) {
        _state.update { it.withRgb(red, green, blue) }
    }

    fun setRed(red: Int) {
        val current = _state.value.toSelectedColor()
        setRgb(red, current.green, current.blue)
    }

    fun setGreen(green: Int) {
        val current = _state.value.toSelectedColor()
        setRgb(current.red, green, current.blue)
    }

    fun setBlue(blue: Int) {
        val current = _state.value.toSelectedColor()
        setRgb(current.red, current.green, blue)
    }

    fun setColorMode(mode: ColorMode) {
        // HEX / HSV modes reserved for future — keep RGB active for now.
        if (mode == ColorMode.RGB) {
            _colorMode.value = mode
        }
    }

    fun selectedColor(): SelectedColor = _state.value.toSelectedColor()

    enum class ColorMode {
        RGB,
        HEX,
        HSV
    }
}
