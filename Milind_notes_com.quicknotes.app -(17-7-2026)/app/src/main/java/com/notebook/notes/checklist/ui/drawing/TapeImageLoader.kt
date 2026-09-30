package com.notebook.notes.checklist.ui.drawing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.LruCache
import android.widget.ImageView
import androidx.core.view.doOnLayout
import com.notebook.notes.checklist.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object TapeImageLoader {
    private const val MAX_CACHE_ENTRIES = 64
    private val bitmapCache = LruCache<String, Bitmap>(MAX_CACHE_ENTRIES)

    fun load(
        imageView: ImageView,
        fileName: String,
        scope: CoroutineScope,
        assetPathResolver: (String) -> String = TapeAssets::assetPath,
        onFailure: (() -> Unit)? = null
    ) {
        val loadAction = {
            val width = imageView.width.takeIf { it > 0 } ?: 220
            val height = imageView.height.takeIf { it > 0 } ?: 160
            val cacheKey = "$fileName@${width}x$height"

            imageView.setTag(R.id.tag_tape_path, cacheKey)

            bitmapCache.get(cacheKey)?.let { cached ->
                imageView.setImageBitmap(cached)
                return@let
            }
            scope.launch {
                val bitmap = decodeAsset(imageView.context, fileName, assetPathResolver)
                if (bitmap == null) {
                    withContext(Dispatchers.Main) {
                        onFailure?.invoke()
                    }
                    return@launch
                }
                val scaled = scaleBitmapToCover(bitmap, width, height)
                if (scaled !== bitmap) {
                    bitmap.recycle()
                }
                bitmapCache.put(cacheKey, scaled)
                withContext(Dispatchers.Main) {
                    if (imageView.isAttachedToWindow && imageView.getTag(R.id.tag_tape_path) == cacheKey) {
                        imageView.setImageBitmap(scaled)
                    }
                }
            }
        }
        if (imageView.isLaidOut && imageView.width > 0 && imageView.height > 0) {
            loadAction()
        } else {
            imageView.doOnLayout { loadAction() }
        }
    }

    suspend fun renderAssetToCache(
        context: Context,
        fileName: String,
        width: Int,
        height: Int,
        trim: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        val bitmap = decodeAsset(context, fileName) ?: return@withContext null
        val scaled = scaleBitmapToFill(bitmap, width, height, trim)
        if (scaled !== bitmap) {
            bitmap.recycle()
        }
        val suffix = if (trim) "trim" else "full"
        val cacheDir = File(context.cacheDir, "drawing_doodles").apply { mkdirs() }
        val cacheFile = File(cacheDir, "${fileName.substringBeforeLast('.')}_${width}x${height}_$suffix.png")
        FileOutputStream(cacheFile).use { stream ->
            scaled.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        scaled.recycle()
        cacheFile.absolutePath
    }

    fun scaleBitmapToFill(source: Bitmap, targetWidth: Int, targetHeight: Int, trim: Boolean = true): Bitmap {
        val content = if (trim) trimTransparentEdges(source) ?: source else source
        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        
        // Synchronize densities to prevent automatic Canvas scaling
        content.density = output.density

        val canvas = Canvas(output)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(content, null, RectF(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat()), paint)
        if (trim && content !== source && content !== output) {
            content.recycle()
        }
        return output
    }

    fun scaleBitmapToCover(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val trimmed = trimTransparentEdges(source)
        val content = trimmed ?: source
        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.WHITE)
        val scale = max(
            targetWidth.toFloat() / content.width,
            targetHeight.toFloat() / content.height
        )
        val scaledW = content.width * scale
        val scaledH = content.height * scale
        val left = (targetWidth - scaledW) / 2f
        val top = (targetHeight - scaledH) / 2f
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(content, null, RectF(left, top, left + scaledW, top + scaledH), paint)
        if (trimmed != null && trimmed !== source && trimmed !== output) {
            trimmed.recycle()
        }
        return output
    }

    private fun trimTransparentEdges(source: Bitmap): Bitmap? {
        val width = source.width
        val height = source.height
        if (width == 0 || height == 0) return null
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1
        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                if (((pixels[rowOffset + x] ushr 24) and 0xFF) > 10) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        if (maxX < minX || maxY < minY) return null
        val trimmedWidth = maxX - minX + 1
        val trimmedHeight = maxY - minY + 1
        if (trimmedWidth == width && trimmedHeight == height) return null
        return Bitmap.createBitmap(source, minX, minY, trimmedWidth, trimmedHeight)
    }

    fun decodeAsset(
        context: Context,
        fileName: String,
        assetPathResolver: (String) -> String = TapeAssets::assetPath
    ): Bitmap? {
        return runCatching {
            val options = BitmapFactory.Options().apply { inScaled = false }
            context.assets.open(assetPathResolver(fileName)).use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }
        }.getOrNull()
    }
}

fun ImageView.loadTapeAsset(fileName: String, scope: CoroutineScope, onFailure: (() -> Unit)? = null) {
    TapeImageLoader.load(this, fileName, scope, onFailure = onFailure)
}
