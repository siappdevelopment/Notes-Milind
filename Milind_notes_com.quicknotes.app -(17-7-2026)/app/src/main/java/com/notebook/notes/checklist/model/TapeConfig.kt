package com.notebook.notes.checklist.model

data class DrawTapeConfig(
    val version: Int = 0,
    val showList: List<TapeCategory> = emptyList(),
    val resList: List<TapeResource> = emptyList()
)

data class TapeCategory(
    val id: String,
    val cover: String,
    val list: List<Int>,
    val isVip: Boolean? = null,
    val isNew: Boolean? = null
)

data class TapeResource(
    val id: Int,
    val thumb: String,
    val res: String
)
