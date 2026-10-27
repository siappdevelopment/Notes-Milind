package com.notebook.notes.checklist.ui.activity

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.notebook.notes.checklist.Advertisement.ADSConsentManager
import com.notebook.notes.checklist.Advertisement.ADSMainClass
import com.notebook.notes.checklist.ads.AdRemoteConfig
import com.notebook.notes.checklist.ads.AdsManager
import com.notebook.notes.checklist.ads.native_drawing
import com.notebook.notes.checklist.ads.populateNativeAdView
import com.notebook.notes.checklist.Advertisement.ADSRewardedDisplay
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.ActivityDrawingBinding
import com.notebook.notes.checklist.iap.FeatureUnlockDialog
import com.notebook.notes.checklist.iap.FeatureUnlockListener
import com.notebook.notes.checklist.iap.PremiumFeature
import com.notebook.notes.checklist.iap.PremiumFeatureGate
import com.notebook.notes.checklist.ui.home.NotesSelectionDialogs
import com.notebook.notes.checklist.model.drawing.ToolType
import com.notebook.notes.checklist.model.StickerResource
import com.notebook.notes.checklist.model.TapeResource
import com.notebook.notes.checklist.storage.FileStorageManager
import com.notebook.notes.checklist.ui.drawing.DrawingRewardSessionManager
import com.notebook.notes.checklist.ui.drawing.DrawingTool
import com.notebook.notes.checklist.ui.drawing.DrawingToolbarBinder
import com.notebook.notes.checklist.ui.drawing.DrawingToolOptionsBinder
import com.notebook.notes.checklist.ui.drawing.DrawingEraserBinder
import com.notebook.notes.checklist.ui.drawing.BgPanelBinder
import com.notebook.notes.checklist.ui.drawing.DoodlePanelBinder
import com.notebook.notes.checklist.ui.drawing.PhotoPanelBinder
import com.notebook.notes.checklist.ui.drawing.ShapePanelBinder
import com.notebook.notes.checklist.ui.drawing.StickerAssetStorage
import com.notebook.notes.checklist.ui.drawing.StickerPanelBinder
import com.notebook.notes.checklist.ui.drawing.TapeAssetStorage
import com.notebook.notes.checklist.ui.drawing.DrawingExportHelper
import com.notebook.notes.checklist.ui.drawing.loadBackgroundImageFile
import com.notebook.notes.checklist.ui.drawing.loadDirectBackgroundAsset
import com.notebook.notes.checklist.viewmodel.DrawingViewModel
import com.notebook.notes.checklist.viewmodel.utils.finishWithDrawingBackInterAd
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class DrawingActivity : BaseActivity() {

    private lateinit var binding: ActivityDrawingBinding
    private val viewModel: DrawingViewModel by viewModels()
    @Inject lateinit var fileStorageManager: FileStorageManager
    private lateinit var toolbarBinder: DrawingToolbarBinder
    private lateinit var toolOptionsBinder: DrawingToolOptionsBinder
    private lateinit var photoPanelBinder: PhotoPanelBinder
    private lateinit var eraserBinder: DrawingEraserBinder
    private lateinit var doodlePanelBinder: DoodlePanelBinder
    private lateinit var bgPanelBinder: BgPanelBinder
    private lateinit var shapePanelBinder: ShapePanelBinder
    private lateinit var stickerPanelBinder: StickerPanelBinder
    private var isToolOptionsVisible = false
    private var isPhotoPanelVisible = false
    private var isEraserPanelVisible = false
    private var isDoodlePanelVisible = false
    private var isBgPanelVisible = false
    private var isShapePanelVisible = false
    private var isStickerPanelVisible = false
    private var cameraPhotoFile: File? = null
    private var selectedBackgroundAsset: String? = null
    /** When true, resume after Paywall and auto-save if Premium was purchased. */
    private var pendingSaveAfterPaywall: Boolean = false

    private val pickGalleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { applyGalleryPhoto(it) }
        }

    private val takePhotoLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                cameraPhotoFile?.absolutePath?.let { applyPhotoBackground(it) }
            }
            cameraPhotoFile = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrawingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val existingPath = intent.getStringExtra(EXTRA_EXISTING_PATH)
        viewModel.setExistingPath(existingPath)

        setupWindowInsets()
        setupTopToolbar()
        setupBackPress()
        setupDrawingView(existingPath)
        setupToolOptionsPanel()
        setupPhotoPanel()
        setupEraserPanel()
        setupDoodlePanel()
        setupBgPanel()
        setupShapePanel()
        setupStickerPanel()
        setupBottomToolbar()
        observeState()
        showDoodlePanel()
        restoreSelectedBackground(savedInstanceState)
        pendingSaveAfterPaywall =
            savedInstanceState?.getBoolean(STATE_PENDING_SAVE_AFTER_PAYWALL, false) == true
        loadOtherScreenAds()
        preloadRewardedAdIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        if (pendingSaveAfterPaywall && PremiumFeatureGate.isPremiumSafe()) {
            pendingSaveAfterPaywall = false
            performSaveDrawing()
        }
    }

    override fun onDestroy() {
        // One-shot unlock is only for this drawing session — never carry to a future drawing.
        if (isFinishing) {
            DrawingRewardSessionManager.consumeSaveAuthorization()
        }
        super.onDestroy()
    }

    /**
     * Drawing native, controlled by its own remote config key (`native_drawing`) through [AdsManager].
     * Any "no ad" outcome (disabled, premium, offline, failed) collapses the slot.
     */
    private fun loadOtherScreenAds() {
        if (!ADSConsentManager.canRequestAds(this) || ADSMainClass.getAds_Free()
            || !AdRemoteConfig.native_drawing.isEnable
        ) {
            hideOtherScreenAdPlaceholders()
            return
        }
        // Drop a previous screen's ad so it is never rendered twice; the first null is that reset.
        AdsManager.nativeDrawingAdLive.value = null
        var skippedInitialNull = true
        AdsManager.nativeDrawingAdLive.observe(this) { ad ->
            if (ad != null) {
                populateNativeAdView(
                    this,
                    ad,
                    findViewById(R.id.flNativeSmallPlaceholder),
                    findViewById(R.id.shimmer_container_banner)
                )
            } else if (!skippedInitialNull) {
                hideOtherScreenAdPlaceholders()
            }
            skippedInitialNull = false
        }
        AdsManager.loadNativeDrawing(this, R.layout.layout_native_ad_small)
    }

    private fun hideOtherScreenAdPlaceholders() {
        findViewById<View>(R.id.llOtherScreenAds).visibility = View.GONE
        findViewById<View>(R.id.shimmer_container_banner).visibility = View.GONE
        findViewById<View>(R.id.flNativeSmallPlaceholder).visibility = View.GONE
        findViewById<View>(R.id.flBannerSmallPlaceholder).visibility = View.GONE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SELECTED_BACKGROUND_ASSET, selectedBackgroundAsset)
        outState.putBoolean(STATE_PENDING_SAVE_AFTER_PAYWALL, pendingSaveAfterPaywall)
        Log.d("DrawingActivity", "onSaveInstanceState selectedBackgroundAsset=$selectedBackgroundAsset")
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            binding.main.updatePadding(
                left = systemBars.left,
                top = systemBars.top,
                right = systemBars.right,
                bottom = navBar.bottom
            )
            insets
        }
    }

    private fun setupTopToolbar() {
        val topBar = binding.drawingTopToolbar
        topBar.btnBack.setOnClickListener { finishWithDrawingBackInterAd() }
        topBar.btnUndo.setOnClickListener { viewModel.undo() }
        topBar.btnRedo.setOnClickListener { viewModel.redo() }
        topBar.btnDone.setOnClickListener { onDoneClicked() }
    }

    /**
     * Premium gate for drawing save. Intercepts Done only — canvas/editor unchanged.
     */
    private fun onDoneClicked() {
        when {
            PremiumFeatureGate.isPremiumSafe() -> performSaveDrawing()
            DrawingRewardSessionManager.isSaveAuthorized() -> performSaveDrawing()
            DrawingRewardSessionManager.canWatchReward() -> showDrawingSaveUnlockDialog()
            else -> showDrawingSavePurchaseOnly()
        }
    }

    private fun performSaveDrawing() {
        viewModel.saveDrawing {
            DrawingExportHelper.exportComposite(
                backgroundView = binding.ivDrawingBackground,
                drawingView = binding.drawingView
            )
        }
    }

    private fun showDrawingSaveUnlockDialog() {
        FeatureUnlockDialog.show(
            activity = this,
            feature = PremiumFeature.DRAWING_SAVE,
            showRewardOption = true,
            listener = object : FeatureUnlockListener {
                override fun onRewardUnlock() {
                    if (!DrawingRewardSessionManager.authorizeNextSave()) return
                    performSaveDrawing()
                }

                override fun onUpgrade() {
                    pendingSaveAfterPaywall = true
                }

                override fun onCancel() = Unit
            },
        )
    }

    private fun showDrawingSavePurchaseOnly() {
        pendingSaveAfterPaywall = true
        PremiumFeatureGate.requirePremium(
            activity = this,
            feature = PremiumFeature.DRAWING_SAVE,
            onDenied = { pendingSaveAfterPaywall = false },
        ) {
            pendingSaveAfterPaywall = false
            performSaveDrawing()
        }
    }

    private fun preloadRewardedAdIfNeeded() {
        if (PremiumFeatureGate.isPremiumSafe()) return
        if (!ADSConsentManager.canRequestAds(this)) return
        if (ADSMainClass.getAds_Free()) return
        if (!ADSMainClass.getRewardedAdsShow()) return
        if (ADSRewardedDisplay.isRewardedReady() || ADSRewardedDisplay.isRewardedLoading()) return
        ADSRewardedDisplay.preload(this)
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishWithDrawingBackInterAd()
            }
        })
    }

    private fun showMoreOptionsMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menuInflater.inflate(R.menu.menu_drawing, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_color -> {
                        showColorPickerDialog()
                        true
                    }
                    R.id.action_brush_size -> {
                        showBrushSizeDialog()
                        true
                    }
                    R.id.action_clear -> {
                        showClearConfirmation()
                        true
                    }
                    else -> false
                }
            }
        }.show()
    }

    private fun setupDrawingView(existingPath: String?) {
        binding.drawingView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        if (existingPath != null) {
            binding.ivDrawingBackground.loadBackgroundImageFile(existingPath)
        }
        binding.drawingView.onStrokeCompleted = { stroke ->
            viewModel.addStroke(stroke)
        }
        binding.drawingView.onObjectTransformStarted = { viewModel.beginObjectTransform() }
        binding.drawingView.onObjectChanged = { viewModel.updateCanvasObject(it) }
        binding.drawingView.onObjectSelectionChanged = { viewModel.selectCanvasObject(it) }
        binding.drawingView.onObjectDeleteRequested = { viewModel.deleteSelectedObject() }
        binding.drawingView.onObjectDuplicateRequested = { viewModel.duplicateSelectedObject() }
        binding.drawingView.onObjectFlipRequested = { viewModel.flipSelectedObject() }
    }

    private fun setupToolOptionsPanel() {
        toolOptionsBinder = DrawingToolOptionsBinder(
            context = this,
            binding = binding.drawingToolOptionsPanel,
            viewModel = viewModel,
            onClose = { hideDrawingToolOptions() }
        )
        toolOptionsBinder.bind()
    }

    private fun setupPhotoPanel() {
        photoPanelBinder = PhotoPanelBinder(
            binding = binding.drawingPhotoPanel,
            onClose = { hidePhotoPanel() },
            onTakePhoto = {
                hidePhotoPanel()
                launchCamera()
            },
            onChooseFromGallery = {
                hidePhotoPanel()
                pickGalleryLauncher.launch("image/*")
            }
        )
        photoPanelBinder.bind()
    }

    private fun setupEraserPanel() {
        eraserBinder = DrawingEraserBinder(
            context = this,
            binding = binding.drawingEraserPanel,
            viewModel = viewModel,
            onClose = { hideEraserPanel() }
        )
        eraserBinder.bind()
    }

    private fun setupDoodlePanel() {
        doodlePanelBinder = DoodlePanelBinder(
            context = this,
            binding = binding.drawingDoodlePanel,
            scope = lifecycleScope,
            onClose = { hideDoodlePanel() },
            onDoodleApplied = { fileName ->
                selectedBackgroundAsset = fileName
                Log.d("DrawingActivity", "Doodle selected asset=$fileName")
                binding.ivDrawingBackground.loadDirectBackgroundAsset(
                    fileName,
                    lifecycleScope
                )
            }
        )
        doodlePanelBinder.bind()
    }

    private fun setupBgPanel() {
        bgPanelBinder = BgPanelBinder(
            context = this,
            binding = binding.drawingBgPanel,
            scope = lifecycleScope,
            onClose = { hideBgPanel() },
            onBackgroundApplied = { fileName ->
                selectedBackgroundAsset = fileName
                Log.d("DrawingActivity", "Background selected asset=$fileName")
                binding.ivDrawingBackground.loadDirectBackgroundAsset(
                    fileName,
                    lifecycleScope
                ).also {  binding.drawingBgPanel.ivPreview.loadDirectBackgroundAsset(
                    fileName,
                    lifecycleScope
                ) }

            }
        )
        bgPanelBinder.bind()
    }

    private fun restoreSelectedBackground(savedInstanceState: Bundle?) {
        selectedBackgroundAsset = savedInstanceState?.getString(STATE_SELECTED_BACKGROUND_ASSET)
        val restoredAsset = selectedBackgroundAsset
        if (restoredAsset.isNullOrBlank()) {
            return
        }
        Log.d("DrawingActivity", "Restoring selected background asset=$restoredAsset")
        binding.ivDrawingBackground.loadDirectBackgroundAsset(restoredAsset, lifecycleScope)
    }

    private fun setupShapePanel() {
        shapePanelBinder = ShapePanelBinder(
            context = this,
            binding = binding.drawingStickyNotesPanel,
            scope = lifecycleScope,
            onClose = { hideShapePanel() },
            onShapeSelected = { resource ->
                applyTapeImage(resource)
            }
        )
        shapePanelBinder.bind()
    }

    private fun setupStickerPanel() {
        stickerPanelBinder = StickerPanelBinder(
            context = this,
            binding = binding.drawingStickerPanel,
            scope = lifecycleScope,
            onClose = { hideStickerPanel() },
            onStickerSelected = { resource ->
                applyStickerImage(resource)
            }
        )
        stickerPanelBinder.bind()
    }

    private fun setupBottomToolbar() {
        toolbarBinder = DrawingToolbarBinder(
            binding = binding.drawingBottomToolbar,
            onToolSelected = { tool -> handleToolSelection(tool) }
        )
        toolbarBinder.bind()
        toolbarBinder.updateSelection(viewModel.toolbarState.value.selectedTool)
    }

    private fun showDrawingToolOptions() {
        hidePhotoPanel()
        hideEraserPanel()
        hideDoodlePanel()
        hideBgPanel()
        hideShapePanel()
        hideStickerPanel()
        isToolOptionsVisible = true
        binding.drawingToolOptionsPanel.root.visibility = View.VISIBLE
        viewModel.selectDrawingTool(DrawingTool.DRAWING)
        viewModel.selectCanvasObject(null)
        applyEngineToolToView(DrawingTool.DRAWING)
    }

    private fun hideDrawingToolOptions() {
        isToolOptionsVisible = false
        binding.drawingToolOptionsPanel.root.visibility = View.GONE
    }

    private fun toggleDrawingToolOptions() {
        if (isToolOptionsVisible) {
            hideDrawingToolOptions()
        } else {
            showDrawingToolOptions()
        }
    }

    private fun showPhotoPanel() {
        hideDrawingToolOptions()
        hideEraserPanel()
        hideDoodlePanel()
        hideBgPanel()
        hideShapePanel()
        hideStickerPanel()
        isPhotoPanelVisible = true
        binding.drawingPhotoPanel.root.visibility = View.VISIBLE
        viewModel.selectDrawingTool(DrawingTool.PHOTO)
        binding.drawingView.isDrawingEnabled = false
    }

    private fun hidePhotoPanel() {
        isPhotoPanelVisible = false
        binding.drawingPhotoPanel.root.visibility = View.GONE
        clearToolbarSelectionIf(DrawingTool.PHOTO)
    }

    private fun togglePhotoPanel() {
        if (isPhotoPanelVisible) {
            hidePhotoPanel()
        } else {
            showPhotoPanel()
        }
    }

    private fun showEraserPanel() {
        hideDrawingToolOptions()
        hidePhotoPanel()
        hideDoodlePanel()
        hideBgPanel()
        hideShapePanel()
        hideStickerPanel()
        isEraserPanelVisible = true
        binding.drawingEraserPanel.root.visibility = View.VISIBLE
        viewModel.selectDrawingTool(DrawingTool.ERASER)
        viewModel.selectCanvasObject(null)
        viewModel.selectEraserSize(viewModel.uiState.value.eraserSizeDp)
        applyEngineToolToView(DrawingTool.ERASER)
    }

    private fun hideEraserPanel() {
        isEraserPanelVisible = false
        binding.drawingEraserPanel.root.visibility = View.GONE
    }

    private fun toggleEraserPanel() {
        if (isEraserPanelVisible) {
            hideEraserPanel()
        } else {
            showEraserPanel()
        }
    }

    private fun showDoodlePanel() {
        hideDrawingToolOptions()
        hidePhotoPanel()
        hideEraserPanel()
        hideBgPanel()
        hideShapePanel()
        hideStickerPanel()
        isDoodlePanelVisible = true
        binding.drawingDoodlePanel.root.visibility = View.VISIBLE
        binding.drawingDoodlePanel.ivPreview.post { doodlePanelBinder.refreshPreview() }
        viewModel.selectDrawingTool(DrawingTool.PEN)
        binding.drawingView.isDrawingEnabled = false
    }

    private fun hideDoodlePanel() {
        isDoodlePanelVisible = false
        binding.drawingDoodlePanel.root.visibility = View.GONE
        clearToolbarSelectionIf(DrawingTool.PEN)
    }

    private fun toggleDoodlePanel() {
        if (isDoodlePanelVisible) {
            hideDoodlePanel()
        } else {
            showDoodlePanel()
        }
    }

    private fun showBgPanel() {
        hideDrawingToolOptions()
        hidePhotoPanel()
        hideEraserPanel()
        hideDoodlePanel()
        hideShapePanel()
        hideStickerPanel()
        isBgPanelVisible = true
        binding.drawingBgPanel.root.visibility = View.VISIBLE
        viewModel.selectDrawingTool(DrawingTool.PEN)
        binding.drawingView.isDrawingEnabled = false
    }

    private fun hideBgPanel() {
        isBgPanelVisible = false
        binding.drawingBgPanel.root.visibility = View.GONE
        clearToolbarSelectionIf(DrawingTool.PEN)
    }

    private fun toggleBgPanel() {
        if (isBgPanelVisible) {
            hideBgPanel()
        } else {
            showBgPanel()
        }
    }

    private fun showShapePanel() {
        hideDrawingToolOptions()
        hidePhotoPanel()
        hideEraserPanel()
        hideDoodlePanel()
        hideBgPanel()
        hideStickerPanel()
        isShapePanelVisible = true
        binding.drawingStickyNotesPanel.root.visibility = View.VISIBLE
        viewModel.selectDrawingTool(DrawingTool.SHAPE)
        binding.drawingView.isDrawingEnabled = false
    }

    private fun hideShapePanel() {
        isShapePanelVisible = false
        binding.drawingStickyNotesPanel.root.visibility = View.GONE
        clearToolbarSelectionIf(DrawingTool.SHAPE)
    }

    private fun toggleShapePanel() {
        if (isShapePanelVisible) {
            hideShapePanel()
        } else {
            showShapePanel()
        }
    }

    private fun showStickerPanel() {
        hideDrawingToolOptions()
        hidePhotoPanel()
        hideEraserPanel()
        hideDoodlePanel()
        hideBgPanel()
        hideShapePanel()
        isStickerPanelVisible = true
        binding.drawingStickerPanel.root.visibility = View.VISIBLE
        viewModel.selectDrawingTool(DrawingTool.STICKER)
        binding.drawingView.isDrawingEnabled = false
    }

    private fun hideStickerPanel() {
        isStickerPanelVisible = false
        binding.drawingStickerPanel.root.visibility = View.GONE
        clearToolbarSelectionIf(DrawingTool.STICKER)
    }

    private fun toggleStickerPanel() {
        if (isStickerPanelVisible) {
            hideStickerPanel()
        } else {
            showStickerPanel()
        }
    }

    private fun handleToolSelection(tool: DrawingTool) {
        when (tool) {
            DrawingTool.PEN -> toggleDoodlePanel()
            DrawingTool.DRAWING -> toggleDrawingToolOptions()
            DrawingTool.ERASER -> toggleEraserPanel()
            DrawingTool.PHOTO -> togglePhotoPanel()
            DrawingTool.SHAPE -> {
                if (isShapePanelVisible) {
                    hideShapePanel()
                } else {
                    viewModel.selectCanvasObject(null)
                    showShapePanel()
                }
            }
            DrawingTool.STICKER -> {
                if (isStickerPanelVisible) {
                    hideStickerPanel()
                } else {
                    viewModel.selectCanvasObject(null)
                    showStickerPanel()
                }
            }
        }
    }

    private fun applyEngineToolToView(tool: DrawingTool) {
        val engineTool = when (tool) {
            DrawingTool.PEN -> ToolType.PEN
            DrawingTool.DRAWING -> viewModel.uiState.value.brushStyle.engineTool
            DrawingTool.ERASER -> ToolType.ERASER
            else -> return
        }
        binding.drawingView.setTool(engineTool)
        val state = viewModel.uiState.value
        binding.drawingView.setColor(state.selectedColor)
        val brushSize = if (tool == DrawingTool.ERASER) {
            state.eraserSizeDp
        } else {
            state.selectedBrushSize
        }
        binding.drawingView.setBrushSize(brushSize)
        val hasSelectedObject = viewModel.canvasTransformState.value.selectedObjectId != null
        binding.drawingView.isDrawingEnabled = tool.isDrawingTool && !hasSelectedObject
    }

    private fun clearToolbarSelectionIf(tool: DrawingTool) {
        if (viewModel.toolbarState.value.selectedTool == tool) {
            viewModel.clearToolbarSelection()
        }
    }

    private fun updateDrawingEnabled() {
        val hasSelectedObject = viewModel.canvasTransformState.value.selectedObjectId != null
        val toolbarTool = viewModel.toolbarState.value.selectedTool
        binding.drawingView.isDrawingEnabled =
            toolbarTool?.isDrawingTool == true && !hasSelectedObject
    }

    private fun launchCamera() {
        val imagesDir = File(filesDir, "images").apply { mkdirs() }
        val photoFile = File(imagesDir, "camera_${System.currentTimeMillis()}.jpg")
        cameraPhotoFile = photoFile
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )
        takePhotoLauncher.launch(uri)
    }

    private fun applyGalleryPhoto(uri: Uri) {
        lifecycleScope.launch {
            val path = withContext(Dispatchers.IO) {
                runCatching { fileStorageManager.copyUriToStorage(uri, "IMAGE") }.getOrNull()
            }
            if (path == null) {
                Toast.makeText(this@DrawingActivity, R.string.attachment_import_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }
            applyPhotoBackground(path)
        }
    }

    private fun applyPhotoBackground(path: String) {
        hidePhotoPanel()
        binding.drawingView.post {
            viewModel.addImageObject(
                imagePath = path,
                canvasWidth = binding.drawingView.width.toFloat(),
                canvasHeight = binding.drawingView.height.toFloat()
            )
            binding.drawingView.isDrawingEnabled = false
        }
    }

    private fun applyTapeImage(resource: TapeResource) {
        runCatching {
            val path = TapeAssetStorage.copyToFilesDir(this, resource.res)
                ?: error("Missing tape asset")
            applyTapeObject(path)
        }.onFailure {
            Toast.makeText(this, R.string.attachment_import_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyStickerImage(resource: StickerResource) {
        lifecycleScope.launch {
            runCatching {
                val path = StickerAssetStorage.prepareCanvasFile(this@DrawingActivity, resource)
                    ?: error("Missing sticker asset")
                applyStickerObject(path)
            }.onFailure {
                Toast.makeText(this@DrawingActivity, R.string.attachment_import_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyStickerObject(path: String) {
        binding.drawingView.post {
            viewModel.addStickerObject(
                imagePath = path,
                canvasWidth = binding.drawingView.width.toFloat(),
                canvasHeight = binding.drawingView.height.toFloat()
            )
            binding.drawingView.isDrawingEnabled = false
        }
    }

    private fun applyTapeObject(path: String) {
        binding.drawingView.post {
            viewModel.addStickerObject(
                imagePath = path,
                canvasWidth = binding.drawingView.width.toFloat(),
                canvasHeight = binding.drawingView.height.toFloat()
            )
            binding.drawingView.isDrawingEnabled = false
        }
    }

    private fun showShapePicker() {
        viewModel.selectDrawingTool(DrawingTool.SHAPE)
        binding.drawingView.isDrawingEnabled = false
        val shapes = arrayOf(
            getString(R.string.shape_rectangle),
            getString(R.string.shape_circle),
            getString(R.string.shape_arrow),
            getString(R.string.shape_line)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.shape)
            .setItems(shapes) { _, _ ->
                Toast.makeText(this, R.string.shape_coming_soon, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showStickerPicker() {
        viewModel.selectDrawingTool(DrawingTool.STICKER)
        binding.drawingView.isDrawingEnabled = false
        Toast.makeText(this, R.string.sticker_coming_soon, Toast.LENGTH_SHORT).show()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        val topBar = binding.drawingTopToolbar
                        topBar.btnDone.isEnabled = !state.isSaving
                        topBar.btnUndo.isEnabled = state.canUndo
                        topBar.btnRedo.isEnabled = state.canRedo
                        binding.drawingView.setColor(state.selectedColor)
                        val activeBrushSize = if (state.selectedTool == ToolType.ERASER) {
                            state.eraserSizeDp
                        } else {
                            state.selectedBrushSize
                        }
                        binding.drawingView.setBrushSize(activeBrushSize)
                        binding.drawingView.setTool(state.selectedTool)
                        binding.drawingView.setBrushStyle(state.brushStyle)
                        binding.drawingView.setShapeMode(state.shapeMode)
                        toolOptionsBinder.render(state)
                        eraserBinder.render(state)

                        state.errorMessage?.let { message ->
                            Toast.makeText(this@DrawingActivity, message, Toast.LENGTH_LONG).show()
                            viewModel.clearError()
                        }

                        if (state.saveSuccess && state.savedPath != null) {
                            DrawingRewardSessionManager.consumeSaveAuthorization()
                            pendingSaveAfterPaywall = false
                            val result = Intent().apply {
                                putExtra(EXTRA_RESULT_PATH, state.savedPath)
                                putExtra(EXTRA_BLOCK_ID, intent.getLongExtra(EXTRA_BLOCK_ID, -1L))
                            }
                            setResult(RESULT_OK, result)
                            viewModel.consumeSaveSuccess()
                            finishWithDrawingBackInterAd()
                        }
                    }
                }
                launch {
                    viewModel.canvasTransformState.collect { state ->
                        binding.drawingView.setCanvasObjects(state.objects)
                        binding.drawingView.setSelectedObjectId(state.selectedObjectId)
                        updateDrawingEnabled()
                    }
                }
                launch {
                    viewModel.toolbarState.collect { state ->
                        toolbarBinder.updateSelection(state.selectedTool)
                        updateDrawingEnabled()
                    }
                }
                launch {
                    viewModel.strokes.collect { strokes ->
                        binding.drawingView.setStrokes(strokes)
                    }
                }
            }
        }
    }

    private fun showColorPickerDialog() {
        val grid = GridLayout(this).apply {
            columnCount = 3
            setPadding(48, 24, 48, 24)
        }
        val swatchSize = (48 * resources.displayMetrics.density).toInt()
        val swatchMargin = (8 * resources.displayMetrics.density).toInt()
        val strokeWidth = (2 * resources.displayMetrics.density).toInt()
        val selectedColor = viewModel.uiState.value.selectedColor

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.text_color)
            .setView(grid)
            .setNegativeButton(R.string.cancel, null)
            .create()

        DrawingViewModel.DEFAULT_COLORS.forEach { color ->
            val swatch = View(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = swatchSize
                    height = swatchSize
                    setMargins(swatchMargin, swatchMargin, swatchMargin, swatchMargin)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    setStroke(
                        if (color == selectedColor) (3 * resources.displayMetrics.density).toInt() else strokeWidth,
                        if (color == selectedColor) Color.BLACK else Color.parseColor("#FFBDBDBD")
                    )
                }
                setOnClickListener {
                    viewModel.selectColor(color)
                    applyEngineToolToView(DrawingTool.DRAWING)
                    dialog.dismiss()
                }
            }
            grid.addView(swatch)
        }

        val customButton = com.google.android.material.button.MaterialButton(
            this,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = getString(R.string.custom_color)
            layoutParams = GridLayout.LayoutParams().apply {
                width = GridLayout.LayoutParams.MATCH_PARENT
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(0, 3)
                setMargins(swatchMargin, swatchMargin, swatchMargin, swatchMargin)
            }
            gravity = Gravity.CENTER
            setOnClickListener {
                dialog.dismiss()
                showCustomColorDialog()
            }
        }
        grid.addView(customButton)
        dialog.show()
    }

    private fun showCustomColorDialog() {
        val colors = listOf(
            Color.RED, Color.GREEN, Color.BLUE,
            Color.CYAN, Color.MAGENTA, Color.YELLOW
        )
        val grid = GridLayout(this).apply {
            columnCount = 3
            setPadding(48, 24, 48, 24)
        }
        val swatchSize = (48 * resources.displayMetrics.density).toInt()
        val swatchMargin = (8 * resources.displayMetrics.density).toInt()
        val strokeWidth = (2 * resources.displayMetrics.density).toInt()

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.custom_color)
            .setView(grid)
            .setNegativeButton(R.string.cancel, null)
            .create()

        val selectedColor = viewModel.uiState.value.selectedColor

        colors.forEachIndexed { index, color ->
            val swatch = View(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = swatchSize
                    height = swatchSize
                    setMargins(swatchMargin, swatchMargin, swatchMargin, swatchMargin)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    setStroke(
                        if (color == selectedColor) (3 * resources.displayMetrics.density).toInt() else strokeWidth,
                        if (color == selectedColor) Color.BLACK else Color.parseColor("#FFBDBDBD")
                    )
                }
                contentDescription = listOf("Red", "Green", "Blue", "Cyan", "Magenta", "Yellow")[index]
                setOnClickListener {
                    viewModel.selectColor(color)
                    applyEngineToolToView(DrawingTool.DRAWING)
                    dialog.dismiss()
                }
            }
            grid.addView(swatch)
        }

        dialog.show()
    }

    private fun showBrushSizeDialog() {
        val sizes = DrawingViewModel.BRUSH_SIZES_DP
        val labels = sizes.map { "${it.toInt()}dp" }.toTypedArray()
        val currentIndex = sizes.indexOf(viewModel.uiState.value.selectedBrushSize)
            .takeIf { it >= 0 } ?: sizes.indexOf(6f).coerceAtLeast(0)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.brush_size)
            .setSingleChoiceItems(labels, currentIndex) { dialog, which ->
                val size = sizes[which]
                viewModel.selectBrushSize(size)
                binding.drawingView.setBrushSize(size)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showClearConfirmation() {
        NotesSelectionDialogs.showConfirmation(
            context = this,
            layoutInflater = layoutInflater,
            titleRes = R.string.clear_drawing_title,
            messageRes = R.string.clear_drawing_message,
            positiveButtonRes = R.string.clear,
            iconRes = R.drawable.ic_recylerbin,
            onPositiveClick = { viewModel.clearCanvas() }
        )
    }

    companion object {
        const val EXTRA_EXISTING_PATH = "extra_existing_path"
        const val EXTRA_BLOCK_ID = "extra_block_id"
        const val EXTRA_RESULT_PATH = "extra_result_path"
        private const val STATE_SELECTED_BACKGROUND_ASSET = "state_selected_background_asset"
        private const val STATE_PENDING_SAVE_AFTER_PAYWALL = "state_pending_save_after_paywall"
    }
}
