package com.notebook.notes.checklist.ui.richtext

import android.graphics.Color
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.BottomSheetTextFormattingBinding
import com.notebook.notes.checklist.model.RichTextDocument
import com.notebook.notes.checklist.ui.adapter.FormattingToolAdapter
import com.notebook.notes.checklist.ui.adapter.TextStylePresetAdapter

class TextFormattingPanel(
    private val activity: AppCompatActivity,
    private val binding: BottomSheetTextFormattingBinding,
    private val isNumberListModeEnabled: () -> Boolean,
    private val isBulletListModeEnabled: () -> Boolean,
    private val onStateChanged: () -> Unit,
    private val onVisibilityChanged: (Boolean) -> Unit
) {
    private var boundController: RichTextEditorController? = null
    private var boundHost: RichTextEditorHost? = null
    private var previousFormatListener: (() -> Unit)? = null

    private val styleAdapter = TextStylePresetAdapter { preset ->
        applyAndRefresh { boundController?.setFontSize(preset.fontSizeSp) }
    }

    private val characterAdapter = FormattingToolAdapter { toolId ->
        val controller = boundController ?: return@FormattingToolAdapter
        handleCharacterTool(activity, controller, toolId) {
            refresh()
            onStateChanged()
        }
    }

    private val paragraphAdapter = FormattingToolAdapter { toolId ->
        val controller = boundController ?: return@FormattingToolAdapter
        handleParagraphTool(controller, boundHost, toolId) {
            refresh()
            onStateChanged()
        }
    }

    val isVisible: Boolean
        get() = binding.root.isVisible

    init {
        binding.rvTextStyles.apply {
            layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
            adapter = styleAdapter
            itemAnimator = null
        }
        binding.rvCharacterTools.apply {
            layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
            adapter = characterAdapter
            itemAnimator = null
        }
        binding.rvParagraphTools.apply {
            layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
            adapter = paragraphAdapter
            itemAnimator = null
        }
        binding.btnClose.setOnClickListener { hide() }
        binding.root.visibility = View.GONE
    }

    fun toggle(controller: RichTextEditorController, host: RichTextEditorHost?) {
        if (isVisible && boundController == controller) {
            hide()
            return
        }
        show(controller, host)
    }

    fun show(
        controller: RichTextEditorController,
        host: RichTextEditorHost?,
        animate: Boolean = true
    ) {
        // Keep the user's text selection so bold/color/highlight apply to it.
        controller.rememberSelection()

        if (boundController != controller) {
            detachFormatListener()
            boundController = controller
            boundHost = host
            previousFormatListener = controller.onFormatStateChanged
            controller.onFormatStateChanged = {
                previousFormatListener?.invoke()
                refresh()
            }
        } else {
            boundController = controller
            boundHost = host
        }

        // Keep blur suppressed via toolbar binder; reveal panel only after keyboard is gone.
        refresh()
        EditorPanelAnimator.hideKeyboardThen(activity, binding.root) {
            if (boundController != controller) return@hideKeyboardThen
            EditorPanelAnimator.show(binding.root, animate = animate) {
                onVisibilityChanged(true)
                refresh()
            }
        }
    }

    fun hide(animate: Boolean = true, refocusEditor: Boolean = true) {
        if (!isVisible && boundController == null) return
        val controller = boundController
        EditorPanelAnimator.hide(binding.root, animate = animate) {
            onVisibilityChanged(false)
            detachFormatListener()
            boundController = null
            boundHost = null
            if (refocusEditor) controller?.focusEditor()
        }
    }

    private fun detachFormatListener() {
        val controller = boundController ?: return
        controller.onFormatStateChanged = previousFormatListener
        previousFormatListener = null
    }

    private fun applyAndRefresh(action: () -> Unit) {
        action()
        refresh()
        onStateChanged()
    }

    private fun refresh() {
        val controller = boundController ?: return
        refreshPanel(
            binding = binding,
            controller = controller,
            isNumberListModeEnabled = isNumberListModeEnabled(),
            isBulletListModeEnabled = isBulletListModeEnabled()
        )
    }

    private fun refreshPanel(
        binding: BottomSheetTextFormattingBinding,
        controller: RichTextEditorController,
        isNumberListModeEnabled: Boolean,
        isBulletListModeEnabled: Boolean
    ) {
        val state = controller.getToolbarState()
        val currentPreset = TextStylePreset.fromFontSize(controller.getCurrentFontSize())
        val alignment = controller.getCurrentAlignment()

        styleAdapter.submitPresets(
            TextStylePreset.entries.map { preset ->
                TextStylePresetAdapter.PresetItem(
                    preset = preset,
                    label = binding.root.context.getString(preset.labelRes),
                    isSelected = preset == currentPreset
                )
            }
        )

        characterAdapter.submitTools(buildCharacterTools(state, controller))
        paragraphAdapter.submitTools(
            buildParagraphTools(alignment, isBulletListModeEnabled, isNumberListModeEnabled)
        )
    }

    private fun buildCharacterTools(
        state: RichTextToolbarState,
        controller: RichTextEditorController
    ): List<FormattingToolItem> {
        val textColor = controller.getCurrentTextColor()
        val highlightColor = controller.getCurrentHighlightColor()

        return listOf(
            FormattingToolItem(FormattingToolId.BOLD, iconRes = R.drawable.ic_b, isSelected = state.bold, contentDescriptionRes = R.string.format_bold),
            FormattingToolItem(FormattingToolId.ITALIC, iconRes = R.drawable.ic_italic, isSelected = state.italic, contentDescriptionRes = R.string.format_italic),
            FormattingToolItem(FormattingToolId.UNDERLINE, iconRes = R.drawable.ic_underline, isSelected = state.underline, contentDescriptionRes = R.string.format_underline),
            FormattingToolItem(FormattingToolId.STRIKETHROUGH, iconRes = R.drawable.ic_s, isSelected = state.strikethrough, contentDescriptionRes = R.string.format_strikethrough),
            FormattingToolItem(
                FormattingToolId.TEXT_COLOR,
                contentDescriptionRes = R.string.text_color,
                color = Color.parseColor(textColor)
            ),
            FormattingToolItem(
                FormattingToolId.HIGHLIGHT,
                isSelected = state.highlight,
                contentDescriptionRes = R.string.highlight_color,
                color = if (highlightColor != null) Color.parseColor(highlightColor) else Color.TRANSPARENT
            )
        )
    }

    private fun buildParagraphTools(
        alignment: String,
        isBulletListModeEnabled: Boolean,
        isNumberListModeEnabled: Boolean
    ): List<FormattingToolItem> = listOf(
        FormattingToolItem(FormattingToolId.ALIGN_LEFT, iconRes = R.drawable.ic_align_left, isSelected = alignment == RichTextDocument.ALIGN_LEFT, contentDescriptionRes = R.string.align_left),
        FormattingToolItem(FormattingToolId.ALIGN_CENTER, iconRes = R.drawable.ic_align_center, isSelected = alignment == RichTextDocument.ALIGN_CENTER, contentDescriptionRes = R.string.align_center),
        FormattingToolItem(FormattingToolId.ALIGN_RIGHT, iconRes = R.drawable.ic_align_right, isSelected = alignment == RichTextDocument.ALIGN_RIGHT, contentDescriptionRes = R.string.align_right),
        FormattingToolItem(FormattingToolId.ALIGN_JUSTIFY, iconRes = R.drawable.ic_align_justify, isSelected = alignment == RichTextDocument.ALIGN_JUSTIFY, contentDescriptionRes = R.string.align_justify),
        FormattingToolItem(FormattingToolId.BULLET_LIST, iconRes = R.drawable.ic_format_bullet, isSelected = isBulletListModeEnabled, contentDescriptionRes = R.string.bullet_list),
            FormattingToolItem(FormattingToolId.NUMBER_LIST, iconRes = R.drawable.ic_format_number, isSelected = isNumberListModeEnabled, contentDescriptionRes = R.string.number_list)
    )

    private fun handleCharacterTool(
        activity: AppCompatActivity,
        controller: RichTextEditorController,
        toolId: FormattingToolId,
        onApplied: () -> Unit
    ) {
        when (toolId) {
            FormattingToolId.BOLD -> {
                controller.toggleBold()
                onApplied()
            }
            FormattingToolId.ITALIC -> {
                controller.toggleItalic()
                onApplied()
            }
            FormattingToolId.UNDERLINE -> {
                controller.toggleUnderline()
                onApplied()
            }
            FormattingToolId.STRIKETHROUGH -> {
                controller.toggleStrikethrough()
                onApplied()
            }
            FormattingToolId.HIGHLIGHT -> showColorPicker(
                activity = activity,
                title = activity.getString(R.string.highlight_color),
                colors = RichTextFormats.highlightColors
            ) { color ->
                controller.setHighlightColor(color)
                onApplied()
            }
            FormattingToolId.TEXT_COLOR -> showColorPicker(
                activity = activity,
                title = activity.getString(R.string.text_color),
                colors = RichTextFormats.textColors
            ) { color ->
                controller.setTextColor(color ?: "#000000")
                onApplied()
            }
            else -> Unit
        }
    }

    private fun handleParagraphTool(
        controller: RichTextEditorController,
        host: RichTextEditorHost?,
        toolId: FormattingToolId,
        onApplied: () -> Unit
    ) {
        when (toolId) {
            FormattingToolId.ALIGN_LEFT -> controller.setAlignment(RichTextDocument.ALIGN_LEFT)
            FormattingToolId.ALIGN_CENTER -> controller.setAlignment(RichTextDocument.ALIGN_CENTER)
            FormattingToolId.ALIGN_RIGHT -> controller.setAlignment(RichTextDocument.ALIGN_RIGHT)
            FormattingToolId.ALIGN_JUSTIFY -> controller.setAlignment(RichTextDocument.ALIGN_JUSTIFY)
            FormattingToolId.BULLET_LIST -> host?.onBulletListAction(controller)
            FormattingToolId.NUMBER_LIST -> host?.onNumberListAction(controller)
            else -> return
        }
        onApplied()
    }

    private fun showColorPicker(
        activity: AppCompatActivity,
        title: String,
        colors: List<String>,
        onColorSelected: (String?) -> Unit
    ) {
        val currentColor = when (title) {
            activity.getString(R.string.text_color) -> boundController?.getCurrentTextColor()
            activity.getString(R.string.highlight_color) -> boundController?.getCurrentHighlightColor()
            else -> null
        }

        ColorPickerDialogFragment.newInstance(
            title = activity.getString(R.string.choose_the_color),
            colors = colors,
            selectedColor = currentColor
        ) { color ->
            onColorSelected(color)
        }.show(activity.supportFragmentManager, "ColorPicker")
    }
}
