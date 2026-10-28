package com.note.quick.pad.ui.drawing

object StickerAssets {
    const val STICKER_DIR = "sticker"

    fun uri(fileName: String): String = "file:///android_asset/$STICKER_DIR/$fileName"

    fun assetPath(fileName: String): String = "$STICKER_DIR/$fileName"
}
