package com.notebook.notes.checklist.ui.drawing

import android.content.Context
import com.google.gson.Gson
import com.notebook.notes.checklist.model.BgCategory
import com.notebook.notes.checklist.model.BgResource
import com.notebook.notes.checklist.model.DrawBgConfig
class BgConfigRepository(context: Context) {

    private val appContext = context.applicationContext
    private val gson = Gson()

    private val config: DrawBgConfig by lazy {
        appContext.assets.open(CONFIG_FILE).bufferedReader().use { reader ->
            gson.fromJson(reader, DrawBgConfig::class.java)
        }
    }

    private val resourceById: Map<Int, BgResource> by lazy {
        config.resList
            .filter { resource -> resource.bgVertical != null && assetExists(resource.bgVertical) }
            .associateBy { it.id }
    }

    fun getCategories(): List<BgCategory> = config.showList

    fun getResource(id: Int): BgResource? = resourceById[id]

    fun getResourcesForCategory(categoryId: String): List<BgResource> {
        val category = config.showList.find { it.id == categoryId } ?: return emptyList()
        return category.list.mapNotNull { resourceById[it] }
    }

    fun localizedLabel(category: BgCategory): String = localizedLabel(category.label)

    fun localizedLabel(labels: Map<String, String>): String {
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
        return candidates.firstNotNullOfOrNull { key -> labels[key] }
            ?: labels.values.firstOrNull()
            ?: ""
    }

    fun thumbFileName(resource: BgResource, isLandscape: Boolean): String {
        val preferred = if (isLandscape) resource.thumbHorizontal else resource.thumbVertical
        val fallback = if (isLandscape) resource.bgHorizontal else resource.bgVertical
        return when {
            preferred != null && assetExists(preferred) -> preferred
            fallback != null -> fallback
            else -> resource.bgVertical.orEmpty()
        }
    }

    fun backgroundFileName(resource: BgResource, isLandscape: Boolean): String {
        val preferred = if (isLandscape) resource.bgHorizontal else resource.bgVertical
        val fallback = if (isLandscape) resource.bgVertical else resource.bgHorizontal
        return when {
            preferred != null && assetExists(preferred) -> preferred
            fallback != null && assetExists(fallback) -> fallback
            else -> resource.bgVertical.orEmpty()
        }
    }

    private fun assetExists(fileName: String): Boolean {
        return runCatching {
            appContext.assets.open(BgAssets.assetPath(fileName)).close()
            true
        }.getOrDefault(false)
    }

    companion object {
        private const val CONFIG_FILE = "draw_bg_config.json"
    }
}
