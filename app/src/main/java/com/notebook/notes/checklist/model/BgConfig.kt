package com.notebook.notes.checklist.model

data class DrawBgConfig(
    val version: Int = 0,
    val showList: List<BgCategory> = emptyList(),
    val resList: List<BgResource> = emptyList()
)

data class BgCategory(
    val id: String,
    val label: Map<String, String>,
    val list: List<Int>
)

data class BgResource(
    val id: Int,
    val type: Int = 1,
    val isVip: Boolean? = null,
    val isThumbReady: Boolean = false,
    val isBgReady: Boolean = false,
    val thumbVertical: String? = null,
    val thumbHorizontal: String? = null,
    val bgVertical: String? = null,
    val bgHorizontal: String? = null
)
