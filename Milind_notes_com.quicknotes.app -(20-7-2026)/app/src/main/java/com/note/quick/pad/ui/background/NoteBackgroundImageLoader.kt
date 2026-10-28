package com.note.quick.pad.ui.background

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.WeakHashMap

/**
 * Loads JSON phoneImages/padImages into [ImageView] as-is (no bitmap resize).
 * Each [ImageView] has its own load job so multiple layers can load in parallel.
 */
object NoteBackgroundImageLoader {

    private const val TAG = "NoteBgImage"

    private val jobs = WeakHashMap<ImageView, Job>()

    fun clear(imageView: ImageView) {
        jobs.remove(imageView)?.cancel()
        imageView.setImageDrawable(null)
        imageView.isVisible = false
        imageView.tag = null
    }

    fun clearContainer(container: ViewGroup) {
        for (i in 0 until container.childCount) {
            (container.getChildAt(i) as? ImageView)?.let { clear(it) }
        }
        container.removeAllViews()
    }

    fun loadNative(
        imageView: ImageView,
        assets: AssetManager,
        relativePaths: List<String>,
        scope: CoroutineScope
    ) {
        jobs.remove(imageView)?.cancel()
        val paths = relativePaths
            .filter { it.isNotBlank() }
            .distinct()
            .filter { assetExists(assets, it) }

        if (paths.isEmpty()) {
            Log.w(TAG, "no asset files found for $relativePaths")
            clear(imageView)
            return
        }

        val requestKey = paths.joinToString("|")
        imageView.tag = requestKey
        imageView.isVisible = true
        imageView.adjustViewBounds = true
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER

        jobs[imageView] = scope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                paths.firstNotNullOfOrNull { path -> decodeNative(assets, path) }
            }

            if (imageView.tag != requestKey) {
                bitmap?.recycle()
                return@launch
            }

            if (bitmap == null) {
                Log.w(TAG, "decode failed for $paths")
                clear(imageView)
                return@launch
            }

            bitmap.density = Bitmap.DENSITY_NONE
            imageView.setImageBitmap(bitmap)
        }
    }

    fun loadNative(
        imageView: ImageView,
        assets: AssetManager,
        relativePath: String,
        scope: CoroutineScope
    ) {
        loadNative(imageView, assets, listOf(relativePath), scope)
    }

    /**
     * Asset streams often fail with [BitmapFactory.decodeStream] (no mark/reset).
     * Read bytes first, then decode — required for reliable WebP from assets.
     */
    private fun decodeNative(assets: AssetManager, relativePath: String): Bitmap? {
        return runCatching {
            val bytes = assets.open(relativePath).use { it.readBytes() }
            val options = BitmapFactory.Options().apply {
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            if (bitmap == null) {
                Log.w(TAG, "decodeByteArray returned null: $relativePath")
            } else {
                Log.d(TAG, "decoded OK: $relativePath ${bitmap.width}x${bitmap.height}")
            }
            bitmap
        }.onFailure {
            Log.w(TAG, "decode error: $relativePath — ${it.message}")
        }.getOrNull()
    }

    private fun assetExists(assets: AssetManager, path: String): Boolean {
        return runCatching {
            assets.open(path).close()
            true
        }.getOrDefault(false)
    }
}
