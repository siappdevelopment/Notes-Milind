package com.notebook.notes.checklist.ui.drawing

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PointF
import com.notebook.notes.checklist.model.drawing.DrawingShapeMode
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

object PencilBrushRenderer {

    private data class SoftLayer(
        val widthFactor: Float,
        val blurFactor: Float,
        val alphaFactor: Float
    )

    private val softLayers = listOf(
        SoftLayer(widthFactor = 1.28f, blurFactor = 0.22f, alphaFactor = 0.20f),
        SoftLayer(widthFactor = 1.02f, blurFactor = 0.10f, alphaFactor = 0.40f),
        SoftLayer(widthFactor = 0.76f, blurFactor = 0.03f, alphaFactor = 0.62f)
    )

    fun drawFreehand(
        canvas: Canvas,
        points: List<PointF>,
        color: Int,
        strokeWidthPx: Float
    ) {
        if (points.isEmpty()) return
        val pencilColor = toPencilColor(color)
        if (points.size == 1) {
            drawGrainDot(canvas, points.first().x, points.first().y, pencilColor, strokeWidthPx, 0.9f, 0)
            return
        }
        val smoothPath = buildSmoothPath(points)
        val pressures = computePressures(points, strokeWidthPx)
        drawSoftBase(canvas, smoothPath, pencilColor, strokeWidthPx, 0.82f)
        drawGrainTexture(canvas, smoothPath, pencilColor, strokeWidthPx, pressures)
    }

    fun drawPath(
        canvas: Canvas,
        path: Path,
        color: Int,
        strokeWidthPx: Float
    ) {
        val pencilColor = toPencilColor(color)
        drawSoftBase(canvas, path, pencilColor, strokeWidthPx, 0.78f)
        drawGrainTexture(canvas, path, pencilColor, strokeWidthPx, null)
    }

    fun drawShape(
        canvas: Canvas,
        shapeMode: DrawingShapeMode,
        start: PointF,
        end: PointF,
        color: Int,
        strokeWidthPx: Float
    ) {
        if (shapeMode == DrawingShapeMode.ARROW) {
            drawFreehand(canvas, listOf(start, end), color, strokeWidthPx)
            drawArrowHead(canvas, start, end, color, strokeWidthPx)
            return
        }
        val path = DrawingShapeRenderer.buildShapePath(shapeMode, start, end)
        drawPath(canvas, path, color, strokeWidthPx)
    }

    private fun drawSoftBase(
        canvas: Canvas,
        path: Path,
        color: Int,
        strokeWidthPx: Float,
        pressure: Float
    ) {
        softLayers.forEach { layer ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                this.color = color
                alpha = (255f * layer.alphaFactor * (0.55f + pressure * 0.45f)).toInt()
                    .coerceIn(48, 235)
                strokeWidth = (strokeWidthPx * layer.widthFactor).coerceAtLeast(1f)
                maskFilter = if (layer.blurFactor > 0f) {
                    BlurMaskFilter(strokeWidthPx * layer.blurFactor, BlurMaskFilter.Blur.NORMAL)
                } else {
                    null
                }
            }
            canvas.drawPath(path, paint)
        }
    }

    private fun drawGrainTexture(
        canvas: Canvas,
        path: Path,
        color: Int,
        strokeWidthPx: Float,
        pressures: FloatArray?
    ) {
        val spacing = (strokeWidthPx * 0.12f).coerceIn(1.1f, 2.4f)
        val pathMeasure = PathMeasure(path, false)
        val position = FloatArray(2)
        val tangent = FloatArray(2)
        val length = pathMeasure.length
        if (length <= 0f) return

        var distance = 0f
        var sampleIndex = 0
        while (distance <= length) {
            if (!pathMeasure.getPosTan(distance, position, tangent)) break
            val pressure = pressures?.getOrNull(
                ((distance / length) * (pressures.size - 1)).toInt().coerceIn(0, pressures.lastIndex)
            ) ?: 0.8f
            stampGrainCluster(
                canvas = canvas,
                centerX = position[0],
                centerY = position[1],
                tangentX = tangent[0],
                tangentY = tangent[1],
                color = color,
                strokeWidthPx = strokeWidthPx,
                pressure = pressure,
                seed = sampleIndex
            )
            distance += spacing
            sampleIndex++
        }
    }

    private fun stampGrainCluster(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        tangentX: Float,
        tangentY: Float,
        color: Int,
        strokeWidthPx: Float,
        pressure: Float,
        seed: Int
    ) {
        val tangentLength = hypot(tangentX.toDouble(), tangentY.toDouble()).toFloat().coerceAtLeast(0.001f)
        val normalX = -tangentY / tangentLength
        val normalY = tangentX / tangentLength
        val spread = strokeWidthPx * 0.42f
        val grainCount = (4 + pressure * 4).toInt()

        repeat(grainCount) { index ->
            val hash = seed * 97 + index * 131
            val offsetNormal = (hashRandom(hash) - 0.5f) * spread * 2f
            val offsetTangent = (hashRandom(hash + 17) - 0.5f) * spread * 0.35f
            val x = centerX + normalX * offsetNormal + (tangentX / tangentLength) * offsetTangent
            val y = centerY + normalY * offsetNormal + (tangentY / tangentLength) * offsetTangent
            val centerBias = 1f - (kotlin.math.abs(offsetNormal) / spread).coerceIn(0f, 1f)
            val grainPressure = (pressure * 0.55f + centerBias * 0.45f).coerceIn(0.2f, 1f)
            drawGrainDot(canvas, x, y, color, strokeWidthPx, grainPressure, hash)
        }
    }

    private fun drawGrainDot(
        canvas: Canvas,
        x: Float,
        y: Float,
        color: Int,
        strokeWidthPx: Float,
        pressure: Float,
        seed: Int
    ) {
        val radius = (strokeWidthPx * (0.045f + hashRandom(seed + 3) * 0.07f))
            .coerceIn(0.35f, strokeWidthPx * 0.16f)
        val alpha = (55f + pressure * 130f + hashRandom(seed + 5) * 50f).toInt().coerceIn(40, 220)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = color
            this.alpha = alpha
            maskFilter = BlurMaskFilter(radius * 0.35f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(x, y, radius, paint)
    }

    private fun hashRandom(seed: Int): Float {
        var value = seed * 0x45d9f3b.toInt()
        value = value xor (value ushr 16)
        value *= 0x45d9f3b.toInt()
        value = value xor (value ushr 16)
        return (value and 0x7fffffff) / 2147483647f
    }

    private fun computePressures(points: List<PointF>, baseWidthPx: Float): FloatArray {
        val pressures = FloatArray(points.size)
        val speedNormalizer = (baseWidthPx * 2.8f).coerceAtLeast(6f)
        pressures[0] = 0.92f
        for (index in 1 until points.size) {
            val dx = points[index].x - points[index - 1].x
            val dy = points[index].y - points[index - 1].y
            val distance = sqrt(dx * dx + dy * dy)
            val speedFactor = (distance / speedNormalizer).coerceIn(0f, 1f)
            val targetPressure = 1f - speedFactor * 0.68f
            pressures[index] = pressures[index - 1] * 0.35f + targetPressure * 0.65f
        }
        return pressures
    }

    private fun buildSmoothPath(points: List<PointF>): Path {
        val path = Path()
        path.moveTo(points[0].x, points[0].y)
        if (points.size == 1) {
            path.lineTo(points[0].x + 0.1f, points[0].y + 0.1f)
            return path
        }
        var index = 1
        while (index < points.size - 1) {
            val cx = points[index].x
            val cy = points[index].y
            val ex = (cx + points[index + 1].x) / 2f
            val ey = (cy + points[index + 1].y) / 2f
            path.quadTo(cx, cy, ex, ey)
            index++
        }
        path.lineTo(points.last().x, points.last().y)
        return path
    }

    /**
     * Keeps the picker hue recognizable while applying a light pencil shade.
     * Avoids the previous near-grayscale conversion that hid selected colors.
     */
    private fun toPencilColor(color: Int): Int {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        val luminance = red * 0.299f + green * 0.587f + blue * 0.114f

        // Soft graphite darkening only — chroma stays dominant.
        val shadeFactor = when {
            luminance > 230f -> 0.90f
            luminance < 40f -> 1f
            else -> 0.94f
        }
        val grayMix = 0.08f
        val gray = luminance

        return Color.rgb(
            ((red * (1f - grayMix) + gray * grayMix) * shadeFactor).toInt().coerceIn(0, 255),
            ((green * (1f - grayMix) + gray * grayMix) * shadeFactor).toInt().coerceIn(0, 255),
            ((blue * (1f - grayMix) + gray * grayMix) * shadeFactor).toInt().coerceIn(0, 255)
        )
    }

    private fun drawArrowHead(
        canvas: Canvas,
        start: PointF,
        end: PointF,
        color: Int,
        strokeWidthPx: Float
    ) {
        val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
        val headLength = min(strokeWidthPx * 3f, 36f)
        val wingAngle = Math.toRadians(28.0)
        val wingOne = PointF(
            end.x - headLength * cos(angle - wingAngle).toFloat(),
            end.y - headLength * sin(angle - wingAngle).toFloat()
        )
        val wingTwo = PointF(
            end.x - headLength * cos(angle + wingAngle).toFloat(),
            end.y - headLength * sin(angle + wingAngle).toFloat()
        )
        drawFreehand(canvas, listOf(end, wingOne), color, strokeWidthPx * 0.85f)
        drawFreehand(canvas, listOf(end, wingTwo), color, strokeWidthPx * 0.85f)
    }
}
