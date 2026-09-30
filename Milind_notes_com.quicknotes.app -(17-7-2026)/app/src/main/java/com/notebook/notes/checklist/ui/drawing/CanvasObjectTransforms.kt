package com.notebook.notes.checklist.ui.drawing

import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import com.notebook.notes.checklist.model.drawing.CanvasObject
import kotlin.math.sqrt

object CanvasObjectTransforms {

    fun buildMatrix(obj: CanvasObject): Matrix {
        val matrix = Matrix()
        matrix.postTranslate(-obj.width / 2f, -obj.height / 2f)
        matrix.postScale(obj.scaleX, obj.scaleY)
        matrix.postRotate(obj.rotation)
        matrix.postTranslate(obj.centerX, obj.centerY)
        return matrix
    }

    fun cornerPoints(obj: CanvasObject): List<PointF> {
        val points = floatArrayOf(
            0f, 0f,
            obj.width, 0f,
            obj.width, obj.height,
            0f, obj.height
        )
        buildMatrix(obj).mapPoints(points)
        return listOf(
            PointF(points[0], points[1]),
            PointF(points[2], points[3]),
            PointF(points[4], points[5]),
            PointF(points[6], points[7])
        )
    }

    fun mapWorldToLocal(obj: CanvasObject, worldX: Float, worldY: Float): PointF {
        val inverse = Matrix()
        if (!buildMatrix(obj).invert(inverse)) return PointF(worldX, worldY)
        val points = floatArrayOf(worldX, worldY)
        inverse.mapPoints(points)
        return PointF(points[0] - obj.width / 2f, points[1] - obj.height / 2f)
    }

    fun containsPoint(obj: CanvasObject, worldX: Float, worldY: Float): Boolean {
        val inverse = Matrix()
        if (!buildMatrix(obj).invert(inverse)) return false
        val points = floatArrayOf(worldX, worldY)
        inverse.mapPoints(points)
        return points[0] in 0f..obj.width && points[1] in 0f..obj.height
    }

    fun offsetFromCenter(
        center: PointF,
        corner: PointF,
        offsetPx: Float
    ): PointF {
        val dx = corner.x - center.x
        val dy = corner.y - center.y
        val length = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        return PointF(
            corner.x + dx / length * offsetPx,
            corner.y + dy / length * offsetPx
        )
    }

    fun edgeMidPoint(a: PointF, b: PointF): PointF {
        return PointF((a.x + b.x) / 2f, (a.y + b.y) / 2f)
    }

    fun axisAlignedBounds(obj: CanvasObject): RectF {
        val corners = cornerPoints(obj)
        val xs = corners.map { it.x }
        val ys = corners.map { it.y }
        return RectF(
            xs.min(),
            ys.min(),
            xs.max(),
            ys.max()
        )
    }

    fun offsetAabbCorner(bounds: RectF, corner: AabbCorner, offsetPx: Float): PointF {
        return when (corner) {
            AabbCorner.TOP_LEFT -> PointF(bounds.left - offsetPx, bounds.top - offsetPx)
            AabbCorner.TOP_RIGHT -> PointF(bounds.right + offsetPx, bounds.top - offsetPx)
            AabbCorner.BOTTOM_RIGHT -> PointF(bounds.right + offsetPx, bounds.bottom + offsetPx)
            AabbCorner.BOTTOM_LEFT -> PointF(bounds.left - offsetPx, bounds.bottom + offsetPx)
        }
    }
}

enum class AabbCorner {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_RIGHT,
    BOTTOM_LEFT
}
