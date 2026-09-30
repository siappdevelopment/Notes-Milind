package com.notebook.notes.checklist.util.extensions

import android.widget.ImageView
import androidx.core.view.doOnLayout
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.util.SvgAssetLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun ImageView.loadSvgFromAssets(
    fileName: String,
    scope: CoroutineScope,
    placeholderRes: Int = R.drawable.ic_placeholder,
    useCache: Boolean = true,
    onFailure: (() -> Unit)? = null
) {
    val assetPath = SvgAssetLoader.normalizeAssetPath(fileName)
    val requestKey = assetPath
    setTag(R.id.tag_svg_path, requestKey)

    SvgAssetLoader.getCachedPicture(assetPath)?.let { cached ->
        setImageDrawable(cached)
        return
    }

    if (!SvgAssetLoader.assetExists(context, assetPath)) {
        setImageResource(placeholderRes)
        onFailure?.invoke()
        return
    }

    fun startLoad() {
        val width = width.takeIf { it > 0 } ?: return
        val height = height.takeIf { it > 0 } ?: return
        val sizedKey = "$requestKey@${width}x$height"
        setTag(R.id.tag_svg_path, sizedKey)

        scope.launch {
            val bitmap = SvgAssetLoader.renderToBitmap(
                context = context,
                assetPath = assetPath,
                width = width,
                height = height,
                stretchToFill = false,
                fillBackground = false,
                useCache = useCache
            )
            withContext(Dispatchers.Main) {
                if (getTag(R.id.tag_svg_path) != sizedKey) return@withContext
                if (bitmap != null) {
                    setImageBitmap(bitmap)
                } else {
                    setImageResource(placeholderRes)
                    onFailure?.invoke()
                }
            }
        }
    }

    if (width > 0 && height > 0) {
        startLoad()
    } else {
        doOnLayout { startLoad() }
    }
}

fun ImageView.loadSvgFromAssets(
    fileName: String,
    placeholderRes: Int = R.drawable.ic_placeholder,
    useCache: Boolean = true
) {
    val assetPath = SvgAssetLoader.normalizeAssetPath(fileName)
    val drawable = SvgAssetLoader.loadPictureDrawableSync(
        context = context,
        assetPath = assetPath,
        useCache = useCache
    )
    if (drawable != null) {
        setImageDrawable(drawable)
    } else {
        setImageResource(placeholderRes)
    }
}
