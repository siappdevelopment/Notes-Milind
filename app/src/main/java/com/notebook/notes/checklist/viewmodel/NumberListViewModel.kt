package com.notebook.notes.checklist.viewmodel

import androidx.lifecycle.ViewModel
import com.notebook.notes.checklist.model.BlockType
import com.notebook.notes.checklist.model.NoteBlock
import com.notebook.notes.checklist.model.NumberListHelper
import com.notebook.notes.checklist.model.NumberListItem
import com.notebook.notes.checklist.model.NumberListMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class NumberListUiState(
    val isNumberModeEnabled: Boolean = false,
    val currentNumber: Int = 1,
    val lastAddedBlockId: Long? = null
)

@HiltViewModel
class NumberListViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(NumberListUiState())
    val uiState: StateFlow<NumberListUiState> = _uiState.asStateFlow()
    private var nextListGroupId = 1
    private var activeListGroupId: Int? = null

    fun createListGroupId(): Int {
        val id = nextListGroupId++
        activeListGroupId = id
        return id
    }

    fun activeListGroupId(): Int? = activeListGroupId

    fun syncListGroupCounter(blocks: List<NoteBlock>) {
        val maxGroup = blocks
            .filter { it.type == BlockType.NUMBER_LIST }
            .maxOfOrNull { NumberListMetadata.fromBlock(it).listGroupId }
            ?: 0
        nextListGroupId = maxOf(nextListGroupId, maxGroup + 1)
    }

    fun enableMode(startNumber: Int = 1) {
        _uiState.update {
            it.copy(isNumberModeEnabled = true, currentNumber = startNumber.coerceAtLeast(1))
        }
    }

    fun disableMode() {
        _uiState.update {
            it.copy(isNumberModeEnabled = false, currentNumber = 1, lastAddedBlockId = null)
        }
        activeListGroupId = null
    }

    fun toggleMode(nextNumber: Int = 1) {
        if (_uiState.value.isNumberModeEnabled) {
            disableMode()
        } else {
            enableMode(nextNumber)
        }
    }

    fun setLastAddedBlockId(blockId: Long?) {
        _uiState.update { it.copy(lastAddedBlockId = blockId) }
    }

    fun clearLastAddedBlockId() {
        _uiState.update { it.copy(lastAddedBlockId = null) }
    }

    fun advanceCurrentNumber() {
        _uiState.update { it.copy(currentNumber = it.currentNumber + 1) }
    }

    fun computeDisplayOrder(blocks: List<NoteBlock>, blockId: Long): Int =
        NumberListHelper.computeDisplayOrders(blocks)[blockId] ?: 1

    fun fixCorruptedOrders(blocks: List<NoteBlock>): List<NoteBlock> =
        NumberListHelper.normalizeBlocks(blocks)

    fun splitSelectionToItems(text: String): List<String> =
        NumberListHelper.splitLinesToItems(text)

    fun buildBlocksFromItems(
        items: List<String>,
        startPosition: Int,
        startOrder: Int = 1,
        listGroupId: Int = 0
    ): List<NoteBlock> {
        var nextTempId = -1L
        return items.mapIndexed { index, text ->
            val order = startOrder + index
            NoteBlock(
                id = nextTempId--,
                type = BlockType.NUMBER_LIST,
                content = text,
                extraData = NumberListMetadata.toJson(
                    NumberListItem(text = text, order = order, listGroupId = listGroupId)
                ),
                position = startPosition + index
            )
        }
    }
}
