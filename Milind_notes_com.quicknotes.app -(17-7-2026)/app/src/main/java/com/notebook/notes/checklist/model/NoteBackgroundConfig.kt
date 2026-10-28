package com.notebook.notes.checklist.model

data class NoteBackgroundConfig(
    val version: Int = 0,
    val showList: List<NoteBgCategory>? = emptyList(),
    val bgList: List<NoteBgItem>? = emptyList()
)

data class NoteBgCategory(
    val id: String,
    val label: Map<String, String>? = emptyMap(),
    val list: List<Int>? = emptyList()
)

data class NoteBgItem(
    val id: Int,
    val thumbnail: String? = null,
    val thumbnailReady: Boolean = false,
    val isVip: Boolean = false,
    val isDarkBg: Boolean = false,
    val bg: NoteBgStyle? = null,
    val frame: NoteBgFrame? = null,
    val line: NoteBgLine? = null,
    val phoneImages: List<NoteBgImage>? = emptyList(),
    val padImages: List<NoteBgImage>? = emptyList()
)

data class NoteBgStyle(
    val gradientColors: List<String>? = emptyList(),
    val orientation: String? = null,
    val bgName: String? = null,
    val bgType: String? = null
)

data class NoteBgFrame(
    val editBgColor: String? = null
)

data class NoteBgLine(
    val lineStyle: String? = null,
    val lineType: String? = null,
    val lineColor: String? = null,
    val lineWidth: Float = 0.5f,
    val lineSpacing: Float = 15f
)

data class NoteBgImage(
    val imageName: String? = null,
    val imagePosition: String? = null,
    val imageWidthRatio: Float = 1f,
    val imageLandscapeRatio: Float = 0f,
    val imagePortraitRatio: Float = 0f,
    val isFloat: Boolean = false
)

sealed class NoteBackgroundOption {
    abstract val key: String

    data class SolidColor(val hex: String) : NoteBackgroundOption() {
        override val key: String = hex
    }

    data class Asset(val item: NoteBgItem) : NoteBackgroundOption() {
        override val key: String = KEY_PREFIX + item.id
    }

    companion object {
        const val KEY_PREFIX = "bg:"

        fun assetKey(id: Int): String = KEY_PREFIX + id

        fun parseAssetId(key: String): Int? =
            key.removePrefix(KEY_PREFIX).toIntOrNull().takeIf { key.startsWith(KEY_PREFIX) }
    }
}
