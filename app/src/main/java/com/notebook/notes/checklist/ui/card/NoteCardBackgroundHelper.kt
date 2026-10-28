package com.notebook.notes.checklist.ui.card

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.cardview.widget.CardView
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.model.NoteBackgroundOption
import com.notebook.notes.checklist.ui.background.NoteBackgroundApplier
import com.notebook.notes.checklist.ui.background.NoteBackgroundRepository
import com.notebook.notes.checklist.ui.model.NoteCardUiModel
import com.notebook.notes.checklist.util.NoteColorUtils

/**
 * Applies the same background → ink color mapping used by CreateNotesActivity.
 */
object NoteCardBackgroundHelper {

    fun apply(cardView: View, model: NoteCardUiModel): NoteBackgroundApplier.Appearance {
        val repository = NoteBackgroundRepository(cardView.context)
        val key = model.backgroundKey.takeIf { it.isNotBlank() }
        val appearance = NoteBackgroundApplier.resolveAppearance(key, repository)
        val radius = cardView.resources.getDimension(R.dimen.note_card_corner_radius)

        // Pure white note fills blend into the white home list — use a distinct surface instead.
        val fillColor = if (shouldUseWhiteCardSurface(key, repository, appearance.backgroundColor)) {
            ContextCompat.getColor(cardView.context, R.color.note_card_white_surface)
        } else {
            null
        }

        if (fillColor != null) {
            cardView.background = solidDrawable(fillColor, radius)
            (cardView.parent as? CardView)?.setCardBackgroundColor(fillColor)
        } else {
            cardView.background = backgroundDrawable(key, repository, appearance.backgroundColor, radius)
            // CardView/MaterialCardView only draws elevation when cardBackgroundColor is opaque.
            (cardView.parent as? CardView)?.setCardBackgroundColor(appearance.backgroundColor)
        }
        return appearance
    }

    /**
     * White (or near-white) solid fills should not be painted as-is on the home list.
     * Gradient / multi-stop assets still bind normally.
     */
    private fun shouldUseWhiteCardSurface(
        key: String?,
        repository: NoteBackgroundRepository,
        fallbackColor: Int
    ): Boolean {
        return when (val option = key?.let { repository.resolveOption(it) }) {
            is NoteBackgroundOption.SolidColor -> {
                NoteColorUtils.isTotallyWhite(
                    NoteColorUtils.parseColorOrNull(option.hex) ?: fallbackColor
                )
            }

            is NoteBackgroundOption.Asset -> {
                val colors = option.item.bg?.gradientColors.orEmpty()
                    .mapNotNull { NoteColorUtils.parseColorOrNull(it) }
                when {
                    colors.isEmpty() -> NoteColorUtils.isTotallyWhite(fallbackColor)
                    colors.size == 1 -> NoteColorUtils.isTotallyWhite(colors.first())
                    else -> false
                }
            }

            null -> NoteColorUtils.isTotallyWhite(fallbackColor)
        }
    }

    fun applyTextColors(
        appearance: NoteBackgroundApplier.Appearance,
        title: TextView? = null,
        body: TextView? = null,
        muted: TextView? = null,
        icons: List<ImageView> = emptyList(),
        categoryPill: TextView? = null,
        divider: View? = null
    ) {
        val ink = appearance.textColor
        val mutedInk = appearance.mutedColor
        val hint = appearance.hintColor

        title?.setTextColor(ink)
        body?.setTextColor(mutedInk)
        muted?.setTextColor(hint)
        divider?.setBackgroundColor(appearance.dividerColor)
        icons.forEach { it.setColorFilter(ink) }

        categoryPill?.let { pill ->
            pill.setTextColor(ink)
            pill.background = pillDrawable(
                ink,
                pill.resources.getDimension(R.dimen.note_card_category_pill_radius)
            )
        }
    }

    /**
     * Applies themed colors + fill ratio for the custom todo progress bar.
     * Uses measured parent width so the fill is always drawn correctly inside a FrameLayout.
     */
    fun applyTodoProgress(
        container: View,
        fill: View,
        ink: Int,
        completed: Int,
        total: Int
    ) {
        val ratio = if (total > 0) {
            (completed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        val trackColor = ColorUtils.setAlphaComponent(ink, 0x40)
        container.background = pillShape(trackColor)
        fill.background = pillShape(ink)
        fill.isVisible = ratio > 0f

        val applyWidth = {
            val parentWidth = container.width
            if (parentWidth > 0) {
                val target = (parentWidth * ratio).toInt()
                val lp = fill.layoutParams
                if (lp.width != target) {
                    lp.width = target
                    fill.layoutParams = lp
                }
            }
        }
        container.post(applyWidth)
        if (container.width > 0) applyWidth()
    }

    private fun pillShape(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f
            setColor(color)
        }
    }

    fun tintTodoCheckbox(imageView: ImageView, completed: Boolean, ink: Int) {
        if (completed) {
            imageView.setImageDrawable(checkedTodoDrawable(imageView.context, ink))
            imageView.clearColorFilter()
        } else {
            imageView.setImageResource(R.drawable.ic_todo_unchecked)
            imageView.setColorFilter(ink)
        }
    }

    private fun checkedTodoDrawable(context: Context, ink: Int): LayerDrawable {
        val density = context.resources.displayMetrics.density
        val size = (20 * density).toInt().coerceAtLeast(1)
        val circle = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ink)
            setSize(size, size)
        }
        val check = ContextCompat.getDrawable(context, R.drawable.ic_todo_check_mark)?.mutate()
        return if (check != null) {
            LayerDrawable(arrayOf(circle, check)).apply {
                val inset = (4 * density).toInt()
                setLayerInset(1, inset, inset, inset, inset)
            }
        } else {
            LayerDrawable(arrayOf(circle))
        }
    }

    private fun backgroundDrawable(
        key: String?,
        repository: NoteBackgroundRepository,
        fallbackColor: Int,
        cornerRadius: Float
    ): GradientDrawable {
        return when (val option = key?.let { repository.resolveOption(it) }) {
            is NoteBackgroundOption.SolidColor -> {
                solidDrawable(
                    NoteColorUtils.parseColorOrNull(option.hex) ?: fallbackColor,
                    cornerRadius
                )
            }

            is NoteBackgroundOption.Asset -> {
                val colors = option.item.bg?.gradientColors.orEmpty()
                    .mapNotNull { NoteColorUtils.parseColorOrNull(it) }
                GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    this.cornerRadius = cornerRadius
                    when {
                        colors.isEmpty() -> setColor(fallbackColor)
                        colors.size == 1 -> setColor(colors.first())
                        else -> {
                            orientation = orientationFor(option.item.bg?.orientation)
                            this.colors = colors.toIntArray()
                        }
                    }
                }
            }

            null -> solidDrawable(fallbackColor, cornerRadius)
        }
    }

    private fun solidDrawable(color: Int, cornerRadius: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            this.cornerRadius = cornerRadius
            setColor(color)
        }
    }

    private fun pillDrawable(ink: Int, radius: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(ColorUtils.setAlphaComponent(ink, 0x2E))
        }
    }

    private fun orientationFor(value: String?): GradientDrawable.Orientation {
        return when (value) {
            "topLeft_bottomRight" -> GradientDrawable.Orientation.TL_BR
            "topRight_bottomLeft" -> GradientDrawable.Orientation.TR_BL
            "left_right" -> GradientDrawable.Orientation.LEFT_RIGHT
            else -> GradientDrawable.Orientation.TOP_BOTTOM
        }
    }
}
