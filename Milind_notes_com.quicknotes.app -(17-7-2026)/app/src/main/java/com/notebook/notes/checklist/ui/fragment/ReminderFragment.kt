package com.notebook.notes.checklist.ui.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.FragmentReminderBinding
import com.notebook.notes.checklist.helper.NotePasswordManager
import com.notebook.notes.checklist.iap.PremiumFeature
import com.notebook.notes.checklist.iap.PremiumFeatureGate
import com.notebook.notes.checklist.model.Note
import com.notebook.notes.checklist.ui.activity.CreateNotesActivity
import com.notebook.notes.checklist.ui.activity.HomeActivity
import com.notebook.notes.checklist.ui.activity.HomeTopBarCallback
import com.notebook.notes.checklist.ui.activity.HomeTopBarConfiguration
import com.notebook.notes.checklist.ui.activity.NoteSelectionCallback
import com.notebook.notes.checklist.ui.activity.SetPasswordActivity
import com.notebook.notes.checklist.ui.card.NoteCardAdapter
import com.notebook.notes.checklist.sort.NoteSortOrder
import com.notebook.notes.checklist.ui.home.NotesSelectionDialogs
import com.notebook.notes.checklist.repository.NoteRepository
import com.notebook.notes.checklist.repository.TodoRepository
import com.notebook.notes.checklist.util.DateTimeUtils
import com.notebook.notes.checklist.util.NoteShareTextFormatter
import com.notebook.notes.checklist.utils.JsonUtils
import com.notebook.notes.checklist.viewmodel.NoteEditViewModel
import com.notebook.notes.checklist.viewmodel.ReminderViewModel
import com.notebook.notes.checklist.viewmodel.utils.launchActivity
import com.notebook.notes.checklist.viewmodel.utils.openWithHomeInterAd
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderFragment : Fragment(), HomeTopBarCallback, NoteSelectionCallback {

    private var _binding: FragmentReminderBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReminderViewModel by viewModels()

    @Inject lateinit var notePasswordManager: NotePasswordManager
    @Inject lateinit var noteRepository: NoteRepository
    @Inject lateinit var todoRepository: TodoRepository

    private lateinit var noteCardAdapter: NoteCardAdapter
    private lateinit var notesLayoutManager: RecyclerView.LayoutManager
    private var pendingUnlockNoteId: Long = -1L
    private var pendingLockNoteIds: List<Long> = emptyList()
    private var pendingClearLockNoteIds: List<Long> = emptyList()
    private var isGridView = true

    private val unlockNoteLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                pendingUnlockNoteId = -1L
                return@registerForActivityResult
            }
            val noteId = result.data?.getLongExtra(SetPasswordActivity.EXTRA_NOTE_ID, -1L)
                ?.takeIf { it > 0 }
                ?: pendingUnlockNoteId
            pendingUnlockNoteId = -1L
            if (noteId > 0) {
                openWithHomeInterAd {
                    requireActivity().launchActivity<CreateNotesActivity> {
                        putExtra(NoteEditViewModel.EXTRA_NOTE_ID, noteId)
                        putExtra(NoteEditViewModel.EXTRA_LOCK_AUTHENTICATED, true)
                    }
                }
            }
        }

    private val setPasswordForLockLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val noteIds = pendingLockNoteIds
            pendingLockNoteIds = emptyList()
            if (result.resultCode != Activity.RESULT_OK || noteIds.isEmpty()) return@registerForActivityResult
            val alreadyLockedId = result.data
                ?.getLongExtra(SetPasswordActivity.EXTRA_NOTE_ID, -1L)
                ?.takeIf { it > 0 }
            val remainingIds = if (alreadyLockedId != null) {
                noteIds.filter { it != alreadyLockedId }
            } else {
                noteIds
            }
            if (remainingIds.isNotEmpty()) {
                viewModel.setNotesLocked(remainingIds, isLocked = true)
            }
            Toast.makeText(requireContext(), R.string.note_locked, Toast.LENGTH_SHORT).show()
            if (::noteCardAdapter.isInitialized && noteCardAdapter.isInSelectionMode()) {
                noteCardAdapter.exitSelectionMode()
            }
        }

    private val verifyClearLockLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val noteIds = pendingClearLockNoteIds
            pendingClearLockNoteIds = emptyList()
            if (result.resultCode != Activity.RESULT_OK || noteIds.isEmpty()) return@registerForActivityResult
            viewModel.setNotesLocked(noteIds, isLocked = false)
            Toast.makeText(requireContext(), R.string.note_unlocked, Toast.LENGTH_SHORT).show()
            if (::noteCardAdapter.isInitialized && noteCardAdapter.isInSelectionMode()) {
                noteCardAdapter.exitSelectionMode()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReminderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeNotes()
        (activity as? HomeActivity)?.onFragmentTopBarReady()
    }

    override fun onResume() {
        super.onResume()
        DateTimeUtils.refresh(requireContext())
        refreshTimeFormat()
        syncListUiFromViewModel()
    }

    override fun getTopBarConfiguration() = HomeTopBarConfiguration(
        titleRes = R.string.nav_reminders,
        showAutoSavedBadge = false
    )

    override fun syncTopBarNotesCount() {
        updateNotesCount(viewModel.uiState.value.noteCount)
    }

    override fun onTopBarSearchQuery(query: String) {
        viewModel.setSearchQuery(query)
    }

    override fun isNotesGridView(): Boolean = isGridView

    override fun toggleNotesViewMode() {
        toggleViewMode()
    }

    fun refreshTimeFormat() {
        if (::noteCardAdapter.isInitialized) {
            noteCardAdapter.refreshTimeFormatIfNeeded()
        }
    }

    private fun toggleViewMode() {
        isGridView = !isGridView
        TransitionManager.beginDelayedTransition(binding.rvNotes, AutoTransition().apply {
            duration = 300
        })
        applyNotesLayoutManager()
    }

    private fun applyNotesLayoutManager() {
        notesLayoutManager = NoteCardAdapter.createLayoutManager(
            context = requireContext(),
            isGridView = isGridView,
            adapter = noteCardAdapter
        )
        binding.rvNotes.layoutManager = notesLayoutManager
    }

    private fun setupRecyclerView() {
        noteCardAdapter = NoteCardAdapter(
            onNoteClick = { note -> openNote(note) },
            onPinClick = { note ->
                val wasPinned = note.isPinned
                viewModel.togglePin(note)
                Toast.makeText(
                    requireContext(),
                    if (wasPinned) R.string.note_unpinned else R.string.note_pinned,
                    Toast.LENGTH_SHORT
                ).show()
            },
            onFavoriteClick = { note ->
                val wasFavorite = note.isFavorite
                viewModel.toggleFavorite(note)
                Toast.makeText(
                    requireContext(),
                    if (wasFavorite) R.string.note_unbookmarked else R.string.note_bookmarked,
                    Toast.LENGTH_SHORT
                ).show()
            },
            onDeleteClick = { note -> confirmDeleteNote(note) },
            onShareClick = { note -> shareNote(note) },
            onArchiveClick = { note ->
                viewModel.toggleArchive(note)
                Toast.makeText(requireContext(), R.string.note_archived, Toast.LENGTH_SHORT).show()
            }
        )
        applyNotesLayoutManager()
        binding.rvNotes.apply {
            layoutManager = notesLayoutManager
            adapter = noteCardAdapter
            setHasFixedSize(false)
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }
        noteCardAdapter.onDragReorderFinished = { orderedCards ->
            viewModel.reorderNotes(orderedCards)
        }
        noteCardAdapter.onSelectionModeChanged = { inSelectionMode, selectedCount ->
            (activity as? HomeActivity)?.onNoteSelectionModeChanged(inSelectionMode, selectedCount)
        }
        noteCardAdapter.attachDragHelper(binding.rvNotes)
    }

    override fun exitSelectionModeIfActive(): Boolean {
        if (!::noteCardAdapter.isInitialized || !noteCardAdapter.isInSelectionMode()) return false
        noteCardAdapter.exitSelectionMode()
        return true
    }

    override fun exitSelectionMode() {
        if (::noteCardAdapter.isInitialized) {
            noteCardAdapter.exitSelectionMode()
        }
    }

    override fun selectAllNotes() {
        if (!::noteCardAdapter.isInitialized) return
        val noteIds = viewModel.uiState.value.noteCards.map { it.noteId }
        if (noteCardAdapter.isAllSelected(noteIds)) {
            noteCardAdapter.exitSelectionMode()
        } else {
            noteCardAdapter.selectAll(noteIds)
        }
    }

    override fun enterSelectionSelectingAll() {
        if (!::noteCardAdapter.isInitialized) return
        val noteIds = viewModel.uiState.value.noteCards.map { it.noteId }
        if (noteIds.isEmpty()) return
        noteCardAdapter.selectAll(noteIds)
    }

    override fun handleBulkArchive() {
        val selectedNotes = getSelectedNotes()
        if (selectedNotes.isEmpty()) return
        val allArchived = selectedNotes.all { it.isArchived }
        viewModel.archiveNotes(selectedNotes)
        Toast.makeText(
            requireContext(),
            if (allArchived) R.string.note_unarchived else R.string.note_archived,
            Toast.LENGTH_SHORT
        ).show()
        noteCardAdapter.exitSelectionMode()
    }

    override fun isAllNotesSelected(): Boolean {
        if (!::noteCardAdapter.isInitialized) return false
        val noteIds = viewModel.uiState.value.noteCards.map { it.noteId }
        return noteCardAdapter.isAllSelected(noteIds)
    }

    override fun areAllSelectedNotesPinned(): Boolean {
        val selected = getSelectedNotes()
        return selected.isNotEmpty() && selected.all { it.isPinned }
    }

    override fun areAllSelectedNotesLocked(): Boolean {
        val selected = getSelectedNotes()
        return selected.isNotEmpty() && selected.all { it.isLocked }
    }

    override fun areAllSelectedNotesArchived(): Boolean {
        val selected = getSelectedNotes()
        return selected.isNotEmpty() && selected.all { it.isArchived }
    }

    override fun showsSelectionReminderAction(): Boolean = false

    override fun handleBulkLock() {
        val selectedNotes = getSelectedNotes()
        if (selectedNotes.isEmpty()) return
        if (selectedNotes.all { it.isLocked }) {
            proceedWithUnlock(selectedNotes)
        } else {
            proceedWithLock(selectedNotes.filter { !it.isLocked })
        }
    }

    override fun handleBulkPin() {
        val selectedNotes = getSelectedNotes()
        if (selectedNotes.isEmpty()) return
        val allPinned = selectedNotes.all { it.isPinned }
        viewModel.pinNotes(selectedNotes)
        Toast.makeText(
            requireContext(),
            if (allPinned) R.string.note_unpinned else R.string.note_pinned,
            Toast.LENGTH_SHORT
        ).show()
        noteCardAdapter.exitSelectionMode()
    }

    override fun handleBulkReminder() {
        // Reminder tab intentionally has no Add Reminder action on the selection bar.
    }

    override fun handleBulkCopy() {
        val selectedNotes = getSelectedNotes()
        if (selectedNotes.size != 1) return
        viewModel.duplicateNote(selectedNotes.first())
        Toast.makeText(requireContext(), R.string.note_copied, Toast.LENGTH_SHORT).show()
        noteCardAdapter.exitSelectionMode()
    }

    override fun handleBulkDelete() {
        val selectedNotes = getSelectedNotes()
        if (selectedNotes.isEmpty()) return
        NotesSelectionDialogs.showConfirmation(
            fragment = this,
            titleRes = R.string.delete_selected_notes_title,
            messageRes = R.string.delete_selected_notes_message,
            positiveButtonRes = R.string.delete,
            iconRes = R.drawable.ic_delete,
            onPositiveClick = {
                viewModel.deleteNotes(selectedNotes)
                noteCardAdapter.exitSelectionMode()
            }
        )
    }

    private fun confirmDeleteNote(note: Note) {
        NotesSelectionDialogs.showConfirmation(
            fragment = this,
            titleRes = R.string.delete_note,
            messageRes = R.string.delete_note_confirm,
            positiveButtonRes = R.string.delete_note,
            iconRes = R.drawable.ic_recylerbin,
            onPositiveClick = { viewModel.deleteNote(note) }
        )
    }

    private fun getSelectedNotes(): List<Note> {
        if (!::noteCardAdapter.isInitialized) return emptyList()
        val selectedIds = noteCardAdapter.getSelectedNoteIds()
        if (selectedIds.isEmpty()) return emptyList()
        return viewModel.uiState.value.noteCards
            .filter { it.noteId in selectedIds }
            .map { it.note }
    }

    private fun proceedWithLock(notesToLock: List<Note>) {
        if (notesToLock.isEmpty()) return
        PremiumFeatureGate.requirePremium(this, PremiumFeature.LOCK_NOTE) {
            if (notePasswordManager.hasPassword()) {
                viewModel.setNotesLocked(notesToLock.map { it.id }, isLocked = true)
                Toast.makeText(requireContext(), R.string.note_locked, Toast.LENGTH_SHORT).show()
                if (::noteCardAdapter.isInitialized && noteCardAdapter.isInSelectionMode()) {
                    noteCardAdapter.exitSelectionMode()
                }
                return@requirePremium
            }
            pendingLockNoteIds = notesToLock.map { it.id }
            setPasswordForLockLauncher.launch(
                SetPasswordActivity.createSetIntent(requireContext(), notesToLock.first().id)
            )
        }
    }

    private fun proceedWithUnlock(notesToUnlock: List<Note>) {
        if (notesToUnlock.isEmpty()) return
        if (!notePasswordManager.hasPassword()) {
            viewModel.setNotesLocked(notesToUnlock.map { it.id }, isLocked = false)
            Toast.makeText(requireContext(), R.string.note_unlocked, Toast.LENGTH_SHORT).show()
            if (::noteCardAdapter.isInitialized && noteCardAdapter.isInSelectionMode()) {
                noteCardAdapter.exitSelectionMode()
            }
            return
        }
        pendingClearLockNoteIds = notesToUnlock.map { it.id }
        verifyClearLockLauncher.launch(
            SetPasswordActivity.createVerifyIntent(requireContext(), notesToUnlock.first().id)
        )
    }

    private fun observeNotes() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val canReorder = state.searchQuery.isBlank() &&
                        state.sortOrder == NoteSortOrder.CUSTOM_ORDER
                    noteCardAdapter.setDragReorderEnabled(canReorder)
                    noteCardAdapter.setSearchQuery(state.searchQuery)
                    if (!noteCardAdapter.isDragInProgress()) {
                        noteCardAdapter.submitNotes(state.noteCards)
                    }
                    val isEmpty = state.noteCards.isEmpty()
                    binding.emptyState.isVisible = isEmpty
                    binding.rvNotes.isVisible = !isEmpty
                    updateNotesCount(state.noteCount)
                }
            }
        }
    }

    private fun updateNotesCount(count: Int) {
        val homeActivity = activity as? HomeActivity ?: return
        if (!homeActivity.canUpdateTopBarNotesCount(this)) return
        homeActivity.updateTopBarNotesCount(count)
    }

    private fun syncListUiFromViewModel() {
        if (_binding == null || !::noteCardAdapter.isInitialized) return
        val state = viewModel.uiState.value
        if (!noteCardAdapter.isDragInProgress()) {
            noteCardAdapter.submitNotes(state.noteCards)
        }
        val isEmpty = state.noteCards.isEmpty()
        binding.emptyState.isVisible = isEmpty
        binding.rvNotes.isVisible = !isEmpty
        updateNotesCount(state.noteCount)
    }

    private fun openNote(note: Note) {
        if (note.isLocked) {
            authenticateAndOpen(note)
        } else {
            launchNoteEditor(note)
        }
    }

    private fun authenticateAndOpen(note: Note) {
        if (notePasswordManager.hasPassword()) {
            pendingUnlockNoteId = note.id
            unlockNoteLauncher.launch(SetPasswordActivity.createVerifyIntent(requireContext(), note.id))
            return
        }

        val biometricManager = BiometricManager.from(requireContext())
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(requireContext(), R.string.authenticate_to_open, Toast.LENGTH_SHORT).show()
            launchNoteEditor(note)
            return
        }

        val executor = ContextCompat.getMainExecutor(requireContext())
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    launchNoteEditor(note, lockAuthenticated = true)
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.locked_note))
            .setSubtitle(getString(R.string.authenticate_to_open))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        prompt.authenticate(promptInfo)
    }

    private fun launchNoteEditor(note: Note, lockAuthenticated: Boolean = false) {
        openWithHomeInterAd {
            requireActivity().launchActivity<CreateNotesActivity> {
                putExtra(NoteEditViewModel.EXTRA_NOTE_ID, note.id)
                if (lockAuthenticated) {
                    putExtra(NoteEditViewModel.EXTRA_LOCK_AUTHENTICATED, true)
                }
            }
        }
    }

    private fun shareNote(note: Note) {
        viewLifecycleOwner.lifecycleScope.launch {
            val shareText = NoteShareTextFormatter.buildForNote(
                noteId = note.id,
                title = note.title,
                noteRepository = noteRepository,
                todoRepository = todoRepository,
                untitledFallback = getString(R.string.untitled)
            )
            val subject = JsonUtils.blockContentPlainText(note.title)
                .ifBlank { getString(R.string.untitled) }
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share)))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
