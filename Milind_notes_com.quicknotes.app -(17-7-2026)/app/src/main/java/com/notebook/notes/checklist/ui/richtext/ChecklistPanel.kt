package com.notebook.notes.checklist.ui.richtext

import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.PanelChecklistBinding

class ChecklistPanel(
    private val activity: AppCompatActivity,
    private val binding: PanelChecklistBinding,
    private val onOptionSelected: (ChecklistOption, RichTextEditorController?) -> Unit,
    private val onVisibilityChanged: (Boolean) -> Unit
) {
    private var boundController: RichTextEditorController? = null

    val isVisible: Boolean
        get() = binding.root.isVisible

    init {
        setupOption(
            optionView = binding.btnCheckbox.root,
            iconView = binding.btnCheckbox.ivOptionIcon,
                iconRes = R.drawable.ic_todo_checked_op,
            contentDescriptionRes = R.string.add_checkbox,
            option = ChecklistOption.CHECKBOX
        )
        setupOption(
            optionView = binding.btnLine.root,
            iconView = binding.btnLine.ivOptionIcon,
            iconRes = R.drawable.ic_todo_line,
            contentDescriptionRes = R.string.todo_top_line,
            option = ChecklistOption.TOP_LINE
        )
        setupOption(
            optionView = binding.btnBulletList.root,
            iconView = binding.btnBulletList.ivOptionIcon,
            iconRes = R.drawable.ic_view_list,
            contentDescriptionRes = R.string.bullet_list,
            option = ChecklistOption.BULLET_LIST
        )
        setupOption(
            optionView = binding.btnNumberList.root,
            iconView = binding.btnNumberList.ivOptionIcon,
            iconRes = R.drawable.ic_format_number,
            contentDescriptionRes = R.string.number_list,
            option = ChecklistOption.NUMBER_LIST
        )
        binding.btnClose.setOnClickListener { hide() }
        binding.root.visibility = View.GONE
    }

    fun show(controller: RichTextEditorController?, animate: Boolean = true) {
        boundController = controller
        EditorPanelAnimator.hideKeyboardThen(activity, binding.root) {
            if (boundController != controller) return@hideKeyboardThen
            EditorPanelAnimator.show(binding.root, animate = animate) {
                onVisibilityChanged(true)
            }
        }
    }

    fun hide(animate: Boolean = true) {
        if (!isVisible && boundController == null) return
        EditorPanelAnimator.hide(binding.root, animate = animate) {
            onVisibilityChanged(false)
            boundController = null
        }
    }

    private fun setupOption(
        optionView: View,
        iconView: ImageView,
        iconRes: Int,
        contentDescriptionRes: Int,
        option: ChecklistOption
    ) {
        iconView.setImageResource(iconRes)
        optionView.contentDescription = activity.getString(contentDescriptionRes)
        optionView.setOnClickListener {
            onOptionSelected(option, boundController)
            hide()
        }
    }
}
