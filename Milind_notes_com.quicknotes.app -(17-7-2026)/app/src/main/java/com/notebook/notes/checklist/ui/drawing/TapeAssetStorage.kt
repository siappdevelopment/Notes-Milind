package com.notebook.notes.checklist.ui.drawing

import android.content.Context
import java.io.File
import java.util.UUID

object TapeAssetStorage {

    fun copyToFilesDir(context: Context, fileName: String): String? {
        return runCatching {
            val dir = File(context.filesDir, "tapes").apply { mkdirs() }
            val outFile = File(dir, "${UUID.randomUUID()}_$fileName")
            context.assets.open(TapeAssets.assetPath(fileName)).use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            }
            outFile.absolutePath
        }.getOrNull()
    }
}
