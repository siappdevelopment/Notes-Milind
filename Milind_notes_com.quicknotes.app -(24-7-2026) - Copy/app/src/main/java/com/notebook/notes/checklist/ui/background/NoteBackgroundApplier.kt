package com.notebook.notes.checklist.ui.background

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.model.NoteBackgroundOption
import com.notebook.notes.checklist.model.NoteBgItem
import com.notebook.notes.checklist.util.NoteColorUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Applies JSON background config to CreateNotes:
 * color/gradient (+ optional tiled bgName) → edit surface → line grid → phoneImages.
 */
object NoteBackgroundApplier {

    data class Appearance(
        val backgroundColor: Int,
        val textColor: Int,
        val hintColor: Int,
        val mutedColor: Int,
        val dividerColor: Int,
        val textColorHex: String
    )

    fun resolveAppearance(key: String?, repository: NoteBackgroundRepository): Appearance {
        val bg = resolveBackgroundColor(key, repository)
        val text = NoteColorUtils.inkColorFromBackground(bg)
        return Appearance(
            backgroundColor = bg,
            textColor = text,
            hintColor = NoteColorUtils.hintInkColor(bg),
            mutedColor = NoteColorUtils.mutedInkColor(bg),
            dividerColor = NoteColorUtils.dividerInkColor(bg),
            textColorHex = NoteColorUtils.toHex(text)
        )
    }

    fun apply(
        root: View,
        colorLayer: View,
        artContainer: ViewGroup,
        key: String?,
        repository: NoteBackgroundRepository,
        scope: CoroutineScope,
        editSurface: View? = null,
        lineOverlay: View? = null
    ): Appearance {
        val appearance = resolveAppearance(key, repository)
        root.background = null
        NoteBackgroundImageLoader.clearContainer(artContainer)
        clearExtras(editSurface, lineOverlay)

        when (val option = key?.let { repository.resolveOption(it) }) {
            is NoteBackgroundOption.SolidColor -> {
                colorLayer.background = solidDrawable(appearance.backgroundColor)
            }

            is NoteBackgroundOption.Asset -> {
                applyColorLayer(colorLayer, option.item, appearance.backgroundColor, scope)
                applyEditSurface(editSurface, option.item)
                applyLineOverlay(lineOverlay, option.item)

                val layers = repository.resolveDisplayImageLayers(option.item)
                    .sortedBy { if (it.isFloat) 1 else 0 }
                layers.forEach { resolved ->
                    val imageView = ImageView(artContainer.context).apply {
                        adjustViewBounds = true
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        isClickable = false
                        isFocusable = false
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                        contentDescription = null
                    }
                    artContainer.addView(imageView)
                    applyImageLayout(imageView, artContainer, resolved)
                    NoteBackgroundImageLoader.loadNative(
                        imageView = imageView,
                        assets = imageView.context.assets,
                        relativePath = resolved.path,
                        scope = scope
                    )
                }
            }

            null -> {
                colorLayer.setBackgroundColor(appearance.backgroundColor)
            }
        }

        return appearance
    }

    private fun clearExtras(editSurface: View?, lineOverlay: View?) {
        editSurface?.let {
            it.background = null
            it.isVisible = false
        }
        lineOverlay?.let {
            it.background = null
            it.isVisible = false
        }
    }

    private fun applyEditSurface(editSurface: View?, item: NoteBgItem) {
        if (editSurface == null) return
        val color = item.frame?.editBgColor
            ?.let { NoteColorUtils.parseColorOrNull(it) }
        if (color == null) {
            editSurface.isVisible = false
            editSurface.background = null
            return
        }
        editSurface.isVisible = true
        editSurface.background = solidDrawable(color)
    }

    private fun applyLineOverlay(lineOverlay: View?, item: NoteBgItem) {
        if (lineOverlay == null) return
        val line = item.line
        val color = line?.lineColor?.let { NoteColorUtils.parseColorOrNull(it) }
        if (line == null || color == null) {
            lineOverlay.isVisible = false
            lineOverlay.background = null
            return
        }
        val density = lineOverlay.resources.displayMetrics.density
        lineOverlay.isVisible = true
        lineOverlay.background = NoteLineDrawable(
            lineColor = color,
            lineWidthPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                line.lineWidth,
                lineOverlay.resources.displayMetrics
            ),
            spacingPx = line.lineSpacing * density,
            style = line.lineStyle
        )
    }

    private fun applyColorLayer(
        colorLayer: View,
        item: NoteBgItem,
        fallback: Int,
        scope: CoroutineScope
    ) {
        val base = colorBackgroundDrawable(item, fallback)
        val tilePath = repositoryTilePath(item)
        if (tilePath == null) {
            colorLayer.background = base
            return
        }

        colorLayer.background = base
        val requestKey = tilePath
        colorLayer.setTag(R.id.viewNoteBackgroundColor, requestKey)
        scope.launch {
            val tiled = withContext(Dispatchers.IO) {
                decodeTiledDrawable(colorLayer.context.assets, tilePath, colorLayer.resources)
            }
            if (colorLayer.getTag(R.id.viewNoteBackgroundColor) != requestKey) return@launch
            colorLayer.background = if (tiled != null) {
                LayerDrawable(arrayOf(base, tiled))
            } else {
                base
            }
        }
    }

    private fun repositoryTilePath(item: NoteBgItem): String? {
        val style = item.bg ?: return null
        val name = style.bgName?.takeIf { it.isNotBlank() } ?: return null
        if (!style.bgType.equals("repeat", ignoreCase = true)) return null
        return "img/$name"
    }

    private fun decodeTiledDrawable(
        assets: AssetManager,
        relativePath: String,
        resources: android.content.res.Resources
    ): BitmapDrawable? {
        return runCatching {
            val bytes = assets.open(relativePath).use { it.readBytes() }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            BitmapDrawable(resources, bitmap).apply {
                setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                isFilterBitmap = true
            }
        }.getOrNull()
    }

    /** Places one art layer using JSON imagePosition + imageWidthRatio. */
    private fun applyImageLayout(
        artImage: ImageView,
        parent: ViewGroup,
        resolved: NoteBackgroundRepository.ResolvedBgImage
    ) {
        if (parent !is ConstraintLayout) {
            artImage.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            return
        }

        val lp = ConstraintLayout.LayoutParams(0, ConstraintLayout.LayoutParams.WRAP_CONTENT)
        lp.matchConstraintPercentWidth = resolved.widthRatio
        lp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        lp.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID

        val position = resolved.position.lowercase()
        when (position) {
            "topcenter", "topleft", "topright" -> {
                lp.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            }
            "center", "centerleft", "centerright" -> {
                lp.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                lp.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            }
            else -> {
                lp.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            }
        }

        lp.horizontalBias = when (position) {
            "topleft", "centerleft", "bottomleft" -> 0f
            "topright", "centerright", "bottomright" -> 1f
            else -> 0.5f
        }

        if (resolved.isFloat) {
            artImage.elevation = 1f
        }

        artImage.layoutParams = lp
    }

    private fun solidDrawable(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
        }
    }

    private fun colorBackgroundDrawable(item: NoteBgItem, fallback: Int): GradientDrawable {
        val colors = item.bg?.gradientColors.orEmpty()
            .mapNotNull { NoteColorUtils.parseColorOrNull(it) }
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            when {
                colors.isEmpty() -> setColor(fallback)
                colors.size == 1 -> setColor(colors.first())
                else -> {
                    orientation = orientationFor(item.bg?.orientation)
                    this.colors = colors.toIntArray()
                }
            }
        }
    }

    fun resolveBackgroundColor(key: String?, repository: NoteBackgroundRepository): Int {
        val defaultBg = Color.parseColor("#FFDAC7")
        if (key.isNullOrBlank()) return defaultBg
        return when (val option = repository.resolveOption(key)) {
            is NoteBackgroundOption.SolidColor -> {
                NoteColorUtils.parseColorOrNull(option.hex) ?: defaultBg
            }

            is NoteBackgroundOption.Asset -> {
                // Prefer frame paper color for ink contrast when present.
                option.item.frame?.editBgColor
                    ?.let { NoteColorUtils.parseColorOrNull(it) }
                    ?.let { return it }

                if (option.item.isDarkBg) {
                    option.item.bg?.gradientColors.orEmpty()
                        .mapNotNull { NoteColorUtils.parseColorOrNull(it) }
                        .minByOrNull { ColorUtils.calculateLuminance(it) }
                        ?: Color.BLACK
                } else {
                    option.item.bg?.gradientColors.orEmpty()
                        .firstOrNull()
                        ?.let { NoteColorUtils.parseColorOrNull(it) }
                        ?: defaultBg
                }
            }

            null -> NoteColorUtils.parseColorOrNull(key) ?: defaultBg
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
