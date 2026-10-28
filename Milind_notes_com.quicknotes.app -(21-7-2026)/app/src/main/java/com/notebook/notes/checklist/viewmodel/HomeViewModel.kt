package com.notebook.notes.checklist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notebook.notes.checklist.model.Note
import com.notebook.notes.checklist.model.RepeatType
import com.notebook.notes.checklist.repository.NoteRepository
import com.notebook.notes.checklist.sort.NoteSortOrder
import com.notebook.notes.checklist.sort.NoteSortPreference
import com.notebook.notes.checklist.sort.NoteSorter
import com.notebook.notes.checklist.ui.card.NoteCardMapper
import com.notebook.notes.checklist.ui.model.NoteCardUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val noteCards: List<NoteCardUiModel> = emptyList(),
    val searchQuery: String = "",
    val noteCount: Int = 0,
    val sortOrder: NoteSortOrder = NoteSortOrder.CUSTOM_ORDER
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: NoteRepository,
    private val noteCardMapper: NoteCardMapper,
    private val sortPreference: NoteSortPreference
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    private val notes = _searchQuery
        .flatMapLatest { query -> repository.searchNotes(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val sortedNotes = combine(notes, sortPreference.sortOrder) { list, order ->
        NoteSorter.sort(list, order)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val noteCards = sortedNotes.flatMapLatest { noteList ->
        flow { emit(noteCardMapper.mapAll(noteList)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<HomeUiState> =
        combine(noteCards, _searchQuery, sortPreference.sortOrder) { cards, query, order ->
            HomeUiState(
                noteCards = cards,
                searchQuery = query,
                noteCount = cards.size,
                sortOrder = order
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    val currentSortOrder: NoteSortOrder get() = sortPreference.current

    fun setSortOrder(order: NoteSortOrder) {
        sortPreference.setSortOrder(order)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch { repository.deleteNote(note) }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch { repository.togglePin(note) }
    }

    fun toggleFavorite(note: Note) {
        viewModelScope.launch { repository.toggleFavorite(note) }
    }

    fun toggleArchive(note: Note) {
        viewModelScope.launch { repository.toggleArchive(note) }
    }

    fun toggleLock(note: Note) {
        viewModelScope.launch { repository.setLocked(note.id, !note.isLocked) }
    }

    fun setNotesLocked(noteIds: Collection<Long>, isLocked: Boolean) {
        if (noteIds.isEmpty()) return
        viewModelScope.launch {
            noteIds.forEach { noteId ->
                repository.setLocked(noteId, isLocked)
            }
        }
    }

    fun pinNotes(notes: List<Note>) {
        if (notes.isEmpty()) return
        viewModelScope.launch {
            val allPinned = notes.all { it.isPinned }
            notes.forEach { note ->
                if (allPinned) {
                    if (note.isPinned) repository.togglePin(note)
                } else if (!note.isPinned) {
                    repository.togglePin(note)
                }
            }
        }
    }

    fun archiveNotes(notes: List<Note>) {
        if (notes.isEmpty()) return
        viewModelScope.launch {
            val allArchived = notes.all { it.isArchived }
            notes.forEach { note ->
                if (allArchived) {
                    if (note.isArchived) repository.toggleArchive(note)
                } else if (!note.isArchived) {
                    repository.toggleArchive(note)
                }
            }
        }
    }

    fun deleteNotes(notes: List<Note>) {
        viewModelScope.launch {
            notes.forEach { note -> repository.deleteNote(note) }
        }
    }

    fun duplicateNote(note: Note) {
        viewModelScope.launch { repository.duplicateNote(note) }
    }

    fun setReminder(noteId: Long, timeMillis: Long, repeatType: RepeatType, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val note = repository.getNoteById(noteId)
            if (note == null) {
                onResult(false)
                return@launch
            }
            repository.setReminder(noteId, timeMillis, repeatType)
            onResult(true)
        }
    }

    fun reorderNotes(orderedCards: List<Pair<Long, Boolean>>) {
        viewModelScope.launch {
            repository.updateHomeNoteOrder(orderedCards)
        }
    }
}
