package com.notebook.notes.checklist.repository

import android.content.Context
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.data.NoteBlockDao
import com.notebook.notes.checklist.data.NoteDao
import com.notebook.notes.checklist.model.Note
import com.notebook.notes.checklist.model.NoteBlock
import com.notebook.notes.checklist.model.RepeatType
import com.notebook.notes.checklist.reminder.ReminderScheduler
import com.notebook.notes.checklist.util.NoteTitleHelper
import com.notebook.notes.checklist.utils.JsonUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class NoteRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val noteDao: NoteDao,
    private val noteBlockDao: NoteBlockDao,
    private val attachmentRepository: AttachmentRepository,
    private val categoryRepository: CategoryRepository,
    private val todoRepository: TodoRepository,
    private val reminderScheduler: ReminderScheduler
) {

    fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()

    fun observeFavoriteNotesCount(): Flow<Int> = noteDao.observeFavoriteNotesCount()

    fun observeArchivedNotesCount(): Flow<Int> = noteDao.observeArchivedNotesCount()

    fun observeDeletedNotesCount(): Flow<Int> = noteDao.observeDeletedNotesCount()

    fun getDeletedNotes(): Flow<List<Note>> = noteDao.getDeletedNotes()

    fun searchDeletedNotes(query: String): Flow<List<Note>> {
        return if (query.isBlank()) {
            noteDao.getDeletedNotes()
        } else {
            noteDao.searchDeletedNotes(query)
        }
    }

    fun getArchivedNotes(): Flow<List<Note>> = noteDao.getArchivedNotes()

    fun searchArchivedNotes(query: String): Flow<List<Note>> {
        return if (query.isBlank()) {
            noteDao.getArchivedNotes()
        } else {
            noteDao.searchArchivedNotes(query)
        }
    }

    fun searchFavoriteNotes(query: String): Flow<List<Note>> {
        return if (query.isBlank()) {
            noteDao.getFavoriteNotes()
        } else {
            noteDao.searchFavoriteNotes(query)
        }
    }

    fun searchReminderNotes(query: String): Flow<List<Note>> {
        return if (query.isBlank()) {
            noteDao.getReminderNotes()
        } else {
            noteDao.searchReminderNotes(query)
        }
    }

    fun searchNotes(query: String): Flow<List<Note>> {
        return combine(
            categoryRepository.selectedFilterCategoryId,
            categoryRepository.allNotesCategoryId
        ) { selectedId, allNotesId -> selectedId to allNotesId }
            .flatMapLatest { (selectedId, allNotesId) ->
                val showAll = selectedId == null || selectedId == allNotesId
                when {
                    showAll && query.isBlank() -> noteDao.getAllNotes()
                    showAll -> noteDao.searchNotes(query)
                    query.isBlank() -> noteDao.getNotesByCategory(selectedId!!)
                    else -> noteDao.searchNotesByCategory(query, selectedId!!)
                }
            }
    }

    fun getBlocksForNote(noteId: Long): Flow<List<NoteBlock>> =
        noteBlockDao.getBlocksForNote(noteId)

    suspend fun getNoteById(noteId: Long): Note? = noteDao.getNoteById(noteId)

    suspend fun getBlocksOnce(noteId: Long): List<NoteBlock> =
        noteBlockDao.getBlocksForNoteOnce(noteId)

    suspend fun saveNoteWithBlocks(note: Note, blocks: List<NoteBlock>): Long {
        val now = System.currentTimeMillis()
        categoryRepository.ensureDefaultCategories()
        val defaultCategoryId = if (note.categoryId > 0) {
            note.categoryId
        } else {
            categoryRepository.getDefaultNoteCategoryId()
        }

        val noteToSave = if (note.id == 0L) {
            val nextPosition = noteDao.getMaxPosition() + 1
            note.copy(
                categoryId = defaultCategoryId,
                createdAt = now,
                updatedAt = now,
                position = nextPosition
            )
        } else {
            val existing = noteDao.getNoteById(note.id)
            note.copy(
                categoryId = defaultCategoryId,
                updatedAt = now,
                createdAt = existing?.createdAt ?: now,
                position = existing?.position ?: note.position,
                isPinned = existing?.isPinned ?: note.isPinned,
                isFavorite = existing?.isFavorite ?: note.isFavorite,
                isArchived = existing?.isArchived ?: note.isArchived,
                isDeleted = existing?.isDeleted ?: note.isDeleted,
                deletedAt = existing?.deletedAt ?: note.deletedAt,
                isLocked = existing?.isLocked ?: note.isLocked,
                color = note.color,
                listItemHeight = note.listItemHeight ?: existing?.listItemHeight,
                reminderEnabled = existing?.reminderEnabled ?: note.reminderEnabled,
                reminderTimeMillis = existing?.reminderTimeMillis ?: note.reminderTimeMillis,
                repeatType = existing?.repeatType ?: note.repeatType,
                notificationId = existing?.notificationId ?: note.notificationId
            )
        }

        val noteId = if (noteToSave.id == 0L) {
            noteDao.insertNote(noteToSave)
        } else {
            noteDao.updateNote(noteToSave)
            noteToSave.id
        }

        noteBlockDao.deleteBlocksForNote(noteId)
        if (blocks.isNotEmpty()) {
            val blocksToInsert = blocks.map { block ->
                block.copy(id = 0, noteId = noteId)
            }
            noteBlockDao.insertBlocks(blocksToInsert)
        }
        attachmentRepository.syncAttachmentsForNote(noteId, blocks)
        return noteId
    }

    suspend fun setReminder(
        noteId: Long,
        timeMillis: Long,
        repeatType: RepeatType
    ) {
        val note = noteDao.getNoteById(noteId) ?: return
        reminderScheduler.cancel(note)

        val resolvedTime = reminderScheduler.resolveNextTrigger(timeMillis, repeatType)
            ?: timeMillis
        val notificationId = ReminderScheduler.notificationIdFor(noteId)
        val updated = note.copy(
            reminderEnabled = true,
            reminderTimeMillis = resolvedTime,
            repeatType = repeatType.name,
            notificationId = notificationId,
            updatedAt = System.currentTimeMillis()
        )
        noteDao.updateReminder(
            noteId = noteId,
            reminderEnabled = true,
            reminderTimeMillis = resolvedTime,
            repeatType = repeatType.name,
            notificationId = notificationId,
            updatedAt = updated.updatedAt
        )
        reminderScheduler.schedule(updated)
    }

    suspend fun clearReminder(noteId: Long, cancelAlarm: Boolean = true) {
        val note = noteDao.getNoteById(noteId) ?: return
        if (cancelAlarm) {
            reminderScheduler.cancel(note)
        }
        noteDao.updateReminder(
            noteId = noteId,
            reminderEnabled = false,
            reminderTimeMillis = 0L,
            repeatType = RepeatType.NEVER.name,
            notificationId = 0,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun rescheduleAllReminders() {
        noteDao.getNotesWithActiveReminders().forEach { note ->
            reminderScheduler.schedule(note)
        }
    }

    suspend fun moveToRecycleBin(note: Note) {
        reminderScheduler.cancel(note)
        val now = System.currentTimeMillis()
        noteDao.updateDeleted(
            noteId = note.id,
            isDeleted = true,
            deletedAt = now,
            updatedAt = now
        )
    }

    suspend fun restoreNote(note: Note) {
        val now = System.currentTimeMillis()
        noteDao.updateDeleted(
            noteId = note.id,
            isDeleted = false,
            deletedAt = 0L,
            updatedAt = now
        )
        val restored = noteDao.getNoteById(note.id) ?: return
        if (restored.reminderEnabled && restored.reminderTimeMillis > 0) {
            reminderScheduler.schedule(restored)
        }
    }

    suspend fun permanentlyDeleteNote(note: Note) {
        reminderScheduler.cancel(note)
        noteDao.deleteNote(note)
    }

    suspend fun permanentlyDeleteAllDeletedNotes() {
        noteDao.getDeletedNotesOnce().forEach { note ->
            permanentlyDeleteNote(note)
        }
    }

    /**
     * Permanently removes notes that have been in the recycle bin longer than [RECYCLE_BIN_RETENTION_MS].
     */
    suspend fun purgeExpiredDeletedNotes() {
        val cutoff = System.currentTimeMillis() - RECYCLE_BIN_RETENTION_MS
        noteDao.getExpiredDeletedNotes(cutoff).forEach { note ->
            permanentlyDeleteNote(note)
        }
    }

    suspend fun deleteNote(note: Note) {
        moveToRecycleBin(note)
    }

    suspend fun togglePin(note: Note) {
        noteDao.updatePinned(
            noteId = note.id,
            isPinned = !note.isPinned,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun toggleFavorite(note: Note) {
        noteDao.updateFavorite(
            noteId = note.id,
            isFavorite = !note.isFavorite,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun setArchived(noteId: Long, isArchived: Boolean) {
        noteDao.updateArchived(
            noteId = noteId,
            isArchived = isArchived,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun toggleArchive(note: Note) {
        setArchived(note.id, !note.isArchived)
    }

    suspend fun duplicateNote(note: Note): Long {
        val blocks = noteBlockDao.getBlocksForNoteOnce(note.id)
        val now = System.currentTimeMillis()
        val newNote = note.copy(
            id = 0,
            title = titleWithCopySuffix(note.title),
            createdAt = now,
            updatedAt = now,
            position = noteDao.getMaxPosition() + 1,
            reminderEnabled = false,
            reminderTimeMillis = 0L,
            repeatType = RepeatType.NEVER.name,
            notificationId = 0
        )
        val newNoteId = saveNoteWithBlocks(newNote, blocks)
        todoRepository.duplicateListForNote(note.id, newNoteId)
        return newNoteId
    }

    /**
     * Titles are stored as rich-text JSON. Append the localized " (Copy)" suffix
     * to the last span so the document stays valid (string concat would corrupt the JSON).
     * If the title already ends with that suffix, leave it as-is so copying a
     * copy does not stack suffixes.
     */
    private fun titleWithCopySuffix(titleJson: String): String {
        val document = JsonUtils.jsonToRichText(titleJson)
        val suffix = context.getString(R.string.note_copy_suffix)
        val plain = document.plainText().trim()
        val (baseTitle, alreadyCopied) = NoteTitleHelper.stripCopySuffix(plain)

        if (alreadyCopied) {
            // Keep a single suffix; refresh untitled + copy language for display storage
            if (NoteTitleHelper.isUntitledPlaceholder(baseTitle)) {
                return JsonUtils.plainTextToRichJson(
                    context.getString(R.string.untitled) + suffix
                )
            }
            return titleJson
        }
        if (NoteTitleHelper.isUntitledPlaceholder(plain)) {
            return JsonUtils.plainTextToRichJson(
                context.getString(R.string.untitled) + suffix
            )
        }
        val spans = document.spans.toMutableList()
        if (spans.isEmpty()) {
            return JsonUtils.plainTextToRichJson(
                context.getString(R.string.untitled) + suffix
            )
        }
        val last = spans.last()
        spans[spans.lastIndex] = last.copy(text = last.text + suffix)
        return JsonUtils.richTextToJson(document.copy(spans = spans))
    }

    suspend fun setLocked(noteId: Long, isLocked: Boolean) {
        noteDao.updateLocked(
            noteId = noteId,
            isLocked = isLocked,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun unlockAllNotes(): Int {
        return noteDao.unlockAllNotes(updatedAt = System.currentTimeMillis())
    }

    suspend fun getPreview(noteId: Long): String {
        val raw = noteBlockDao.getPreviewForNote(noteId).orEmpty()
        return when {
            raw.trimStart().startsWith("[") || raw.trimStart().startsWith("{") ->
                JsonUtils.jsonToRichText(raw).plainText()
            else -> raw
        }
    }

    /**
     * Persists manual home-screen order after drag-and-drop.
     * Pinned and unpinned notes maintain separate position sequences.
     */
    suspend fun updateHomeNoteOrder(orderedCards: List<Pair<Long, Boolean>>) {
        if (orderedCards.isEmpty()) return
        var pinnedPosition = 0L
        var othersPosition = 0L
        val updates = orderedCards.map { (noteId, isPinned) ->
            val position = if (isPinned) pinnedPosition++ else othersPosition++
            noteId to position
        }
        noteDao.updatePositions(updates)
    }

    companion object {
        /** Notes stay in the recycle bin for 7 days, then are permanently deleted. */
        const val RECYCLE_BIN_RETENTION_MS = 7L * 24 * 60 * 60 * 1000
    }
}
