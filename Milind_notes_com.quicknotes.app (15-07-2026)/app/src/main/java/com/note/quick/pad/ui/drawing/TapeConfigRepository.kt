package com.note.quick.pad.ui.drawing

import android.content.Context
import com.google.gson.Gson
import com.note.quick.pad.model.DrawTapeConfig
import com.note.quick.pad.model.TapeResource

class TapeConfigRepository(context: Context) {

    private val appContext = context.applicationContext
    private val gson = Gson()

    private val config: DrawTapeConfig by lazy {
        appContext.assets.open(CONFIG_FILE).bufferedReader().use { reader ->
            gson.fromJson(reader, DrawTapeConfig::class.java)
        }
    }

    private val resourceById: Map<Int, TapeResource> by lazy {
        config.resList.associateBy { it.id }
    }

    fun getCategories() = config.showList

    fun getResource(id: Int): TapeResource? = resourceById[id]

    fun getResourcesForCategory(categoryId: String): List<TapeResource> {
        val category = config.showList.find { it.id == categoryId } ?: return emptyList()
        return category.list.mapNotNull { resourceById[it] }
    }

    companion object {
        private const val CONFIG_FILE = "draw_tape_config.json"
    }
}
