package com.notebook.notes.checklist.viewmodel

import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notebook.notes.checklist.model.drawing.CanvasObject
import com.notebook.notes.checklist.model.drawing.CanvasObjectType
import com.notebook.notes.checklist.model.drawing.Stroke
import com.notebook.notes.checklist.model.drawing.ToolType
import com.notebook.notes.checklist.repository.DrawingRepository
import com.notebook.notes.checklist.model.drawing.BrushStyle
import com.notebook.notes.checklist.model.drawing.DrawingShapeMode
import com.notebook.notes.checklist.ui.drawing.CanvasBitmapDecoder
import com.notebook.notes.checklist.ui.drawing.DrawingTool
import com.notebook.notes.checklist.ui.drawing.DrawingToolbarState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.notebook.notes.checklist.ui.drawing.CanvasTransformState
import com.notebook.notes.checklist.ui.drawing.DrawingToolSettingsState
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DrawingUiState(
    val selectedColor: Int = Color.parseColor("#9C27B0"),
    val selectedBrushSize: Float = BrushStyle.WATERCOLOR.defaultSizeDp,
    val brushSizesByStyle: Map<BrushStyle, Float> = BrushStyle.defaultSizes(),
    val eraserSizeDp: Float = 18f,
    val selectedTool: ToolType = ToolType.BRUSH,
    val brushStyle: BrushStyle = BrushStyle.WATERCOLOR,
    val shapeMode: DrawingShapeMode = DrawingShapeMode.FREEHAND,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false,
    val savedPath: String? = null
)

private data class CanvasSnapshot(
    val strokes: List<Stroke>,
    val objects: List<CanvasObject>,
    val selectedObjectId: String?
)

@HiltViewModel
class DrawingViewModel @Inject constructor(
    private val drawingRepository: DrawingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DrawingUiState())
    val uiState: StateFlow<DrawingUiState> = _uiState.asStateFlow()

    val toolSettingsState: StateFlow<DrawingToolSettingsState> = uiState.map { state ->
        DrawingToolSettingsState(
            brushStyle = state.brushStyle,
            selectedColor = state.selectedColor,
            brushSizeDp = state.selectedBrushSize,
            shapeMode = state.shapeMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DrawingToolSettingsState()
    )

    private val _toolbarState = MutableStateFlow(DrawingToolbarState())
    val toolbarState: StateFlow<DrawingToolbarState> = _toolbarState.asStateFlow()

    private val _strokes = MutableStateFlow<List<Stroke>>(emptyList())
    val strokes: StateFlow<List<Stroke>> = _strokes.asStateFlow()

    private val _canvasObjects = MutableStateFlow<List<CanvasObject>>(emptyList())
    private val _selectedObjectId = MutableStateFlow<String?>(null)

    val canvasTransformState: StateFlow<CanvasTransformState> = combine(
        _canvasObjects,
        _selectedObjectId
    ) { objects, selectedId ->
        CanvasTransformState(objects = objects, selectedObjectId = selectedId)
    }.stateIn(
        scope = viewModelScope,
        // Eagerly avoids WhileSubscribed restarts re-emitting empty initialValue
        // which would clear stickers/images from DrawingView.
        started = SharingStarted.Eagerly,
        initialValue = CanvasTransformState()
    )

    private val undoStack = ArrayDeque<CanvasSnapshot>()
    private val redoStack = ArrayDeque<CanvasSnapshot>()

    var existingPath: String? = null
        private set

    fun setExistingPath(path: String?) {
        existingPath = path
    }

    fun selectTool(tool: ToolType) {
        _uiState.update { it.copy(selectedTool = tool) }
    }

    fun selectDrawingTool(tool: DrawingTool) {
        _toolbarState.update { it.copy(selectedTool = tool) }
        when (tool) {
            DrawingTool.PEN -> _uiState.update { it.copy(selectedTool = ToolType.PEN) }
            DrawingTool.DRAWING -> _uiState.update {
                it.copy(selectedTool = it.brushStyle.engineTool)
            }
            DrawingTool.ERASER -> _uiState.update { it.copy(selectedTool = ToolType.ERASER) }
            else -> Unit
        }
    }

    fun clearToolbarSelection() {
        _toolbarState.update { it.copy(selectedTool = null) }
    }

    fun selectBrushStyle(style: BrushStyle) {
        _uiState.update { state ->
            val size = state.brushSizesByStyle[style] ?: style.defaultSizeDp
            state.copy(
                brushStyle = style,
                selectedTool = style.engineTool,
                selectedBrushSize = style.clampSize(size)
            )
        }
        _toolbarState.update { it.copy(selectedTool = DrawingTool.DRAWING) }
    }

    fun selectToolOptionsColor(color: Int) {
        _uiState.update { it.copy(selectedColor = color) }
        _toolbarState.update { it.copy(selectedTool = DrawingTool.DRAWING) }
    }

    fun selectToolOptionsSize(sizeDp: Float) {
        _uiState.update { state ->
            val style = state.brushStyle
            val clamped = style.clampSize(sizeDp)
            state.copy(
                selectedBrushSize = clamped,
                brushSizesByStyle = state.brushSizesByStyle + (style to clamped)
            )
        }
        _toolbarState.update { it.copy(selectedTool = DrawingTool.DRAWING) }
    }

    fun selectShapeMode(mode: DrawingShapeMode) {
        _uiState.update { it.copy(shapeMode = mode) }
        _toolbarState.update { it.copy(selectedTool = DrawingTool.DRAWING) }
    }

    fun selectColor(color: Int) {
        _uiState.update { it.copy(selectedColor = color, selectedTool = ToolType.PEN) }
        _toolbarState.update { it.copy(selectedTool = DrawingTool.DRAWING) }
    }

    fun selectBrushSize(sizeDp: Float) {
        _uiState.update { it.copy(selectedBrushSize = sizeDp) }
    }

    fun selectEraserSize(sizeDp: Float) {
        val clamped = sizeDp.coerceIn(ERASER_MIN_DP, ERASER_MAX_DP)
        _uiState.update { it.copy(eraserSizeDp = clamped, selectedTool = ToolType.ERASER) }
        _toolbarState.update { it.copy(selectedTool = DrawingTool.ERASER) }
    }

    fun clearStrokes() {
        if (_strokes.value.isEmpty()) return
        recordHistory()
        _strokes.value = emptyList()
        updateUndoRedoState()
    }

    fun addStroke(stroke: Stroke) {
        recordHistory()
        val layered = stroke.copy(zIndex = nextZIndex())
        _strokes.update { it + layered }
        updateUndoRedoState()
    }

    fun addImageObject(imagePath: String, canvasWidth: Float, canvasHeight: Float) {
        addBitmapObject(
            imagePath = imagePath,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            type = CanvasObjectType.IMAGE,
            selectedTool = DrawingTool.PHOTO
        )
    }

    fun addStickerObject(imagePath: String, canvasWidth: Float, canvasHeight: Float) {
        addBitmapObject(
            imagePath = imagePath,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            type = CanvasObjectType.STICKER,
            selectedTool = DrawingTool.STICKER
        )
    }

    private fun addBitmapObject(
        imagePath: String,
        canvasWidth: Float,
        canvasHeight: Float,
        type: CanvasObjectType,
        selectedTool: DrawingTool
    ) {
        if (canvasWidth <= 0f || canvasHeight <= 0f) return
        val bounds = CanvasBitmapDecoder.decodeBounds(imagePath) ?: return
        recordHistory()
        val borderWidth = canvasWidth * IMAGE_BORDER_WIDTH_RATIO
        val borderHeight = canvasHeight * IMAGE_BORDER_HEIGHT_RATIO
        val fitScale = minOf(
            borderWidth / bounds.outWidth,
            borderHeight / bounds.outHeight,
            1f
        )
        val width = bounds.outWidth * fitScale
        val height = bounds.outHeight * fitScale
        val (x, y) = nextTopHorizontalPlacement(width, height, canvasWidth, canvasHeight)
        val objectId = java.util.UUID.randomUUID().toString()
        val canvasObject = CanvasObject(
            id = objectId,
            type = type,
            x = x,
            y = y,
            width = width,
            height = height,
            imagePath = imagePath,
            zIndex = nextZIndex()
        )
        _canvasObjects.update { it + canvasObject }
        _selectedObjectId.value = objectId
        _toolbarState.update { it.copy(selectedTool = selectedTool) }
        updateUndoRedoState()
    }

    /**
     * Places stickers, tape, and images along the top of the canvas, tiling
     * horizontally (and wrapping to the next row when needed). If the next
     * slot would cover an existing movable object, keep scanning before
     * falling back to a small cascade offset so additions never appear to
     * replace the previous item.
     */
    private fun nextTopHorizontalPlacement(
        width: Float,
        height: Float,
        canvasWidth: Float,
        canvasHeight: Float
    ): Pair<Float, Float> {
        val sideMargin = canvasWidth * IMAGE_SIDE_MARGIN_RATIO
        val topMargin = canvasHeight * IMAGE_TOP_MARGIN_RATIO
        val gap = IMAGE_PLACEMENT_GAP_PX
        val maxX = (canvasWidth - width - sideMargin).coerceAtLeast(sideMargin)
        val maxY = (canvasHeight - height - topMargin).coerceAtLeast(topMargin)

        val existingObjects = _canvasObjects.value
        val usableWidth = (canvasWidth - sideMargin * 2f).coerceAtLeast(width)
        val stepX = width + gap
        val stepY = height + gap
        val columns = maxOf(1, (usableWidth / stepX).toInt())
        val usableHeight = (maxY - topMargin + height).coerceAtLeast(height)
        val rows = maxOf(1, (usableHeight / stepY).toInt())
        val slotsToTry = maxOf(existingObjects.size + 1, columns * rows)

        for (index in 0 until slotsToTry) {
            val column = index % columns
            val row = index / columns
            val candidateX = (sideMargin + column * stepX).coerceIn(sideMargin, maxX)
            val candidateY = (topMargin + row * stepY).coerceIn(topMargin, maxY)
            if (existingObjects.none { overlaps(candidateX, candidateY, width, height, it) }) {
                return candidateX to candidateY
            }
        }

        val cascadeOffset = (existingObjects.size * IMAGE_CASCADE_OFFSET_PX)
        val cascadeXRange = (maxX - sideMargin).coerceAtLeast(1f)
        val cascadeYRange = (maxY - topMargin).coerceAtLeast(1f)
        val x = (sideMargin + cascadeOffset % cascadeXRange).coerceIn(sideMargin, maxX)
        val y = (topMargin + cascadeOffset % cascadeYRange).coerceIn(topMargin, maxY)
        return x to y
    }

    private fun overlaps(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        existing: CanvasObject
    ): Boolean {
        val right = x + width
        val bottom = y + height
        val existingRight = existing.x + existing.scaledWidth
        val existingBottom = existing.y + existing.scaledHeight
        return x < existingRight &&
            right > existing.x &&
            y < existingBottom &&
            bottom > existing.y
    }

    fun selectCanvasObject(objectId: String?) {
        if (objectId == null) {
            _selectedObjectId.value = null
            return
        }
        bringObjectToFront(objectId, recordUndo = true)
        _selectedObjectId.value = objectId
    }

    fun updateCanvasObject(updated: CanvasObject) {
        _canvasObjects.update { objects ->
            objects.map { existing ->
                if (existing.id != updated.id) {
                    existing
                } else {
                    // Keep any newer layer order if a transform still carries a stale zIndex.
                    updated.copy(zIndex = maxOf(existing.zIndex, updated.zIndex))
                }
            }
        }
    }

    fun beginObjectTransform() {
        recordHistory()
    }

    fun deleteSelectedObject() {
        val selectedId = _selectedObjectId.value ?: return
        recordHistory()
        _canvasObjects.update { objects -> objects.filterNot { it.id == selectedId } }
        _selectedObjectId.value = null
        updateUndoRedoState()
    }

    fun duplicateSelectedObject() {
        val selected = canvasTransformState.value.selectedObject ?: return
        recordHistory()
        val duplicate = selected.copy(
            id = java.util.UUID.randomUUID().toString(),
            x = selected.x + 24f,
            y = selected.y + 24f,
            zIndex = nextZIndex()
        )
        _canvasObjects.update { it + duplicate }
        _selectedObjectId.value = duplicate.id
        updateUndoRedoState()
    }

    fun bringSelectedToFront() {
        val selectedId = _selectedObjectId.value ?: return
        bringObjectToFront(selectedId, recordUndo = true)
    }

    fun sendSelectedToBack() {
        val selectedId = _selectedObjectId.value ?: return
        recordHistory()
        val bottomZ = minOf(
            _canvasObjects.value.minOfOrNull { it.zIndex } ?: 0,
            _strokes.value.minOfOrNull { it.zIndex } ?: 0
        ) - 1
        _canvasObjects.update { objects ->
            objects.map { if (it.id == selectedId) it.copy(zIndex = bottomZ) else it }
        }
        updateUndoRedoState()
    }

    fun flipSelectedObject() {
        val selectedId = _selectedObjectId.value ?: return
        recordHistory()
        _canvasObjects.update { objects ->
            objects.map { obj ->
                if (obj.id == selectedId) obj.copy(scaleX = -obj.scaleX) else obj
            }
        }
        updateUndoRedoState()
    }

    fun undo() {
        val current = snapshot()
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(current)
        applySnapshot(previous)
        updateUndoRedoState()
    }

    fun redo() {
        val current = snapshot()
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(current)
        applySnapshot(next)
        updateUndoRedoState()
    }

    fun clearCanvas() {
        if (_strokes.value.isEmpty() && _canvasObjects.value.isEmpty()) return
        recordHistory()
        _strokes.value = emptyList()
        _canvasObjects.value = emptyList()
        _selectedObjectId.value = null
        updateUndoRedoState()
    }

    fun saveDrawing(bitmapProvider: () -> android.graphics.Bitmap?) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, errorMessage = null, saveSuccess = false) }
        viewModelScope.launch {
            val bitmap = bitmapProvider()
            if (bitmap == null) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Failed to create drawing image")
                }
                return@launch
            }
            val result = drawingRepository.saveDrawing(bitmap, existingPath)
            result.fold(
                onSuccess = { path ->
                    _uiState.update {
                        it.copy(isSaving = false, saveSuccess = true, savedPath = path)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Failed to save drawing"
                        )
                    }
                }
            )
        }
    }

    fun consumeSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false, savedPath = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun recordHistory() {
        undoStack.addLast(snapshot())
        redoStack.clear()
    }

    private fun snapshot(): CanvasSnapshot {
        return CanvasSnapshot(
            strokes = _strokes.value,
            objects = _canvasObjects.value,
            selectedObjectId = _selectedObjectId.value
        )
    }

    private fun applySnapshot(snapshot: CanvasSnapshot) {
        _strokes.value = snapshot.strokes
        _canvasObjects.value = snapshot.objects
        _selectedObjectId.value = snapshot.selectedObjectId
    }

    /**
     * Next slot in the single ordering sequence shared by strokes and movable objects, so whatever
     * the user adds last is composited on top of everything already on the canvas.
     */
    private fun nextZIndex(): Int {
        val topObjectZ = _canvasObjects.value.maxOfOrNull { it.zIndex } ?: 0
        val topStrokeZ = _strokes.value.maxOfOrNull { it.zIndex } ?: 0
        return maxOf(topObjectZ, topStrokeZ) + 1
    }

    /**
     * Moves [objectId] above every other stroke and object. No-ops when it is already on top.
     */
    private fun bringObjectToFront(objectId: String, recordUndo: Boolean): Boolean {
        val objects = _canvasObjects.value
        val target = objects.firstOrNull { it.id == objectId } ?: return false
        val maxZ = maxOf(
            objects.maxOfOrNull { it.zIndex } ?: 0,
            _strokes.value.maxOfOrNull { it.zIndex } ?: 0
        )
        val alreadyOnTop = target.zIndex == maxZ &&
            objects.count { it.zIndex == maxZ } == 1 &&
            _strokes.value.none { it.zIndex == maxZ }
        if (alreadyOnTop) return false
        if (recordUndo) {
            recordHistory()
        }
        val topZ = maxZ + 1
        _canvasObjects.update { list ->
            list.map { if (it.id == objectId) it.copy(zIndex = topZ) else it }
        }
        if (recordUndo) {
            updateUndoRedoState()
        }
        return true
    }

    private fun updateUndoRedoState() {
        _uiState.update {
            it.copy(
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    companion object {
        val TOOL_PALETTE_COLORS = listOf(
            Color.parseColor("#9C27B0"),
            Color.parseColor("#FFEB3B"),
            Color.parseColor("#E91E63"),
            Color.parseColor("#03A9F4"),
            Color.parseColor("#673AB7"),
            Color.parseColor("#4CAF50"),
            Color.parseColor("#FF9800"),
            Color.parseColor("#F44336")
        )

        val DEFAULT_COLORS = listOf(
            Color.BLACK,
            Color.parseColor("#1976D2"),
            Color.RED,
            Color.GREEN,
            Color.YELLOW,
            Color.parseColor("#FF9800"),
            Color.parseColor("#9C27B0"),
            Color.parseColor("#E91E63"),
            Color.parseColor("#795548")
        )

        val BRUSH_SIZES_DP = listOf(2f, 4f, 6f, 8f, 12f, 16f, 24f)
        val ERASER_SIZES_DP = listOf(12f, 24f, 48f)

        const val ERASER_MIN_DP = 6f
        const val ERASER_MAX_DP = 32f
        const val ERASER_DEFAULT_DP = 18f

        fun progressToEraserSize(progress: Int): Float {
            val fraction = progress.coerceIn(0, 100) / 100f
            return ERASER_MIN_DP + (ERASER_MAX_DP - ERASER_MIN_DP) * fraction
        }

        fun eraserSizeToProgress(sizeDp: Float): Int {
            val fraction = ((sizeDp - ERASER_MIN_DP) / (ERASER_MAX_DP - ERASER_MIN_DP))
                .coerceIn(0f, 1f)
            return (fraction * 100f).toInt()
        }

        const val IMAGE_BORDER_WIDTH_RATIO = 0.62f
        const val IMAGE_BORDER_HEIGHT_RATIO = 0.50f
        const val IMAGE_TOP_MARGIN_RATIO = 0.04f
        const val IMAGE_SIDE_MARGIN_RATIO = 0.04f
        const val IMAGE_PLACEMENT_GAP_PX = 24f
        const val IMAGE_CASCADE_OFFSET_PX = 36f
    }
}
