package com.notebook.notes.checklist.ui.background

import android.content.Context
import com.google.gson.Gson
import com.notebook.notes.checklist.model.NoteBackgroundConfig
import com.notebook.notes.checklist.model.NoteBackgroundOption
import com.notebook.notes.checklist.model.NoteBgCategory
import com.notebook.notes.checklist.model.NoteBgImage
import com.notebook.notes.checklist.model.NoteBgItem

class NoteBackgroundRepository(context: Context) {

    private val appContext = context.applicationContext
    private val gson = Gson()

    private val config: NoteBackgroundConfig by lazy {
        runCatching {
            appContext.assets.open(CONFIG_FILE).bufferedReader().use { reader ->
                gson.fromJson(reader, NoteBackgroundConfig::class.java)
            }
        }.getOrDefault(NoteBackgroundConfig())
    }

    private val itemById: Map<Int, NoteBgItem> by lazy {
        config.bgList.orEmpty().associateBy { it.id }
    }

    fun getCategories(): List<NoteBgCategory> {
        val categories = config.showList.orEmpty().toMutableList()
        if (categories.none { it.id == SOLID_COLOR_CATEGORY_ID }) {
            val insertAt = (categories.indexOfFirst { it.id == "hot" } + 1)
                .coerceIn(0, categories.size)
            categories.add(
                insertAt,
                NoteBgCategory(
                    id = SOLID_COLOR_CATEGORY_ID,
                    label = mapOf("en" to "Color"),
                    list = emptyList()
                )
            )
        }
        return categories
    }

    fun localizedLabel(category: NoteBgCategory): String {
        val locale = appContext.resources.configuration.locales[0]
        val candidates = buildList {
            val language = locale.language
            val country = locale.country
            if (language.isNotEmpty() && country.isNotEmpty()) {
                add("${language}_$country")
            }
            if (language == "id") add("in")
            if (language.isNotEmpty()) add(language)
            add("en")
        }
        return candidates.firstNotNullOfOrNull { key -> category.label.orEmpty()[key] }
            ?: category.label.orEmpty().values.firstOrNull()
            ?: category.id.replaceFirstChar { it.uppercase() }
    }

    fun getOptionsForCategory(categoryId: String): List<NoteBackgroundOption> {
        if (categoryId == SOLID_COLOR_CATEGORY_ID) {
            return SOLID_COLORS.map { NoteBackgroundOption.SolidColor(it) }
        }
        val category = config.showList.orEmpty().find { it.id == categoryId } ?: return emptyList()
        return category.list.orEmpty().mapNotNull { id ->
            itemById[id]?.let { NoteBackgroundOption.Asset(it) }
        }
    }

    fun getItem(id: Int): NoteBgItem? = itemById[id]

    fun resolveOption(key: String?): NoteBackgroundOption? {
        if (key.isNullOrBlank()) return null
        NoteBackgroundOption.parseAssetId(key)?.let { id ->
            return getItem(id)?.let { NoteBackgroundOption.Asset(it) }
        }
        if (key.startsWith("#")) {
            return NoteBackgroundOption.SolidColor(key)
        }
        return null
    }

    fun thumbnailAssetPath(item: NoteBgItem): String? {
        val fileName = item.thumbnail?.takeIf { it.isNotBlank() } ?: return null
        val path = "$IMG_DIR/$fileName"
        return path.takeIf { assetExists(path) }
    }

    /**
     * Layers to draw on the note editor: every existing phoneImages entry.
     * Falls back to padImages only when no phone art files are packaged.
     * phoneImages[].imageName → `img/{imageName}`
     */
    fun resolveDisplayImageLayers(item: NoteBgItem): List<ResolvedBgImage> {
        val phone = item.phoneImages.orEmpty()
            .mapNotNull { toResolvedImage(it, isPad = false) }
            .filter { assetExists(it.path) }
        if (phone.isNotEmpty()) return phone

        return item.padImages.orEmpty()
            .mapNotNull { toResolvedImage(it, isPad = true) }
            .filter { assetExists(it.path) }
    }

    fun resolveDisplayImage(item: NoteBgItem): ResolvedBgImage? =
        resolveDisplayImageLayers(item).firstOrNull()

    fun phoneImagePath(item: NoteBgItem): String? = resolveDisplayImage(item)?.path

    fun overlayAssetPath(item: NoteBgItem): String? {
        resolveDisplayImageLayers(item).firstOrNull()?.let { return it.path }
        return null
    }

    fun primaryBackgroundImagePath(item: NoteBgItem): String? = overlayAssetPath(item)

    private fun toResolvedImage(img: NoteBgImage, isPad: Boolean): ResolvedBgImage? {
        val fileName = img.imageName?.takeIf { it.isNotBlank() } ?: return null
        val path = "$IMG_DIR/$fileName"

        val widthRatio = if (isPad) {
            when {
                img.imagePortraitRatio > 0f -> img.imagePortraitRatio
                img.imageLandscapeRatio > 0f -> img.imageLandscapeRatio
                else -> 1f
            }
        } else {
            img.imageWidthRatio.takeIf { it > 0f } ?: 1f
        }

        return ResolvedBgImage(
            path = path,
            position = img.imagePosition?.takeIf { it.isNotBlank() } ?: "bottomCenter",
            widthRatio = widthRatio.coerceIn(0.1f, 1f),
            isFloat = img.isFloat
        )
    }

    data class ResolvedBgImage(
        val path: String,
        val position: String,
        val widthRatio: Float,
        val isFloat: Boolean = false
    )

    fun findCategoryForKey(key: String?): NoteBgCategory? {
        if (key.isNullOrBlank()) return null
        if (key.startsWith("#")) {
            return getCategories().find { it.id == SOLID_COLOR_CATEGORY_ID }
        }
        val id = NoteBackgroundOption.parseAssetId(key) ?: return null
        return getCategories().firstOrNull { category ->
            category.id != SOLID_COLOR_CATEGORY_ID && category.list.orEmpty().contains(id)
        }
    }

    fun assetUri(relativePath: String): String = "file:///android_asset/$relativePath"

    private fun assetExists(path: String): Boolean {
        return runCatching {
            appContext.assets.open(path).close()
            true
        }.getOrDefault(false)
    }

    companion object {
        const val SOLID_COLOR_CATEGORY_ID = "color"
        private const val CONFIG_FILE = "background_config.json"
        private const val IMG_DIR = "img"

        val SOLID_COLORS = listOf(
            "#FFE5D9",
            "#FFD6A5",
            "#FDFFB6",
            "#CAFFBF",
            "#9BF6FF",
            "#A0C4FF",
            "#BDB2FF",
            "#FFC6FF",
            "#F1F0C0",
            "#D8F3DC",
            "#BDE0FE",
            "#FAD2E1",
            "#E2ECE9",
            "#FFF1E6",
            "#E4C1F9",
            "#CDEAC0",
            "#FFD8BE",
            "#B8F2E6",
            "#F9F7CF",
            "#FEC89A",
            "#F7D6E0",
            "#D0F4DE",
            "#A9DEF9",
            "#E4BAD4"
        )
    }
}
