package com.notebook.notes.checklist.ui.drawing

import android.content.Context
import com.notebook.notes.checklist.model.StickerResource

object StickerAssetResolver {

    fun exists(context: Context, fileName: String): Boolean {
        return runCatching {
            context.assets.open(StickerAssets.assetPath(fileName)).close()
            true
        }.getOrDefault(false)
    }

    fun resolve(context: Context, vararg candidates: String): String? {
        return candidates
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .firstOrNull { exists(context, it) }
    }

    fun resolveThumb(context: Context, resource: StickerResource): String? {
        val baseName = resource.res.substringBeforeLast('.')
        return resolve(
            context,
            resource.thumb,
            resource.res,
            "${baseName}.webp",
            "${baseName}_thumb.webp"
        )
    }

    fun resolveCanvas(context: Context, resource: StickerResource): String? {
        val baseName = resource.res.substringBeforeLast('.')
        return resolve(
            context,
            resource.res,
            resource.thumb,
            "${baseName}.webp",
            "${baseName}_thumb.webp"
        )
    }

    fun isRaster(fileName: String): Boolean {
        val lower = fileName.lowercase()
        return lower.endsWith(".webp") ||
            lower.endsWith(".png") ||
            lower.endsWith(".jpg") ||
            lower.endsWith(".jpeg")
    }

    fun isSvg(fileName: String): Boolean = fileName.lowercase().endsWith(".svg")
}
