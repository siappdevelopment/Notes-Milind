package com.notebook.notes.checklist.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notebook.notes.checklist.model.NoteBlock
import com.notebook.notes.checklist.model.TodoItem
import com.notebook.notes.checklist.repository.TodoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TodoUiState(
    val listId: Long? = null,
    val noteId: Long? = null,
    val items: List<TodoItem> = emptyList(),
    val isLoading: Boolean = false,
    val hasList: Boolean = false,
    val lastAddedItemId: Long? = null,
    val focusItemId: Long? = null
)

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val repository: TodoRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialNoteId: Long? = savedStateHandle.get<Long>(NoteEditViewModel.EXTRA_NOTE_ID)
        ?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(TodoUiState(isLoading = initialNoteId != null))
    val uiState: StateFlow<TodoUiState> = _uiState.asStateFlow()

    private var itemsJob: Job? = null

    fun initForNote(noteId: Long?, title: String, blocks: List<NoteBlock> = emptyList()) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, noteId = noteId) }
            val listId = when {
                noteId != null && noteId > 0 -> {
                    val existing = repository.getListByNoteId(noteId)
                    existing?.id ?: repository.migrateCheckboxBlocks(noteId, title, blocks)
                }
                else -> _uiState.value.listId
            }
            if (listId != null) {
                observeList(listId)
            } else {
                _uiState.update { it.copy(isLoading = false, hasList = false) }
            }
        }
    }

    fun setTitle(title: String) {
        val listId = _uiState.value.listId ?: return
        viewModelScope.launch {
            repository.updateListTitle(listId, title)
        }
    }

    fun linkToNote(noteId: Long) {
        val listId = _uiState.value.listId ?: return
        viewModelScope.launch {
            repository.linkListToNote(listId, noteId)
            _uiState.update { it.copy(noteId = noteId) }
        }
    }

    fun addItem(text: String = "", title: String = "", position: Int, requestFocus: Boolean = true) {
        viewModelScope.launch {
            val listId = ensureListId(title)
            val itemId = repository.addItem(listId, text, position)
            _uiState.update {
                it.copy(
                    lastAddedItemId = if (requestFocus) itemId else null,
                    focusItemId = if (requestFocus) itemId else it.focusItemId
                )
            }
        }
    }

    fun insertItemAt(position: Int, title: String = "", requestFocus: Boolean = true) {
        viewModelScope.launch {
            val listId = ensureListId(title)
            val itemId = repository.insertItemAt(listId, "", position)
            _uiState.update {
                it.copy(
                    lastAddedItemId = if (requestFocus) itemId else null,
                    focusItemId = if (requestFocus) itemId else it.focusItemId
                )
            }
        }
    }

    fun insertItemAfter(afterItemId: Long, title: String = "", requestFocus: Boolean = true) {
        viewModelScope.launch {
            val listId = ensureListId(title)
            val afterItem = _uiState.value.items.find { it.id == afterItemId }
            if (afterItem == null) {
                val position = _uiState.value.items.maxOfOrNull { it.position }?.plus(1) ?: 0
                val itemId = repository.addItem(listId, "", position)
                _uiState.update {
                    it.copy(
                        lastAddedItemId = if (requestFocus) itemId else null,
                        focusItemId = if (requestFocus) itemId else it.focusItemId
                    )
                }
                return@launch
            }
            val insertPosition = afterItem.position + 1
            val itemId = repository.insertItemAt(listId, "", insertPosition)
            _uiState.update {
                it.copy(
                    lastAddedItemId = if (requestFocus) itemId else null,
                    focusItemId = if (requestFocus) itemId else it.focusItemId
                )
            }
        }
    }

    fun shiftPositionsFrom(fromPosition: Int, delta: Int) {
        val listId = _uiState.value.listId ?: return
        viewModelScope.launch {
            repository.shiftPositionsFrom(listId, fromPosition, delta)
        }
    }

    fun applyItemPositions(positions: Map<Long, Int>) {
        viewModelScope.launch {
            repository.applyItemPositions(positions)
        }
    }

    fun deleteItemAndFocusPrevious(itemId: Long) {
        val sorted = _uiState.value.items.sortedBy { it.position }
        val index = sorted.indexOfFirst { it.id == itemId }
        val previousId = sorted.getOrNull(index - 1)?.id
        viewModelScope.launch {
            repository.deleteItem(itemId)
            _uiState.update {
                it.copy(
                    focusItemId = previousId,
                    lastAddedItemId = previousId
                )
            }
        }
    }

    fun requestFocus(itemId: Long) {
        _uiState.update { it.copy(focusItemId = itemId, lastAddedItemId = itemId) }
    }

    fun clearFocusItemId() {
        _uiState.update { it.copy(focusItemId = null) }
    }

    fun updateItemText(itemId: Long, text: String) {
        viewModelScope.launch {
            repository.updateItemText(itemId, text)
        }
    }

    fun toggleItem(itemId: Long) {
        viewModelScope.launch {
            repository.toggleItem(itemId)
        }
    }

    fun deleteItem(itemId: Long) {
        viewModelScope.launch {
            repository.deleteItem(itemId)
        }
    }

    fun clearLastAddedItemId() {
        _uiState.update { it.copy(lastAddedItemId = null, focusItemId = null) }
    }

    private suspend fun ensureListId(title: String): Long {
        return _uiState.value.listId ?: run {
            val newListId = repository.createList(title, _uiState.value.noteId)
            observeList(newListId)
            newListId
        }
    }

    private fun observeList(listId: Long) {
        itemsJob?.cancel()
        itemsJob = viewModelScope.launch {
            repository.observeItems(listId).collect { items ->
                _uiState.update {
                    it.copy(
                        listId = listId,
                        items = items,
                        hasList = true,
                        isLoading = false
                    )
                }
            }
        }
    }
}
