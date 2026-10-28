package com.notebook.notes.checklist.ui.drawing

import android.content.Context
import android.graphics.Bitmap
import com.notebook.notes.checklist.model.StickerResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object StickerAssetStorage {

    suspend fun prepareCanvasFile(context: Context, resource: StickerResource): String? {
        return withContext(Dispatchers.IO) {
            val resolved = StickerAssetResolver.resolveCanvas(context, resource) ?: return@withContext null
            val dir = File(context.filesDir, "stickers").apply { mkdirs() }
            if (StickerAssetResolver.isRaster(resolved)) {
                copyAssetToFile(context, resolved, dir) ?: return@withContext null
            } else if (StickerAssetResolver.isSvg(resolved)) {
                renderAssetToFile(context, resolved, dir) ?: return@withContext null
            } else {
                return@withContext null
            }
        }
    }

    private fun copyAssetToFile(context: Context, fileName: String, dir: File): String? {
        return runCatching {
            val outFile = File(dir, "${UUID.randomUUID()}_$fileName")
            context.assets.open(StickerAssets.assetPath(fileName)).use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            }
            outFile.absolutePath
        }.getOrNull()
    }

    private suspend fun renderAssetToFile(context: Context, fileName: String, dir: File): String? {
        val bitmap = StickerImageLoader.renderAsset(context, fileName, 1024, 1024) ?: return null
        return runCatching {
            val outFile = File(dir, "${UUID.randomUUID()}_${fileName.substringBeforeLast('.')}.png")
            FileOutputStream(outFile).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            bitmap.recycle()
            outFile.absolutePath
        }.getOrNull()
    }
}
