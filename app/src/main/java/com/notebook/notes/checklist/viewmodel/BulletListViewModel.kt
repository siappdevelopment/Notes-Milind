package com.notebook.notes.checklist.viewmodel

import androidx.lifecycle.ViewModel
import com.notebook.notes.checklist.model.BlockType
import com.notebook.notes.checklist.model.BulletListHelper
import com.notebook.notes.checklist.model.BulletListItem
import com.notebook.notes.checklist.model.BulletListMetadata
import com.notebook.notes.checklist.model.NoteBlock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class BulletListUiState(
    val isBulletModeEnabled: Boolean = false,
    val lastAddedBlockId: Long? = null
)

@HiltViewModel
class BulletListViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(BulletListUiState())
    val uiState: StateFlow<BulletListUiState> = _uiState.asStateFlow()
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
            .filter { it.type == BlockType.BULLET_LIST }
            .maxOfOrNull { BulletListMetadata.fromBlock(it).listGroupId }
            ?: 0
        nextListGroupId = maxOf(nextListGroupId, maxGroup + 1)
    }

    fun enableMode() {
        _uiState.update { it.copy(isBulletModeEnabled = true) }
    }

    fun disableMode() {
        _uiState.update { it.copy(isBulletModeEnabled = false, lastAddedBlockId = null) }
        activeListGroupId = null
    }

    fun toggleMode() {
        if (_uiState.value.isBulletModeEnabled) {
            disableMode()
        } else {
            enableMode()
        }
    }

    fun setLastAddedBlockId(blockId: Long?) {
        _uiState.update { it.copy(lastAddedBlockId = blockId) }
    }

    fun clearLastAddedBlockId() {
        _uiState.update { it.copy(lastAddedBlockId = null) }
    }

    fun computeDisplayPosition(blocks: List<NoteBlock>, blockId: Long): Int =
        BulletListHelper.computeDisplayPositions(blocks)[blockId] ?: 0

    fun fixCorruptedPositions(blocks: List<NoteBlock>): List<NoteBlock> =
        BulletListHelper.normalizeBlocks(BulletListHelper.removeEmptyBlocks(blocks))

    fun splitSelectionToItems(text: String): List<String> =
        BulletListHelper.splitLinesToItems(text)

    fun buildBlocksFromItems(
        items: List<String>,
        startPosition: Int,
        listGroupId: Int = 0
    ): List<NoteBlock> {
        var nextTempId = -1L
        return items.mapIndexed { index, text ->
            NoteBlock(
                id = nextTempId--,
                type = BlockType.BULLET_LIST,
                content = text,
                extraData = BulletListMetadata.toJson(
                    BulletListItem(text = text, position = index, listGroupId = listGroupId)
                ),
                position = startPosition + index
            )
        }
    }
}
