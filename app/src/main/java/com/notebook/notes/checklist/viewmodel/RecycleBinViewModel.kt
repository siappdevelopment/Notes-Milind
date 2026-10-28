package com.notebook.notes.checklist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notebook.notes.checklist.model.Note
import com.notebook.notes.checklist.repository.NoteRepository
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

data class RecycleBinUiState(
    val noteCards: List<NoteCardUiModel> = emptyList(),
    val searchQuery: String = "",
    val noteCount: Int = 0,
    val isEmpty: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecycleBinViewModel @Inject constructor(
    private val repository: NoteRepository,
    private val noteCardMapper: NoteCardMapper
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            repository.purgeExpiredDeletedNotes()
        }
    }

    private val notes = _searchQuery
        .flatMapLatest { query -> repository.searchDeletedNotes(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val noteCards = notes.flatMapLatest { noteList ->
        flow { emit(noteCardMapper.mapAll(noteList)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<RecycleBinUiState> = combine(noteCards, _searchQuery) { cards, query ->
        RecycleBinUiState(
            noteCards = cards,
            searchQuery = query,
            noteCount = cards.size,
            isEmpty = cards.isEmpty()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecycleBinUiState())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun restoreNote(note: Note) {
        viewModelScope.launch { repository.restoreNote(note) }
    }

    fun restoreNotes(notes: List<Note>) {
        viewModelScope.launch { notes.forEach { repository.restoreNote(it) } }
    }

    fun permanentlyDeleteNote(note: Note) {
        viewModelScope.launch { repository.permanentlyDeleteNote(note) }
    }

    fun permanentlyDeleteNotes(notes: List<Note>) {
        viewModelScope.launch { notes.forEach { repository.permanentlyDeleteNote(it) } }
    }

    fun permanentlyDeleteAll() {
        viewModelScope.launch { repository.permanentlyDeleteAllDeletedNotes() }
    }
}
