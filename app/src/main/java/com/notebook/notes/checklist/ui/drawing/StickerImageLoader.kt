package com.notebook.notes.checklist.ui.drawing

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import androidx.core.view.doOnLayout
import coil.imageLoader
import coil.load
import com.notebook.notes.checklist.R
import coil.request.ImageRequest
import coil.size.Size
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

object StickerImageLoader {
    private const val MAX_SVG_CACHE_ENTRIES = 256
    private val svgBitmapCache = LruCache<String, Bitmap>(MAX_SVG_CACHE_ENTRIES)

    fun load(
        imageView: ImageView,
        fileName: String,
        scope: CoroutineScope,
        fallbacks: List<String> = emptyList(),
        onFailure: (() -> Unit)? = null
    ) {
        val resolved = StickerAssetResolver.resolve(
            imageView.context,
            fileName,
            *fallbacks.toTypedArray()
        )
        if (resolved == null) {
            imageView.setImageDrawable(null)
            onFailure?.invoke()
            return
        }
        if (StickerAssetResolver.isRaster(resolved)) {
            loadRaster(imageView, resolved, onFailure)
            return
        }
        if (StickerAssetResolver.isSvg(resolved)) {
            loadSvg(imageView, resolved, scope, onFailure)
        }
    }

    private fun loadRaster(imageView: ImageView, fileName: String, onFailure: (() -> Unit)? = null) {
        val cacheKey = StickerAssets.assetPath(fileName)
        imageView.load(StickerAssets.uri(fileName)) {
            crossfade(false)
            size(Size.ORIGINAL)
            allowHardware(false)
            memoryCacheKey(cacheKey)
            diskCacheKey(cacheKey)
            listener(onError = { _, _ -> onFailure?.invoke() })
        }
    }

    private fun loadSvg(
        imageView: ImageView,
        fileName: String,
        scope: CoroutineScope,
        onFailure: (() -> Unit)? = null
    ) {
        fun startLoad() {
            val width = imageView.width.takeIf { it > 0 } ?: 96
            val height = imageView.height.takeIf { it > 0 } ?: 96
            val cacheKey = "${StickerAssets.assetPath(fileName)}@${width}x$height"
            
            imageView.setTag(R.id.tag_sticker_path, cacheKey)

            svgBitmapCache.get(cacheKey)?.let { cached ->
                imageView.setImageBitmap(cached)
                return
            }
            scope.launch {
                val bitmap = renderSvg(imageView.context, fileName, width, height)
                if (bitmap == null) {
                    withContext(Dispatchers.Main) {
                        onFailure?.invoke()
                    }
                    return@launch
                }
                svgBitmapCache.put(cacheKey, bitmap)
                withContext(Dispatchers.Main) {
                    if (imageView.isAttachedToWindow && imageView.getTag(R.id.tag_sticker_path) == cacheKey) {
                        imageView.setImageBitmap(bitmap)
                    }
                }
            }
        }
        if (imageView.width > 0 && imageView.height > 0) {
            startLoad()
        } else {
            imageView.doOnLayout { startLoad() }
        }
    }

    suspend fun renderRaster(context: Context, fileName: String): Bitmap? = withContext(Dispatchers.IO) {
        val request = ImageRequest.Builder(context)
            .data(StickerAssets.uri(fileName))
            .size(Size.ORIGINAL)
            .allowHardware(false)
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .build()
        val result = context.imageLoader.execute(request)
        (result.drawable as? BitmapDrawable)?.bitmap?.apply { density = Bitmap.DENSITY_NONE }
    }

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun renderSvg(
        context: Context,
        fileName: String,
        width: Int,
        height: Int
    ): Bitmap? = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val appContext = context.applicationContext
            val webView = WebView(appContext)
            val svgContent = runCatching {
                appContext.assets.open(StickerAssets.assetPath(fileName)).bufferedReader().use { it.readText() }
            }.getOrNull() ?: run {
                webView.destroy()
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }
            val adjustedSvg = if (svgContent.contains("preserveAspectRatio")) {
                svgContent
            } else {
                svgContent.replaceFirst("<svg", "<svg preserveAspectRatio=\"xMidYMid meet\"")
            }
            val html = """
                <html>
                <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <style>
                html,body{margin:0;padding:0;width:${width}px;height:${height}px;background:transparent;overflow:hidden;}
                svg{display:block;width:100%;height:100%;}
                </style>
                </head>
                <body>$adjustedSvg</body>
                </html>
            """.trimIndent()
            webView.settings.javaScriptEnabled = false
            webView.isVerticalScrollBarEnabled = false
            webView.isHorizontalScrollBarEnabled = false
            webView.setBackgroundColor(Color.TRANSPARENT)
            webView.layout(0, 0, width, height)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.TRANSPARENT)
                    webView.draw(canvas)
                    bitmap.density = Bitmap.DENSITY_NONE
                    webView.destroy()
                    continuation.resume(bitmap)
                }
            }
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            continuation.invokeOnCancellation { webView.destroy() }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun renderAsset(context: Context, fileName: String, width: Int, height: Int): Bitmap? {
        return if (StickerAssetResolver.isRaster(fileName)) {
            renderRaster(context, fileName)
        } else if (StickerAssetResolver.isSvg(fileName)) {
            renderSvg(context, fileName, width, height)
        } else {
            null
        }
    }
}

fun ImageView.loadStickerAsset(
    fileName: String,
    scope: CoroutineScope,
    fallbacks: List<String> = emptyList(),
    onFailure: (() -> Unit)? = null
) {
    StickerImageLoader.load(this, fileName, scope, fallbacks, onFailure)
}
