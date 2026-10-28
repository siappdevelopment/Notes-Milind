package com.notebook.notes.checklist.ui.drawing

import android.content.Context
import com.google.gson.Gson
import com.notebook.notes.checklist.model.DrawStickerConfig
import com.notebook.notes.checklist.model.StickerCategory
import com.notebook.notes.checklist.model.StickerResource

class StickerConfigRepository(context: Context) {

    private val appContext = context.applicationContext
    private val gson = Gson()

    private val config: DrawStickerConfig by lazy {
        appContext.assets.open(CONFIG_FILE).bufferedReader().use { reader ->
            gson.fromJson(reader, DrawStickerConfig::class.java)
        }
    }

    private val resourceById: Map<Int, StickerResource> by lazy {
        config.resList.associateBy { it.id }
    }

    fun getCategories() = config.showList

    fun getResource(id: Int): StickerResource? = resourceById[id]

    fun getResourcesForCategory(categoryId: String): List<StickerResource> {
        val category = config.showList.find { it.id == categoryId } ?: return emptyList()
        return category.list.mapNotNull { resourceById[it] }
    }

    fun categoryCoverFileName(context: Context, category: StickerCategory): String {
        return StickerAssetResolver.resolve(context, category.cover)
            ?: getResourcesForCategory(category.id)
                .firstNotNullOfOrNull { StickerAssetResolver.resolveThumb(context, it) }
            ?: category.cover
    }

    fun thumbFileName(context: Context, resource: StickerResource): String {
        return StickerAssetResolver.resolveThumb(context, resource) ?: resource.thumb
    }

    fun canvasFileName(context: Context, resource: StickerResource): String {
        return StickerAssetResolver.resolveCanvas(context, resource) ?: resource.res
    }

    companion object {
        private const val CONFIG_FILE = "draw_sticker_config.json"
    }
}
