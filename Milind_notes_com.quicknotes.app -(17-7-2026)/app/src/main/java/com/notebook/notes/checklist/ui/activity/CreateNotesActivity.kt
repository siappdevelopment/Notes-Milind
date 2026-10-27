package com.notebook.notes.checklist.ui.activity

import android.content.Intent
import android.Manifest
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.facebook.shimmer.ShimmerFrameLayout
import android.view.inputmethod.EditorInfo
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.viewModels
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import kotlin.math.max
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.text.InputFilter
import android.text.InputType
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.notebook.notes.checklist.Advertisement.ADSConsentManager
import com.notebook.notes.checklist.Advertisement.ADSMainClass
import com.notebook.notes.checklist.ads.AdRemoteConfig
import com.notebook.notes.checklist.ads.AdsManager
import com.notebook.notes.checklist.ads.create_note
import com.notebook.notes.checklist.ads.populateNativeAdView
import com.notebook.notes.checklist.Advertisement.ADSRewardedDisplay
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.ActivityCreateNotesBinding
import com.notebook.notes.checklist.databinding.DialogAddLinkBinding
import com.notebook.notes.checklist.iap.PremiumFeature
import com.notebook.notes.checklist.iap.PremiumFeatureGate
import com.notebook.notes.checklist.ui.home.NotesSelectionDialogs
import com.notebook.notes.checklist.helper.NotePasswordManager
import com.notebook.notes.checklist.helper.PermissionHelper
import com.notebook.notes.checklist.helper.PreferenceHelper
import com.notebook.notes.checklist.utils.Constant
import com.notebook.notes.checklist.model.BlockType
import com.notebook.notes.checklist.model.BulletListHelper
import com.notebook.notes.checklist.model.BulletListMetadata
import com.notebook.notes.checklist.model.NoteBlock
import com.notebook.notes.checklist.model.NoteBlockExtra
import com.notebook.notes.checklist.model.NoteContentItem
import com.notebook.notes.checklist.model.NumberListHelper
import com.notebook.notes.checklist.model.NumberListMetadata
import com.notebook.notes.checklist.model.RepeatType
import com.notebook.notes.checklist.model.RichTextDocument
import com.notebook.notes.checklist.model.RichTextSpan
import com.notebook.notes.checklist.ui.adapter.NoteContentAdapter
import com.notebook.notes.checklist.ui.background.NoteBackgroundApplier
import com.notebook.notes.checklist.ui.background.NoteBackgroundPanel
import com.notebook.notes.checklist.ui.background.NoteBackgroundRepository
import com.notebook.notes.checklist.ui.category.CategorySelectorPopup
import com.notebook.notes.checklist.ui.category.CreateLabelBottomSheet
import com.notebook.notes.checklist.ui.emoji.EmojiPanel
import com.notebook.notes.checklist.ui.emoji.EmojiViewModel
import com.notebook.notes.checklist.ui.fragment.ReminderPermissionBottomSheet
import com.notebook.notes.checklist.ui.note.AttachmentOption
import com.notebook.notes.checklist.ui.note.ExportBottomSheet
import com.notebook.notes.checklist.ui.note.InsertAudioBottomSheet
import com.notebook.notes.checklist.ui.note.NoteDetails
import com.notebook.notes.checklist.ui.note.NoteDetailsBottomSheet
import com.notebook.notes.checklist.ui.note.NoteHeaderBottomSheets
import com.notebook.notes.checklist.ui.note.NoteMoreOptionsConfig
import com.notebook.notes.checklist.ui.note.NoteMoreOptionsListener
import com.notebook.notes.checklist.ui.note.NoteMoreOptionsPopup
import com.notebook.notes.checklist.ui.note.SetReminderBottomSheet
import com.notebook.notes.checklist.ui.richtext.ChecklistOption
import com.notebook.notes.checklist.ui.richtext.ChecklistPanel
import com.notebook.notes.checklist.ui.richtext.FontSizePanel
import com.notebook.notes.checklist.ui.richtext.FontStylePanel
import com.notebook.notes.checklist.ui.richtext.MoreToolAction
import com.notebook.notes.checklist.ui.richtext.RichTextConverter
import com.notebook.notes.checklist.ui.richtext.RichTextEditorController
import com.notebook.notes.checklist.ui.richtext.RichTextEditorHost
import com.notebook.notes.checklist.ui.richtext.RichTextFormats
import com.notebook.notes.checklist.ui.richtext.RichTextToolbarBinder
import com.notebook.notes.checklist.ui.richtext.TextFormattingPanel
import com.notebook.notes.checklist.util.CategoryIconResolver
import com.notebook.notes.checklist.util.BottomSheetHelper
import com.notebook.notes.checklist.util.NoteColorUtils
import com.notebook.notes.checklist.util.DateTimeUtils
import com.notebook.notes.checklist.util.NoteShareTextFormatter
import com.notebook.notes.checklist.util.SystemBars
import com.notebook.notes.checklist.util.NoteDateFormatter
import com.notebook.notes.checklist.util.SearchHighlightHelper
import com.notebook.notes.checklist.util.SearchHighlightPalette
import com.notebook.notes.checklist.utils.JsonUtils
import com.notebook.notes.checklist.viewmodel.AttachmentViewModel
import com.notebook.notes.checklist.viewmodel.BulletListViewModel
import com.notebook.notes.checklist.viewmodel.CategoryViewModel
import com.notebook.notes.checklist.viewmodel.NoteEditViewModel
import com.notebook.notes.checklist.viewmodel.NumberListViewModel
import com.notebook.notes.checklist.viewmodel.TodoViewModel
import com.notebook.notes.checklist.viewmodel.VoiceNoteViewModel
import com.notebook.notes.checklist.viewmodel.utils.finishWithCreateNotesBackInterAd
import com.notebook.notes.checklist.viewmodel.utils.openWithCreateNotesInterAd
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.FileProvider
import com.notebook.notes.checklist.databinding.ItemBlockDividerBinding
import com.notebook.notes.checklist.databinding.ItemBlockLinkBinding
import com.notebook.notes.checklist.databinding.ItemBlockTextBinding
import com.notebook.notes.checklist.databinding.ItemBulletListBinding
import com.notebook.notes.checklist.databinding.ItemNoteMediaRowBinding
import com.notebook.notes.checklist.databinding.ItemNumberListBinding
import com.notebook.notes.checklist.databinding.ItemTodoBinding
import com.notebook.notes.checklist.model.AttachmentMetadata
import com.notebook.notes.checklist.model.MediaBlockHelper
import com.notebook.notes.checklist.ui.richtext.RichTextEditText
import com.notebook.notes.checklist.util.extension.changeNavigationBarToWhite
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class CreateNotesActivity : BaseActivity(), RichTextEditorHost {

    private lateinit var binding: ActivityCreateNotesBinding
    private val viewModel: NoteEditViewModel by viewModels()
    private val categoryViewModel: CategoryViewModel by viewModels()
    private val todoViewModel: TodoViewModel by viewModels()
    private val numberListViewModel: NumberListViewModel by viewModels()
    private val bulletListViewModel: BulletListViewModel by viewModels()
    private val voiceNoteViewModel: VoiceNoteViewModel by viewModels()
    private val attachmentViewModel: AttachmentViewModel by viewModels()
    private val emojiViewModel: EmojiViewModel by viewModels()
    @Inject lateinit var permissionHelper: PermissionHelper
    private val backgroundRepository by lazy { NoteBackgroundRepository(this) }
    @Inject lateinit var notePasswordManager: NotePasswordManager
    @Inject lateinit var preferenceHelper: PreferenceHelper
    private lateinit var contentAdapter: NoteContentAdapter
    private lateinit var richTextToolbarBinder: RichTextToolbarBinder
    private lateinit var titleController: RichTextEditorController
    private var emojiPanelRef: EmojiPanel? = null
    private var noteBackgroundPanelRef: NoteBackgroundPanel? = null
    private var isAutoSavingOnExit = false
    private var isExiting = false
    private var todoInitialized = false
    private var defaultTextBlockInitialized = false
    private val isNewNote: Boolean by lazy {
        intent.getLongExtra(NoteEditViewModel.EXTRA_NOTE_ID, -1L) <= 0L
    }
    private val defaultFontSize: Int by lazy {
        preferenceHelper.getInt(Constant.PREF_DEFAULT_FONT_SIZE, Constant.DEFAULT_FONT_SIZE)
    }
    private val hasSavedDefaultFontSize: Boolean by lazy {
        preferenceHelper.contains(Constant.PREF_DEFAULT_FONT_SIZE)
    }
    private val defaultFontFamily: String by lazy {
        preferenceHelper.getString(
            Constant.PREF_DEFAULT_FONT_FAMILY,
            Constant.DEFAULT_FONT_FAMILY
        )?.takeIf { it.isNotBlank() } ?: Constant.DEFAULT_FONT_FAMILY
    }
    private var lastAppliedListItemHeight: Int? = null
    private var hasAppliedListItemHeight = false
    private var lastContentSignature = ""
    private var isMovingFromTitle = false
    private var pendingSaveAfterRecording = false
    private var noteSearchQuery: String = ""
    private var noteSearchMatchOrdinal: Int = 0
    private var lastAppliedTitleJson: String? = null
    private var isLoadingTitleDocument = false
    private var lastAppliedAppearanceKey: String? = null
    private var hasAppliedAppearance = false
    /** Ink contrast color for the current note background; used when appearance apply is skipped. */
    private var lastAppearanceInkColor: Int? = null
    /** Resolved note surface color; drives the dynamic search-highlight palette. */
    private var lastAppearanceBackgroundColor: Int? = null
    private var pendingEditorScrollImmediate = false
    /** Title may only take focus from an explicit user tap — never from focus search / scroll. */
    private var permitTitleFocusFromUser = false
    /** Guards against re-entrant title-focus rejection (clearFocus -> refocus loop -> ANR). */
    private var isRejectingTitleFocus = false
    /** While true (list is scrolling) the headline cannot receive focus at all. */
    private var titleFocusLockedForScroll = false
    /** Headline touch-focusability captured before a scroll lock, restored on idle. */
    private var titleFocusableBeforeScrollLock = true
    /** True while the block list is dragging/settling, when recycling blurs row editors. */
    private var isBlockListScrolling = false
    /** Last focused body editor (todo / list / text); used to reject header focus steals. */
    private var lastBodyEditor: android.widget.EditText? = null
    private var isLockAuthenticated = false
    private var lockAuthPromptLaunched = false
    private var isEditorRevealAllowed = false
    /** Bumped after lock auth so UI collectors re-apply the already-loaded note. */
    private val lockRevealTick = MutableStateFlow(0)
    private val scrollEditorRunnable = Runnable {
        val immediate = pendingEditorScrollImmediate
        pendingEditorScrollImmediate = false
        scrollActiveEditorIntoView(immediate = immediate)
    }

    private val pickMediaLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { handlePickedMedia(it) }
        }

    private val imageViewerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val data = result.data ?: return@registerForActivityResult
            if (!data.hasExtra(ImageViewerActivity.EXTRA_DELETED_BLOCK_ID)) return@registerForActivityResult
            // Temp unsaved blocks use negative IDs (nextTempId); 0 means "no id".
            val deletedId = data.getLongExtra(ImageViewerActivity.EXTRA_DELETED_BLOCK_ID, 0L)
            if (deletedId == 0L) return@registerForActivityResult
            removeContentBlock(deletedId)
            syncContentList()
        }

    private val videoPlayerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val data = result.data ?: return@registerForActivityResult
            if (!data.hasExtra(VideoPlayerActivity.EXTRA_DELETED_BLOCK_ID)) return@registerForActivityResult
            // Temp unsaved blocks use negative IDs (nextTempId); 0 means "no id".
            val deletedId = data.getLongExtra(VideoPlayerActivity.EXTRA_DELETED_BLOCK_ID, 0L)
            if (deletedId == 0L) return@registerForActivityResult
            removeContentBlock(deletedId)
            syncContentList()
        }

    private val pickFileLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { importAttachment(it) }
        }

    private val requestMicPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                voiceNoteViewModel.startRecording()
            } else {
                Toast.makeText(
                    this,
                    R.string.microphone_permission_required,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                requestExactAlarmPermissionOrOpenReminder()
            }
        }

    private val requestExactAlarmPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (permissionHelper.canScheduleExactAlarms()) {
                openSetReminderBottomSheet()
            }
        }

    private val drawingLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val path = result.data?.getStringExtra(DrawingActivity.EXTRA_RESULT_PATH) ?: return@registerForActivityResult
            val blockId = result.data?.getLongExtra(DrawingActivity.EXTRA_BLOCK_ID, -1L) ?: -1L
            if (blockId > 0) {
                viewModel.updateDrawingBlock(blockId, path)
                syncContentList()
            } else {
                val position = prepareSmartContentInsert()
                viewModel.addDrawingBlock(path, position)
                completeSmartContentInsert(position)
            }
        }

    private val setPasswordLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            viewModel.setLocked(true)
            Toast.makeText(this, R.string.note_locked, Toast.LENGTH_SHORT).show()
        }

    private val verifyUnlockLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            viewModel.setLocked(false)
            Toast.makeText(this, R.string.note_unlocked, Toast.LENGTH_SHORT).show()
        }

    private val lockAuthLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                isLockAuthenticated = true
                lockAuthPromptLaunched = false
                revealEditorIfAllowed()
                lockRevealTick.value = lockRevealTick.value + 1
            } else {
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.changeNavigationBarToWhite()
        isLockAuthenticated =
            intent.getBooleanExtra(NoteEditViewModel.EXTRA_LOCK_AUTHENTICATED, false)
        binding = ActivityCreateNotesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Hide editor until lock status is known / authenticated to avoid content flash.
        if (!isNewNote && !isLockAuthenticated) {
            binding.main.visibility = View.INVISIBLE
        } else {
            isEditorRevealAllowed = true
        }

        setupWindowInsets()
        setupRichTextToolbar()
        setupTitleRichText()
        setupNoteHeader()
        setupContentRecycler()
        setupActions()
        setupBackPressAutosave()
        observeState()
        observeCategoryState()
        observeContent()
        observeVoiceState()
        observeAttachmentState()
        observeEmojiState()
        loadOtherScreenAds()
        hideKeyboardAdSlot()
        preloadRewardedAdIfNeeded()
    }

    /**
     * Create-note top ad, controlled from remote config (`create_note`) through [AdsManager]:
     * `type` "banner" shows a banner, anything else the banner-style small native. Any "no ad"
     * outcome (disabled, premium, offline, failed) collapses the slot.
     */
    private fun loadOtherScreenAds() {
        val config = AdRemoteConfig.create_note
        val useBanner = config.type.equals("banner", ignoreCase = true)
        if (!ADSConsentManager.canRequestAds(this) || ADSMainClass.getAds_Free() || !config.isEnable) {
            hideOtherScreenAdPlaceholders()
            return
        }
        binding.llOtherScreenAds.root.visibility = View.VISIBLE
        applyTopInsets()
        if (useBanner) loadCreateNoteBanner() else loadCreateNoteNative()
    }

    private fun loadCreateNoteBanner() {
        // The ads SDK resolves R.id.shimmer_container_banner with Activity.findViewById (first match),
        // and the native card uses that id too, so detach it or the wrong shimmer gets hidden.
        (findViewById<View>(R.id.flNativeSmallPlaceholder)?.parent as? View)?.let { nativeCard ->
            (nativeCard.parent as? ViewGroup)?.removeView(nativeCard)
        }
        val bannerSlot = findViewById<FrameLayout>(R.id.flBannerSmallPlaceholder)
        bannerSlot.visibility = View.VISIBLE
        AdsManager.loadBanner(this, AdRemoteConfig.create_note.forBanner(), bannerSlot, isCollapse = false)
    }

    private fun loadCreateNoteNative() {
        // Banner-style small native: use its own shimmer placeholder inside the shared slot.
        findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)?.let { shimmer ->
            shimmer.removeAllViews()
            shimmer.addView(layoutInflater.inflate(R.layout.fill_in_ad_unifiled_small, shimmer, false))
        }
        // Drop a previous screen's ad so it is never rendered twice; the first null is that reset.
        AdsManager.nativeCreateNoteAdLive.value = null
        var skippedInitialNull = true
        var hasAd = false
        AdsManager.nativeCreateNoteAdLive.observe(this) { ad ->
            if (ad != null) {
                hasAd = true
                populateNativeAdView(
                    this,
                    ad,
                    findViewById(R.id.flNativeSmallPlaceholder),
                    findViewById(R.id.shimmer_container_banner)
                )
            } else if (!skippedInitialNull && !hasAd) {
                // A failed periodic reload keeps the ad that is already showing.
                hideOtherScreenAdPlaceholders()
            }
            skippedInitialNull = false
        }
        AdsManager.loadNativeCreateNote(this, R.layout.fill_in_admob_mid_native_ad)
    }

    private val adReloadHandler = Handler(Looper.getMainLooper())
    private val adReloadRunnable = Runnable {
        if (isFinishing || isDestroyed) return@Runnable
        reloadCreateNoteAd()
        scheduleAdReload()
    }

    /** Re-requests the top ad every `reloadIntervalSeconds` (remote config) while the screen is in front. */
    private fun scheduleAdReload() {
        adReloadHandler.removeCallbacks(adReloadRunnable)
        val config = AdRemoteConfig.create_note
        val intervalSeconds = config.reloadIntervalSeconds ?: 0
        if (!config.isEnable || intervalSeconds <= 0 || !binding.llOtherScreenAds.root.isVisible) return
        adReloadHandler.postDelayed(adReloadRunnable, intervalSeconds * 1000L)
    }

    private fun reloadCreateNoteAd() {
        val config = AdRemoteConfig.create_note
        if (ADSMainClass.getAds_Free() || !config.isEnable) return
        if (config.type.equals("banner", ignoreCase = true)) {
            findViewById<FrameLayout>(R.id.flBannerSmallPlaceholder)?.let { bannerSlot ->
                AdsManager.loadBanner(this, config.forBanner(), bannerSlot, isCollapse = false)
            }
        } else {
            AdsManager.loadNativeCreateNote(this, R.layout.fill_in_admob_mid_native_ad)
        }
    }

    /**
     * Preload rewarded ad for premium unlock flows (e.g. VIP backgrounds)
     * when ads are allowed and Remote Config enables rewarded.
     */
    private fun preloadRewardedAdIfNeeded() {
        if (!ADSConsentManager.canRequestAds(this)) return
        if (ADSMainClass.getAds_Free()) return
        if (!ADSMainClass.getRewardedAdsShow()) return
        if (ADSRewardedDisplay.isRewardedReady() || ADSRewardedDisplay.isRewardedLoading()) return
        ADSRewardedDisplay.preload(this)
    }

    private fun hideOtherScreenAdPlaceholders() {
        binding.llOtherScreenAds.root.visibility = View.GONE
        applyTopInsets()
        // Null-safe: the native card is detached when the banner type is used.
        findViewById<View>(R.id.shimmer_container_banner)?.visibility = View.GONE
        findViewById<View>(R.id.flNativeSmallPlaceholder)?.visibility = View.GONE
        findViewById<View>(R.id.flBannerSmallPlaceholder)?.visibility = View.GONE
    }

    /**
     * The editor keeps a single ad, pinned to the bottom of the screen: opening the
     * keyboard must neither move it nor swap it for a different format. The in-panel
     * slot above the keyboard therefore stays collapsed and is never requested.
     */
    private fun hideKeyboardAdSlot() {
        binding.keyboardNativeAdContainer.visibility = View.GONE
        binding.shimmerKeyboardNative.stopShimmer()
        binding.shimmerKeyboardNative.visibility = View.GONE
    }

    private fun isLockAccessGranted(isLocked: Boolean): Boolean {
        return isNewNote || !isLocked || isLockAuthenticated
    }

    private fun ensureLockAuthenticated(isLocked: Boolean, noteId: Long?) {
        // Once the editor has been revealed this session, allow locking/unlocking in-place
        // without re-prompting (list/notification entry still requires auth first).
        if (isEditorRevealAllowed || isLockAccessGranted(isLocked)) {
            revealEditorIfAllowed()
            return
        }
        if (lockAuthPromptLaunched) return
        val id = noteId?.takeIf { it > 0 } ?: return
        lockAuthPromptLaunched = true
        binding.main.visibility = View.INVISIBLE

        if (notePasswordManager.hasPassword()) {
            lockAuthLauncher.launch(SetPasswordActivity.createVerifyIntent(this, id))
            return
        }

        // Locked note without an app password: require device biometric when available.
        val biometricManager = androidx.biometric.BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(
            androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        if (canAuthenticate != androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, R.string.authenticate_to_open, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val prompt = androidx.biometric.BiometricPrompt(
            this,
            executor,
            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: androidx.biometric.BiometricPrompt.AuthenticationResult
                ) {
                    isLockAuthenticated = true
                    lockAuthPromptLaunched = false
                    revealEditorIfAllowed()
                    lockRevealTick.value = lockRevealTick.value + 1
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    finish()
                }

                override fun onAuthenticationFailed() {
                    // Keep prompt open; user can retry or cancel.
                }
            }
        )
        val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.locked_note))
            .setSubtitle(getString(R.string.authenticate_to_open))
            .setAllowedAuthenticators(
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        prompt.authenticate(promptInfo)
    }

    private fun revealEditorIfAllowed() {
        if (!isEditorRevealAllowed &&
            !isLockAccessGranted(viewModel.uiState.value.isLocked) &&
            !viewModel.uiState.value.isLoading
        ) {
            return
        }
        isEditorRevealAllowed = true
        isLockAuthenticated = true
        binding.main.visibility = View.VISIBLE
    }

    private fun launchMediaPicker() {
        pickMediaLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageAndVideo))
    }

    private fun handlePickedMedia(uri: Uri) {
        val mimeType = contentResolver.getType(uri).orEmpty()
        if (mimeType.startsWith("video/")) {
            importAttachment(uri)
        } else {
            copyAndAddBlock(uri, BlockType.IMAGE)
        }
    }

    private fun importAttachment(uri: Uri) {
        attachmentViewModel.importAttachment(uri) { imported, metadata ->
            val position = prepareSmartContentInsert()
            // Picked audio (and all other files) use the file-attachment UI.
            // In-app voice recording still creates VOICE blocks separately.
            viewModel.addAttachmentBlock(metadata, imported.filePath, position)
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some providers do not support persistable permissions.
            }
            completeSmartContentInsert(position)
        }
    }

    private fun observeAttachmentState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                attachmentViewModel.uiState.collect { state ->
                    state.error?.let { error ->
                        val message = when (error) {
                            "missing_file" -> getString(R.string.attachment_missing)
                            "no_viewer" -> getString(R.string.no_viewer_installed)
                            "import_failed", "Unable to read selected file." ->
                                getString(R.string.attachment_import_failed)
                            "playback_failed" -> getString(R.string.playback_failed)
                            else -> error
                        }
                        Toast.makeText(this@CreateNotesActivity, message, Toast.LENGTH_SHORT).show()
                        attachmentViewModel.clearError()
                    }
                    contentAdapter.refreshVoicePlayback()
                }
            }
        }
    }

    private fun setupBackPressAutosave() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                autoSaveAndExit()
            }
        })
    }

    /** When true, IME movement is driven by translationY — skip padding jumps. */
    private var deferImeInsets = false
    private var overlayBottomInsetPx = 0
    private var rvBasePaddingBottom = 0
    /** Extra RV padding so the caret isn't flush against the toolbar edge. */
    private var editorSafeInsetPx = 0

    private fun setupWindowInsets() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        this.changeNavigationBarToWhite()
        rvBasePaddingBottom = binding.rvBlocks.paddingBottom
        editorSafeInsetPx = (24 * resources.displayMetrics.density).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Keep color + phone_image edge-to-edge (including under status bar).
            // Only pad the chrome / content so it clears the system bars.
            binding.main.updatePadding(left = 0, top = 0, right = 0, bottom = 0)
            statusBarInsetPx = systemBars.top
            headerSideInsetLeft = systemBars.left
            headerSideInsetRight = systemBars.right
            applyTopInsets()
            if (!deferImeInsets) {
                applyBottomEditorInsets(insets)
            } else {
                // Keep a stable nav-bar pad while the IME animates via translation.
                val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                binding.bottomEditorPanel.updatePadding(bottom = navBar)
                applyRecyclerImeLift(imeLift(ime, navBar))
            }
            insets
        }

        ViewCompat.setWindowInsetsAnimationCallback(
            binding.bottomEditorPanel,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                    if (animation.typeMask and WindowInsetsCompat.Type.ime() == 0) return
                    deferImeInsets = true
                    val insets = ViewCompat.getRootWindowInsets(binding.bottomEditorPanel) ?: return
                    val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                    val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                    // Convert current padding-based lift into translation so the next
                    // frames can interpolate without layout thrashing.
                    binding.bottomEditorPanel.updatePadding(bottom = nav)
                    binding.bottomEditorPanel.translationY = -imeLift(ime, nav).toFloat()
                    applyRecyclerImeLift(imeLift(ime, nav))
                }

                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat {
                    val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                    val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                    val lift = imeLift(ime, nav)
                    // Use the live animated IME inset — follows the system curve.
                    binding.bottomEditorPanel.translationY = -lift.toFloat()
                    applyRecyclerImeLift(lift)
                    return insets
                }

                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    if (animation.typeMask and WindowInsetsCompat.Type.ime() == 0) return
                    deferImeInsets = false
                    binding.bottomEditorPanel.translationY = 0f
                    ViewCompat.getRootWindowInsets(binding.bottomEditorPanel)?.let {
                        applyBottomEditorInsets(it)
                    }
                    ViewCompat.requestApplyInsets(binding.main)
                    scheduleScrollActiveEditorIntoView(immediate = false)
                }
            }
        )

        // Toolbar / emoji / format panels grow the bottom panel — re-scroll after layout.
        binding.bottomEditorPanel.addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
            val newHeight = bottom - top
            val oldHeight = oldBottom - oldTop
            if (newHeight != oldHeight) {
                scheduleScrollActiveEditorIntoView(immediate = false)
            }
        }

    }

    private var statusBarInsetPx = 0
    private var headerSideInsetLeft = 0
    private var headerSideInsetRight = 0

    /**
     * The native ad is pinned to the very top, so it takes the status-bar inset while it is
     * visible; otherwise the header (which sits right below it) takes it.
     */
    private fun applyTopInsets() {
        val adVisible = binding.llOtherScreenAds.root.isVisible
        binding.llOtherScreenAds.root.updatePadding(top = if (adVisible) statusBarInsetPx else 0)
        binding.noteHeader.root.updatePadding(
            left = headerSideInsetLeft,
            top = if (adVisible) 0 else statusBarInsetPx,
            right = headerSideInsetRight
        )
    }

    /** The ad slot is at the top now, so nothing at the bottom is reserved for the keyboard math. */
    private fun bottomAdsHeight(): Int = 0

    private fun imeLift(imeBottom: Int, navBottom: Int): Int =
        (imeBottom - navBottom - bottomAdsHeight()).coerceAtLeast(0)

    private fun applyBottomEditorInsets(insets: WindowInsetsCompat) {
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        binding.bottomEditorPanel.updatePadding(bottom = max(ime - bottomAdsHeight(), nav))
        binding.bottomEditorPanel.translationY = 0f
        // Panel padding already shrinks RV via constraint; clear temporary IME lift padding.
        applyRecyclerImeLift(0)
    }

    /**
     * The ad slot changes height when its ad finishes loading (shimmer -> rendered ad).
     * Re-apply the current IME offsets so the toolbar stays flush with the keyboard.
     */
    private fun reapplyImeOffsets() {
        val insets = ViewCompat.getRootWindowInsets(binding.main) ?: return
        if (!deferImeInsets) {
            applyBottomEditorInsets(insets)
            return
        }
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        val lift = imeLift(ime, nav)
        binding.bottomEditorPanel.translationY = -lift.toFloat()
        applyRecyclerImeLift(lift)
    }

    /**
     * While the bottom panel is translated over the IME, give the RecyclerView matching
     * bottom padding so content is never laid out under the keyboard/panel.
     * Always keep [editorSafeInsetPx] so the caret isn't flush against the toolbar.
     */
    private fun applyRecyclerImeLift(imeLiftPx: Int) {
        binding.rvBlocks.updatePadding(
            bottom = rvBasePaddingBottom + editorSafeInsetPx + imeLiftPx + overlayBottomInsetPx
        )
    }

    private fun setOverlayBottomInset(px: Int) {
        overlayBottomInsetPx = px.coerceAtLeast(0)
        val insets = ViewCompat.getRootWindowInsets(binding.main)
        when {
            deferImeInsets && insets != null -> {
                val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                applyRecyclerImeLift(imeLift(ime, nav))
            }
            insets != null -> applyBottomEditorInsets(insets)
            else -> applyRecyclerImeLift(0)
        }
        scheduleScrollActiveEditorIntoView(immediate = false)
    }

    private fun setupRichTextToolbar() {
        lateinit var toolbarBinder: RichTextToolbarBinder
        val onPanelVisibilityChanged = {
            toolbarBinder.syncBottomEditorPanelState()
            toolbarBinder.refreshToolbarState()
            // In-layout panels grow bottomEditorPanel — keep caret above them.
            scheduleScrollActiveEditorIntoView(immediate = true)
        }
        val textFormattingPanel = TextFormattingPanel(
            activity = this,
            binding = binding.textFormattingPanel,
            isNumberListModeEnabled = { numberListViewModel.uiState.value.isNumberModeEnabled },
            isBulletListModeEnabled = { bulletListViewModel.uiState.value.isBulletModeEnabled },
            onStateChanged = { toolbarBinder.refreshToolbarState() },
            onVisibilityChanged = { onPanelVisibilityChanged() }
        )
        val fontSizePanel = FontSizePanel(
            binding = binding.fontSizePanel,
            onStateChanged = { toolbarBinder.refreshToolbarState() },
            onVisibilityChanged = { onPanelVisibilityChanged() }
        )
        val fontStylePanel = FontStylePanel(
            binding = binding.fontStylePanel,
            onStateChanged = { toolbarBinder.refreshToolbarState() },
            onVisibilityChanged = { onPanelVisibilityChanged() }
        )
        val checklistPanel = ChecklistPanel(
            activity = this,
            binding = binding.checklistPanel,
            onOptionSelected = { option, controller ->
                handleChecklistOption(option, controller)
            },
            onVisibilityChanged = { onPanelVisibilityChanged() }
        )
        val emojiPanel = EmojiPanel(
            activity = this,
            binding = binding.emojiPanel,
            onEmojiSelected = { emoji ->
                val selected = emojiViewModel.onEmojiSelected(emoji)
                insertEmoji(selected, richTextToolbarBinder.getActiveController())
            },
            onVisibilityChanged = { onPanelVisibilityChanged() }
        ).also { panel ->
            panel.onCategorySelected = { categoryId -> emojiViewModel.selectCategory(categoryId) }
            emojiPanelRef = panel
            panel.render(emojiViewModel.uiState.value)
        }
        val noteBackgroundPanel = NoteBackgroundPanel(
            binding = binding.noteBackgroundPanel,
            repository = backgroundRepository,
            onBackgroundSelected = { key ->
                viewModel.setBackgroundColor(key)
                applyNoteAppearance(key)
            },
            onVisibilityChanged = { onPanelVisibilityChanged() }
        ).also { noteBackgroundPanelRef = it }
        toolbarBinder = RichTextToolbarBinder(
            activity = this,
            binding = binding.richTextToolbar,
            textFormattingPanel = textFormattingPanel,
            fontSizePanel = fontSizePanel,
            fontStylePanel = fontStylePanel,
            checklistPanel = checklistPanel,
            emojiPanel = emojiPanel,
            noteBackgroundPanel = noteBackgroundPanel,
            bottomEditorPanel = binding.bottomEditorPanel,
            selectedBackgroundKey = { viewModel.uiState.value.color },
            isNumberListModeEnabled = { numberListViewModel.uiState.value.isNumberModeEnabled },
            isBulletListModeEnabled = { bulletListViewModel.uiState.value.isBulletModeEnabled }
        )
        richTextToolbarBinder = toolbarBinder
        richTextToolbarBinder.bind()
        richTextToolbarBinder.setActiveController(null)
    }

    private fun observeEmojiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                emojiViewModel.uiState.collect { state ->
                    // Panel is created in setupRichTextToolbar; re-render via binding root tag is fragile.
                    // The EmojiPanel instance is captured by toolbar — update through a field.
                    emojiPanelRef?.render(state)
                }
            }
        }
    }

    override fun onNumberListAction(controller: RichTextEditorController) {
        handleNumberListFromRichText(controller)
    }

    override fun onBulletListAction(controller: RichTextEditorController) {
        handleBulletListFromRichText(controller)
    }

    override fun onRichTextEditorFocused(controller: RichTextEditorController) {
        val editor = controller.editorView
        if (editor !== binding.etTitle) {
            lastBodyEditor = editor
            permitTitleFocusFromUser = false
        }
        if (viewModel.uiState.value.isReadingMode) {
            binding.richTextToolbar.root.visibility = View.INVISIBLE
            return
        }
        richTextToolbarBinder.setActiveController(controller)
        // Quiet focus restore (e.g. after emoji insert) must not close the bottom panel / force IME.
        if ((editor as? RichTextEditText)?.suppressTypingKeyboardRequest == true) return
        richTextToolbarBinder.dismissOpenPanelsThenShowKeyboard(controller)
        // Defer scroll until after layout/IME settle — immediate scroll on tall todos
        // caused focus loss and jumped the caret into the title.
        scheduleScrollActiveEditorIntoView(immediate = false)
    }

    override fun onRichTextEditorTypingRequested(controller: RichTextEditorController) {
        if (controller.editorView !== binding.etTitle) {
            lastBodyEditor = controller.editorView
        }
        richTextToolbarBinder.setActiveController(controller)
        if ((controller.editorView as? RichTextEditText)?.suppressTypingKeyboardRequest == true) return
        richTextToolbarBinder.dismissOpenPanelsThenShowKeyboard(controller)
    }

    override fun onRichTextEditorCaretMoved(controller: RichTextEditorController) {
        // Never force an immediate jump for caret moves; only nudge if obscured.
        scheduleScrollActiveEditorIntoView(immediate = false)
    }

    override fun onRichTextEditorBlurred() {
        if (richTextToolbarBinder.suppressEditorBlurHandling) return
        // A scroll recycles the row the user is editing, which blurs its editor even
        // though editing has not ended. The caret is restored once the list settles
        // (see restoreRetainedFocusIfVisible), so the toolbar must stay in place.
        if (isBlockListScrolling) return
        hideRichTextToolbar()
    }

    override fun onChecklistFromToolbar() {
        // Checklist actions are handled through panel_checklist options.
    }

    override fun onChecklistOption(option: ChecklistOption, controller: RichTextEditorController?) {
        handleChecklistOption(option, controller ?: richTextToolbarBinder.getActiveController())
    }

    private fun handleChecklistOption(
        option: ChecklistOption,
        controller: RichTextEditorController?
    ) {
        when (option) {
            ChecklistOption.CHECKBOX -> addTodoItem()
            ChecklistOption.TOP_LINE -> addDividerBlock()
            ChecklistOption.BULLET_LIST -> {
                if (controller != null) {
                    handleBulletListFromRichText(controller)
                } else {
                    startBulletListMode()
                }
            }
            ChecklistOption.NUMBER_LIST -> {
                if (controller != null) {
                    handleNumberListFromRichText(controller)
                } else {
                    startNumberListMode()
                }
            }
        }
        richTextToolbarBinder.refreshToolbarState()
    }

    override fun onDrawingFromToolbar() {
        openDrawingActivity()
    }

    override fun onBackgroundFromToolbar() {
        // Background picker is toggled from RichTextToolbarBinder as an in-layout panel.
    }

    override fun onEmojiFromToolbar() {
        // Emoji panel is toggled from RichTextToolbarBinder.
    }

    private fun insertEmoji(emoji: String, controller: RichTextEditorController?) {
        if (binding.etTitle.isFocused && ::titleController.isInitialized) {
            titleController.insertEmoji(emoji)
            scheduleScrollActiveEditorIntoView(immediate = true)
            return
        }
        val target = controller ?: richTextToolbarBinder.getActiveController()
        target?.insertEmoji(emoji)
        scheduleScrollActiveEditorIntoView(immediate = true)
    }

    private fun scheduleScrollActiveEditorIntoView(immediate: Boolean = false) {
        pendingEditorScrollImmediate = pendingEditorScrollImmediate || immediate
        binding.rvBlocks.removeCallbacks(scrollEditorRunnable)
        // Coalesce rapid focus/IME/caret scrolls so tall todos don't jump repeatedly.
        if (immediate) {
            binding.rvBlocks.post(scrollEditorRunnable)
        } else {
            binding.rvBlocks.postDelayed(scrollEditorRunnable, 64L)
        }
    }

    /**
     * Keeps the focused body-editor caret visible above the keyboard and bottom panels.
     * Scrolls only when the caret is actually obscured — never recenters or pulls tops.
     */
    private fun scrollActiveEditorIntoView(
        controller: RichTextEditorController? = null,
        immediate: Boolean = false
    ) {
        val focused = when {
            binding.etTitle.isFocused -> binding.etTitle
            controller != null -> controller.editorView
            else -> (currentFocus as? EditText)
                ?: (binding.rvBlocks.findFocus() as? EditText)
                ?: lastBodyEditor?.takeIf { it.isAttachedToWindow }
        } ?: return

        // Title and search live outside the RecyclerView — never auto-scroll body for them.
        if (focused === binding.etTitle) return
        if (focused === binding.noteHeader.etSearch) return
        if (!focused.hasFocus() && focused !== lastBodyEditor) return

        val textLength = focused.text?.length ?: 0
        val caret = focused.selectionEnd.coerceIn(0, textLength)

        // wrap_content editors are fully laid out — bringPointIntoView only helps
        // internally-scrollable fields and can destabilize tall RecyclerView rows.
        if (focused.canScrollVertically(1) || focused.canScrollVertically(-1)) {
            focused.bringPointIntoView(caret)
        }

        val focusedLoc = IntArray(2)
        focused.getLocationOnScreen(focusedLoc)
        val rvLoc = IntArray(2)
        binding.rvBlocks.getLocationOnScreen(rvLoc)

        val layout = focused.layout
        val caretTopOnScreen: Int
        val caretBottomOnScreen: Int
        if (layout == null) {
            caretTopOnScreen = focusedLoc[1]
            caretBottomOnScreen = focusedLoc[1] + focused.height
        } else {
            val line = layout.getLineForOffset(caret)
            caretTopOnScreen =
                focusedLoc[1] + focused.totalPaddingTop + layout.getLineTop(line) - focused.scrollY
            caretBottomOnScreen =
                focusedLoc[1] + focused.totalPaddingTop + layout.getLineBottom(line) - focused.scrollY
        }

        val density = resources.displayMetrics.density
        val margin = (12 * density).toInt()
        val hysteresis = (8 * density).toInt()
        val visibleTop = rvLoc[1] + binding.rvBlocks.paddingTop
        val visibleBottom = rvLoc[1] + binding.rvBlocks.height - binding.rvBlocks.paddingBottom

        val overflowBottom = caretBottomOnScreen - visibleBottom + margin
        val overflowTop = visibleTop - caretTopOnScreen + margin

        val delta = when {
            overflowBottom > hysteresis -> overflowBottom
            overflowTop > hysteresis -> -overflowTop
            else -> 0
        }
        if (delta == 0) return

        // Cap so a single tall todo never leaps by a full viewport.
        val maxStep = (binding.rvBlocks.height * 0.45f).toInt().coerceAtLeast(1)
        val clamped = delta.coerceIn(-maxStep, maxStep)
        if (immediate) {
            binding.rvBlocks.scrollBy(0, clamped)
        } else {
            binding.rvBlocks.smoothScrollBy(0, clamped)
        }
    }

    override fun onMoreToolAction(action: MoreToolAction) {
        when (action) {
            MoreToolAction.ATTACHMENT -> pickFileLauncher.launch(arrayOf("*/*"))
            MoreToolAction.VOICE_NOTE -> startVoiceRecording()
            MoreToolAction.NUMBER_LIST -> {
                richTextToolbarBinder.getActiveController()?.let { handleNumberListFromRichText(it) }
            }
            MoreToolAction.BULLET_LIST -> {
                richTextToolbarBinder.getActiveController()?.let { handleBulletListFromRichText(it) }
            }
            MoreToolAction.LINK -> showAddLinkDialog()
            MoreToolAction.CATEGORY -> showCategorySelectionDialog()
        }
    }

    private fun hideRichTextToolbar() {
        richTextToolbarBinder.setActiveController(null)
    }

    private fun setupNoteHeader() {
        val header = binding.noteHeader

        header.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        header.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                scrollToFirstSearchMatch()
                hideNoteSearch()
                true
            } else {
                false
            }
        }
        header.etSearch.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                // Defer so programmatic focus moves (title/editor) settle first.
                header.etSearch.post {
                    if (!header.etSearch.hasFocus() && header.searchContainer.isVisible) {
                        hideNoteSearch()
                    }
                }
            }
        }
        header.etSearch.doAfterTextChanged { editable ->
            applyNoteSearch(editable?.toString().orEmpty())
        }

        header.btnBookmark.setOnClickListener {
            val willFavorite = !viewModel.uiState.value.isFavorite
            viewModel.toggleFavorite()
            Toast.makeText(
                this,
                if (willFavorite) R.string.note_bookmarked else R.string.note_unbookmarked,
                Toast.LENGTH_SHORT
            ).show()
        }

        header.btnAttachment.setOnClickListener {
            openBottomSheet {
                NoteHeaderBottomSheets.showAttachmentSheet(
                    activity = this,
                    onSheetLaidOut = { height -> setOverlayBottomInset(height) },
                    onDismissed = { setOverlayBottomInset(0) }
                ) { option ->
                    when (option) {
                        AttachmentOption.AUDIO -> startVoiceRecording()
                        AttachmentOption.IMAGE -> launchMediaPicker()
                        AttachmentOption.DOODLE -> openDrawingActivity()
                        AttachmentOption.FILE -> pickFileLauncher.launch(arrayOf("*/*"))
                        AttachmentOption.LINK -> showAddLinkDialog()
                    }
                }
            }
        }

        header.btnMore.setOnClickListener { anchor ->
            BottomSheetHelper.showAfterImeHidden(this, anchor) {
                val state = viewModel.uiState.value
                NoteMoreOptionsPopup.show(
                    anchor = anchor,
                    config = NoteMoreOptionsConfig(
                        isArchived = state.isArchived,
                        isPinned = state.isPinned,
                        isFavorite = state.isFavorite,
                        isReadingMode = state.isReadingMode,
                        isLocked = state.isLocked
                    ),
                    listener = NoteMoreOptionsListener(
                        onAddReminder = { openReminderFlow() },
                        onArchive = { viewModel.toggleArchive() },
                        onLock = { toggleNoteLock() },
                        onBookmarkNote = {
                            val willFavorite = !viewModel.uiState.value.isFavorite
                            viewModel.toggleFavorite()
                            Toast.makeText(
                                this,
                                if (willFavorite) R.string.note_bookmarked else R.string.note_unbookmarked,
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onReadingMode = {
                            viewModel.toggleReadingMode()
                        },
                        onSearch = { showNoteSearch() },
                        onCopy = { duplicateCurrentNote() },
                        onExport = {
                            openBottomSheet {
                                ExportBottomSheet.show(this) { type ->
                                    when (type) {
                                        ExportBottomSheet.ExportType.PDF -> exportAsPdf()
                                        ExportBottomSheet.ExportType.TEXT -> exportAsText()
                                        ExportBottomSheet.ExportType.JPG -> exportAsJpg()
                                    }
                                }
                            }
                        },
                        onNoteDetails = {
                            showNoteDetails()
                        },
                        onPin = {
                            val willPin = !viewModel.uiState.value.isPinned
                            viewModel.togglePin()
                            Toast.makeText(
                                this,
                                if (willPin) R.string.note_pinned else R.string.note_unpinned,
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onShare = { shareCurrentNote() },
                        onDelete = { confirmDeleteNote() }
                    )
                )
            }
        }

        header.btnCategorySelector.setOnClickListener { showCategorySelectionDialog() }
    }

    private fun applyNoteListItemHeight(heightPx: Int?) {
        if (hasAppliedListItemHeight && heightPx == lastAppliedListItemHeight) return
        hasAppliedListItemHeight = true
        lastAppliedListItemHeight = heightPx
        if (::contentAdapter.isInitialized) {
            contentAdapter.setLineSpacingExtra(heightPx)
        }
    }

    private fun applyNoteAppearance(colorKey: String?) {
        // New notes start with color=null; null==null must not skip first apply.
        if (hasAppliedAppearance && colorKey == lastAppliedAppearanceKey) return
        hasAppliedAppearance = true
        lastAppliedAppearanceKey = colorKey
        val appearance = NoteBackgroundApplier.apply(
            root = binding.main,
            colorLayer = binding.viewNoteBackgroundColor,
            artContainer = binding.noteBackgroundArt,
            key = colorKey,
            repository = backgroundRepository,
            scope = lifecycleScope,
            editSurface = binding.viewNoteEditSurface,
            lineOverlay = binding.viewNoteLineOverlay
        )

        // Persist only background; ink is derived at display time for default text.
        NoteColorUtils.defaultTextColorOverrideHex = appearance.textColorHex

        val ink = appearance.textColor
        val hint = appearance.hintColor
        val muted = appearance.mutedColor
        lastAppearanceInkColor = ink
        lastAppearanceBackgroundColor = appearance.backgroundColor

        binding.etTitle.setTextColor(ink)
        binding.etTitle.setHintTextColor(hint)
        binding.titleDivider.setBackgroundColor(appearance.dividerColor)

        val header = binding.noteHeader
        header.btnBack.setColorFilter(ink)
        header.btnAttachment.setColorFilter(ink)
        header.btnMore.setColorFilter(ink)
        applyBookmarkIconColor(isFavorite = viewModel.uiState.value.isFavorite, ink = ink)
        header.tvNoteDate.setTextColor(ink)
        header.tvCategoryName.setTextColor(ink)
        header.etSearch.setTextColor(ink)
        header.etSearch.setHintTextColor(hint)
        header.searchContainer.background = noteHeaderChipBackground(
            fillColor = appearance.backgroundColor,
            strokeColor = appearance.dividerColor
        )
        header.btnCategorySelector.background = noteHeaderChipBackground(
            fillColor = appearance.backgroundColor,
            strokeColor = appearance.dividerColor
        )
        for (i in 0 until header.searchContainer.childCount) {
            val child = header.searchContainer.getChildAt(i)
            if (child is android.widget.ImageView) {
                child.setColorFilter(muted)
            }
        }
        val categoryIconBlack = ContextCompat.getColor(this, R.color.app_txt_black)
        for (i in 0 until header.btnCategorySelector.childCount) {
            when (val child = header.btnCategorySelector.getChildAt(i)) {
                is android.widget.ImageView -> child.setColorFilter(categoryIconBlack)
                is android.widget.TextView -> child.setTextColor(ink)
            }
        }

        if (::contentAdapter.isInitialized) {
            contentAdapter.setAdaptiveTextColors(ink, hint, muted)
        }

        for (i in 0 until binding.emptyContentHint.childCount) {
            when (val child = binding.emptyContentHint.getChildAt(i)) {
                is android.widget.TextView -> child.setTextColor(hint)
                is android.widget.ImageView -> child.setColorFilter(hint)
            }
        }

        // Active search must re-derive its palette for the new surface color.
        if (noteSearchQuery.isNotEmpty()) {
            if (::contentAdapter.isInitialized) {
                contentAdapter.setSearchQuery(noteSearchQuery, currentSearchHighlightPalette())
            }
            applyTitleSearchHighlight()
        }
    }

    private fun noteHeaderChipBackground(fillColor: Int, strokeColor: Int): GradientDrawable {
        val density = resources.displayMetrics.density
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 24f * density
            setColor(fillColor)
            setStroke((1 * density).toInt().coerceAtLeast(1), strokeColor)
        }
    }

    private fun resolveHeaderInkColor(): Int {
        return lastAppearanceInkColor
            ?: ContextCompat.getColor(this, R.color.app_txt_black)
    }

    private fun applyBookmarkIconColor(isFavorite: Boolean, ink: Int = resolveHeaderInkColor()) {
        val bookmark = binding.noteHeader.btnBookmark
        bookmark.setImageResource(
            if (isFavorite) R.drawable.ic_unbookmark else R.drawable.ic_bookmark
        )
        bookmark.setColorFilter(
            if (isFavorite) {
                ContextCompat.getColor(this, R.color.app_primary_color)
            } else {
                ink
            }
        )
    }

    private fun showNoteSearch() {
        val header = binding.noteHeader
        // Move focus to search and stop retaining/restoring any body editor's caret,
        // otherwise the scroll-idle restore steals focus back and the search closes.
        if (::contentAdapter.isInitialized) contentAdapter.clearRetainedFocus()
        header.searchContainer.isVisible = true
        header.etSearch.requestFocus()
        ViewCompat.getWindowInsetsController(header.etSearch)
            ?.show(WindowInsetsCompat.Type.ime())
        applyNoteSearch(header.etSearch.text?.toString().orEmpty())
    }

    private fun hideNoteSearch() {
        val header = binding.noteHeader
        if (!header.searchContainer.isVisible) return
        header.searchContainer.visibility = View.INVISIBLE
        if (header.etSearch.text?.isNotEmpty() == true) {
            header.etSearch.setText("")
        } else if (noteSearchQuery.isNotEmpty()) {
            applyNoteSearch("")
        }
        header.etSearch.clearFocus()
    }

    /**
     * Highlight colors derived from the current note surface so matches stay
     * readable (WCAG-contrast text) on dark, light, gradient, and pattern themes.
     */
    private fun currentSearchHighlightPalette(): SearchHighlightPalette {
        val background = lastAppearanceBackgroundColor
            ?: NoteBackgroundApplier.resolveBackgroundColor(
                viewModel.uiState.value.color,
                backgroundRepository
            )
        return SearchHighlightHelper.paletteFor(background)
    }

    private fun applyNoteSearch(query: String) {
        val normalized = query.trim()
        noteSearchQuery = normalized
        noteSearchMatchOrdinal = 0
        if (::contentAdapter.isInitialized) {
            contentAdapter.setSearchQuery(normalized, currentSearchHighlightPalette())
        }
        applyTitleSearchHighlight()
        if (normalized.isNotEmpty()) {
            // Keep search field focused while typing; only scroll/highlight live matches.
            navigateToSearchMatch(moveFocus = !binding.noteHeader.etSearch.hasFocus())
        }
    }

    private fun applyTitleSearchHighlight() {
        if (!::titleController.isInitialized) return
        titleController.setSearchHighlight(noteSearchQuery, currentSearchHighlightPalette())
    }

    private fun scrollToFirstSearchMatch() {
        noteSearchMatchOrdinal = 0
        navigateToSearchMatch(moveFocus = true)
    }

    /**
     * Search priority:
     * 1) Match in rich-text content (preferring current editor caret when possible)
     * 2) Title match only when content has no match
     */
    private fun navigateToSearchMatch(moveFocus: Boolean) {
        if (noteSearchQuery.isEmpty()) return

        val activeController = richTextToolbarBinder.getActiveController()
            ?.takeUnless { ::titleController.isInitialized && it === titleController }
        val preferBlockId = activeController?.blockId
        val preferStartOffset = activeController?.editorView?.selectionStart ?: 0

        if (::contentAdapter.isInitialized) {
            val contentMatched = contentAdapter.navigateToSearchMatch(
                recyclerView = binding.rvBlocks,
                moveFocus = moveFocus,
                matchOrdinal = noteSearchMatchOrdinal,
                preferBlockId = preferBlockId,
                preferStartOffset = preferStartOffset
            )
            if (contentMatched) return
        }

        // Title is only focused when it is the sole match location.
        val titleMatchStart = if (::titleController.isInitialized) {
            titleController.firstSearchMatchIndex()
        } else {
            SearchHighlightHelper.firstMatchIndex(titlePlainText(), noteSearchQuery)
        }
        if (titleMatchStart < 0) return
        if (!moveFocus) return
        permitTitleFocusFromUser = true
        if (::titleController.isInitialized) {
            titleController.focusSearchMatch(matchStart = titleMatchStart, showKeyboard = false)
        } else {
            binding.etTitle.requestFocus()
            val end = (titleMatchStart + noteSearchQuery.length)
                .coerceAtMost(binding.etTitle.text?.length ?: 0)
            binding.etTitle.setSelection(titleMatchStart, end)
        }
    }

    /** Reserved for future next/previous search UI. */
    @Suppress("unused")
    private fun navigateToNextSearchMatch() {
        if (noteSearchQuery.isEmpty() || !::contentAdapter.isInitialized) return
        val contentCount = contentAdapter.countSearchMatches()
        val titleHasMatch = if (::titleController.isInitialized) {
            titleController.hasSearchMatch()
        } else {
            SearchHighlightHelper.firstMatchIndex(titlePlainText(), noteSearchQuery) >= 0
        }
        val total = contentCount + if (titleHasMatch) 1 else 0
        if (total <= 0) return
        noteSearchMatchOrdinal = (noteSearchMatchOrdinal + 1) % total
        if (noteSearchMatchOrdinal < contentCount) {
            contentAdapter.navigateToSearchMatch(
                recyclerView = binding.rvBlocks,
                moveFocus = true,
                matchOrdinal = noteSearchMatchOrdinal
            )
        } else if (::titleController.isInitialized) {
            val titleMatchStart = titleController.firstSearchMatchIndex()
            if (titleMatchStart >= 0) {
                titleController.focusSearchMatch(matchStart = titleMatchStart, showKeyboard = false)
            }
        }
    }

    /** Reserved for future next/previous search UI. */
    @Suppress("unused")
    private fun navigateToPreviousSearchMatch() {
        if (noteSearchQuery.isEmpty() || !::contentAdapter.isInitialized) return
        val contentCount = contentAdapter.countSearchMatches()
        val titleHasMatch = if (::titleController.isInitialized) {
            titleController.hasSearchMatch()
        } else {
            SearchHighlightHelper.firstMatchIndex(titlePlainText(), noteSearchQuery) >= 0
        }
        val total = contentCount + if (titleHasMatch) 1 else 0
        if (total <= 0) return
        noteSearchMatchOrdinal = if (noteSearchMatchOrdinal <= 0) total - 1 else noteSearchMatchOrdinal - 1
        if (noteSearchMatchOrdinal < contentCount) {
            contentAdapter.navigateToSearchMatch(
                recyclerView = binding.rvBlocks,
                moveFocus = true,
                matchOrdinal = noteSearchMatchOrdinal
            )
        } else if (::titleController.isInitialized) {
            val titleMatchStart = titleController.firstSearchMatchIndex()
            if (titleMatchStart >= 0) {
                titleController.focusSearchMatch(matchStart = titleMatchStart, showKeyboard = false)
            }
        }
    }


    private fun showNoteDetails() {
        val uiState = viewModel.uiState.value
        val title = JsonUtils.blockContentPlainText(uiState.title)
        val blocks = uiState.blocks

        var fullText = title
        blocks.forEach { block ->
            if (block.type == BlockType.TEXT || block.type == BlockType.BULLET_LIST || block.type == BlockType.NUMBER_LIST) {
                fullText += "\n" + JsonUtils.blockContentPlainText(block.content)
            }
        }

        val words = fullText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
        val charCountIncludeSpaces = fullText.length
        val charCountExcludeSpaces = fullText.replace(" ", "").replace("\n", "").length

        val details = NoteDetails(
            createdAt = uiState.createdAt.takeIf { it > 0 } ?: System.currentTimeMillis(),
            modifiedAt = uiState.updatedAt.takeIf { it > 0 } ?: System.currentTimeMillis(),
            wordCount = words,
            charCountIncludeSpaces = charCountIncludeSpaces,
            charCountExcludeSpaces = charCountExcludeSpaces
        )

        openBottomSheet {
            NoteDetailsBottomSheet.show(supportFragmentManager, details)
        }
    }

    /** Hide IME first so bottom sheets open at the bottom, not above a closing keyboard. */
    private fun openBottomSheet(show: () -> Unit) {
        BottomSheetHelper.showAfterImeHidden(this, binding.root, show)
    }

    private fun shareCurrentNote() {
        contentAdapter.flushAllTextBlocks()
        titleController.flushContent()
        val state = viewModel.uiState.value
        val titlePlain = JsonUtils.blockContentPlainText(state.title)
            .ifBlank { getString(R.string.untitled) }
        val shareText = NoteShareTextFormatter.build(
            title = state.title,
            blocks = state.blocks,
            todos = todoViewModel.uiState.value.items,
            untitledFallback = getString(R.string.untitled)
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, titlePlain)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share)))
    }

    private fun duplicateCurrentNote() {
        lifecycleScope.launch {
            flushAllEditors()
            val copied = viewModel.duplicateCurrentNote(
                todoListId = todoViewModel.uiState.value.listId
            )
            Toast.makeText(
                this@CreateNotesActivity,
                if (copied) R.string.note_copied else R.string.note_copy_failed,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun exportAsPdf() {
        contentAdapter.flushAllTextBlocks()
        titleController.flushContent()
        val bitmap = getNoteBitmap() ?: return
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
            val page = pdfDocument.startPage(pageInfo)

            page.canvas.drawBitmap(bitmap, 0f, 0f, null)
            pdfDocument.finishPage(page)

            val exportDir = File(filesDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            val exportFile = File(exportDir, "note_${System.currentTimeMillis()}.pdf")

            FileOutputStream(exportFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            saveAndShareFile(exportFile, "application/pdf")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to export PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportAsText() {
        contentAdapter.flushAllTextBlocks()
        titleController.flushContent()
        val state = viewModel.uiState.value
        val title = JsonUtils.blockContentPlainText(state.title).ifBlank { getString(R.string.untitled) }
        val content = NoteShareTextFormatter.build(
            title = state.title,
            blocks = state.blocks,
            todos = todoViewModel.uiState.value.items,
            untitledFallback = getString(R.string.untitled)
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, content)
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share)))
    }

    private fun exportAsJpg() {
        contentAdapter.flushAllTextBlocks()
        titleController.flushContent()
        val bitmap = getNoteBitmap() ?: return
        try {
            val exportDir = File(filesDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            val exportFile = File(exportDir, "note_${System.currentTimeMillis()}.jpg")

            FileOutputStream(exportFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            saveAndShareFile(exportFile, "image/jpeg")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to export Image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getNoteBitmap(): Bitmap? {
        val width = binding.rvBlocks.width.takeIf { it > 0 } ?: return null
        val noteState = viewModel.uiState.value
        val todoState = todoViewModel.uiState.value
        val appearance = NoteBackgroundApplier.resolveAppearance(
            noteState.color,
            backgroundRepository
        )
        val ink = appearance.textColor
        val previousInkOverride = NoteColorUtils.defaultTextColorOverrideHex
        NoteColorUtils.defaultTextColorOverrideHex = appearance.textColorHex
        val mergedItems = NoteContentItem.merge(noteState.blocks, todoState.items)
            .filterNot { it is NoteContentItem.AddTodoFooter }

        val inflater = LayoutInflater.from(this)

        try {
        // 1. Prepare Title Section
        val titleText = JsonUtils.jsonToRichText(noteState.title)
        val titleDoc = if (titleText.plainText().isBlank()) {
            RichTextDocument(spans = listOf(RichTextSpan(text = getString(R.string.untitled))))
        } else {
            titleText
        }

        val titleLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val margin = resources.getDimensionPixelSize(R.dimen.editor_horizontal_margin)
            setPadding(margin, 24, margin, 12)
            setBackgroundColor(Color.TRANSPARENT)
        }

        val tvTitle = RichTextEditText(this).apply {
            background = null
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ink)
            setText(RichTextConverter.toSpannable(titleDoc))
        }
        titleLayout.addView(tvTitle)

        val titleDivider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2)
            setBackgroundColor(appearance.dividerColor)
        }
        titleLayout.addView(titleDivider)

        titleLayout.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        titleLayout.layout(0, 0, width, titleLayout.measuredHeight)

        // 2. Prepare Block Views
        val blockViews = mutableListOf<View>()
        var totalHeight = titleLayout.measuredHeight

        val numberOrders = NumberListHelper.computeDisplayOrders(noteState.blocks)

        mergedItems.forEach { item ->
            val blockView: View? = when (item) {
                is NoteContentItem.BlockItem -> {
                    val block = item.block
                    when (block.type) {
                        BlockType.TEXT -> {
                            val b = ItemBlockTextBinding.inflate(inflater)
                            val doc = JsonUtils.jsonToRichText(block.content)
                            b.richTextEditor.setTextColor(ink)
                            b.richTextEditor.setText(RichTextConverter.toSpannable(doc))
                            b.root
                        }
                        BlockType.NUMBER_LIST -> {
                            val b = ItemNumberListBinding.inflate(inflater)
                            val doc = JsonUtils.jsonToRichText(block.content)
                            b.etText.setTextColor(ink)
                            b.etText.setText(RichTextConverter.toSpannable(doc))
                            val order = numberOrders[block.id] ?: 1
                            b.tvNumber.setTextColor(ink)
                            b.tvNumber.text = "$order."
                            b.root
                        }
                        BlockType.BULLET_LIST -> {
                            val b = ItemBulletListBinding.inflate(inflater)
                            val doc = JsonUtils.jsonToRichText(block.content)
                            b.tvBullet.setTextColor(ink)
                            b.etText.setTextColor(ink)
                            b.etText.setText(RichTextConverter.toSpannable(doc))
                            b.root
                        }
                        BlockType.DIVIDER -> {
                            val b = ItemBlockDividerBinding.inflate(inflater)
                            b.root
                        }
                        BlockType.LINK -> {
                            val b = ItemBlockLinkBinding.inflate(inflater)
                            b.tvLink.setTextColor(ink)
                            b.tvLink.text = JsonUtils.blockContentPlainText(block.content)
                            b.root
                        }
                        else -> null
                    }
                }
                is NoteContentItem.MediaRow -> {
                    createExportMediaRowView(inflater, item)
                }
                is NoteContentItem.TodoEntry -> {
                    val todo = item.item
                    val b = ItemTodoBinding.inflate(inflater)
                    val doc = JsonUtils.jsonToRichText(todo.text)
                    b.etTodoText.setTextColor(ink)
                    b.etTodoText.setText(RichTextConverter.toSpannable(doc))
                    b.ivTodoCheck.setImageResource(if (todo.isCompleted) R.drawable.ic_todo_checked else R.drawable.ic_todo_unchecked)
                    b.ivDragHandle.visibility = View.GONE
                    b.ivDelete.visibility = View.GONE
                    b.root
                }
                else -> null
            }

            blockView?.let { view ->
                view.setBackgroundColor(Color.TRANSPARENT)
                view.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                view.layout(0, 0, width, view.measuredHeight)
                blockViews.add(view)
                totalHeight += view.measuredHeight
            }
        }

        totalHeight += 60 // Bottom margin

        return try {
            val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            NoteBackgroundApplier.drawExportBackground(
                canvas = canvas,
                width = width,
                height = totalHeight,
                key = noteState.color,
                repository = backgroundRepository,
                assets = assets,
                resources = resources
            )

            titleLayout.draw(canvas)
            var currentY = titleLayout.measuredHeight.toFloat()

            blockViews.forEach { view ->
                canvas.save()
                canvas.translate(0f, currentY)
                view.draw(canvas)
                canvas.restore()
                currentY += view.measuredHeight
            }
            bitmap
        } catch (e: OutOfMemoryError) {
            Toast.makeText(this, "Note too large to export as image", Toast.LENGTH_SHORT).show()
            null
        }
        } finally {
            NoteColorUtils.defaultTextColorOverrideHex = previousInkOverride
        }
    }

    private fun createExportMediaRowView(
        inflater: LayoutInflater,
        row: NoteContentItem.MediaRow
    ): View {
        val binding = ItemNoteMediaRowBinding.inflate(inflater)
        binding.btnMoreStart.visibility = View.GONE
        binding.btnMoreEnd.visibility = View.GONE

        bindExportMediaTile(
            block = row.blocks[0],
            imageView = binding.ivImageStart,
            playOverlay = binding.ivPlayStart,
            labelView = binding.tvLabelStart
        )

        val endBlock = row.blocks.getOrNull(1)
        if (endBlock != null) {
            binding.mediaTileSpacer.visibility = View.VISIBLE
            binding.mediaTilePlaceholder.visibility = View.GONE
            binding.cardTileEnd.visibility = View.VISIBLE
            bindExportMediaTile(
                block = endBlock,
                imageView = binding.ivImageEnd,
                playOverlay = binding.ivPlayEnd,
                labelView = binding.tvLabelEnd
            )
        } else {
            binding.mediaTileSpacer.visibility = View.VISIBLE
            binding.mediaTilePlaceholder.visibility = View.VISIBLE
            binding.cardTileEnd.visibility = View.GONE
        }
        return binding.root
    }

    private fun bindExportMediaTile(
        block: NoteBlock,
        imageView: ImageView,
        playOverlay: ImageView,
        labelView: TextView
    ) {
        val metadata = AttachmentMetadata.fromBlock(block)
        val isVideo = MediaBlockHelper.isVideo(block)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP

        if (isVideo) {
            val thumb = metadata?.thumbnailPath?.let { File(it) }?.takeIf { it.exists() }
            val bitmap = thumb?.let { decodeExportBitmap(it.absolutePath) }
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
            } else {
                imageView.setImageResource(R.drawable.ic_image)
            }
            playOverlay.visibility = View.VISIBLE
            labelView.visibility = View.VISIBLE
            labelView.text = metadata?.displayLabel().orEmpty()
        } else {
            val file = File(block.content)
            val bitmap = file.takeIf { it.exists() }?.let { decodeExportBitmap(it.absolutePath) }
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
            } else {
                imageView.setImageResource(R.drawable.ic_image)
            }
            playOverlay.visibility = View.GONE
            labelView.visibility = View.GONE
        }
    }

    private fun decodeExportBitmap(path: String): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            val maxSide = (resources.displayMetrics.widthPixels).coerceAtLeast(1)
            var sampleSize = 1
            while (bounds.outWidth / sampleSize > maxSide * 2 ||
                bounds.outHeight / sampleSize > maxSide * 2
            ) {
                sampleSize *= 2
            }

            BitmapFactory.decodeFile(
                path,
                BitmapFactory.Options().apply { inSampleSize = sampleSize }
            )
        } catch (_: OutOfMemoryError) {
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun saveAndShareFile(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }


    private fun toggleNoteLock() {
        if (viewModel.uiState.value.isLocked) {
            unlockCurrentNote()
        } else {
            lockCurrentNote()
        }
    }

    private fun lockCurrentNote() {
            PremiumFeatureGate.requirePremium(this, PremiumFeature.LOCK_NOTE) {
                lifecycleScope.launch {
                    val noteId = viewModel.uiState.value.noteId?.takeIf { it > 0 }
                        ?: viewModel.persistAndGetId()
                    todoViewModel.linkToNote(noteId)
                    if (notePasswordManager.hasPassword()) {
                        viewModel.setLocked(true)
                        Toast.makeText(this@CreateNotesActivity, R.string.note_locked, Toast.LENGTH_SHORT).show()
                    } else {
                        openWithCreateNotesInterAd {

                            setPasswordLauncher.launch(SetPasswordActivity.createSetIntent(this@CreateNotesActivity, noteId))
                        }
                    }
                }
            }
    }

    private fun unlockCurrentNote() {
        val noteId = viewModel.uiState.value.noteId?.takeIf { it > 0 } ?: return
        if (!notePasswordManager.hasPassword()) {
            viewModel.setLocked(false)
            Toast.makeText(this, R.string.note_unlocked, Toast.LENGTH_SHORT).show()
            return
        }
        openWithCreateNotesInterAd {
            verifyUnlockLauncher.launch(SetPasswordActivity.createVerifyIntent(this, noteId))
        }
    }

    private fun confirmDeleteNote() {
        NotesSelectionDialogs.showConfirmation(
            context = this,
            layoutInflater = layoutInflater,
            titleRes = R.string.delete_note,
            messageRes = R.string.delete_note_confirm,
            positiveButtonRes = R.string.delete_note,
            iconRes = R.drawable.ic_recylerbin,
            onPositiveClick = { viewModel.deleteCurrentNote() }
        )
    }

    private fun showCategorySelectionDialog() {
        val header = binding.noteHeader
        CategorySelectorPopup.show(
            activity = this,
            anchor = header.btnCategorySelector,
            categories = categoryViewModel.uiState.value.categories,
            onCategorySelected = { category ->
                viewModel.setCategory(category.id)

            },
            onCategoryCreated = {
                showAddCategorySheet()
            }
        )
    }

    private fun showAddCategorySheet() {
        val categories = categoryViewModel.uiState.value.categories
        // Do not use openBottomSheet here: Create Label needs the keyboard, and
        // waiting for IME hide then re-showing it makes the sheet open incorrectly.
        // Post so the category popup can finish dismissing first.
        binding.root.post {
            if (isFinishing || isDestroyed) return@post
            CreateLabelBottomSheet.show(
                activity = this,
                existingCategories = categories,
                onLabelAdded = { category ->
                    viewModel.setCategory(category.id)
                    Toast.makeText(this, R.string.note_category_updated, Toast.LENGTH_SHORT).show()
                },
                onCreateLabel = { name, onSuccess ->
                    categoryViewModel.createCategory(name, onSuccess)
                }
            )
        }
    }

    private fun observeCategoryState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                categoryViewModel.uiState.collect { state ->
                    state.error?.let { error ->
                        val message = when {
                            error.contains("empty", ignoreCase = true) ->
                                getString(R.string.category_name_empty)
                            error.contains("exists", ignoreCase = true) ->
                                getString(R.string.category_already_exists)
                            else -> error
                        }
                        Toast.makeText(this@CreateNotesActivity, message, Toast.LENGTH_SHORT).show()
                        categoryViewModel.clearError()
                    }
                }
            }
        }
    }

    private fun setupContentRecycler() {
        contentAdapter = NoteContentAdapter(
            richTextHost = this,
            defaultTypingFontSize = if (isNewNote) defaultFontSize else null,
            defaultTypingFontFamily = if (isNewNote) defaultFontFamily else null,
            defaultLineSpacingExtra = null,
            onBlockChanged = { blockId, content, checked ->
                viewModel.updateBlock(blockId, content, checked)
            },
            onBlockRemoved = { blockId -> removeContentBlock(blockId) },
            onTodoToggled = { itemId -> todoViewModel.toggleItem(itemId) },
            onTodoTextChanged = { itemId, text -> todoViewModel.updateItemText(itemId, text) },
            onTodoDeleted = { itemId -> removeTodoItem(itemId, focusPrevious = false) },
            onAddNextTodoAfter = { afterItemId -> addTodoAfter(afterItemId) },
            onDeleteEmptyTodo = { itemId -> removeTodoItem(itemId, focusPrevious = true) },
            onTodoReordered = { ordered -> persistContentOrder(ordered) },
            onAddTodoFooterClick = { addTodoAfterLastItem() },
            onNumberListTextChanged = { blockId, text, order ->
                viewModel.updateNumberListBlock(blockId, text, order)
            },
            onAddNextNumberList = { blockId -> addNextNumberListItem(blockId) },
            onExitNumberListOnEmpty = { blockId -> removeNumberListItem(blockId, focusPrevious = true) },
            numberListOrders = { currentNumberListOrders() },
            onBulletListTextChanged = { blockId, text, position ->
                viewModel.updateBulletListBlock(blockId, text, position)
            },
            onAddNextBulletList = { blockId -> addNextBulletListItem(blockId) },
            onExitBulletListOnEmpty = { blockId -> removeBulletListItem(blockId, focusPrevious = true) },
            bulletListPositions = { currentBulletListPositions() },
            onVoicePlayPause = { blockId, filePath, durationMs ->
                hideRichTextToolbar()
                attachmentViewModel.stopPlayback()
                voiceNoteViewModel.togglePlayback(blockId, filePath, durationMs)
            },
            onVoiceSeek = { blockId, positionMs ->
                hideRichTextToolbar()
                attachmentViewModel.stopPlayback()
                val voiceState = voiceNoteViewModel.uiState.value
                if (voiceState.activeBlockId == blockId) {
                    voiceNoteViewModel.seekPlayback(
                        positionMs = positionMs,
                        resumeIfPaused = voiceState.isPlaying,
                        startIfIdle = true
                    )
                } else {
                    val block = viewModel.uiState.value.blocks.find { it.id == blockId } ?: return@NoteContentAdapter
                    val metadata = com.notebook.notes.checklist.model.VoiceNoteMetadata.fromJson(block.extraData)
                    voiceNoteViewModel.startPlayback(blockId, block.content, metadata.durationMs, positionMs)
                }
            },
            onVoiceRename = { blockId, name -> viewModel.renameVoiceBlock(blockId, name.orEmpty()) },
            onDrawingEdit = { blockId, path -> openDrawingActivity(blockId, path) },
            onAttachmentImageOpen = { blockId, path, title, mimeType ->
                openWithCreateNotesInterAd {
                    val (items, index) = ImageViewerActivity.collectFromBlocks(
                        viewModel.uiState.value.blocks,
                        path
                    )
                    val intent = if (items.isEmpty()) {
                        ImageViewerActivity.createIntent(this, path, title, mimeType, blockId)
                    } else {
                        ImageViewerActivity.createIntent(this, items, index)
                    }
                    imageViewerLauncher.launch(intent)
                }
            },
            onAttachmentVideoOpen = { blockId, path, title, mimeType ->
                openWithCreateNotesInterAd {
                    videoPlayerLauncher.launch(
                        VideoPlayerActivity.createIntent(
                            context = this,
                            path = path,
                            title = title,
                            mimeType = mimeType,
                            blockId = blockId
                        )
                    )
                }
            },
            onAttachmentAudioPlayPause = { blockId, path, durationMs ->
                hideRichTextToolbar()
                voiceNoteViewModel.stopPlayback()
                attachmentViewModel.toggleAudioPlayback(blockId, path, durationMs)
            },
            onAttachmentAudioSeek = { blockId, path, durationMs, positionMs ->
                attachmentViewModel.seekAudio(blockId, path, durationMs, positionMs)
            },
            onAttachmentOpen = { path, mimeType ->
                attachmentViewModel.openWithSystemApp(this, path, mimeType)
            },
            onAttachmentRename = { blockId, name ->
                viewModel.renameAttachmentBlock(blockId, name.orEmpty())
            },
            voicePlaybackState = {
                val state = voiceNoteViewModel.uiState.value
                NoteContentAdapter.VoicePlaybackState(
                    activeBlockId = state.activeBlockId,
                    isPlaying = state.isPlaying,
                    position = state.currentPosition,
                    duration = state.playbackDuration
                )
            },
            attachmentPlaybackState = {
                val state = attachmentViewModel.uiState.value
                NoteContentAdapter.VoicePlaybackState(
                    activeBlockId = state.activeBlockId,
                    isPlaying = state.isPlaying,
                    position = state.currentPosition,
                    duration = state.playbackDuration
                )
            }
        )
        binding.rvBlocks.apply {
            layoutManager = LinearLayoutManager(this@CreateNotesActivity)
            itemAnimator = null
            // Hold focus on the list when a row editor blurs during scroll so focus
            // search does not jump the caret into etTitle (note header).
            isFocusable = true
            isFocusableInTouchMode = true
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            adapter = contentAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    // Requirement: focus must NEVER change automatically while scrolling.
                    // We do not move focus here (that would itself be an auto change);
                    // we only stop the headline from being able to grab focus so a
                    // recycled focused row can't bounce focus up to it.
                    when (newState) {
                        RecyclerView.SCROLL_STATE_DRAGGING,
                        RecyclerView.SCROLL_STATE_SETTLING -> {
                            isBlockListScrolling = true
                            lockTitleFocusForScroll()
                        }
                        RecyclerView.SCROLL_STATE_IDLE -> {
                            isBlockListScrolling = false
                            unlockTitleFocusAfterScroll()
                            // Put the caret back on the item the user was editing once
                            // the scroll settles (only if that row is visible). Skipped
                            // while the title or the note search is active so their focus
                            // isn't yanked away.
                            val searchActive = binding.noteHeader.searchContainer.isVisible ||
                                binding.noteHeader.etSearch.isFocused
                            if (!binding.etTitle.isFocused && !searchActive) {
                                binding.rvBlocks.post {
                                    contentAdapter.restoreRetainedFocusIfVisible(binding.rvBlocks)
                                }
                            }
                        }
                    }
                }
            })
        }
        contentAdapter.attachTodoDragHelper(binding.rvBlocks)
    }

    /**
     * Focus must never change automatically while the list scrolls. Rather than
     * juggling focus (which is itself an auto focus change), we drop the headline's
     * touch-focusability for the scroll's duration so a recycled focused row can't
     * bounce focus up to it. Active title editing is left untouched.
     */
    private fun lockTitleFocusForScroll() {
        if (titleFocusLockedForScroll) return
        // Never disturb the caret if the user is actually editing the title.
        if (binding.etTitle.isFocused) return
        titleFocusLockedForScroll = true
        titleFocusableBeforeScrollLock = binding.etTitle.isFocusableInTouchMode
        binding.etTitle.isFocusable = false
        binding.etTitle.isFocusableInTouchMode = false
    }

    /** Restores headline focusability once the list stops scrolling. */
    private fun unlockTitleFocusAfterScroll() {
        if (!titleFocusLockedForScroll) return
        titleFocusLockedForScroll = false
        binding.etTitle.isFocusable = titleFocusableBeforeScrollLock
        binding.etTitle.isFocusableInTouchMode = titleFocusableBeforeScrollLock
    }

    private fun rejectStolenTitleFocus() {
        if (isRejectingTitleFocus) return
        if (!binding.etTitle.isFocused) return
        if (permitTitleFocusFromUser) return
        isRejectingTitleFocus = true
        // Dropping touch-focusability before clearFocus() stops the framework from
        // immediately re-assigning focus back to the title (etTitle is the first
        // focusable-in-touch-mode view in the layout). Without this, clearFocus ->
        // refocus -> onFocusChanged(true) -> post reject loops forever and ANRs.
        binding.etTitle.isFocusableInTouchMode = false
        binding.etTitle.clearFocus()
        val body = lastBodyEditor?.takeIf { it.isAttachedToWindow }
            ?: (richTextToolbarBinder.getActiveController()?.editorView?.takeIf {
                it !== binding.etTitle && it.isAttachedToWindow
            })
        if (body != null) {
            body.requestFocus()
        } else {
            binding.rvBlocks.requestFocus()
        }
        // Restore touch-focusability next frame, after focus has settled elsewhere,
        // so a genuine user tap on the title can still focus it later.
        binding.etTitle.post {
            binding.etTitle.isFocusableInTouchMode = true
            isRejectingTitleFocus = false
        }
    }

    private fun setupTitleRichText() {
        val editText = binding.etTitle
        // Keep title on a single line; Next/Enter moves into the first body item.
        editText.setSingleLine(true)
        editText.maxLines = 1
        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        editText.imeOptions = EditorInfo.IME_ACTION_NEXT
        val titleSize = titleDefaultFontSize()
        editText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, titleSize.toFloat())
        if (isNewNote) {
            val titleStyle = defaultTitleStyle()
            editText.typeface = RichTextFormats.resolveTypeface(
                family = titleStyle.fontFamily,
                bold = titleStyle.bold,
                italic = titleStyle.italic,
                context = this
            )
        }
        editText.filters = arrayOf(
            InputFilter.LengthFilter(TITLE_MAX_LENGTH),
            InputFilter { source, start, end, _, _, _ ->
                var hasNewline = false
                for (i in start until end) {
                    if (source[i] == '\n' || source[i] == '\r') {
                        hasNewline = true
                        break
                    }
                }
                if (!hasNewline) return@InputFilter null
                buildString(end - start) {
                    for (i in start until end) {
                        val ch = source[i]
                        if (ch != '\n' && ch != '\r') append(ch)
                    }
                }
            }
        )

        val controller = RichTextEditorController(editText)
        editText.controller = controller
        controller.typingStyle = defaultTitleStyle()

        // Only an explicit tap may focus the title — never focus-search after RV scroll.
        editText.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    permitTitleFocusFromUser = true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    v.performClick()
                }
            }
            false
        }

        editText.onFocusChangedListener = { _, focused ->
            handleTitleFocusChanged(focused = focused, controller = controller)
        }

        controller.onContentChanged = { document ->
            if (!isLoadingTitleDocument) {
                val json = JsonUtils.richTextToJson(document)
                lastAppliedTitleJson = json
                viewModel.setTitle(json)
                todoViewModel.setTitle(document.plainText())
            }
        }
        controller.onFormatStateChanged = {
            if (editText.isFocused) {
                richTextToolbarBinder.refreshToolbarState()
            }
        }

        titleController = controller
        loadTitleDocument("")
    }

    private fun handleTitleFocusChanged(focused: Boolean, controller: RichTextEditorController) {
        if (focused) {
            if (!permitTitleFocusFromUser) {
                // Don't stack reject runnables while one is already in flight — that
                // is what turned the focus bounce into a busy-loop / ANR.
                if (!isRejectingTitleFocus) {
                    binding.etTitle.post { rejectStolenTitleFocus() }
                }
                return
            }
            permitTitleFocusFromUser = false
            // User is now editing the title — stop retaining a body editor's caret.
            if (::contentAdapter.isInitialized) contentAdapter.clearRetainedFocus()
            onRichTextEditorFocused(controller)
            binding.richTextToolbar.root.visibility = View.VISIBLE
            return
        }
        if (!richTextToolbarBinder.suppressEditorBlurHandling) {
            onRichTextEditorBlurred()
        }
    }

    private fun titleDefaultFontSize(): Int = when {
        !isNewNote -> TITLE_DEFAULT_FONT_SIZE
        hasSavedDefaultFontSize -> defaultFontSize
        else -> TITLE_DEFAULT_FONT_SIZE
    }

    private fun defaultTitleStyle() = RichTextSpan(
        bold = true,
        fontSize = titleDefaultFontSize(),
        fontFamily = if (isNewNote) defaultFontFamily else Constant.DEFAULT_FONT_FAMILY
    )

    private fun withTitleDefaults(document: RichTextDocument): RichTextDocument {
        val plain = document.plainText()
        if (plain.isBlank()) {
            return RichTextDocument(spans = listOf(defaultTitleStyle()))
        }
        val isLegacyPlain = document.spans.all { span ->
            !span.bold &&
                span.fontSize <= 16 &&
                !span.italic &&
                !span.underline &&
                !span.strikethrough &&
                span.highlightColor.isNullOrBlank()
        }
        return if (isLegacyPlain) {
            RichTextDocument(
                spans = listOf(defaultTitleStyle().copy(text = plain)),
                alignment = document.alignment
            )
        } else {
            document
        }
    }

    private fun loadTitleDocument(rawTitle: String) {
        if (!::titleController.isInitialized) return
        val document = withTitleDefaults(JsonUtils.jsonToRichText(rawTitle))
        titleController.loadDocument(document)
        titleController.typingStyle = if (document.plainText().isBlank()) {
            defaultTitleStyle()
        } else {
            document.spans.lastOrNull()?.copy(text = "") ?: defaultTitleStyle()
        }
    }

    private fun titlePlainText(): String {
        return if (::titleController.isInitialized) {
            titleController.getDocument().plainText()
        } else {
            JsonUtils.blockContentPlainText(binding.etTitle.text?.toString().orEmpty())
        }
    }

    private fun setupActions() {
        binding.etTitle.imeOptions = EditorInfo.IME_ACTION_NEXT
        binding.etTitle.setOnEditorActionListener { _, actionId, event ->
            if (shouldMoveFromTitle(actionId, event)) {
                moveFromTitleToTextBlock()
                true
            } else {
                false
            }
        }
        binding.etTitle.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                moveFromTitleToTextBlock()
                true
            } else {
                false
            }
        }
    }

    private fun openDrawingActivity(blockId: Long = -1L, existingPath: String? = null) {
        openWithCreateNotesInterAd {
            val intent = Intent(this, DrawingActivity::class.java).apply {
                if (existingPath != null) {
                    putExtra(DrawingActivity.EXTRA_EXISTING_PATH, existingPath)
                    putExtra(DrawingActivity.EXTRA_BLOCK_ID, blockId)
                }
            }
            drawingLauncher.launch(intent)
        }
    }

    private fun showInsertAudioSheet() {
        hideRichTextToolbar()
        attachmentViewModel.stopPlayback()
        openBottomSheet {
            InsertAudioBottomSheet.show(
                activity = this,
                voiceNoteViewModel = voiceNoteViewModel,
                onStartRecording = { startRecordingWithPermission() }
            )
        }
    }

    private fun startRecordingWithPermission() {
        when (permissionHelper.checkMicrophonePermission(this)) {
            PermissionHelper.PermissionStatus.GRANTED -> voiceNoteViewModel.startRecording()
            else -> requestMicPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun openReminderFlow() {
        if (permissionHelper.hasReminderPermissions(this)) {
            openSetReminderBottomSheet()
            return
        }
        openBottomSheet {
            ReminderPermissionBottomSheet.show(
                fragmentManager = supportFragmentManager,
                onAllow = { requestReminderPermissions() }
            )
        }
    }

    private fun requestReminderPermissions() {
        if (permissionHelper.checkNotificationPermission(this) != PermissionHelper.PermissionStatus.GRANTED &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        requestExactAlarmPermissionOrOpenReminder()
    }

    private fun requestExactAlarmPermissionOrOpenReminder() {
        if (permissionHelper.canScheduleExactAlarms()) {
            openSetReminderBottomSheet()
            return
        }
        val intent = permissionHelper.createExactAlarmSettingsIntent()
        if (intent != null) {
            requestExactAlarmPermissionLauncher.launch(intent)
        } else {
            openSetReminderBottomSheet()
        }
    }

    private fun openSetReminderBottomSheet() {
        val state = viewModel.uiState.value
        openBottomSheet {
            SetReminderBottomSheet.show(
                fragmentManager = supportFragmentManager,
                initialTimeMillis = state.reminderTimeMillis.takeIf { state.reminderEnabled && it > 0L },
                initialRepeatType = RepeatType.fromStorage(state.repeatType)
            ) { calendar, repeatType ->
                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    Toast.makeText(this, R.string.reminder_time_in_past, Toast.LENGTH_SHORT).show()
                    return@show
                }
                lifecycleScope.launch {
                    flushAllEditors()
                    val saved = viewModel.setReminder(calendar.timeInMillis, repeatType)
                    if (saved) {
                        Toast.makeText(this@CreateNotesActivity, R.string.reminder_set, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun startVoiceRecording() {
        showInsertAudioSheet()
    }

    private fun observeVoiceState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                voiceNoteViewModel.uiState.collect { state ->
                    state.completedRecording?.let { recording ->
                        val position = prepareSmartContentInsert()
                        viewModel.addVoiceBlock(
                            path = recording.filePath,
                            durationMs = recording.durationMs,
                            fileSize = recording.fileSize,
                            waveform = recording.waveform,
                            position = position
                        )
                        voiceNoteViewModel.clearCompletedRecording()
                        if (pendingSaveAfterRecording) {
                            pendingSaveAfterRecording = false
                            flushAllEditors()
                            viewModel.saveNote()
                        } else {
                            completeSmartContentInsert(position)
                        }
                    }

                    state.error?.let { error ->
                        val message = when (error) {
                            "Recording failed" -> getString(R.string.recording_failed)
                            "Audio file not found" -> getString(R.string.audio_file_not_found)
                            "Playback failed" -> getString(R.string.playback_failed)
                            else -> error
                        }
                        Toast.makeText(this@CreateNotesActivity, message, Toast.LENGTH_SHORT).show()
                        voiceNoteViewModel.clearError()
                    }

                    contentAdapter.refreshVoicePlayback()
                }
            }
        }
    }

    private fun nextContentPosition(): Int {
        val blockMax = viewModel.uiState.value.blocks.maxOfOrNull { it.position } ?: -1
        val todoMax = todoViewModel.uiState.value.items.maxOfOrNull { it.position } ?: -1
        return max(blockMax, todoMax) + 1
    }

    private fun prepareChecklistInsert() {
        flushAllEditors()
    }

    private fun prepareSmartContentInsert(): Int {
        prepareChecklistInsert()
        val position = positionForNewChecklistContent()
        shiftIfNeededForInsert(position)
        return position
    }

    private fun completeSmartContentInsert(insertPosition: Int) {
        ensureTrailingNotesSection(insertPosition)
        syncContentList()
    }

    private fun ensureDefaultTextBlockAndFocus() {
        if (defaultTextBlockInitialized) return
        val state = viewModel.uiState.value
        if (state.isLoading || state.noteId != null) return
        defaultTextBlockInitialized = true
        if (state.blocks.isNotEmpty()) return
        val blockId = viewModel.addNotesTextBlock(0)
        contentAdapter.setPendingFocusBlockId(blockId)
        syncContentList(focusBlockId = blockId)
    }

    private fun findTrailingNotesHereBlock(): NoteBlock? =
        viewModel.uiState.value.blocks
            .filter { it.extraData == NoteBlockExtra.NOTES_HERE }
            .maxByOrNull { it.position }

    private fun isNotesBlockFilled(block: NoteBlock): Boolean {
        val liveText = contentAdapter.getLivePlainTextForBlock(block.id)
        if (!liveText.isNullOrBlank()) return true
        return JsonUtils.jsonToRichText(block.content).plainText().isNotBlank()
    }

    private fun positionForNewChecklistContent(): Int {
        val trailing = findTrailingNotesHereBlock()
        return when {
            trailing == null -> nextContentPosition()
            isNotesBlockFilled(trailing) -> trailing.position + 1
            else -> trailing.position
        }
    }

    private fun resolveInsertPosition(preferredPosition: Int): Int {
        val trailing = findTrailingNotesHereBlock() ?: return preferredPosition
        return when {
            !isNotesBlockFilled(trailing) && preferredPosition >= trailing.position -> trailing.position
            isNotesBlockFilled(trailing) && preferredPosition <= trailing.position -> trailing.position + 1
            else -> preferredPosition
        }
    }

    private fun shiftIfNeededForInsert(insertPosition: Int) {
        val trailing = findTrailingNotesHereBlock()
        if (trailing != null && !isNotesBlockFilled(trailing) && insertPosition == trailing.position) {
            shiftContentFrom(insertPosition)
            return
        }
        val blockOccupied = viewModel.uiState.value.blocks.any { it.position == insertPosition }
        val todoOccupied = todoViewModel.uiState.value.items.any { it.position == insertPosition }
        if (blockOccupied || todoOccupied) {
            shiftContentFrom(insertPosition)
        }
    }

    private fun shiftContentFrom(position: Int) {
        viewModel.shiftPositionsFrom(position, 1)
        todoViewModel.shiftPositionsFrom(position, 1)
    }

    private fun ensureTrailingNotesSection(afterPosition: Int): Long {
        val trailing = findTrailingNotesHereBlock()
        if (trailing != null && !isNotesBlockFilled(trailing)) {
            return trailing.id
        }
        return viewModel.addNotesTextBlock(afterPosition + 1)
    }

    private fun addTodoItem() {
        prepareChecklistInsert()
        val todoPosition = positionForNewChecklistContent()
        shiftIfNeededForInsert(todoPosition)
        todoViewModel.addItem(
            title = titlePlainText(),
            position = todoPosition
        )
        ensureTrailingNotesSection(todoPosition)
        syncContentList()
    }

    private fun addDividerBlock() {
        prepareChecklistInsert()
        val dividerPosition = positionForNewChecklistContent()
        shiftIfNeededForInsert(dividerPosition)
        viewModel.addDividerBlock(dividerPosition)
        ensureTrailingNotesSection(dividerPosition)
        syncContentList()
    }

    private fun removeDividerAboveContentIfNeeded(contentId: Long, isTodo: Boolean) {
        val merged = NoteContentItem.merge(
            viewModel.uiState.value.blocks,
            todoViewModel.uiState.value.items
        ).filterNot { it is NoteContentItem.AddTodoFooter }
        val index = merged.indexOfFirst { item ->
            when {
                isTodo && item is NoteContentItem.TodoEntry -> item.item.id == contentId
                !isTodo && item is NoteContentItem.BlockItem -> item.block.id == contentId
                !isTodo && item is NoteContentItem.MediaRow -> item.blocks.any { it.id == contentId }
                else -> false
            }
        }
        if (index <= 0) return
        val previous = merged[index - 1]
        if (previous is NoteContentItem.BlockItem && previous.block.type == BlockType.DIVIDER) {
            viewModel.removeBlock(previous.block.id)
        }
    }

    private fun removeContentBlock(blockId: Long) {
        val block = viewModel.uiState.value.blocks.find { it.id == blockId } ?: return
        // Keep the default "Notes Here" placeholder block; backspace must not remove it.
        if (block.extraData == NoteBlockExtra.NOTES_HERE) return
        if (block.type != BlockType.DIVIDER) {
            removeDividerAboveContentIfNeeded(blockId, isTodo = false)
        }
        viewModel.removeBlock(blockId)
        when (block.type) {
            BlockType.NUMBER_LIST -> {
                viewModel.normalizeNumberListOrders()
                if (viewModel.uiState.value.blocks.none { it.type == BlockType.NUMBER_LIST }) {
                    numberListViewModel.disableMode()
                }
            }
            BlockType.BULLET_LIST -> {
                viewModel.normalizeBulletListPositions()
                if (viewModel.uiState.value.blocks.none { it.type == BlockType.BULLET_LIST }) {
                    bulletListViewModel.disableMode()
                }
            }
            else -> Unit
        }
    }

    private fun removeNumberListItem(blockId: Long, focusPrevious: Boolean) {
        val numberBlocks = viewModel.uiState.value.blocks
            .filter { it.type == BlockType.NUMBER_LIST }
            .sortedBy { it.position }
        val previousId = if (focusPrevious) {
            val index = numberBlocks.indexOfFirst { it.id == blockId }
            numberBlocks.getOrNull(index - 1)?.id
        } else {
            null
        }
        removeContentBlock(blockId)
        if (viewModel.uiState.value.blocks.none { it.type == BlockType.NUMBER_LIST }) {
            numberListViewModel.disableMode()
            richTextToolbarBinder.refreshToolbarState()
        }
        if (previousId != null) {
            contentAdapter.setPendingFocusNumberListId(previousId)
            syncContentList(pendingNumberListId = previousId)
        } else {
            syncContentList()
        }
    }

    private fun removeBulletListItem(blockId: Long, focusPrevious: Boolean) {
        val bulletBlocks = viewModel.uiState.value.blocks
            .filter { it.type == BlockType.BULLET_LIST }
            .sortedBy { it.position }
        val previousId = if (focusPrevious) {
            val index = bulletBlocks.indexOfFirst { it.id == blockId }
            bulletBlocks.getOrNull(index - 1)?.id
        } else {
            null
        }
        removeContentBlock(blockId)
        if (viewModel.uiState.value.blocks.none { it.type == BlockType.BULLET_LIST }) {
            bulletListViewModel.disableMode()
            richTextToolbarBinder.refreshToolbarState()
        }
        if (previousId != null) {
            contentAdapter.setPendingFocusBulletListId(previousId)
            syncContentList(pendingBulletListId = previousId)
        } else {
            syncContentList()
        }
    }

    private fun removeTodoItem(itemId: Long, focusPrevious: Boolean) {
        removeDividerAboveContentIfNeeded(itemId, isTodo = true)
        todoViewModel.uiState.value.items.find { it.id == itemId }?.let { item ->
            viewModel.shiftPositionsFrom(item.position + 1, -1)
        }
        if (focusPrevious) {
            todoViewModel.deleteItemAndFocusPrevious(itemId)
        } else {
            todoViewModel.deleteItem(itemId)
        }
    }

    private fun addTodoAfterLastItem() {
        val lastTodo = todoViewModel.uiState.value.items.maxByOrNull { it.position } ?: run {
            addTodoItem()
            return
        }
        addTodoAfter(lastTodo.id)
    }

    private fun addTodoAfter(afterItemId: Long) {
        val afterItem = todoViewModel.uiState.value.items.find { it.id == afterItemId }
        if (afterItem == null) {
            addTodoItem()
            return
        }
        prepareChecklistInsert()
        val insertPosition = resolveInsertPosition(afterItem.position + 1)
        shiftIfNeededForInsert(insertPosition)
        todoViewModel.insertItemAt(insertPosition, titlePlainText())
        ensureTrailingNotesSection(insertPosition)
        syncContentList()
    }

    private fun persistContentOrder(ordered: List<NoteContentItem>) {
        val blockPositions = mutableMapOf<Long, Int>()
        val todoPositions = mutableMapOf<Long, Int>()
        var position = 0
        ordered.filterNot { it is NoteContentItem.AddTodoFooter }.forEach { item ->
            when (item) {
                is NoteContentItem.BlockItem -> {
                    if (item.block.position != position) {
                        blockPositions[item.block.id] = position
                    }
                    position++
                }
                is NoteContentItem.MediaRow -> {
                    item.blocks.forEach { block ->
                        if (block.position != position) {
                            blockPositions[block.id] = position
                        }
                        position++
                    }
                }
                is NoteContentItem.TodoEntry -> {
                    if (item.item.position != position) {
                        todoPositions[item.item.id] = position
                    }
                    position++
                }
                is NoteContentItem.AddTodoFooter -> Unit
            }
        }
        viewModel.applyBlockPositions(blockPositions)
        todoViewModel.applyItemPositions(todoPositions)
    }

    private fun currentNumberListOrders(): Map<Long, Int> =
        NumberListHelper.computeDisplayOrders(viewModel.uiState.value.blocks)

    private fun currentBulletListPositions(): Map<Long, Int> =
        BulletListHelper.computeDisplayPositions(viewModel.uiState.value.blocks)

    private fun isTrailingNotesFilled(): Boolean {
        val trailing = findTrailingNotesHereBlock() ?: return false
        return isNotesBlockFilled(trailing)
    }

    private fun insertBulletListFromChecklist() {
        val position = positionForNewChecklistContent()
        val groupId = bulletListViewModel.createListGroupId()
        shiftIfNeededForInsert(position)
        val blockId = viewModel.addBulletListBlock(
            text = "",
            position = position,
            listGroupId = groupId
        )
        ensureTrailingNotesSection(position)
        bulletListViewModel.enableMode()
        bulletListViewModel.setLastAddedBlockId(blockId)
        contentAdapter.setPendingFocusBulletListId(blockId)
        syncContentList()
    }

    private fun insertNumberListFromChecklist() {
        // Smart group insertion: treat all numbered items as one logical list. When
        // the note already has a numbered list, insert the new item directly AFTER
        // the last numbered item (reusing its group so numbering continues) instead
        // of appending it below unrelated text/media blocks. This keeps the list
        // grouped and pushes any following blocks down. With no existing numbered
        // list, fall back to starting a fresh list at the normal insert position.
        val lastNumbered = viewModel.uiState.value.blocks
            .filter { it.type == BlockType.NUMBER_LIST }
            .maxWithOrNull(compareBy({ it.position }, { it.id }))

        if (lastNumbered == null) {
            insertFirstNumberListItem()
            return
        }

        val groupId = NumberListMetadata.fromBlock(lastNumbered).listGroupId
        val insertPosition = lastNumbered.position + 1
        shiftIfNeededForInsert(insertPosition)
        val blockId = viewModel.addNumberListBlock(
            text = "",
            position = insertPosition,
            order = 1,
            listGroupId = groupId
        )
        ensureNotesHereBelowNumberList()
        val startNumber = currentNumberListOrders()[blockId] ?: 1
        numberListViewModel.enableMode(startNumber)
        numberListViewModel.setLastAddedBlockId(blockId)
        contentAdapter.setPendingFocusNumberListId(blockId)
        syncContentList()
    }

    /** Starts a brand-new numbered list at the end of the note (no existing list). */
    private fun insertFirstNumberListItem() {
        val position = positionForNewChecklistContent()
        val groupId = numberListViewModel.createListGroupId()
        shiftIfNeededForInsert(position)
        val blockId = viewModel.addNumberListBlock(
            text = "",
            position = position,
            order = 1,
            listGroupId = groupId
        )
        ensureNotesHereBelowNumberList()
        numberListViewModel.enableMode(1)
        numberListViewModel.setLastAddedBlockId(blockId)
        contentAdapter.setPendingFocusNumberListId(blockId)
        syncContentList()
    }

    /**
     * Keeps the default "Notes Here" placeholder immediately below the numbered-list
     * group. If an empty Notes Here is currently above the list (which splits the
     * editor layout), it is moved under the last numbered item. If none exists, one
     * is created there. Filled notes content is left in place and a fresh empty
     * Notes Here is added below the list instead.
     */
    private fun ensureNotesHereBelowNumberList() {
        val blocks = viewModel.uiState.value.blocks
        val numbered = blocks
            .filter { it.type == BlockType.NUMBER_LIST }
            .sortedWith(compareBy({ it.position }, { it.id }))
        if (numbered.isEmpty()) return

        val lastNumbered = numbered.last()
        val notesBlocks = blocks.filter { it.extraData == NoteBlockExtra.NOTES_HERE }
        val emptyNotes = notesBlocks.firstOrNull { !isNotesBlockFilled(it) }
        val anyNotes = emptyNotes ?: notesBlocks.minByOrNull { it.position }

        if (anyNotes == null) {
            val target = lastNumbered.position + 1
            shiftIfNeededForInsert(target)
            viewModel.addNotesTextBlock(target)
            return
        }

        // Empty Notes Here already sits below the number group — done.
        if (emptyNotes != null && emptyNotes.position > lastNumbered.position) return

        // Empty Notes Here is above (or between) the list — move it under the group.
        if (emptyNotes != null && emptyNotes.position <= lastNumbered.position) {
            relocateEmptyNotesHereBelowNumberList(emptyNotes, numbered)
            return
        }

        // Only filled Notes Here exists (and it may sit above the list) — add a fresh
        // empty placeholder below the numbered group without moving the filled text.
        val target = lastNumbered.position + 1
        shiftIfNeededForInsert(target)
        viewModel.addNotesTextBlock(target)
    }

    /**
     * Rebuilds block order so all numbered items stay contiguous and the empty
     * Notes Here placeholder is placed immediately after them.
     */
    private fun relocateEmptyNotesHereBelowNumberList(
        emptyNotes: NoteBlock,
        numbered: List<NoteBlock>
    ) {
        val numberedIds = numbered.map { it.id }.toSet()
        val sorted = viewModel.uiState.value.blocks
            .sortedWith(compareBy({ it.position }, { it.id }))
        val before = sorted.filter {
            it.id != emptyNotes.id &&
                it.id !in numberedIds &&
                it.position < numbered.first().position
        }
        val after = sorted.filter {
            it.id != emptyNotes.id &&
                it.id !in numberedIds &&
                it.position >= numbered.first().position
        }
        val reordered = before + numbered + listOf(emptyNotes) + after
        val positions = reordered.mapIndexed { index, block -> block.id to index }.toMap()
        viewModel.applyBlockPositions(positions)
    }

    private fun startBulletListMode() {
        hideRichTextToolbar()
        if (bulletListViewModel.uiState.value.isBulletModeEnabled) {
            bulletListViewModel.disableMode()
            richTextToolbarBinder.refreshToolbarState()
            return
        }
        if (numberListViewModel.uiState.value.isNumberModeEnabled) {
            numberListViewModel.disableMode()
        }
        prepareChecklistInsert()
        insertBulletListFromChecklist()
        richTextToolbarBinder.refreshToolbarState()
    }

    private fun addNextBulletListItem(afterBlockId: Long) {
        richTextToolbarBinder.suppressEditorBlurHandling = true
        prepareChecklistInsert()
        val blocks = viewModel.uiState.value.blocks
        val afterBlock = blocks.find { it.id == afterBlockId } ?: run {
            richTextToolbarBinder.suppressEditorBlurHandling = false
            return
        }
        val insertPosition = resolveInsertPosition(afterBlock.position + 1)
        shiftIfNeededForInsert(insertPosition)
        val groupId = BulletListMetadata.fromBlock(afterBlock).listGroupId
        val blockId = viewModel.insertBulletListItems(
            items = listOf(""),
            atPosition = insertPosition,
            listGroupId = groupId
        ) ?: run {
            richTextToolbarBinder.suppressEditorBlurHandling = false
            return
        }
        bulletListViewModel.enableMode()
        bulletListViewModel.setLastAddedBlockId(blockId)
        contentAdapter.setPendingFocusBulletListId(blockId)
        syncContentList()
        binding.rvBlocks.post {
            richTextToolbarBinder.suppressEditorBlurHandling = false
        }
    }

    private fun handleBulletListFromRichText(controller: RichTextEditorController) {
        prepareChecklistInsert()
        hideRichTextToolbar()
        if (isTrailingNotesFilled()) {
            val selectedText = controller.getSelectedPlainText()
            if (selectedText.isNotBlank()) {
                convertSelectionToBulletList(controller, selectedText)
            } else {
                bulletListViewModel.disableMode()
                numberListViewModel.disableMode()
                insertBulletListFromChecklist()
            }
            richTextToolbarBinder.refreshToolbarState()
            return
        }
        if (bulletListViewModel.uiState.value.isBulletModeEnabled) {
            bulletListViewModel.disableMode()
            richTextToolbarBinder.refreshToolbarState()
            return
        }
        if (numberListViewModel.uiState.value.isNumberModeEnabled) {
            numberListViewModel.disableMode()
        }

        val selectedText = controller.getSelectedPlainText()
        if (selectedText.isNotBlank()) {
            convertSelectionToBulletList(controller, selectedText)
            return
        }

        startBulletListMode()
    }

    private fun convertSelectionToBulletList(
        controller: RichTextEditorController,
        selectedText: String
    ) {
        val items = bulletListViewModel.splitSelectionToItems(selectedText)
        if (items.isEmpty()) {
            startBulletListMode()
            return
        }

        prepareChecklistInsert()
        controller.removeSelectedText()
        controller.flushContent()

        val anchorBlockId = controller.blockId
        val blocks = viewModel.uiState.value.blocks
        val anchorBlock = anchorBlockId?.let { id -> blocks.find { it.id == id } }
        val insertPosition = resolveInsertPosition(
            (anchorBlock?.position ?: nextContentPosition()) + 1
        )
        val groupId = bulletListViewModel.createListGroupId()
        shiftIfNeededForInsert(insertPosition)

        val lastBlockId = viewModel.insertBulletListItems(
            items = items,
            atPosition = insertPosition,
            listGroupId = groupId
        ) ?: return

        ensureTrailingNotesSection(insertPosition + items.lastIndex)

        bulletListViewModel.enableMode()
        bulletListViewModel.setLastAddedBlockId(lastBlockId)
        contentAdapter.setPendingFocusBulletListId(lastBlockId)
        syncContentList()
        richTextToolbarBinder.refreshToolbarState()
    }

    private fun startNumberListMode() {
        hideRichTextToolbar()
        if (numberListViewModel.uiState.value.isNumberModeEnabled) {
            numberListViewModel.disableMode()
            richTextToolbarBinder.refreshToolbarState()
            return
        }
        if (bulletListViewModel.uiState.value.isBulletModeEnabled) {
            bulletListViewModel.disableMode()
        }
        prepareChecklistInsert()
        insertNumberListFromChecklist()
        richTextToolbarBinder.refreshToolbarState()
    }

    private fun addNextNumberListItem(afterBlockId: Long) {
        // Keep toolbar stable while focus moves from the current row to the new one.
        richTextToolbarBinder.suppressEditorBlurHandling = true
        prepareChecklistInsert()
        val blocks = viewModel.uiState.value.blocks
        val afterBlock = blocks.find { it.id == afterBlockId } ?: run {
            richTextToolbarBinder.suppressEditorBlurHandling = false
            return
        }
        val orders = currentNumberListOrders()
        val nextOrder = (orders[afterBlockId] ?: 1) + 1
        // Insert the next item directly after the current one so the list stays
        // grouped (…3, 4, then "Notes Here"). Do NOT route through
        // resolveInsertPosition, which would push the new item below a filled
        // trailing notes block and split the numbered list.
        val insertPosition = afterBlock.position + 1
        shiftIfNeededForInsert(insertPosition)
        val groupId = NumberListMetadata.fromBlock(afterBlock).listGroupId
        val blockId = viewModel.insertNumberListItems(
            items = listOf(""),
            atPosition = insertPosition,
            startOrder = nextOrder,
            listGroupId = groupId
        ) ?: run {
            richTextToolbarBinder.suppressEditorBlurHandling = false
            return
        }
        ensureNotesHereBelowNumberList()
        numberListViewModel.enableMode(nextOrder)
        numberListViewModel.setLastAddedBlockId(blockId)
        numberListViewModel.advanceCurrentNumber()
        contentAdapter.setPendingFocusNumberListId(blockId)
        syncContentList()
        binding.rvBlocks.post {
            richTextToolbarBinder.suppressEditorBlurHandling = false
        }
    }

    private fun handleNumberListFromRichText(controller: RichTextEditorController) {
        prepareChecklistInsert()
        hideRichTextToolbar()
        if (isTrailingNotesFilled()) {
            val selectedText = controller.getSelectedPlainText()
            if (selectedText.isNotBlank()) {
                convertSelectionToNumberList(controller, selectedText)
            } else {
                bulletListViewModel.disableMode()
                numberListViewModel.disableMode()
                insertNumberListFromChecklist()
            }
            richTextToolbarBinder.refreshToolbarState()
            return
        }
        if (numberListViewModel.uiState.value.isNumberModeEnabled) {
            numberListViewModel.disableMode()
            richTextToolbarBinder.refreshToolbarState()
            return
        }
        if (bulletListViewModel.uiState.value.isBulletModeEnabled) {
            bulletListViewModel.disableMode()
        }

        val selectedText = controller.getSelectedPlainText()
        if (selectedText.isNotBlank()) {
            convertSelectionToNumberList(controller, selectedText)
            return
        }

        startNumberListMode()
    }

    private fun convertSelectionToNumberList(
        controller: RichTextEditorController,
        selectedText: String
    ) {
        val items = numberListViewModel.splitSelectionToItems(selectedText)
        if (items.isEmpty()) {
            startNumberListMode()
            return
        }

        prepareChecklistInsert()
        controller.removeSelectedText()
        controller.flushContent()

        val anchorBlockId = controller.blockId
        val blocks = viewModel.uiState.value.blocks
        val anchorBlock = anchorBlockId?.let { id -> blocks.find { it.id == id } }
        val insertPosition = resolveInsertPosition(
            (anchorBlock?.position ?: nextContentPosition()) + 1
        )
        val groupId = numberListViewModel.createListGroupId()
        shiftIfNeededForInsert(insertPosition)

        val lastBlockId = viewModel.insertNumberListItems(
            items = items,
            atPosition = insertPosition,
            startOrder = 1,
            listGroupId = groupId
        ) ?: return

        ensureNotesHereBelowNumberList()

        numberListViewModel.enableMode(items.size + 1)
        numberListViewModel.setLastAddedBlockId(lastBlockId)
        contentAdapter.setPendingFocusNumberListId(lastBlockId)
        syncContentList()
        richTextToolbarBinder.refreshToolbarState()
    }

    private fun shouldMoveFromTitle(actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_NEXT ||
            actionId == EditorInfo.IME_ACTION_DONE ||
            actionId == EditorInfo.IME_ACTION_GO
        ) {
            return true
        }
        return event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN
    }

    private fun moveFromTitleToTextBlock() {
        if (isMovingFromTitle) return
        isMovingFromTitle = true

        val first = findFirstFocusableContentItem()
        when (first) {
            is NoteContentItem.BlockItem -> when (first.block.type) {
                BlockType.TEXT -> {
                    contentAdapter.setPendingFocusBlockId(first.block.id)
                    syncContentList(focusBlockId = first.block.id)
                }
                BlockType.NUMBER_LIST -> {
                    contentAdapter.setPendingFocusNumberListId(first.block.id)
                    syncContentList(pendingNumberListId = first.block.id)
                }
                BlockType.BULLET_LIST -> {
                    contentAdapter.setPendingFocusBulletListId(first.block.id)
                    syncContentList(pendingBulletListId = first.block.id)
                }
                else -> focusNewlyAddedTitleBodyBlock()
            }
            is NoteContentItem.TodoEntry -> {
                syncContentList(pendingTodoId = first.item.id)
            }
            else -> focusNewlyAddedTitleBodyBlock()
        }
    }

    private fun focusNewlyAddedTitleBodyBlock() {
        shiftIfNeededForInsert(0)
        val focusId = viewModel.addNotesTextBlock(0)
        contentAdapter.setPendingFocusBlockId(focusId)
        syncContentList(focusBlockId = focusId)
    }

    private fun findFirstFocusableContentItem(): NoteContentItem? {
        val merged = NoteContentItem.merge(
            viewModel.uiState.value.blocks,
            todoViewModel.uiState.value.items
        )
        return merged.firstOrNull { item ->
            when (item) {
                is NoteContentItem.TodoEntry -> true
                is NoteContentItem.BlockItem -> when (item.block.type) {
                    BlockType.TEXT,
                    BlockType.NUMBER_LIST,
                    BlockType.BULLET_LIST -> true
                    else -> false
                }
                else -> false
            }
        }
    }

    private fun finishMoveFromTitle() {
        binding.etTitle.clearFocus()
        isMovingFromTitle = false
    }

    private fun initTodoIfNeeded() {
        if (todoInitialized) return
        val state = viewModel.uiState.value
        if (state.isLoading) return
        todoInitialized = true
        todoViewModel.initForNote(
            state.noteId,
            JsonUtils.blockContentPlainText(state.title),
            state.blocks
        )
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(viewModel.uiState, lockRevealTick) { state, _ -> state }.collect { state ->
                    if (!state.isLoading) {
                        ensureLockAuthenticated(state.isLocked, state.noteId)
                    }
                    if (!isEditorRevealAllowed &&
                        !isLockAccessGranted(state.isLocked) &&
                        !state.isLoading
                    ) {
                        return@collect
                    }

                    if (!binding.etTitle.isFocused && state.title != lastAppliedTitleJson) {
                        isLoadingTitleDocument = true
                        lastAppliedTitleJson = state.title
                        loadTitleDocument(state.title)
                        isLoadingTitleDocument = false
                        applyTitleSearchHighlight()
                    }

                    initTodoIfNeeded()
                    ensureDefaultTextBlockAndFocus()
                    numberListViewModel.syncListGroupCounter(state.blocks)
                    bulletListViewModel.syncListGroupCounter(state.blocks)

                    val header = binding.noteHeader
                    updateHeaderTimestamp()

//                    header.btnBookmark.isChecked = state.isPinned

                    header.btnBookmark.isSelected = state.isFavorite
                    header.btnBookmark.contentDescription = getString(
                        if (state.isFavorite) R.string.remove_bookmark else R.string.bookmark_note
                    )

                    header.ivCategoryIcon.setImageResource(
                        CategoryIconResolver.resolve(state.categoryIcon, state.categoryName)
                    )
                    header.tvCategoryName.text = state.categoryName.ifBlank {
                        getString(R.string.category)
                    }
                    header.btnCategorySelector.contentDescription = state.categoryName.ifBlank {
                        getString(R.string.select_category)
                    }

                    applyNoteAppearance(state.color)
                    // Re-apply when appearance is unchanged (early return) so icons still track bg ink.
                    val ink = resolveHeaderInkColor()
                    applyBookmarkIconColor(isFavorite = state.isFavorite, ink = ink)
                    header.ivCategoryIcon.setColorFilter(
                        ContextCompat.getColor(this@CreateNotesActivity, R.color.app_txt_black)
                    )

                    applyNoteListItemHeight(state.listItemHeight)

                    applyReadingMode(state.isReadingMode)

                    if (state.isDeleted) {
                        finish()
                        return@collect
                    }

                    if (state.isArchivedActionDone) {
                        val messageRes = if (state.isArchived) {
                            R.string.note_archived
                        } else {
                            R.string.note_unarchived
                        }
                        Toast.makeText(this@CreateNotesActivity, messageRes, Toast.LENGTH_SHORT).show()
                        finish()
                        return@collect
                    }

                    if (state.isSaved) {
                        val savedNoteId = state.noteId
                        if (savedNoteId != null && savedNoteId > 0) {
                            todoViewModel.linkToNote(savedNoteId)
                        }
                        if (!isAutoSavingOnExit) {
                            Toast.makeText(this@CreateNotesActivity, R.string.note_saved, Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            finishWithCreateNotesBackInterAd()
                        }
                    }
                }
            }
        }
    }

    private fun observeContent() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    viewModel.uiState,
                    todoViewModel.uiState,
                    lockRevealTick
                ) { noteState, todoState, _ ->
                    noteState to todoState
                }.collect { (noteState, todoState) ->
                    if (!isEditorRevealAllowed &&
                        !isLockAccessGranted(noteState.isLocked) &&
                        !noteState.isLoading
                    ) {
                        return@collect
                    }
                    val blocks = noteState.blocks
                    syncContentList(
                        blocks = blocks,
                        todoState = todoState,
                        pendingTodoId = todoState.lastAddedItemId ?: todoState.focusItemId,
                        pendingNumberListId = numberListViewModel.uiState.value.lastAddedBlockId,
                        pendingBulletListId = bulletListViewModel.uiState.value.lastAddedBlockId
                    )
                }
            }
        }
    }

    private fun syncContentList(
        blocks: List<com.notebook.notes.checklist.model.NoteBlock> = viewModel.uiState.value.blocks,
        todoState: com.notebook.notes.checklist.viewmodel.TodoUiState = todoViewModel.uiState.value,
        focusBlockId: Long? = null,
        pendingTodoId: Long? = null,
        pendingNumberListId: Long? = null,
        pendingBulletListId: Long? = null
    ) {
        val merged = NoteContentItem.merge(blocks, todoState.items)
        val numberOrders = NumberListHelper.computeDisplayOrders(blocks)
        val bulletPositions = BulletListHelper.computeDisplayPositions(blocks)
        val signature = merged.filterNot { it is NoteContentItem.AddTodoFooter }.joinToString("|") { item ->
            when (item) {
                is NoteContentItem.BlockItem -> {
                    val orderKey = when (item.block.type) {
                        BlockType.NUMBER_LIST -> numberOrders[item.block.id] ?: 0
                        BlockType.BULLET_LIST -> bulletPositions[item.block.id] ?: 0
                        else -> 0
                    }
                    // Live rich-text editors own TEXT/list content; including it caused
                    // submitList + rebind after every formatting flush and broke IME focus.
                    val contentKey = when (item.block.type) {
                        BlockType.TEXT,
                        BlockType.BULLET_LIST,
                        BlockType.NUMBER_LIST -> ""
                        else -> item.block.content
                    }
                    "b:${item.block.id}:${item.block.type}:${item.block.position}:${orderKey}:${contentKey}:${item.block.extraData}"
                }
                is NoteContentItem.MediaRow -> item.blocks.joinToString(",") { block ->
                    "m:${block.id}:${block.type}:${block.position}:${block.content}:${block.extraData}"
                }
                is NoteContentItem.TodoEntry ->
                    "t:${item.item.id}:${item.item.isCompleted}:${item.item.position}"
                is NoteContentItem.AddTodoFooter -> ""
            }
        }

        fun focusPending() {
            when {
                focusBlockId != null -> {
                    contentAdapter.focusTextBlock(binding.rvBlocks, focusBlockId) {
                        finishMoveFromTitle()
                    }
                }
                pendingTodoId != null -> {
                    // Skip if user is already editing this todo — re-focus jumps caret to end.
                    val alreadyEditing = lastBodyEditor?.hasFocus() == true &&
                        richTextToolbarBinder.getActiveController()?.blockId == pendingTodoId
                    if (!alreadyEditing) {
                        contentAdapter.focusTodoItem(binding.rvBlocks, pendingTodoId)
                    }
                    if (isMovingFromTitle) finishMoveFromTitle()
                    todoViewModel.clearLastAddedItemId()
                }
                pendingNumberListId != null -> {
                    // Skip if user is already editing this item — re-focus jumps caret to end
                    // and can steal focus mid-typing.
                    val alreadyEditing = lastBodyEditor?.hasFocus() == true &&
                        richTextToolbarBinder.getActiveController()?.blockId == pendingNumberListId
                    if (!alreadyEditing) {
                        contentAdapter.focusNumberListBlock(binding.rvBlocks, pendingNumberListId)
                    }
                    if (isMovingFromTitle) finishMoveFromTitle()
                    numberListViewModel.clearLastAddedBlockId()
                }
                pendingBulletListId != null -> {
                    val alreadyEditing = lastBodyEditor?.hasFocus() == true &&
                        richTextToolbarBinder.getActiveController()?.blockId == pendingBulletListId
                    if (!alreadyEditing) {
                        contentAdapter.focusBulletListBlock(binding.rvBlocks, pendingBulletListId)
                    }
                    if (isMovingFromTitle) finishMoveFromTitle()
                    bulletListViewModel.clearLastAddedBlockId()
                }
            }
        }

        if (signature != lastContentSignature) {
            lastContentSignature = signature
            contentAdapter.submitList(merged) { focusPending() }
        } else if (focusBlockId != null ||
            pendingTodoId != null ||
            pendingNumberListId != null ||
            pendingBulletListId != null
        ) {
            // Only run focus side-effects when something actually requested focus.
            focusPending()
        }
        updateEmptyState(merged.isEmpty())
    }

    private var appliedReadingMode: Boolean? = null

    private fun applyReadingMode(enabled: Boolean) {
        binding.etTitle.isFocusable = !enabled
        binding.etTitle.isFocusableInTouchMode = !enabled
        binding.etTitle.isCursorVisible = !enabled

        if (::contentAdapter.isInitialized) {
            contentAdapter.setReadingMode(enabled)
        }

        binding.noteHeader.btnAttachment.isEnabled = !enabled
        binding.noteHeader.btnAttachment.alpha = if (enabled) 0.5f else 1.0f
        binding.noteHeader.btnCategorySelector.isEnabled = !enabled
        binding.noteHeader.btnCategorySelector.alpha = if (enabled) 0.5f else 1.0f

        // Only change chrome when the mode actually flips. Re-applying INVISIBLE on every
        // uiState emission (e.g. adding the next numbered item) briefly hides the
        // rich-text toolbar until focus lands on the new row.
        if (appliedReadingMode == enabled) return
        appliedReadingMode = enabled

        if (enabled) {
            binding.etTitle.clearFocus()
            hideRichTextToolbar()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.hideSoftInputFromWindow(binding.root.windowToken, 0)
            binding.richTextToolbar.root.visibility = View.INVISIBLE
        }
        // Leaving reading mode: leave toolbar visibility to RichTextToolbarBinder / focus.
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyContentHint.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun flushAllEditors() {
        if (::titleController.isInitialized) {
            titleController.flushContent()
        }
        contentAdapter.flushAllTextBlocks()
    }

    private fun autoSaveAndExit() {
        if (isExiting) return
        isExiting = true
        isAutoSavingOnExit = true
        saveNoteWithVoiceCleanup()
    }

    private fun saveNoteWithVoiceCleanup() {
        voiceNoteViewModel.stopPlayback()
        attachmentViewModel.stopPlayback()
        if (voiceNoteViewModel.uiState.value.isRecording) {
            pendingSaveAfterRecording = true
            voiceNoteViewModel.stopRecording()
            return
        }
        flushAllEditors()
        viewModel.saveNote()
    }

    override fun onResume() {
        super.onResume()
        DateTimeUtils.refresh(this)
        updateHeaderTimestamp()
        scheduleAdReload()
        // Clear lock overlays immediately after Premium purchase from Paywall.
        if (noteBackgroundPanelRef?.isVisible == true) {
            noteBackgroundPanelRef?.refreshAccessState()
        }
    }

    override fun onPause() {
        super.onPause()
        adReloadHandler.removeCallbacks(adReloadRunnable)
        voiceNoteViewModel.pausePlayback()
        attachmentViewModel.pausePlayback()
    }

    private fun updateHeaderTimestamp() {
        if (!::binding.isInitialized) return
        val state = viewModel.uiState.value
        val displayTimestamp = when {
            state.updatedAt > 0L -> state.updatedAt
            state.createdAt > 0L -> state.createdAt
            else -> System.currentTimeMillis()
        }
        binding.noteHeader.tvNoteDate.text = NoteDateFormatter.formatHeaderDate(displayTimestamp)
    }

    override fun onDestroy() {
        adReloadHandler.removeCallbacks(adReloadRunnable)
        if (::binding.isInitialized) {
            binding.rvBlocks.removeCallbacks(scrollEditorRunnable)
        }
        NoteColorUtils.defaultTextColorOverrideHex = null
        super.onDestroy()
    }

    private fun copyAndAddBlock(uri: Uri, type: String) {
        val path = viewModel.getFileStorageManager().copyUriToStorage(uri, type)
        when (type) {
            BlockType.IMAGE -> {
                val position = prepareSmartContentInsert()
                viewModel.addImageBlock(path, position)
                completeSmartContentInsert(position)
            }
        }
    }

    private fun showAddLinkDialog() {
        val dialogBinding = DialogAddLinkBinding.inflate(layoutInflater)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnAdd.setOnClickListener {
            val url = dialogBinding.etLinkUrl.text?.toString()?.trim().orEmpty()
            if (url.isNotEmpty()) {
                val position = prepareSmartContentInsert()
                viewModel.addLinkBlock(
                    if (url.startsWith("http")) url else "https://$url",
                    position
                )
                completeSmartContentInsert(position)
                dialog.dismiss()
            }
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        SystemBars.hideNavigationBar(dialog)
        dialog.setOnDismissListener {
            SystemBars.hideNavigationBar(this)
        }
    }

    companion object {
        private const val TITLE_MAX_LENGTH = 50
        private const val TITLE_DEFAULT_FONT_SIZE = 24
    }
}
