package com.notebook.notes.checklist.ui.drawing

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.model.drawing.CanvasObject
import kotlin.math.sqrt

class CanvasSelectionRenderer(context: Context) {

    private val deleteIcon = tintedIcon(context, R.drawable.ic_canvas_delete)
    private val flipIcon = tintedIcon(context, R.drawable.ic_canvas_layer)
    private val duplicateIcon = tintedIcon(context, R.drawable.ic_canvas_duplicate)
    private val rotateIcon = tintedIcon(context, R.drawable.ic_canvas_rotate)
    private val dragIcon = tintedIcon(context, R.drawable.ic_canvas_drag)

    private val handleShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#33000000")
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#CC000000")
    }

    private val handleFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val handleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#33000000")
    }

    private val dragPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val dragPillStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#33000000")
    }

    fun draw(canvas: Canvas, obj: CanvasObject, density: Float) {
        borderPaint.strokeWidth = 2.5f * density
        val corners = CanvasObjectTransforms.cornerPoints(obj)
        for (i in corners.indices) {
            val next = corners[(i + 1) % corners.size]
            canvas.drawLine(corners[i].x, corners[i].y, next.x, next.y, borderPaint)
        }

        val buttonRadius = ACTION_BUTTON_RADIUS_DP * density
        handleCenters(obj, density).forEach { (handle, center) ->
            when (handle) {
                TransformHandle.MOVE_HANDLE -> drawDragPill(canvas, center, density)
                else -> drawActionButton(canvas, center, buttonRadius, iconForHandle(handle))
            }
        }
    }

    fun handleCenters(obj: CanvasObject, density: Float): Map<TransformHandle, PointF> {
        val bounds = CanvasObjectTransforms.axisAlignedBounds(obj)
        val offset = HANDLE_OUTSET_DP * density
        return mapOf(
            TransformHandle.DELETE to CanvasObjectTransforms.offsetAabbCorner(
                bounds, AabbCorner.TOP_LEFT, offset
            ),
            TransformHandle.FLIP to CanvasObjectTransforms.offsetAabbCorner(
                bounds, AabbCorner.TOP_RIGHT, offset
            ),
            TransformHandle.ROTATE to CanvasObjectTransforms.offsetAabbCorner(
                bounds, AabbCorner.BOTTOM_RIGHT, offset
            ),
            TransformHandle.DUPLICATE to CanvasObjectTransforms.offsetAabbCorner(
                bounds, AabbCorner.BOTTOM_LEFT, offset
            ),
            TransformHandle.MOVE_HANDLE to PointF(
                bounds.centerX(),
                bounds.top - offset * 0.85f
            )
        )
    }

    fun containsPoint(obj: CanvasObject, x: Float, y: Float): Boolean {
        return CanvasObjectTransforms.containsPoint(obj, x, y)
    }

    fun hitTestHandle(
        obj: CanvasObject,
        x: Float,
        y: Float,
        density: Float,
        slop: Float
    ): TransformHandle? {
        val buttonRadius = ACTION_BUTTON_RADIUS_DP * density + slop
        handleCenters(obj, density).forEach { (handle, center) ->
            if (handle == TransformHandle.MOVE_HANDLE) {
                if (dragPillBounds(center, density).contains(x, y)) return handle
            } else if (distance(x, y, center.x, center.y) <= buttonRadius) {
                return handle
            }
        }
        return null
    }

    fun hitTestObject(objects: List<CanvasObject>, x: Float, y: Float): CanvasObject? {
        return objects.sortedByDescending { it.zIndex }.firstOrNull { containsPoint(it, x, y) }
    }

    private fun drawActionButton(
        canvas: Canvas,
        center: PointF,
        radius: Float,
        icon: Drawable
    ) {
        val shadowOffset = 2f
        canvas.drawCircle(center.x, center.y + shadowOffset, radius, handleShadowPaint)
        canvas.drawCircle(center.x, center.y, radius, handleFillPaint)
        canvas.drawCircle(center.x, center.y, radius, handleStrokePaint)
        val iconSize = (radius * 1.15f).toInt()
        val left = (center.x - iconSize / 2f).toInt()
        val top = (center.y - iconSize / 2f).toInt()
        icon.setBounds(left, top, left + iconSize, top + iconSize)
        icon.draw(canvas)
    }

    private fun drawDragPill(canvas: Canvas, center: PointF, density: Float) {
        val pillWidth = DRAG_PILL_WIDTH_DP * density
        val pillHeight = DRAG_PILL_HEIGHT_DP * density
        val rect = RectF(
            center.x - pillWidth / 2f,
            center.y - pillHeight / 2f,
            center.x + pillWidth / 2f,
            center.y + pillHeight / 2f
        )
        val shadowRect = RectF(rect).apply { offset(0f, 2f) }
        canvas.drawRoundRect(shadowRect, pillHeight / 2f, pillHeight / 2f, handleShadowPaint)
        canvas.drawRoundRect(rect, pillHeight / 2f, pillHeight / 2f, dragPillPaint)
        canvas.drawRoundRect(rect, pillHeight / 2f, pillHeight / 2f, dragPillStrokePaint)
        val iconSize = (pillHeight * 0.9f).toInt()
        val left = (center.x - iconSize / 2f).toInt()
        val top = (center.y - iconSize / 2f).toInt()
        dragIcon.setBounds(left, top, left + iconSize, top + iconSize)
        dragIcon.draw(canvas)
    }

    private fun dragPillBounds(center: PointF, density: Float): RectF {
        val pillWidth = DRAG_PILL_WIDTH_DP * density
        val pillHeight = DRAG_PILL_HEIGHT_DP * density
        return RectF(
            center.x - pillWidth / 2f,
            center.y - pillHeight / 2f,
            center.x + pillWidth / 2f,
            center.y + pillHeight / 2f
        )
    }

    private fun iconForHandle(handle: TransformHandle): Drawable = when (handle) {
        TransformHandle.DELETE -> deleteIcon
        TransformHandle.FLIP -> flipIcon
        TransformHandle.DUPLICATE -> duplicateIcon
        TransformHandle.ROTATE -> rotateIcon
        else -> deleteIcon
    }

    private fun tintedIcon(context: Context, drawableId: Int): Drawable {
        val drawable = AppCompatResources.getDrawable(context, drawableId)!!.mutate()
        DrawableCompat.setTint(drawable, Color.BLACK)
        return drawable
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return sqrt(dx * dx + dy * dy)
    }

    companion object {
        const val ACTION_BUTTON_RADIUS_DP = 18f
        const val HANDLE_OUTSET_DP = 20f
        const val DRAG_PILL_WIDTH_DP = 34f
        const val DRAG_PILL_HEIGHT_DP = 18f
    }
}

enum class TransformHandle {
    MOVE,
    MOVE_HANDLE,
    DELETE,
    FLIP,
    DUPLICATE,
    ROTATE
}
