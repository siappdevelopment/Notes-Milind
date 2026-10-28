package com.notebook.notes.checklist.ui.activity

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.notebook.notes.checklist.Advertisement.ADSBannerSmall
import com.notebook.notes.checklist.Advertisement.ADSConsentManager
import com.notebook.notes.checklist.Advertisement.ADSMainClass
import com.notebook.notes.checklist.Advertisement.ADSNativeDisplay
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.ActivityRecycleBinBinding
import com.notebook.notes.checklist.databinding.PopupRecycleBinToolbarBinding
import com.notebook.notes.checklist.ui.home.NotesSelectionDialogs
import com.notebook.notes.checklist.model.Note
import com.notebook.notes.checklist.ui.card.NoteCardAdapter
import com.notebook.notes.checklist.util.DateTimeUtils
import com.notebook.notes.checklist.viewmodel.NoteEditViewModel
import com.notebook.notes.checklist.viewmodel.RecycleBinViewModel
import com.notebook.notes.checklist.viewmodel.utils.finishWithRecycleBinBackInterAd
import com.notebook.notes.checklist.viewmodel.utils.launchActivity
import com.notebook.notes.checklist.viewmodel.utils.openWithRecycleBinInterAd
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RecycleBinActivity : BaseActivity() {

    private lateinit var binding: ActivityRecycleBinBinding
    private val viewModel: RecycleBinViewModel by viewModels()
    private lateinit var noteCardAdapter: NoteCardAdapter
    private lateinit var notesLayoutManager: RecyclerView.LayoutManager
    private var isGridView = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecycleBinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        DateTimeUtils.refresh(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupBottomBar()
        setupBackPressHandler()
        observeNotes()
        loadOtherScreenAds()
    }

    private fun loadOtherScreenAds() {
        if (!ADSMainClass.getOtherAdsShow()) {
            hideOtherScreenAdPlaceholders()
            return
        }

        if (!ADSConsentManager.canRequestAds(this) || ADSMainClass.getAds_Free()) {
            hideOtherScreenAdPlaceholders()
            return
        }
        if (ADSMainClass.getOtherAdsType().equals("native")) {
            findViewById<View>(R.id.flBannerSmallPlaceholder).visibility = View.GONE
            ADSNativeDisplay.loadAdmobNativeAdBig(
                ADSMainClass.getStringValue(ADSMainClass.OTHER_SCREEN_NATIVE),
                findViewById(R.id.flNativeSmallPlaceholder),
                findViewById(R.id.shimmer_container_banner),
                "small",
                this
            )
        } else {
            // The card only ever hosts the native ad; in banner mode its vertical
            // margins would remain as an empty white strip.
            ADSBannerSmall.loadAdMobBanner(
                ADSMainClass.getStringValue(ADSMainClass.OTHER_SCREEN_BANNER),
                findViewById(R.id.flBannerSmallPlaceholder),
                findViewById(R.id.shimmer_container_banner),
                this
            )
        }
    }

    /**
     * The ad loader hides the shimmer and the native slot when a load fails, but leaves the
     * card behind, so its margins show as a white strip. Collapse it once the load resolves.
     */


    private fun hideOtherScreenAdPlaceholders() {
        findViewById<View>(R.id.llOtherScreenAds).visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        DateTimeUtils.refresh(this)
        if (::noteCardAdapter.isInitialized) {
            noteCardAdapter.refreshTimeFormatIfNeeded()
        }
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finishWithRecycleBinBackInterAd() }
        binding.btnSelectAll.setOnClickListener {
            val noteIds = viewModel.uiState.value.noteCards.map { it.noteId }
            if (noteCardAdapter.isAllSelected(noteIds)) {
                noteCardAdapter.deselectAll()
            } else {
                noteCardAdapter.selectAll(noteIds)
            }
            updateSelectAllButton(noteIds)
        }
        binding.btnMore.setOnClickListener { showToolbarMenu() }
    }

    private fun showToolbarMenu() {
        val menuBinding = PopupRecycleBinToolbarBinding.inflate(layoutInflater)
        val hasNotes = !viewModel.uiState.value.isEmpty
        val popup = PopupWindow(
            menuBinding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 16f
            setBackgroundDrawable(ContextCompat.getDrawable(this@RecycleBinActivity, android.R.color.transparent))
        }

        setToolbarMenuOptionEnabled(menuBinding.optionRestoreAll, hasNotes)
        setToolbarMenuOptionEnabled(menuBinding.optionEmptyTrash, hasNotes)

        menuBinding.optionRestoreAll.setOnClickListener {
            if (!hasNotes) return@setOnClickListener
            popup.dismiss()
            val allNotes = viewModel.uiState.value.noteCards.map { it.note }
            showConfirmationDialog(
                titleRes = R.string.restore_all_title,
                messageRes = R.string.restore_all_message,
                positiveButtonRes = R.string.restore_all,
                onPositiveClick = {
                    viewModel.restoreNotes(allNotes)
                    Toast.makeText(this, R.string.note_restored, Toast.LENGTH_SHORT).show()
                }
            )
        }

        menuBinding.optionEmptyTrash.setOnClickListener {
            if (!hasNotes) return@setOnClickListener
            popup.dismiss()
            confirmDeleteAll()
        }

        menuBinding.root.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val density = resources.displayMetrics.density
        val xOffset = binding.btnMore.width - menuBinding.root.measuredWidth - (8 * density).toInt()
        popup.showAsDropDown(binding.btnMore, xOffset, 0)
    }

    private fun setToolbarMenuOptionEnabled(option: View, enabled: Boolean) {
        option.isEnabled = enabled
        option.isClickable = enabled
        option.alpha = if (enabled) 1f else 0.4f
    }

    private fun setupRecyclerView() {
        noteCardAdapter = NoteCardAdapter(
            onNoteClick = { note -> openNote(note) },
            onPinClick = { },
            onFavoriteClick = { },
            onDeleteClick = { },
            cardMenuMode = NoteCardAdapter.CardMenuMode.RECYCLE_BIN,
            onRestoreClick = { note -> confirmRestore(note) },
            onPermanentDeleteClick = { note -> confirmPermanentDelete(note) }
        )
        applyNotesLayoutManager()
        binding.rvNotes.apply {
            layoutManager = notesLayoutManager
            adapter = noteCardAdapter
            setHasFixedSize(false)
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }
        noteCardAdapter.onSelectionModeChanged = { inSelectionMode, selectedCount ->
            binding.bottomBar.isVisible = inSelectionMode && selectedCount > 0
            binding.btnSelectAll.isVisible = inSelectionMode
            binding.btnMore.isVisible = !inSelectionMode

            if (inSelectionMode) {
                binding.tvTitle.text = getString(R.string.notes_selected, selectedCount)
            } else {
                binding.tvTitle.text = getString(R.string.trash_bin)
            }

            updateSelectAllButton(viewModel.uiState.value.noteCards.map { it.noteId })
        }
    }

    private fun applyNotesLayoutManager() {
        notesLayoutManager = NoteCardAdapter.createLayoutManager(
            context = this,
            isGridView = isGridView,
            adapter = noteCardAdapter
        )
        binding.rvNotes.layoutManager = notesLayoutManager
    }

    private fun setupSearch() {
        binding.etSearch.doAfterTextChanged { editable ->
            viewModel.setSearchQuery(editable?.toString().orEmpty())
        }
    }

    private fun observeNotes() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    noteCardAdapter.setSearchQuery(state.searchQuery)
                    noteCardAdapter.submitDeletedNotes(state.noteCards)
                    val isEmpty = state.isEmpty
                    binding.emptyState.isVisible = isEmpty
                    binding.rvNotes.isVisible = !isEmpty
                    binding.btnSelectAll.isVisible = noteCardAdapter.isInSelectionMode() && !isEmpty
                    binding.btnMore.isEnabled = !isEmpty
                    binding.btnMore.alpha = if (isEmpty) 0.4f else 1f
                    updateSelectAllButton(state.noteCards.map { it.noteId })
                }
            }
        }
    }

    private fun updateSelectAllButton(noteIds: List<Long>) {
        if (!::noteCardAdapter.isInitialized) return
        binding.btnSelectAll.setText(
            if (noteCardAdapter.isAllSelected(noteIds)) R.string.deselect_all else R.string.select_all
        )
    }

    private fun getSelectedNotes(): List<Note> {
        val selectedIds = noteCardAdapter.getSelectedNoteIds()
        return viewModel.uiState.value.noteCards
            .filter { it.noteId in selectedIds }
            .map { it.note }
    }

    private fun openNote(note: Note) {
//        openWithRecycleBinInterAd {
//            launchActivity<CreateNotesActivity> {
//                putExtra(NoteEditViewModel.EXTRA_NOTE_ID, note.id)
//            }
//        }
    }

    private fun confirmRestore(note: Note) {
        showConfirmationDialog(
            titleRes = R.string.restore_note_title,
            messageRes = R.string.restore_note_message,
            positiveButtonRes = R.string.restore_note,
            onPositiveClick = {
                viewModel.restoreNote(note)
                Toast.makeText(this, R.string.note_restored, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun confirmPermanentDelete(note: Note) {
        showConfirmationDialog(
            titleRes = R.string.delete_permanently_title,
            messageRes = R.string.delete_permanently_message,
            positiveButtonRes = R.string.delete_permanently,
            iconRes = R.drawable.ic_delete,
            onPositiveClick = {
                viewModel.permanentlyDeleteNote(note)
                Toast.makeText(this, R.string.note_deleted_permanently, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun confirmDeleteAll() {
        showConfirmationDialog(
            titleRes = R.string.delete_all_notes_title,
            messageRes = R.string.delete_all_notes_message,
            positiveButtonRes = R.string.empty_trash,
            iconRes = R.drawable.ic_delete,
            onPositiveClick = {
                viewModel.permanentlyDeleteAll()
                Toast.makeText(this, R.string.recycle_bin_emptied, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun showConfirmationDialog(
        titleRes: Int,
        messageRes: Int,
        positiveButtonRes: Int,
        onPositiveClick: () -> Unit,
        iconRes: Int = R.drawable.ic_recylerbin,
        positiveButtonColorRes: Int = R.color.app_primary_color
    ) {
        NotesSelectionDialogs.showConfirmation(
            context = this,
            layoutInflater = layoutInflater,
            titleRes = titleRes,
            messageRes = messageRes,
            positiveButtonRes = positiveButtonRes,
            iconRes = iconRes,
            positiveButtonColorRes = if (positiveButtonColorRes == R.color.app_primary_color) null else positiveButtonColorRes,
            onPositiveClick = onPositiveClick
        )
    }

    private fun setupBottomBar() {
        binding.btnRestoreSelected.setOnClickListener {
            val notes = getSelectedNotes()
            if (notes.isEmpty()) return@setOnClickListener
            showConfirmationDialog(
                titleRes = R.string.restore_note_title,
                messageRes = R.string.restore_note_message,
                positiveButtonRes = R.string.restore_note,
                onPositiveClick = {
                    viewModel.restoreNotes(notes)
                    noteCardAdapter.exitSelectionMode()
                    Toast.makeText(this, R.string.note_restored, Toast.LENGTH_SHORT).show()
                }
            )
        }
        binding.btnDeleteSelected.setOnClickListener {
            val notes = getSelectedNotes()
            if (notes.isEmpty()) return@setOnClickListener
            showConfirmationDialog(
                titleRes = R.string.delete_permanently_selected_title,
                messageRes = R.string.delete_permanently_selected_message,
                positiveButtonRes = R.string.delete_permanently,
                iconRes = R.drawable.ic_delete,
                onPositiveClick = {
                    viewModel.permanentlyDeleteNotes(notes)
                    noteCardAdapter.exitSelectionMode()
                    Toast.makeText(this, R.string.note_deleted_permanently, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (::noteCardAdapter.isInitialized && noteCardAdapter.isInSelectionMode()) {
                    noteCardAdapter.exitSelectionMode()
                    return
                }
                finishWithRecycleBinBackInterAd()
            }
        })
    }
}
