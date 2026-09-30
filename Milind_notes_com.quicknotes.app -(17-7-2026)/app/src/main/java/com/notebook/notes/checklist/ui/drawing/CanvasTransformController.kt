package com.notebook.notes.checklist.ui.drawing

import android.view.MotionEvent
import com.notebook.notes.checklist.model.drawing.CanvasObject
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.sqrt

class CanvasTransformController(
    private val selectionRenderer: CanvasSelectionRenderer,
    private val density: Float,
    private val onTransformStarted: () -> Unit,
    private val onObjectUpdated: (CanvasObject) -> Unit,
    private val onSelectionChanged: (String?) -> Unit,
    private val onDeleteRequested: () -> Unit,
    private val onDuplicateRequested: () -> Unit,
    private val onFlipRequested: () -> Unit
) {
    private var activeHandle: TransformHandle? = null
    private var startObject: CanvasObject? = null
    private var startTouchX = 0f
    private var startTouchY = 0f
    private var startObjX = 0f
    private var startObjY = 0f
    private var startRotation = 0f
    private var startAngle = 0f
    private var startScaleX = 1f
    private var startScaleY = 1f
    private var startDistance = 1f
    private var movedDuringGesture = false
    /** True only when this controller claimed ACTION_DOWN for the current gesture. */
    private var gestureOwned = false

    fun onTouchEvent(
        event: MotionEvent,
        objects: List<CanvasObject>,
        selectedObject: CanvasObject?
    ): Boolean {
        val x = event.x
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                movedDuringGesture = false
                gestureOwned = false
                val slop = 8f * density
                selectedObject?.let { selected ->
                    val handle = selectionRenderer.hitTestHandle(selected, x, y, density, slop)
                    if (handle != null) {
                        gestureOwned = true
                        beginTransform(selected, handle, x, y)
                        return true
                    }
                    if (selectionRenderer.containsPoint(selected, x, y)) {
                        gestureOwned = true
                        beginTransform(selected, TransformHandle.MOVE, x, y)
                        return true
                    }
                }
                val hit = selectionRenderer.hitTestObject(objects, x, y)
                if (hit != null) {
                    gestureOwned = true
                    beginTransform(hit, TransformHandle.MOVE, x, y)
                    return true
                }
                if (selectedObject != null) {
                    // Consume the whole tap so deselect does not also start a pen stroke.
                    gestureOwned = true
                    onSelectionChanged(null)
                    return true
                }
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!gestureOwned) return false
                val start = startObject ?: return true
                val handle = activeHandle ?: return true
                // Prefer live object so selection bring-to-front zIndex is not overwritten.
                val obj = objects.firstOrNull { it.id == start.id } ?: start
                if (distance(x, y, startTouchX, startTouchY) > 10f * density) {
                    movedDuringGesture = true
                }
                when (handle) {
                    TransformHandle.MOVE, TransformHandle.MOVE_HANDLE -> {
                        onObjectUpdated(
                            obj.copy(
                                x = startObjX + (x - startTouchX),
                                y = startObjY + (y - startTouchY)
                            )
                        )
                    }
                    TransformHandle.ROTATE -> {
                        val angle = Math.toDegrees(
                            atan2(
                                (y - obj.centerY).toDouble(),
                                (x - obj.centerX).toDouble()
                            )
                        ).toFloat()
                        val local = CanvasObjectTransforms.mapWorldToLocal(obj, x, y)
                        val distance = max(sqrt(local.x * local.x + local.y * local.y), 1f)
                        val scaleFactor = (distance / startDistance).coerceIn(0.2f, 4f)
                        onObjectUpdated(
                            obj.copy(
                                rotation = startRotation + (angle - startAngle),
                                scaleX = startScaleX * scaleFactor,
                                scaleY = startScaleY * scaleFactor
                            )
                        )
                    }
                    TransformHandle.DELETE,
                    TransformHandle.DUPLICATE,
                    TransformHandle.FLIP -> Unit
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Critical: if we did not own DOWN, return false so DrawingView can commit the stroke.
                if (!gestureOwned) return false
                val handle = activeHandle
                if (!movedDuringGesture) {
                    when (handle) {
                        TransformHandle.DELETE -> onDeleteRequested()
                        TransformHandle.DUPLICATE -> onDuplicateRequested()
                        TransformHandle.FLIP -> onFlipRequested()
                        else -> Unit
                    }
                }
                activeHandle = null
                startObject = null
                gestureOwned = false
                return true
            }
        }
        return false
    }

    private fun beginTransform(obj: CanvasObject, handle: TransformHandle, x: Float, y: Float) {
        // Raise selection first so transform undo snapshots keep the updated layer order.
        onSelectionChanged(obj.id)
        if (handle == TransformHandle.MOVE ||
            handle == TransformHandle.MOVE_HANDLE ||
            handle == TransformHandle.ROTATE
        ) {
            onTransformStarted()
        }
        activeHandle = handle
        startObject = obj
        startTouchX = x
        startTouchY = y
        startObjX = obj.x
        startObjY = obj.y
        startRotation = obj.rotation
        startScaleX = obj.scaleX
        startScaleY = obj.scaleY
        startAngle = Math.toDegrees(
            atan2(
                (y - obj.centerY).toDouble(),
                (x - obj.centerX).toDouble()
            )
        ).toFloat()
        val local = CanvasObjectTransforms.mapWorldToLocal(obj, x, y)
        startDistance = max(sqrt(local.x * local.x + local.y * local.y), 1f)
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return sqrt(dx * dx + dy * dy)
    }
}
