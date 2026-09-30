package com.notebook.notes.checklist.ui.drawing

object BgAssets {
    private const val BG_DIR = "bg"

    fun uri(fileName: String): String = "file:///android_asset/$BG_DIR/$fileName"

    fun assetPath(fileName: String): String = "$BG_DIR/$fileName"
}
