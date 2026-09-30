package com.notebook.notes.checklist.ui.drawing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log

/**
 * Decodes user supplied photos down to a size the canvas can actually hold. A full resolution
 * camera shot is tens of megabytes once expanded to ARGB pixels and its longest edge also runs
 * past the GPU texture limit, so decoding it verbatim takes down the process. Canvas objects are
 * never painted larger than the view, which makes screen sized pixels the most quality that can
 * ever reach the screen.
 */
internal object CanvasBitmapDecoder {

    private const val TAG = "CanvasBitmapDecoder"

    /** Lowest max texture size guaranteed by GLES2, so a hardware canvas can always upload it. */
    private const val MAX_SIDE_PX = 2048
    private const val MAX_PIXELS = 4_000_000L
    private const val OOM_RETRIES = 3

    fun decodeBounds(path: String): BitmapFactory.Options? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching { BitmapFactory.decodeFile(path, options) }
        return options.takeIf { it.outWidth > 0 && it.outHeight > 0 }
    }

    fun decodeSampled(path: String, maxWidth: Int, maxHeight: Int): Bitmap? {
        val bounds = decodeBounds(path) ?: return null
        var sampleSize = calculateInSampleSize(
            width = bounds.outWidth,
            height = bounds.outHeight,
            maxWidth = maxWidth.coerceIn(1, MAX_SIDE_PX),
            maxHeight = maxHeight.coerceIn(1, MAX_SIDE_PX)
        )
        repeat(OOM_RETRIES) {
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            // Also catches OutOfMemoryError, which a fragmented heap can still throw at a size
            // that fits the budget; halving each side again quarters the next attempt.
            runCatching { BitmapFactory.decodeFile(path, options) }
                .getOrNull()
                ?.let { return it }
            sampleSize *= 2
        }
        Log.w(TAG, "decode failed path=$path source=${bounds.outWidth}x${bounds.outHeight}")
        return null
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        maxWidth: Int,
        maxHeight: Int
    ): Int {
        var sampleSize = 1
        while (width / sampleSize > maxWidth || height / sampleSize > maxHeight) {
            sampleSize *= 2
        }
        // Guards extreme aspect ratios, where staying under both edge limits can still leave a
        // pixel count large enough to exhaust the heap.
        while ((width / sampleSize).toLong() * (height / sampleSize) > MAX_PIXELS) {
            sampleSize *= 2
        }
        return sampleSize
    }
}
