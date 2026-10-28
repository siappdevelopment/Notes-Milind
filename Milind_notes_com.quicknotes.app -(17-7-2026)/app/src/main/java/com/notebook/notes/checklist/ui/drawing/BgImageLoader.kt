package com.notebook.notes.checklist.ui.drawing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.widget.ImageView
import androidx.core.view.doOnLayout
import coil.imageLoader
import coil.load
import coil.request.ImageRequest
import coil.size.Size
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.util.SvgAssetLoader
import com.notebook.notes.checklist.util.extensions.loadSvgFromAssets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object BgImageLoader {
    private const val TAG = "BgImageLoader"

    fun getCachedNativeAsset(fileName: String): Bitmap? =
        SvgAssetLoader.getCachedBitmap("doc_${BgAssets.assetPath(fileName)}")

    fun load(
        imageView: ImageView,
        fileName: String,
        scope: CoroutineScope,
        onFailure: (() -> Unit)? = null
    ) {
        if (fileName.isBlank()) {
            onFailure?.invoke()
            return
        }

        if (isRaster(fileName)) {
            imageView.load(BgAssets.uri(fileName)) {
                crossfade(false)
                listener(onError = { _, _ -> onFailure?.invoke() })
            }
            return
        }

        if (fileName.endsWith(".svg", ignoreCase = true)) {
            imageView.loadSvgFromAssets(
                fileName = BgAssets.assetPath(fileName),
                scope = scope,
                placeholderRes = R.drawable.ic_placeholder,
                onFailure = onFailure
            )
            return
        }

        onFailure?.invoke()
    }

    suspend fun loadNativeAsset(context: Context, fileName: String): Bitmap? {
        if (fileName.isBlank()) return null

        val assetPath = BgAssets.assetPath(fileName)
        if (isRaster(fileName)) {
            return withContext(Dispatchers.IO) { renderWebp(context, fileName) }
        }

        return SvgAssetLoader.renderToBitmapAtDocumentSize(
            context = context,
            assetPath = assetPath,
            useCache = true
        ).also { bitmap ->
            if (bitmap == null) {
                Log.w(TAG, "loadNativeAsset failed file=$fileName path=$assetPath")
            }
        }
    }

    suspend fun renderAssetToCache(
        context: Context,
        fileName: String,
        width: Int,
        height: Int,
        stretchToFill: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        val bitmap = renderAsset(context, fileName, width, height, stretchToFill)
            ?: return@withContext null
        val cacheDir = File(context.cacheDir, "drawing_backgrounds").apply { mkdirs() }
        val suffix = if (stretchToFill) "stretch" else "fit"
        val cacheFile = File(
            cacheDir,
            "${fileName.substringBeforeLast('.')}_${width}x${height}_$suffix.png"
        )
        FileOutputStream(cacheFile).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        bitmap.recycle()
        cacheFile.absolutePath
    }

    private suspend fun renderAsset(
        context: Context,
        fileName: String,
        width: Int,
        height: Int,
        stretchToFill: Boolean
    ): Bitmap? {
        if (isRaster(fileName)) {
            return withContext(Dispatchers.IO) { renderWebp(context, fileName) }
        }
        return SvgAssetLoader.renderToBitmap(
            context = context,
            assetPath = BgAssets.assetPath(fileName),
            width = width,
            height = height,
            stretchToFill = stretchToFill,
            fillBackground = true,
            useCache = true
        )
    }

    private suspend fun renderWebp(context: Context, fileName: String): Bitmap? =
        withContext(Dispatchers.IO) {
            val request = ImageRequest.Builder(context)
                .data(BgAssets.uri(fileName))
                .size(Size.ORIGINAL)
                .allowHardware(false)
                .bitmapConfig(Bitmap.Config.ARGB_8888)
                .build()
            val result = context.imageLoader.execute(request)
            val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
            bitmap?.apply { density = Bitmap.DENSITY_NONE }
        }

    private fun isRaster(fileName: String): Boolean {
        return fileName.endsWith(".webp", ignoreCase = true) ||
            fileName.endsWith(".png", ignoreCase = true) ||
            fileName.endsWith(".jpg", ignoreCase = true)
    }
}

fun ImageView.loadPreviewAsset(fileName: String, scope: CoroutineScope, onFailure: (() -> Unit)? = null) {
    scaleType = ImageView.ScaleType.FIT_CENTER
    loadBackgroundAsset(fileName, scope, onFailure)
}

fun ImageView.loadBackgroundAsset(fileName: String, scope: CoroutineScope, onFailure: (() -> Unit)? = null) {
    BgImageLoader.load(this, fileName, scope, onFailure)
}

fun ImageView.loadDirectBackgroundAsset(fileName: String, scope: CoroutineScope) {
    if (fileName.isBlank()) return

    applyNativeImageMatrix()
    val cacheKey = "direct_$fileName"
    setTag(R.id.tag_bg_path, cacheKey)

    BgImageLoader.getCachedNativeAsset(fileName)?.let { cached ->
        setImageBitmap(cached)
        return
    }

    scope.launch {
        val bitmap = BgImageLoader.loadNativeAsset(context, fileName) ?: return@launch
        withContext(Dispatchers.Main) {
            if (getTag(R.id.tag_bg_path) == cacheKey) {
                applyNativeImageMatrix()
                setImageBitmap(bitmap)
            }
        }
    }
}

fun ImageView.loadBackgroundImageFile(path: String) {
    applyNativeImageMatrix()
    val metrics = resources.displayMetrics
    val bitmap = CanvasBitmapDecoder.decodeSampled(
        path = path,
        maxWidth = metrics.widthPixels,
        maxHeight = metrics.heightPixels
    )
    bitmap?.density = Bitmap.DENSITY_NONE
    setImageBitmap(bitmap)
}

private fun ImageView.applyNativeImageMatrix() {
    scaleType = ImageView.ScaleType.FIT_XY
    imageMatrix = android.graphics.Matrix()
}
