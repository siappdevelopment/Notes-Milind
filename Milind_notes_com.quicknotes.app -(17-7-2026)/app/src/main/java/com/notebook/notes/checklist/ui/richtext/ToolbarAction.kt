package com.notebook.notes.checklist.ui.richtext

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.notebook.notes.checklist.R

enum class ActionType {
  FONT_SIZE,
  FONT_FAMILY,
  TEXT_STYLE,
  DIVIDER,
  UNDO,
  REDO,
  CHECKLIST,
  EMOJI,
  DRAWING,
  MORE_TOOLS
}

enum class MoreToolAction {
  ATTACHMENT,
  VOICE_NOTE,
  NUMBER_LIST,
  BULLET_LIST,
  LINK,
  CATEGORY
}

data class ToolbarAction(
  val id: String,
  @DrawableRes val iconRes: Int,
  @StringRes val contentDescriptionRes: Int,
  val actionType: ActionType
)

object RichTextToolbarActions {
  val primaryActions: List<ToolbarAction> = listOf(
    ToolbarAction("font_size", 0, R.string.font_size, ActionType.FONT_SIZE),
    ToolbarAction("font_family", 0, R.string.font_family, ActionType.FONT_FAMILY),
    ToolbarAction("text_style", 0, R.string.text_style, ActionType.TEXT_STYLE),
    ToolbarAction("divider_1", 0, 0, ActionType.DIVIDER),
    ToolbarAction("undo", android.R.drawable.ic_menu_revert, R.string.undo, ActionType.UNDO),
    ToolbarAction("redo", android.R.drawable.ic_menu_rotate, R.string.redo, ActionType.REDO),
    ToolbarAction("divider_2", 0, 0, ActionType.DIVIDER),
    ToolbarAction("checklist", R.drawable.ic_checkbox, R.string.insert_checklist, ActionType.CHECKLIST),
    ToolbarAction("emoji", R.drawable.ic_menu_emoticons, R.string.insert_emoji, ActionType.EMOJI),
    ToolbarAction("drawing", R.drawable.ic_drawing, R.string.add_drawing, ActionType.DRAWING),
    ToolbarAction("more_tools", R.drawable.ic_more_vert, R.string.more_tools, ActionType.MORE_TOOLS)
  )
}
