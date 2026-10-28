package com.notebook.notes.checklist.ui.richtext

import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.notebook.notes.checklist.R

object RichTextBottomSheets {

  fun showTextSizeSheet(
    activity: AppCompatActivity,
    currentSize: Int,
    onSizeSelected: (Int) -> Unit
  ) {
    val sizes = RichTextFormats.fontSizes
    val labels = sizes.map { "$it sp" }.toTypedArray()
    val currentIndex = sizes.indexOf(currentSize).coerceAtLeast(0)

    MaterialAlertDialogBuilder(activity)
      .setTitle(R.string.font_size)
      .setSingleChoiceItems(labels, currentIndex) { dialog, which ->
        onSizeSelected(sizes[which])
        dialog.dismiss()
      }
      .setNegativeButton(R.string.cancel, null)
      .show()
  }

  fun showTextStyleSheet(
    activity: AppCompatActivity,
    state: RichTextToolbarState,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onUnderline: () -> Unit,
    onStrikethrough: () -> Unit
  ) {
    val dialog = BottomSheetDialog(activity)
    val container = LinearLayout(activity).apply {
      orientation = LinearLayout.VERTICAL
      val padding = activity.resources.getDimensionPixelSize(R.dimen.rich_text_toolbar_padding_horizontal)
      setPadding(padding, padding, padding, padding)
    }

    val title = TextView(activity).apply {
      setText(R.string.text_style)
      setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
      setPadding(0, 0, 0, paddingBottom(activity))
    }
    container.addView(title)

    addStyleButton(container, activity, "B", state.bold) {
      onBold()
      dialog.dismiss()
    }
    addStyleButton(container, activity, "I", state.italic) {
      onItalic()
      dialog.dismiss()
    }
    addStyleButton(container, activity, "U", state.underline) {
      onUnderline()
      dialog.dismiss()
    }
    addStyleButton(container, activity, "S", state.strikethrough) {
      onStrikethrough()
      dialog.dismiss()
    }

    dialog.setContentView(container)
    dialog.show()
  }

  fun showMoreToolsSheet(
    activity: AppCompatActivity,
    isNumberListModeEnabled: Boolean,
    isBulletListModeEnabled: Boolean,
    onAction: (MoreToolAction) -> Unit
  ) {
    val items = listOf(
      MoreToolAction.ATTACHMENT to activity.getString(R.string.attachment),
      MoreToolAction.VOICE_NOTE to activity.getString(R.string.voice_note),
      MoreToolAction.NUMBER_LIST to buildString {
        append(activity.getString(R.string.number_list))
        if (isNumberListModeEnabled) append(" ✓")
      },
      MoreToolAction.BULLET_LIST to buildString {
        append(activity.getString(R.string.bullet_list))
        if (isBulletListModeEnabled) append(" ✓")
      },
      MoreToolAction.LINK to activity.getString(R.string.add_link),
      MoreToolAction.CATEGORY to activity.getString(R.string.select_category)
    )

    val dialog = BottomSheetDialog(activity)
    val container = LinearLayout(activity).apply {
      orientation = LinearLayout.VERTICAL
      val padding = activity.resources.getDimensionPixelSize(R.dimen.rich_text_toolbar_padding_horizontal)
      setPadding(padding, padding, padding, padding)
    }

    val title = TextView(activity).apply {
      setText(R.string.more_tools)
      setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
      setPadding(0, 0, 0, paddingBottom(activity))
    }
    container.addView(title)

    items.forEach { (action, label) ->
      val button = MaterialButton(
        activity,
        null,
        com.google.android.material.R.attr.materialButtonOutlinedStyle
      ).apply {
        text = label
        layoutParams = LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT,
          LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
          bottomMargin = activity.resources.getDimensionPixelSize(R.dimen.rich_text_toolbar_item_spacing)
        }
        setOnClickListener {
          onAction(action)
          dialog.dismiss()
        }
      }
      container.addView(button)
    }

    dialog.setContentView(container)
    dialog.show()
  }

  private fun addStyleButton(
    container: LinearLayout,
    context: Context,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
  ) {
    val button = MaterialButton(context).apply {
      text = label
      isSelected = selected
      layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
      ).apply {
        bottomMargin = context.resources.getDimensionPixelSize(R.dimen.rich_text_toolbar_item_spacing)
      }
      setOnClickListener {
        onClick()
      }
    }
    container.addView(button)
  }

  private fun paddingBottom(context: Context): Int =
    context.resources.getDimensionPixelSize(R.dimen.rich_text_toolbar_item_spacing)
}
