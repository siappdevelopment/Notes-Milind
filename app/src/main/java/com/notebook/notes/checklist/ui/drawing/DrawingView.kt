package com.notebook.notes.checklist.ui.drawing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.notebook.notes.checklist.model.drawing.BrushStyle
import com.notebook.notes.checklist.model.drawing.CanvasObject
import com.notebook.notes.checklist.model.drawing.DrawingShapeMode
import com.notebook.notes.checklist.model.drawing.Stroke
import com.notebook.notes.checklist.model.drawing.ToolType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val drawPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val strokes = mutableListOf<Stroke>()
    private val canvasObjects = mutableListOf<CanvasObject>()
    private val objectBitmapCache = mutableMapOf<String, Bitmap>()
    private var selectedObjectId: String? = null
    private val backgroundPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

    private var backgroundBitmap: Bitmap? = null
    private var backgroundScaleMode: BackgroundScaleMode = BackgroundScaleMode.FILL
    private var contentBitmap: Bitmap? = null
    private var contentCanvas: Canvas? = null

    private var currentPoints = mutableListOf<PointF>()
    private var currentColor = Color.BLACK
    private var currentBrushSize = 6f
    private var currentTool = ToolType.PEN
    private var currentBrushStyle = BrushStyle.WATERCOLOR
    private var currentShapeMode = DrawingShapeMode.FREEHAND
    private var shapeStartPoint: PointF? = null
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    var onStrokeCompleted: ((Stroke) -> Unit)? = null
    var onObjectTransformStarted: (() -> Unit)? = null
    var onObjectChanged: ((CanvasObject) -> Unit)? = null
    var onObjectSelectionChanged: ((String?) -> Unit)? = null
    var onObjectDeleteRequested: (() -> Unit)? = null
    var onObjectDuplicateRequested: (() -> Unit)? = null
    var onObjectFlipRequested: (() -> Unit)? = null
    var isDrawingEnabled: Boolean = true

    private lateinit var selectionRenderer: CanvasSelectionRenderer
    private lateinit var transformController: CanvasTransformController

    init {
        setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val density = max(resources.displayMetrics.density, 1f)
        selectionRenderer = CanvasSelectionRenderer(context)
        transformController = CanvasTransformController(
            selectionRenderer = selectionRenderer,
            density = density,
            onTransformStarted = { onObjectTransformStarted?.invoke() },
            onObjectUpdated = { updated ->
                val index = canvasObjects.indexOfFirst { it.id == updated.id }
                if (index >= 0) {
                    val existing = canvasObjects[index]
                    val merged = updated.copy(zIndex = maxOf(existing.zIndex, updated.zIndex))
                    canvasObjects[index] = merged
                    onObjectChanged?.invoke(merged)
                } else {
                    onObjectChanged?.invoke(updated)
                }
            },
            onSelectionChanged = { id ->
                selectedObjectId = id
                if (id != null) {
                    bringLocalObjectToFront(id)
                }
                rebuildContentCache()
                onObjectSelectionChanged?.invoke(id)
                invalidate()
            },
            onDeleteRequested = { onObjectDeleteRequested?.invoke() },
            onDuplicateRequested = { onObjectDuplicateRequested?.invoke() },
            onFlipRequested = { onObjectFlipRequested?.invoke() }
        )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Photo pixels dwarf everything else the screen holds; free them as soon as the canvas
        // goes away instead of waiting for the next collection. A reattach re-decodes on demand.
        objectBitmapCache.values.forEach { it.recycle() }
        objectBitmapCache.clear()
    }

    fun setStrokes(newStrokes: List<Stroke>) {
        strokes.clear()
        strokes.addAll(newStrokes)
        rebuildContentCache()
        invalidate()
    }

    fun setCanvasObjects(objects: List<CanvasObject>) {
        val cachedBefore = cachedObjectSnapshot()
        canvasObjects.clear()
        canvasObjects.addAll(objects)
        val activePaths = objects.mapNotNull { it.imagePath }.toSet()
        val stalePaths = objectBitmapCache.keys - activePaths
        stalePaths.forEach { path ->
            objectBitmapCache.remove(path)?.recycle()
        }
        objects.forEach { obj ->
            obj.imagePath?.let { objectBitmap(it) }
        }
        // Dragging only moves the overlay object, which is painted live, so the cache can stand.
        if (cachedObjectSnapshot() != cachedBefore) {
            rebuildContentCache()
        }
        invalidate()
    }

    /**
     * Imported photos arrive at camera resolution, so they are sampled down to the canvas before
     * they ever reach memory and kept cached by path for reuse across redraws.
     */
    private fun objectBitmap(path: String): Bitmap? {
        objectBitmapCache[path]?.let { cached ->
            if (!cached.isRecycled) return cached
            objectBitmapCache.remove(path)
        }
        val limit = objectBitmapLimitPx()
        val bitmap = CanvasBitmapDecoder.decodeSampled(path, limit, limit) ?: return null
        objectBitmapCache[path] = bitmap
        return bitmap
    }

    private fun objectBitmapLimitPx(): Int {
        val metrics = resources.displayMetrics
        return max(
            max(width, height),
            max(metrics.widthPixels, metrics.heightPixels)
        ).coerceAtLeast(1)
    }

    fun setSelectedObjectId(objectId: String?) {
        val changed = selectedObjectId != objectId
        selectedObjectId = objectId
        if (changed) {
            rebuildContentCache()
        }
        invalidate()
    }

    /**
     * Immediately raises the selected object above every stroke and object in the local draw list so
     * drag/resize never paints underneath other content while ViewModel state catches up.
     */
    private fun bringLocalObjectToFront(objectId: String) {
        val index = canvasObjects.indexOfFirst { it.id == objectId }
        if (index < 0) return
        val target = canvasObjects[index]
        val maxZ = topZIndex()
        val alreadyOnTop = target.zIndex == maxZ &&
            canvasObjects.count { it.zIndex == maxZ } == 1 &&
            strokes.none { it.zIndex == maxZ }
        if (alreadyOnTop) return
        canvasObjects[index] = target.copy(zIndex = maxZ + 1)
    }

    fun setTool(tool: ToolType) {
        currentTool = tool
    }

    fun setBrushStyle(style: BrushStyle) {
        currentBrushStyle = style
    }

    fun setShapeMode(mode: DrawingShapeMode) {
        currentShapeMode = mode
    }

    fun setColor(color: Int) {
        currentColor = color
    }

    fun setBrushSize(sizeDp: Float) {
        currentBrushSize = sizeDp
    }

    fun loadBackgroundImage(path: String?) {
        backgroundScaleMode = BackgroundScaleMode.FILL
        val options = BitmapFactory.Options().apply { inScaled = false }
        val bitmap = path?.let { BitmapFactory.decodeFile(it, options) }
        bitmap?.density = Bitmap.DENSITY_NONE
        applyBackgroundBitmap(bitmap)
    }

    fun loadBackgroundAsset(fileName: String, scope: CoroutineScope) {
        scope.launch {
            val bitmap = BgImageLoader.loadNativeAsset(context, fileName)
            withContext(Dispatchers.Main) {
                backgroundScaleMode = BackgroundScaleMode.NATIVE
                bitmap?.density = Bitmap.DENSITY_NONE
                applyBackgroundBitmap(bitmap)
            }
        }
    }

    fun loadBackgroundFromAssets(relativePath: String) {
        backgroundScaleMode = BackgroundScaleMode.NATIVE
        val options = BitmapFactory.Options().apply { inScaled = false }
        val bitmap = runCatching {
            context.assets.open(relativePath).use {
                BitmapFactory.decodeStream(it, null, options)
            }
        }.getOrNull()
        bitmap?.density = Bitmap.DENSITY_NONE
        applyBackgroundBitmap(bitmap)
    }

    private fun applyBackgroundBitmap(bitmap: Bitmap?) {
        backgroundBitmap = bitmap
        rebuildContentCache()
        invalidate()
    }

    fun exportBitmap(): Bitmap? = exportContentBitmap()

    fun exportContentBitmap(): Bitmap? {
        val width = width.takeIf { it > 0 } ?: return null
        val height = height.takeIf { it > 0 } ?: return null
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        drawContent(Canvas(result), skipObjectId = null)
        return result
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            contentBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            contentCanvas = Canvas(contentBitmap!!)
            rebuildContentCache()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val isErasing = currentPoints.isNotEmpty() && currentTool == ToolType.ERASER
        // An eraser clears pixels instead of painting them, so the composite it eats into has to
        // sit in its own layer for the cleared gap to fall through to the background behind the
        // canvas rather than smear the window black.
        val layerCount = if (isErasing) canvas.saveLayer(null, null) else canvas.save()
        contentBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        overlayObject()?.let { obj ->
            CanvasObjectRenderer.draw(canvas, obj, obj.imagePath?.let(::objectBitmap))
        }
        if (isErasing) {
            drawStroke(canvas, previewStroke())
        }
        canvas.restoreToCount(layerCount)
        if (::selectionRenderer.isInitialized) {
            val density = max(resources.displayMetrics.density, 1f)
            canvasObjects.firstOrNull { it.id == selectedObjectId }?.let { selected ->
                selectionRenderer.draw(canvas, selected, density)
            }
        }
        if (currentPoints.isNotEmpty() && !isErasing) {
            drawStroke(canvas, previewStroke())
        }
    }

    private fun drawLegacyBackground(canvas: Canvas, viewWidth: Int, viewHeight: Int) {
        val bg = backgroundBitmap ?: return
        val dest = computeBackgroundRect(viewWidth, viewHeight, bg)
        canvas.drawBitmap(bg, null, dest, backgroundPaint)
    }

    private fun computeBackgroundRect(viewWidth: Int, viewHeight: Int, bg: Bitmap): RectF {
        return when (backgroundScaleMode) {
            BackgroundScaleMode.FILL -> RectF(
                0f,
                0f,
                viewWidth.toFloat(),
                viewHeight.toFloat()
            )
            BackgroundScaleMode.FIT -> computeFitBackgroundRect(viewWidth, viewHeight, bg)
            BackgroundScaleMode.NATIVE -> RectF(
                0f,
                0f,
                bg.width.toFloat(),
                bg.height.toFloat()
            )
        }
    }

    private fun computeFitBackgroundRect(viewWidth: Int, viewHeight: Int, bg: Bitmap): RectF {
        val scale = min(
            viewWidth.toFloat() / bg.width,
            viewHeight.toFloat() / bg.height
        )
        val scaledW = bg.width * scale
        val scaledH = bg.height * scale
        val left = (viewWidth - scaledW) / 2f
        val top = (viewHeight - scaledH) / 2f
        return RectF(left, top, left + scaledW, top + scaledH)
    }

    private enum class BackgroundScaleMode {
        FILL,
        FIT,
        NATIVE
    }

    /**
     * Paints strokes and movable objects in a single z ordered pass, so ink covers only the images,
     * stickers and shapes that were already on the canvas when the ink was drawn — and anything
     * added afterwards sits on top of that ink.
     */
    private fun drawContent(canvas: Canvas, skipObjectId: String?) {
        val layers = ArrayList<Layer>(canvasObjects.size + strokes.size)
        canvasObjects.forEach { obj ->
            if (obj.id != skipObjectId) layers.add(Layer.Movable(obj))
        }
        strokes.forEach { layers.add(Layer.Ink(it)) }
        layers.sortedBy { it.zIndex }.forEach { layer ->
            when (layer) {
                is Layer.Movable -> CanvasObjectRenderer.draw(
                    canvas,
                    layer.obj,
                    layer.obj.imagePath?.let(::objectBitmap)
                )
                is Layer.Ink -> drawStroke(canvas, layer.stroke)
            }
        }
    }

    private sealed class Layer(val zIndex: Int) {
        class Movable(val obj: CanvasObject) : Layer(obj.zIndex)
        class Ink(val stroke: Stroke) : Layer(stroke.zIndex)
    }

    /**
     * The selected object is kept out of the cached composite and painted live on top, so dragging
     * and resizing stay smooth without re-rendering every stroke each frame. Only applies while it
     * is the topmost layer, which selection guarantees by raising it to the front.
     */
    private fun overlayObject(): CanvasObject? {
        val selected = canvasObjects.firstOrNull { it.id == selectedObjectId } ?: return null
        return selected.takeIf { it.zIndex >= topZIndex() }
    }

    private fun topZIndex(): Int {
        return max(
            canvasObjects.maxOfOrNull { it.zIndex } ?: 0,
            strokes.maxOfOrNull { it.zIndex } ?: 0
        )
    }

    private fun cachedObjectSnapshot(): List<CanvasObject> {
        val overlayId = overlayObject()?.id
        return canvasObjects.filterNot { it.id == overlayId }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (::transformController.isInitialized &&
            (canvasObjects.isNotEmpty() || selectedObjectId != null)
        ) {
            val selected = canvasObjects.firstOrNull { it.id == selectedObjectId }
            if (transformController.onTouchEvent(event, canvasObjects.toList(), selected)) {
                invalidate()
                return true
            }
        }
        if (!isDrawingEnabled) return false
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (activeShapeMode() == DrawingShapeMode.FREEHAND) {
                    currentPoints = mutableListOf(PointF(x, y))
                } else {
                    shapeStartPoint = PointF(x, y)
                    currentPoints = mutableListOf(shapeStartPoint!!, PointF(x, y))
                }
                lastTouchX = x
                lastTouchY = y
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                if (activeShapeMode() == DrawingShapeMode.FREEHAND) {
                    val midX = (lastTouchX + x) / 2f
                    val midY = (lastTouchY + y) / 2f
                    currentPoints.add(PointF(midX, midY))
                    currentPoints.add(PointF(x, y))
                } else {
                    val start = shapeStartPoint ?: return true
                    currentPoints = mutableListOf(start, PointF(x, y))
                }
                lastTouchX = x
                lastTouchY = y
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val hadStrokeGesture = shapeStartPoint != null || currentPoints.isNotEmpty()
                if (hadStrokeGesture) {
                    if (activeShapeMode() == DrawingShapeMode.FREEHAND) {
                        currentPoints.add(PointF(x, y))
                    } else {
                        val start = shapeStartPoint ?: currentPoints.firstOrNull() ?: PointF(x, y)
                        currentPoints = mutableListOf(start, PointF(x, y))
                    }
                    if (currentPoints.isNotEmpty()) {
                        onStrokeCompleted?.invoke(previewStroke())
                    }
                }
                currentPoints = mutableListOf()
                shapeStartPoint = null
                invalidate()
            }
        }
        return true
    }

    private fun previewStroke(): Stroke {
        return Stroke(
            points = currentPoints.toList(),
            color = currentColor,
            brushSize = currentBrushSize,
            toolType = currentTool,
            brushStyle = currentBrushStyle,
            shapeMode = activeShapeMode()
        )
    }

    /**
     * The eraser always follows the finger, whatever shape the brush was left on. Constraining it
     * to a circle or rectangle would only let it remove a whole outline, never the part of a
     * drawing the user actually dragged across. The brush keeps its own shape for when it returns.
     */
    private fun activeShapeMode(): DrawingShapeMode {
        return if (currentTool == ToolType.ERASER) {
            DrawingShapeMode.FREEHAND
        } else {
            currentShapeMode
        }
    }

    private fun rebuildContentCache() {
        val canvas = contentCanvas ?: return
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        drawContent(canvas, skipObjectId = overlayObject()?.id)
    }

    private fun drawStroke(canvas: Canvas, stroke: Stroke) {
        if (stroke.points.isEmpty()) return
        if (stroke.toolType == ToolType.ERASER) {
            configurePaint(stroke)
            if (stroke.shapeMode == DrawingShapeMode.FREEHAND) {
                canvas.drawPath(buildSmoothPath(stroke.points), drawPaint)
            } else if (stroke.points.size >= 2) {
                DrawingShapeRenderer.draw(
                    canvas,
                    stroke.shapeMode,
                    stroke.points.first(),
                    stroke.points.last(),
                    drawPaint
                )
            }
            return
        }
        if (stroke.brushStyle == BrushStyle.OUTLINE) {
            val strokeWidthPx = dpToPx(stroke.brushSize)
            if (stroke.shapeMode == DrawingShapeMode.FREEHAND) {
                OutlineBrushRenderer.drawPath(
                    canvas,
                    buildSmoothPath(stroke.points),
                    stroke.color,
                    strokeWidthPx
                )
            } else if (stroke.points.size >= 2) {
                OutlineBrushRenderer.drawShape(
                    canvas,
                    stroke.shapeMode,
                    stroke.points.first(),
                    stroke.points.last(),
                    stroke.color,
                    strokeWidthPx
                )
            }
            return
        }
        if (stroke.brushStyle == BrushStyle.PENCIL) {
            val strokeWidthPx = dpToPx(stroke.brushSize)
            if (stroke.shapeMode == DrawingShapeMode.FREEHAND) {
                PencilBrushRenderer.drawFreehand(
                    canvas,
                    stroke.points,
                    stroke.color,
                    strokeWidthPx
                )
            } else if (stroke.points.size >= 2) {
                PencilBrushRenderer.drawShape(
                    canvas,
                    stroke.shapeMode,
                    stroke.points.first(),
                    stroke.points.last(),
                    stroke.color,
                    strokeWidthPx
                )
            }
            return
        }
        if (stroke.brushStyle == BrushStyle.DOTTED) {
            val strokeWidthPx = dpToPx(stroke.brushSize)
            if (stroke.shapeMode == DrawingShapeMode.FREEHAND) {
                DottedBrushRenderer.drawFreehand(
                    canvas,
                    stroke.points,
                    stroke.color,
                    strokeWidthPx
                )
            } else if (stroke.points.size >= 2) {
                DottedBrushRenderer.drawShape(
                    canvas,
                    stroke.shapeMode,
                    stroke.points.first(),
                    stroke.points.last(),
                    stroke.color,
                    strokeWidthPx
                )
            }
            return
        }
        if (stroke.shapeMode == DrawingShapeMode.FREEHAND) {
            configurePaint(stroke)
            canvas.drawPath(buildSmoothPath(stroke.points), drawPaint)
        } else if (stroke.points.size >= 2) {
            configureShapePaint(stroke)
            DrawingShapeRenderer.draw(
                canvas,
                stroke.shapeMode,
                stroke.points.first(),
                stroke.points.last(),
                drawPaint
            )
        }
    }

    private fun configurePaint(stroke: Stroke) {
        if (stroke.toolType == ToolType.ERASER) {
            configureEraserPaint(stroke)
            return
        }
        drawPaint.xfermode = null
        DrawingPaintStyle.apply(
            drawPaint,
            stroke.brushStyle,
            stroke.color,
            stroke.brushSize,
            max(resources.displayMetrics.density, 1f)
        )
    }

    /**
     * Clears the pixels it passes over rather than painting white, so a stroke can be rubbed off
     * partway and whatever background sits behind the canvas comes back through the gap.
     */
    private fun configureEraserPaint(stroke: Stroke) {
        drawPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        drawPaint.pathEffect = null
        drawPaint.maskFilter = null
        drawPaint.style = Paint.Style.STROKE
        drawPaint.color = Color.BLACK
        drawPaint.alpha = 255
        drawPaint.strokeWidth = dpToPx(stroke.brushSize)
        drawPaint.strokeCap = Paint.Cap.ROUND
        drawPaint.strokeJoin = Paint.Join.ROUND
    }

    private fun configureShapePaint(stroke: Stroke) {
        if (stroke.toolType == ToolType.ERASER) {
            configureEraserPaint(stroke)
            return
        }
        drawPaint.xfermode = null
        DrawingPaintStyle.applyForShape(
            drawPaint,
            stroke.brushStyle,
            stroke.color,
            stroke.brushSize,
            max(resources.displayMetrics.density, 1f),
            stroke.shapeMode
        )
    }

    private fun buildSmoothPath(points: List<PointF>): Path {
        val path = Path()
        path.moveTo(points[0].x, points[0].y)
        if (points.size == 1) {
            path.lineTo(points[0].x + 0.1f, points[0].y + 0.1f)
            return path
        }
        var i = 1
        while (i < points.size - 1) {
            val cx = points[i].x
            val cy = points[i].y
            val ex = (cx + points[i + 1].x) / 2f
            val ey = (cy + points[i + 1].y) / 2f
            path.quadTo(cx, cy, ex, ey)
            i++
        }
        path.lineTo(points.last().x, points.last().y)
        return path
    }

    private fun dpToPx(dp: Float): Float {
        return dp * max(resources.displayMetrics.density, 1f)
    }
}
