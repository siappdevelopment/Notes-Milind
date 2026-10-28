package com.notebook.notes.checklist.ui.drawing

object TapeAssets {
    private const val TAPE_DIR = "tape"

    fun uri(fileName: String): String = "file:///android_asset/$TAPE_DIR/$fileName"

    fun assetPath(fileName: String): String = "$TAPE_DIR/$fileName"
}
