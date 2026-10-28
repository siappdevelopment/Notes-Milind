package com.notebook.notes.checklist.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notebook.notes.checklist.helper.PreferenceHelper
import com.notebook.notes.checklist.model.BlockType
import com.notebook.notes.checklist.model.Note
import com.notebook.notes.checklist.model.NoteBlock
import com.notebook.notes.checklist.model.NoteBlockExtra
import com.notebook.notes.checklist.model.BulletListHelper
import com.notebook.notes.checklist.model.BulletListItem
import com.notebook.notes.checklist.model.BulletListMetadata
import com.notebook.notes.checklist.model.NumberListItem
import com.notebook.notes.checklist.model.NumberListMetadata
import com.notebook.notes.checklist.model.RepeatType
import com.notebook.notes.checklist.model.RichTextDocument
import com.notebook.notes.checklist.model.RichTextSpan
import com.notebook.notes.checklist.model.VoiceNoteMetadata
import com.notebook.notes.checklist.repository.DrawingRepository
import com.notebook.notes.checklist.model.AttachmentMetadata
import com.notebook.notes.checklist.repository.AttachmentRepository
import com.notebook.notes.checklist.repository.CategoryRepository
import com.notebook.notes.checklist.repository.NoteRepository
import com.notebook.notes.checklist.repository.VoiceNoteRepository
import com.notebook.notes.checklist.repository.TodoRepository
import com.notebook.notes.checklist.storage.FileStorageManager
import com.notebook.notes.checklist.utils.Constant
import com.notebook.notes.checklist.utils.JsonUtils
import com.notebook.notes.checklist.util.NoteTitleHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoteEditUiState(
    val noteId: Long? = null,
    val title: String = "",
    val blocks: List<NoteBlock> = emptyList(),
    val categoryId: Long = 0,
    val categoryName: String = "",
    val categoryIcon: String? = null,
    val color: String? = null,
    val listItemHeight: Int? = null,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isLocked: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val reminderEnabled: Boolean = false,
    val reminderTimeMillis: Long = 0L,
    val repeatType: String = RepeatType.NEVER.name,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val isArchivedActionDone: Boolean = false,
    val isReadingMode: Boolean = false
)

@HiltViewModel
class NoteEditViewModel @Inject constructor(
    private val repository: NoteRepository,
    private val categoryRepository: CategoryRepository,
    private val voiceNoteRepository: VoiceNoteRepository,
    private val drawingRepository: DrawingRepository,
    private val attachmentRepository: AttachmentRepository,
    private val todoRepository: TodoRepository,
    private val fileStorageManager: FileStorageManager,
    private val preferenceHelper: PreferenceHelper,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val noteId: Long? = savedStateHandle.get<Long>(EXTRA_NOTE_ID)?.takeIf { it > 0 }
    /** Optional preselected category when creating a note from Home category filter. */
    private val preferredCategoryId: Long? =
        savedStateHandle.get<Long>(EXTRA_CATEGORY_ID)?.takeIf { it > 0 }
    private val isNewNoteSession: Boolean = noteId == null

    /** Preference key applied only when starting a new note; existing notes keep their stored color. */
    private val defaultNoteBackground: String? =
        if (isNewNoteSession) {
            preferenceHelper.getString(Constant.PREF_DEFAULT_NOTE_BACKGROUND)
                ?.takeIf { it.isNotBlank() }
        } else {
            null
        }

    /** List-item line spacing for new notes; stamped onto the note at create time and reused on reopen. */
    private val defaultListItemHeight: Int? =
        if (isNewNoteSession) {
            preferenceHelper.getInt(
                Constant.PREF_LIST_ITEM_HEIGHT,
                Constant.DEFAULT_ITEM_HEIGHT
            )
        } else {
            null
        }

    private val _uiState = MutableStateFlow(
        NoteEditUiState(
            isLoading = noteId != null,
            color = defaultNoteBackground,
            listItemHeight = defaultListItemHeight
        )
    )
    val uiState: StateFlow<NoteEditUiState> = _uiState.asStateFlow()

    private var nextTempId = -1L

    /** Default size for newly created empty rich-text content in a new-note session. */
    fun defaultFontSize(): Int =
        if (isNewNoteSession) {
            preferenceHelper.getInt(Constant.PREF_DEFAULT_FONT_SIZE, Constant.DEFAULT_FONT_SIZE)
        } else {
            Constant.DEFAULT_FONT_SIZE
        }

    /** Default font family for newly created empty rich-text content in a new-note session. */
    fun defaultFontFamily(): String =
        if (isNewNoteSession) {
            preferenceHelper.getString(
                Constant.PREF_DEFAULT_FONT_FAMILY,
                Constant.DEFAULT_FONT_FAMILY
            )?.takeIf { it.isNotBlank() } ?: Constant.DEFAULT_FONT_FAMILY
        } else {
            Constant.DEFAULT_FONT_FAMILY
        }

    private fun emptyRichContent(): String =
        JsonUtils.emptyRichJson(defaultFontSize(), defaultFontFamily())

    private fun emptyRichDocument(): RichTextDocument =
        RichTextDocument(
            spans = listOf(
                RichTextSpan(
                    fontSize = defaultFontSize(),
                    fontFamily = defaultFontFamily()
                )
            )
        )

    init {
        viewModelScope.launch {
            categoryRepository.ensureDefaultCategories()
            if (noteId != null) {
                loadNote(noteId)
            } else {
                val preferredCategory = preferredCategoryId
                    ?.let { categoryRepository.getCategoryById(it) }
                    ?.takeUnless { it.isAllNotes }
                val categoryId = preferredCategory?.id
                    ?: categoryRepository.getDefaultNoteCategoryId()
                val category = preferredCategory
                    ?: categoryRepository.getCategoryById(categoryId)
                _uiState.update {
                    it.copy(
                        categoryId = categoryId,
                        categoryName = category?.name.orEmpty(),
                        categoryIcon = category?.icon,
                        color = defaultNoteBackground,
                        listItemHeight = defaultListItemHeight,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun loadNote(id: Long) {
        viewModelScope.launch {
            val note = repository.getNoteById(id)
            val blocks = repository.getBlocksOnce(id)
            val category = note?.categoryId?.let { categoryRepository.getCategoryById(it) }
            _uiState.update {
                NoteEditUiState(
                    noteId = id,
                    title = note?.title.orEmpty(),
                    blocks = normalizeAllListBlocks(blocks),
                    categoryId = note?.categoryId ?: categoryRepository.getDefaultNoteCategoryId(),
                    categoryName = category?.name.orEmpty(),
                    categoryIcon = category?.icon,
                    color = note?.color,
                    listItemHeight = note?.listItemHeight,
                    isPinned = note?.isPinned ?: false,
                    isFavorite = note?.isFavorite ?: false,
                    isArchived = note?.isArchived ?: false,
                    isLocked = note?.isLocked ?: false,
                    createdAt = note?.createdAt ?: 0L,
                    updatedAt = note?.updatedAt ?: 0L,
                    reminderEnabled = note?.reminderEnabled ?: false,
                    reminderTimeMillis = note?.reminderTimeMillis ?: 0L,
                    repeatType = note?.repeatType ?: RepeatType.NEVER.name,
                    isLoading = false
                )
            }
        }
    }

    fun setCategory(categoryId: Long) {
        viewModelScope.launch {
            val category = categoryRepository.getCategoryById(categoryId) ?: return@launch
            if (category.isAllNotes) return@launch
            _uiState.update {
                it.copy(
                    categoryId = category.id,
                    categoryName = category.name,
                    categoryIcon = category.icon
                )
            }
        }
    }

    fun setBackgroundColor(colorKey: String?) {
        _uiState.update { it.copy(color = colorKey) }
    }

    fun togglePin() {
        viewModelScope.launch {
            val noteId = persistAndGetId()
            if (noteId <= 0L) return@launch
            val note = repository.getNoteById(noteId) ?: return@launch
            repository.togglePin(note)
            val refreshed = repository.getNoteById(noteId)
            _uiState.update {
                it.copy(
                    noteId = noteId,
                    isPinned = refreshed?.isPinned ?: !note.isPinned
                )
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val noteId = persistAndGetId()
            if (noteId <= 0L) return@launch
            val note = repository.getNoteById(noteId) ?: return@launch
            repository.toggleFavorite(note)
            val refreshed = repository.getNoteById(noteId)
            _uiState.update {
                it.copy(
                    noteId = noteId,
                    isFavorite = refreshed?.isFavorite ?: !note.isFavorite
                )
            }
        }
    }

    fun toggleArchive() {
        viewModelScope.launch {
            val noteId = persistAndGetId()
            if (noteId <= 0L) return@launch
            val note = repository.getNoteById(noteId) ?: return@launch
            val archived = !note.isArchived
            repository.setArchived(noteId, archived)
            _uiState.update {
                it.copy(
                    noteId = noteId,
                    isArchived = archived,
                    isArchivedActionDone = true
                )
            }
        }
    }

    fun setLocked(isLocked: Boolean) {
        val noteId = _uiState.value.noteId ?: return
        if (noteId <= 0) return
        viewModelScope.launch {
            repository.setLocked(noteId, isLocked)
            _uiState.update { it.copy(isLocked = isLocked) }
        }
    }

    fun toggleReadingMode() {
        _uiState.update { it.copy(isReadingMode = !it.isReadingMode) }
    }

    /**
     * Saves pending edits, links any in-progress todo list to this note, then
     * duplicates the note including todos.
     */
    suspend fun duplicateCurrentNote(todoListId: Long? = null): Boolean {
        val noteId = persistAndGetId()
        if (noteId <= 0L) return false
        if (todoListId != null && todoListId > 0L) {
            todoRepository.linkListToNote(todoListId, noteId)
        }
        val note = repository.getNoteById(noteId) ?: return false
        repository.duplicateNote(note)
        return true
    }

    fun deleteCurrentNote() {
        viewModelScope.launch {
            val noteId = _uiState.value.noteId ?: return@launch
            val note = repository.getNoteById(noteId) ?: return@launch
            repository.deleteNote(note)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }

    suspend fun setReminder(timeMillis: Long, repeatType: RepeatType): Boolean {
        val noteId = persistAndGetId()
        if (noteId <= 0L) return false
        repository.setReminder(noteId, timeMillis, repeatType)
        val refreshed = repository.getNoteById(noteId)
        _uiState.update {
            it.copy(
                noteId = noteId,
                reminderEnabled = refreshed?.reminderEnabled ?: true,
                reminderTimeMillis = refreshed?.reminderTimeMillis ?: timeMillis,
                repeatType = refreshed?.repeatType ?: repeatType.name
            )
        }
        return true
    }

    fun setTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun addTextBlock(position: Int): Long {
        val block = NoteBlock(
            id = nextTempId--,
            type = BlockType.TEXT,
            content = JsonUtils.richTextToJson(emptyRichDocument()),
            position = position
        )
        _uiState.update { it.copy(blocks = it.blocks + block) }
        return block.id
    }

    fun addNotesTextBlock(position: Int): Long {
        val block = NoteBlock(
            id = nextTempId--,
            type = BlockType.TEXT,
            content = JsonUtils.richTextToJson(emptyRichDocument()),
            extraData = NoteBlockExtra.NOTES_HERE,
            position = position
        )
        _uiState.update { it.copy(blocks = it.blocks + block) }
        return block.id
    }

    fun addDividerBlock(position: Int): Long {
        val block = NoteBlock(
            id = nextTempId--,
            type = BlockType.DIVIDER,
            content = "",
            position = position
        )
        _uiState.update { it.copy(blocks = it.blocks + block) }
        return block.id
    }

    fun addBulletListBlock(text: String = "", position: Int, listGroupId: Int = 0, itemPosition: Int = 0): Long {
        val content = if (text.isBlank()) emptyRichContent() else JsonUtils.plainTextToRichJson(text)
        val block = NoteBlock(
            id = nextTempId--,
            type = BlockType.BULLET_LIST,
            content = content,
            extraData = BulletListMetadata.toJson(
                BulletListItem(text = content, position = itemPosition, listGroupId = listGroupId)
            ),
            position = position
        )
        _uiState.update { it.copy(blocks = it.blocks + block) }
        return block.id
    }

    fun insertBulletListBlocks(newBlocks: List<NoteBlock>, atPosition: Int) {
        if (newBlocks.isEmpty()) return
        _uiState.update { state ->
            val shiftBy = newBlocks.size
            val shifted = state.blocks.map { block ->
                if (block.position >= atPosition) {
                    block.copy(position = block.position + shiftBy)
                } else {
                    block
                }
            }
            state.copy(blocks = shifted + newBlocks)
        }
    }

    fun insertBulletListItems(
        items: List<String>,
        atPosition: Int,
        listGroupId: Int = 0
    ): Long? {
        if (items.isEmpty()) return null
        val newBlocks = items.mapIndexed { index, text ->
            val content = if (text.isBlank()) emptyRichContent() else JsonUtils.plainTextToRichJson(text)
            NoteBlock(
                id = nextTempId--,
                type = BlockType.BULLET_LIST,
                content = content,
                extraData = BulletListMetadata.toJson(
                    BulletListItem(text = content, position = index, listGroupId = listGroupId)
                ),
                position = atPosition + index
            )
        }
        insertBulletListBlocks(newBlocks, atPosition)
        return newBlocks.last().id
    }

    fun updateBulletListBlock(blockId: Long, text: String, position: Int) {
        _uiState.update { state ->
            state.copy(
                blocks = state.blocks.map { block ->
                    if (block.id == blockId && block.type == BlockType.BULLET_LIST) {
                        val metadata = BulletListMetadata.fromBlock(block)
                        block.copy(
                            content = text,
                            extraData = BulletListMetadata.toJson(
                                metadata.copy(text = text, position = position)
                            )
                        )
                    } else {
                        block
                    }
                }
            )
        }
    }

    fun normalizeBulletListPositions() {
        _uiState.update { state ->
            state.copy(blocks = BulletListHelper.normalizeBlocks(state.blocks))
        }
    }

    fun cleanEmptyBulletListBlocks() {
        _uiState.update { state ->
            state.copy(blocks = BulletListHelper.removeEmptyBlocks(state.blocks))
        }
    }

    fun convertListGroupToBulletList(listGroupId: Int) {
        _uiState.update { state ->
            val converted = state.blocks.map { block ->
                if (block.type == BlockType.NUMBER_LIST &&
                    NumberListMetadata.fromBlock(block).listGroupId == listGroupId
                ) {
                    val metadata = NumberListMetadata.fromBlock(block)
                    block.copy(
                        type = BlockType.BULLET_LIST,
                        extraData = BulletListMetadata.toJson(
                            BulletListItem(
                                text = block.content,
                                position = metadata.order - 1,
                                level = metadata.level,
                                listGroupId = listGroupId
                            )
                        )
                    )
                } else {
                    block
                }
            }
            state.copy(blocks = BulletListHelper.normalizeBlocks(converted))
        }
    }

    fun convertListGroupToNumberList(listGroupId: Int) {
        _uiState.update { state ->
            val converted = state.blocks.map { block ->
                if (block.type == BlockType.BULLET_LIST &&
                    BulletListMetadata.fromBlock(block).listGroupId == listGroupId
                ) {
                    val metadata = BulletListMetadata.fromBlock(block)
                    block.copy(
                        type = BlockType.NUMBER_LIST,
                        extraData = NumberListMetadata.toJson(
                            NumberListItem(
                                text = block.content,
                                order = metadata.position + 1,
                                level = metadata.level,
                                listGroupId = listGroupId
                            )
                        )
                    )
                } else {
                    block
                }
            }
            state.copy(blocks = com.notebook.notes.checklist.model.NumberListHelper.normalizeBlocks(converted))
        }
    }

    fun addNumberListBlock(text: String = "", position: Int, order: Int = 1, listGroupId: Int = 0): Long {
        val content = if (text.isBlank()) emptyRichContent() else JsonUtils.plainTextToRichJson(text)
        val block = NoteBlock(
            id = nextTempId--,
            type = BlockType.NUMBER_LIST,
            content = content,
            extraData = NumberListMetadata.toJson(
                NumberListItem(text = content, order = order, listGroupId = listGroupId)
            ),
            position = position
        )
        _uiState.update { it.copy(blocks = it.blocks + block) }
        return block.id
    }

    fun insertNumberListBlocks(newBlocks: List<NoteBlock>, atPosition: Int) {
        if (newBlocks.isEmpty()) return
        _uiState.update { state ->
            val shiftBy = newBlocks.size
            val shifted = state.blocks.map { block ->
                if (block.position >= atPosition) {
                    block.copy(position = block.position + shiftBy)
                } else {
                    block
                }
            }
            state.copy(blocks = shifted + newBlocks)
        }
    }

    fun insertNumberListItems(
        items: List<String>,
        atPosition: Int,
        startOrder: Int = 1,
        listGroupId: Int = 0
    ): Long? {
        if (items.isEmpty()) return null
        val newBlocks = items.mapIndexed { index, text ->
            val order = startOrder + index
            val content = if (text.isBlank()) emptyRichContent() else JsonUtils.plainTextToRichJson(text)
            NoteBlock(
                id = nextTempId--,
                type = BlockType.NUMBER_LIST,
                content = content,
                extraData = NumberListMetadata.toJson(
                    NumberListItem(text = content, order = order, listGroupId = listGroupId)
                ),
                position = atPosition + index
            )
        }
        insertNumberListBlocks(newBlocks, atPosition)
        return newBlocks.last().id
    }

    fun updateNumberListBlock(blockId: Long, text: String, order: Int) {
        _uiState.update { state ->
            state.copy(
                blocks = state.blocks.map { block ->
                    if (block.id == blockId && block.type == BlockType.NUMBER_LIST) {
                        val metadata = NumberListMetadata.fromBlock(block)
                        block.copy(
                            content = text,
                            extraData = NumberListMetadata.toJson(
                                metadata.copy(text = text, order = order)
                            )
                        )
                    } else {
                        block
                    }
                }
            )
        }
    }

    fun normalizeNumberListOrders() {
        _uiState.update { state ->
            state.copy(blocks = com.notebook.notes.checklist.model.NumberListHelper.normalizeBlocks(state.blocks))
        }
    }

    fun addImageBlock(path: String, position: Int) {
        val block = NoteBlock(
            id = nextTempId--,
            type = BlockType.IMAGE,
            content = path,
            position = position
        )
        _uiState.update { it.copy(blocks = it.blocks + block) }
    }

    fun addDrawingBlock(path: String, position: Int) {
        val block = NoteBlock(
            id = nextTempId--,
            type = BlockType.DRAWING,
            content = path,
            position = position
        )
        _uiState.update { it.copy(blocks = it.blocks + block) }
    }

    fun updateDrawingBlock(blockId: Long, path: String) {
        _uiState.update { state ->
            state.copy(
                blocks = state.blocks.map { block ->
                    if (block.id == blockId && block.type == BlockType.DRAWING) {
                        block.copy(content = path)
                    } else block
                }
            )
        }
    }

    fun addFileBlock(path: String, fileName: String, position: Int) {
        addAttachmentBlock(
            metadata = AttachmentMetadata(
                fileName = fileName,
                mimeType = "application/octet-stream",
                fileSize = java.io.File(path).length(),
                category = com.notebook.notes.checklist.model.AttachmentCategory.UNKNOWN.name,
                displayName = fileName
            ),
            path = path,
            position = position
        )
    }

    fun addAttachmentBlock(metadata: AttachmentMetadata, path: String, position: Int) {
        val block = NoteBlock(
            id = nextTempId--,
            type = BlockType.ATTACHMENT,
            content = path,
            extraData = AttachmentMetadata.toJson(metadata),
            position = position
        )
        _uiState.update { it.copy(blocks = it.blocks + block) }
    }

    fun renameAttachmentBlock(blockId: Long, displayName: String) {
        _uiState.update { state ->
            state.copy(
                blocks = state.blocks.map { block ->
                    if (block.id == blockId && (block.type == BlockType.ATTACHMENT || block.type == BlockType.FILE)) {
                        val metadata = AttachmentMetadata.fromBlock(block)
                            ?.copy(displayName = displayName.trim())
                            ?: return@map block
                        block.copy(
                            type = BlockType.ATTACHMENT,
                            extraData = AttachmentMetadata.toJson(metadata)
                        )
                    } else block
                }
            )
        }
    }

    fun addLinkBlock(url: String, position: Int) {
        val block = NoteBlock(
            id = nextTempId--,
            type = BlockType.LINK,
            content = url,
            position = position
        )
        _uiState.update { it.copy(blocks = it.blocks + block) }
    }

    fun addVoiceBlock(
        path: String,
        durationMs: Long,
        fileSize: Long,
        displayName: String? = null,
        waveform: List<Float> = emptyList(),
        position: Int
    ) {
        val metadata = VoiceNoteMetadata(
            displayName = displayName,
            durationMs = durationMs,
            fileSize = fileSize,
            waveform = waveform
        )
        val block = NoteBlock(
            id = nextTempId--,
            type = BlockType.VOICE,
            content = path,
            extraData = VoiceNoteMetadata.toJson(metadata),
            position = position
        )
        _uiState.update { it.copy(blocks = it.blocks + block) }
    }

    fun renameVoiceBlock(blockId: Long, displayName: String) {
        _uiState.update { state ->
            state.copy(
                blocks = state.blocks.map { block ->
                    if (block.id == blockId && block.type == BlockType.VOICE) {
                        val metadata = VoiceNoteMetadata.fromJson(block.extraData)
                            .copy(displayName = displayName.trim())
                        block.copy(extraData = VoiceNoteMetadata.toJson(metadata))
                    } else block
                }
            )
        }
    }

    fun updateBlock(blockId: Long, content: String, checked: Boolean? = null) {
        _uiState.update { state ->
            state.copy(
                blocks = state.blocks.map { block ->
                    if (block.id == blockId) {
                        block.copy(
                            content = content,
                            checked = checked ?: block.checked
                        )
                    } else block
                }
            )
        }
    }

    fun removeBlock(blockId: Long) {
        val block = _uiState.value.blocks.find { it.id == blockId }
        if (block?.type == BlockType.VOICE && block.content.isNotBlank()) {
            voiceNoteRepository.deleteVoiceFile(block.content)
        }
        if (block?.type == BlockType.DRAWING && block.content.isNotBlank()) {
            drawingRepository.deleteDrawingFile(block.content)
        }
        if ((block?.type == BlockType.ATTACHMENT || block?.type == BlockType.FILE) && block.content.isNotBlank()) {
            attachmentRepository.deleteAttachmentFile(block.content)
            AttachmentMetadata.fromBlock(block)?.thumbnailPath?.let {
                attachmentRepository.deleteAttachmentFile(it)
            }
        }
        _uiState.update { state ->
            state.copy(blocks = state.blocks.filter { it.id != blockId })
        }
    }

    fun shiftPositionsFrom(fromPosition: Int, delta: Int) {
        if (delta == 0) return
        _uiState.update { state ->
            state.copy(
                blocks = state.blocks.map { block ->
                    if (block.position >= fromPosition) {
                        block.copy(position = block.position + delta)
                    } else {
                        block
                    }
                }
            )
        }
    }

    fun applyBlockPositions(positions: Map<Long, Int>) {
        if (positions.isEmpty()) return
        _uiState.update { state ->
            state.copy(
                blocks = state.blocks.map { block ->
                    positions[block.id]?.let { block.copy(position = it) } ?: block
                }
            )
        }
    }

    fun saveNote() {
        viewModelScope.launch {
            persistNoteInternal(closeAfterSave = true)
        }
    }

    suspend fun persistAndGetId(): Long = persistNoteInternal(closeAfterSave = false)

    private suspend fun persistNoteInternal(closeAfterSave: Boolean): Long {
        val state = _uiState.value
        val plainTitle = JsonUtils.blockContentPlainText(state.title)
        val note = Note(
            id = state.noteId ?: 0,
            title = if (plainTitle.isBlank() || NoteTitleHelper.isUntitledPlaceholder(plainTitle)) {
                JsonUtils.emptyRichJson(
                    fontSize = defaultFontSize(),
                    fontFamily = defaultFontFamily()
                )
            } else {
                state.title
            },
            categoryId = state.categoryId,
            color = state.color,
            listItemHeight = state.listItemHeight,
            isPinned = state.isPinned,
            isFavorite = state.isFavorite,
            isArchived = state.isArchived,
            isLocked = state.isLocked
        )
        val nonCheckboxBlocks = state.blocks.filter { it.type != BlockType.CHECKBOX }
        val normalizedBlocks = normalizeAllListBlocks(nonCheckboxBlocks)
        val savedId = repository.saveNoteWithBlocks(note, normalizedBlocks)
        _uiState.update { it.copy(noteId = savedId, isSaved = closeAfterSave) }
        return savedId
    }

    fun getFileStorageManager(): FileStorageManager = fileStorageManager

    private fun normalizeAllListBlocks(blocks: List<NoteBlock>): List<NoteBlock> {
        val withoutEmptyBullets = BulletListHelper.removeEmptyBlocks(blocks)
        return BulletListHelper.normalizeBlocks(
            com.notebook.notes.checklist.model.NumberListHelper.normalizeBlocks(withoutEmptyBullets)
        )
    }

    companion object {
        const val EXTRA_NOTE_ID = "note_id"
        const val EXTRA_CATEGORY_ID = "category_id"
        /** Set when the caller already verified lock credentials for this note open. */
        const val EXTRA_LOCK_AUTHENTICATED = "lock_authenticated"
    }
}
