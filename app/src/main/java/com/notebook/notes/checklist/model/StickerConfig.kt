package com.notebook.notes.checklist.model

data class DrawStickerConfig(
    val version: Int = 0,
    val showList: List<StickerCategory> = emptyList(),
    val resList: List<StickerResource> = emptyList()
)

data class StickerCategory(
    val id: String,
    val cover: String,
    val list: List<Int>,
    val isVip: Boolean? = null,
    val isNew: Boolean? = null
)

data class StickerResource(
    val id: Int,
    val thumb: String,
    val res: String,
    val type: Int? = null,
    val zip: String? = null
)
