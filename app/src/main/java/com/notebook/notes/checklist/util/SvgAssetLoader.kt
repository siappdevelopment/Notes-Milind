package com.notebook.notes.checklist.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Picture
import android.graphics.drawable.PictureDrawable
import android.util.Log
import android.util.LruCache
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

object SvgAssetLoader {
    private const val TAG = "SVG"
    private const val MAX_PICTURE_CACHE_ENTRIES = 64
    private const val MAX_BITMAP_CACHE_ENTRIES = 64
    private const val DEFAULT_WIDTH = 595f
    private const val DEFAULT_HEIGHT = 842f

    private val pictureCache = LruCache<String, PictureDrawable>(MAX_PICTURE_CACHE_ENTRIES)
    private val bitmapCache = LruCache<String, Bitmap>(MAX_BITMAP_CACHE_ENTRIES)

    fun normalizeAssetPath(fileName: String): String = fileName.trim().removePrefix("/")

    fun assetExists(context: Context, assetPath: String): Boolean {
        val normalized = normalizeAssetPath(assetPath)
        if (normalized.isBlank()) return false
        return runCatching {
            context.assets.open(normalized).close()
            true
        }.getOrDefault(false)
    }

    fun getCachedPicture(assetPath: String): PictureDrawable? =
        pictureCache.get(normalizeAssetPath(assetPath))

    fun getCachedBitmap(cacheKey: String): Bitmap? = bitmapCache.get(cacheKey)

    fun loadPictureDrawableSync(
        context: Context,
        assetPath: String,
        useCache: Boolean = true
    ): PictureDrawable? {
        val normalized = normalizeAssetPath(assetPath)
        if (normalized.isBlank()) return null

        if (useCache) {
            pictureCache.get(normalized)?.let { return it }
        }
        if (!assetExists(context, normalized)) return null

        return runCatching {
            context.assets.open(normalized).use { stream ->
                val svg = SVG.getFromInputStream(stream)
                val picture = svg.renderToPicture()
                PictureDrawable(picture).apply {
                    setBounds(0, 0, picture.width, picture.height)
                }.also { drawable ->
                    if (useCache) pictureCache.put(normalized, drawable)
                }
            }
        }.getOrNull()
    }

    suspend fun loadPictureDrawable(
        context: Context,
        assetPath: String,
        useCache: Boolean = true
    ): PictureDrawable? = withContext(Dispatchers.IO) {
        val normalized = normalizeAssetPath(assetPath)
        if (normalized.isBlank()) {
            Log.w(TAG, "Asset Not Found: blank path")
            return@withContext null
        }

        if (useCache) {
            pictureCache.get(normalized)?.let {
                Log.d(TAG, "SVG Load Success (cache): $normalized")
                return@withContext it
            }
        }

        Log.d(TAG, "SVG Load Started: $normalized")
        if (!assetExists(context, normalized)) {
            Log.w(TAG, "Asset Not Found: $normalized")
            return@withContext null
        }

        val drawable = runCatching {
            context.assets.open(normalized).use { stream ->
                val svg = SVG.getFromInputStream(stream)
                val picture = svg.renderToPicture()
                PictureDrawable(picture).apply {
                    setBounds(0, 0, picture.width, picture.height)
                }
            }
        }.onFailure { error ->
            when (error) {
                is SVGParseException -> Log.e(TAG, "SVG Load Failed (invalid svg): $normalized", error)
                is IOException -> Log.e(TAG, "SVG Load Failed (io): $normalized", error)
                else -> Log.e(TAG, "SVG Load Failed: $normalized", error)
            }
        }.getOrNull()

        if (drawable != null) {
            if (useCache) {
                pictureCache.put(normalized, drawable)
            }
            Log.d(TAG, "SVG Load Success: $normalized")
        }
        drawable
    }

    suspend fun renderToBitmap(
        context: Context,
        assetPath: String,
        width: Int,
        height: Int,
        stretchToFill: Boolean = false,
        fillBackground: Boolean = true,
        useCache: Boolean = true
    ): Bitmap? = withContext(Dispatchers.IO) {
        val normalized = normalizeAssetPath(assetPath)
        if (normalized.isBlank() || width <= 0 || height <= 0) return@withContext null

        val cacheKey = buildBitmapCacheKey(normalized, width, height, stretchToFill, fillBackground)
        if (useCache) {
            bitmapCache.get(cacheKey)?.let {
                Log.d(TAG, "SVG bitmap cache hit: $normalized")
                return@withContext it
            }
        }

        Log.d(TAG, "SVG Load Started (bitmap): $normalized ${width}x$height")
        if (!assetExists(context, normalized)) {
            Log.w(TAG, "Asset Not Found: $normalized")
            return@withContext null
        }

        val bitmap = runCatching {
            context.assets.open(normalized).use { stream ->
                val svg = SVG.getFromInputStream(stream)
                renderSvgToBitmap(svg, width, height, stretchToFill, fillBackground)
            }
        }.onFailure { error ->
            when (error) {
                is SVGParseException -> Log.e(TAG, "SVG Load Failed (invalid svg): $normalized", error)
                is IOException -> Log.e(TAG, "SVG Load Failed (io): $normalized", error)
                else -> Log.e(TAG, "SVG Load Failed: $normalized", error)
            }
        }.getOrNull()

        if (bitmap != null) {
            if (useCache) {
                bitmapCache.put(cacheKey, bitmap)
            }
            Log.d(TAG, "SVG Load Success (bitmap): $normalized size=${bitmap.width}x${bitmap.height}")
        }
        bitmap
    }

    suspend fun renderToBitmapAtDocumentSize(
        context: Context,
        assetPath: String,
        useCache: Boolean = true
    ): Bitmap? = withContext(Dispatchers.IO) {
        val normalized = normalizeAssetPath(assetPath)
        if (normalized.isBlank()) return@withContext null

        val cacheKey = "doc_$normalized"
        if (useCache) {
            bitmapCache.get(cacheKey)?.let { return@withContext it }
        }

        if (!assetExists(context, normalized)) {
            Log.w(TAG, "Asset Not Found: $normalized")
            return@withContext null
        }

        val bitmap = runCatching {
            context.assets.open(normalized).use { stream ->
                val svg = SVG.getFromInputStream(stream)
                val (width, height) = documentSize(svg)
                renderSvgToBitmap(svg, width, height, stretchToFill = false, fillBackground = true)
            }
        }.getOrNull()

        if (bitmap != null && useCache) {
            bitmapCache.put(cacheKey, bitmap)
        }
        bitmap
    }

    private fun renderSvgToBitmap(
        svg: SVG,
        width: Int,
        height: Int,
        stretchToFill: Boolean,
        fillBackground: Boolean
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (fillBackground) {
            canvas.drawColor(Color.WHITE)
        }

        val docWidth = documentWidth(svg)
        val docHeight = documentHeight(svg)
        if (stretchToFill) {
            canvas.scale(width / docWidth, height / docHeight)
        } else {
            val scale = minOf(width / docWidth, height / docHeight)
            val scaledWidth = docWidth * scale
            val scaledHeight = docHeight * scale
            canvas.translate((width - scaledWidth) / 2f, (height - scaledHeight) / 2f)
            canvas.scale(scale, scale)
        }
        svg.renderToCanvas(canvas)
        bitmap.density = Bitmap.DENSITY_NONE
        return bitmap
    }

    private fun documentSize(svg: SVG): Pair<Int, Int> {
        val width = documentWidth(svg).toInt().coerceAtLeast(1)
        val height = documentHeight(svg).toInt().coerceAtLeast(1)
        return width to height
    }

    private fun documentWidth(svg: SVG): Float {
        val width = svg.documentWidth
        if (width > 0f) return width
        val viewBox = svg.documentViewBox
        return if (viewBox != null && viewBox.width() > 0f) viewBox.width() else DEFAULT_WIDTH
    }

    private fun documentHeight(svg: SVG): Float {
        val height = svg.documentHeight
        if (height > 0f) return height
        val viewBox = svg.documentViewBox
        return if (viewBox != null && viewBox.height() > 0f) viewBox.height() else DEFAULT_HEIGHT
    }

    private fun buildBitmapCacheKey(
        assetPath: String,
        width: Int,
        height: Int,
        stretchToFill: Boolean,
        fillBackground: Boolean
    ): String {
        val stretch = if (stretchToFill) "stretch" else "fit"
        val fill = if (fillBackground) "fill" else "transparent"
        return "$assetPath@${width}x${height}_${stretch}_${fill}"
    }
}
