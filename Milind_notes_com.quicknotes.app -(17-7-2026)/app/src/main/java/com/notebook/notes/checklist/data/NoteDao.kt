package com.notebook.notes.checklist.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.notebook.notes.checklist.model.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE isArchived = 0 AND isDeleted = 0 ORDER BY isPinned DESC, position ASC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 AND isDeleted = 0 ORDER BY isPinned DESC, position ASC")
    fun getArchivedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isDeleted = 1")
    suspend fun getDeletedNotesOnce(): List<Note>

    @Query(
        """
        SELECT * FROM notes
        WHERE isDeleted = 1
        AND deletedAt > 0
        AND deletedAt <= :cutoffMillis
        """
    )
    suspend fun getExpiredDeletedNotes(cutoffMillis: Long): List<Note>

    @Query("SELECT * FROM notes WHERE isFavorite = 1 AND isArchived = 0 AND isDeleted = 0 ORDER BY isPinned DESC, position ASC")
    fun getFavoriteNotes(): Flow<List<Note>>

    @Query(
        """
        SELECT * FROM notes
        WHERE reminderEnabled = 1
        AND reminderTimeMillis > 0
        AND isArchived = 0
        AND isDeleted = 0
        ORDER BY reminderTimeMillis ASC
        """
    )
    fun getReminderNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE categoryId = :categoryId AND isArchived = 0 AND isDeleted = 0 ORDER BY isPinned DESC, position ASC")
    fun getNotesByCategory(categoryId: Long): Flow<List<Note>>

    @Query("""
        SELECT * FROM notes
        WHERE categoryId = :categoryId
        AND isArchived = 0
        AND isDeleted = 0
        AND (
            title LIKE '%' || :query || '%'
            OR id IN (
                SELECT noteId FROM note_blocks
                WHERE content LIKE '%' || :query || '%'
                AND type IN ('TEXT', 'CHECKBOX', 'NUMBER_LIST', 'BULLET_LIST')
            )
            OR id IN (
                SELECT tl.noteId FROM todo_lists tl
                INNER JOIN todo_items ti ON ti.listId = tl.id
                WHERE tl.noteId IS NOT NULL
                AND ti.text LIKE '%' || :query || '%'
            )
        )
        ORDER BY isPinned DESC, position ASC
    """)
    fun searchNotesByCategory(query: String, categoryId: Long): Flow<List<Note>>

    @Query("""
        SELECT * FROM notes
        WHERE isArchived = 0
        AND isDeleted = 0
        AND (
            title LIKE '%' || :query || '%'
            OR id IN (
                SELECT noteId FROM note_blocks
                WHERE content LIKE '%' || :query || '%'
                AND type IN ('TEXT', 'CHECKBOX', 'NUMBER_LIST', 'BULLET_LIST')
            )
            OR id IN (
                SELECT tl.noteId FROM todo_lists tl
                INNER JOIN todo_items ti ON ti.listId = tl.id
                WHERE tl.noteId IS NOT NULL
                AND ti.text LIKE '%' || :query || '%'
            )
        )
        ORDER BY isPinned DESC, position ASC
    """)
    fun searchNotes(query: String): Flow<List<Note>>

    @Query("""
        SELECT * FROM notes
        WHERE isArchived = 1
        AND isDeleted = 0
        AND (
            title LIKE '%' || :query || '%'
            OR id IN (
                SELECT noteId FROM note_blocks
                WHERE content LIKE '%' || :query || '%'
                AND type IN ('TEXT', 'CHECKBOX', 'NUMBER_LIST', 'BULLET_LIST')
            )
            OR id IN (
                SELECT tl.noteId FROM todo_lists tl
                INNER JOIN todo_items ti ON ti.listId = tl.id
                WHERE tl.noteId IS NOT NULL
                AND ti.text LIKE '%' || :query || '%'
            )
        )
        ORDER BY isPinned DESC, position ASC
    """)
    fun searchArchivedNotes(query: String): Flow<List<Note>>

    @Query("""
        SELECT * FROM notes
        WHERE isDeleted = 1
        AND (
            title LIKE '%' || :query || '%'
            OR id IN (
                SELECT noteId FROM note_blocks
                WHERE content LIKE '%' || :query || '%'
                AND type IN ('TEXT', 'CHECKBOX', 'NUMBER_LIST', 'BULLET_LIST')
            )
            OR id IN (
                SELECT tl.noteId FROM todo_lists tl
                INNER JOIN todo_items ti ON ti.listId = tl.id
                WHERE tl.noteId IS NOT NULL
                AND ti.text LIKE '%' || :query || '%'
            )
        )
        ORDER BY deletedAt DESC
    """)
    fun searchDeletedNotes(query: String): Flow<List<Note>>

    @Query("""
        SELECT * FROM notes
        WHERE isFavorite = 1
        AND isArchived = 0
        AND isDeleted = 0
        AND (
            title LIKE '%' || :query || '%'
            OR id IN (
                SELECT noteId FROM note_blocks
                WHERE content LIKE '%' || :query || '%'
                AND type IN ('TEXT', 'CHECKBOX', 'NUMBER_LIST', 'BULLET_LIST')
            )
            OR id IN (
                SELECT tl.noteId FROM todo_lists tl
                INNER JOIN todo_items ti ON ti.listId = tl.id
                WHERE tl.noteId IS NOT NULL
                AND ti.text LIKE '%' || :query || '%'
            )
        )
        ORDER BY isPinned DESC, position ASC
    """)
    fun searchFavoriteNotes(query: String): Flow<List<Note>>

    @Query("""
        SELECT * FROM notes
        WHERE reminderEnabled = 1
        AND reminderTimeMillis > 0
        AND isArchived = 0
        AND isDeleted = 0
        AND (
            title LIKE '%' || :query || '%'
            OR id IN (
                SELECT noteId FROM note_blocks
                WHERE content LIKE '%' || :query || '%'
                AND type IN ('TEXT', 'CHECKBOX', 'NUMBER_LIST', 'BULLET_LIST')
            )
            OR id IN (
                SELECT tl.noteId FROM todo_lists tl
                INNER JOIN todo_items ti ON ti.listId = tl.id
                WHERE tl.noteId IS NOT NULL
                AND ti.text LIKE '%' || :query || '%'
            )
        )
        ORDER BY reminderTimeMillis ASC
    """)
    fun searchReminderNotes(query: String): Flow<List<Note>>

    @Query("SELECT COALESCE(MAX(position), -1) FROM notes")
    suspend fun getMaxPosition(): Long

    @Query("UPDATE notes SET position = :position, updatedAt = :updatedAt WHERE id = :noteId")
    suspend fun updatePosition(noteId: Long, position: Long, updatedAt: Long)

    @Transaction
    suspend fun updatePositions(updates: List<Pair<Long, Long>>) {
        val now = System.currentTimeMillis()
        updates.forEach { (noteId, position) ->
            updatePosition(noteId, position, now)
        }
    }

    @Query("UPDATE notes SET categoryId = :toCategoryId, updatedAt = :updatedAt WHERE categoryId = :fromCategoryId")
    suspend fun reassignNotesCategory(fromCategoryId: Long, toCategoryId: Long, updatedAt: Long)

    @Query("SELECT * FROM notes WHERE categoryId = :categoryId")
    suspend fun getNotesForCategory(categoryId: Long): List<Note>

    @Query("DELETE FROM notes WHERE categoryId = :categoryId")
    suspend fun deleteNotesByCategory(categoryId: Long)

    @Query(
        """
        UPDATE notes SET isLocked = 1, updatedAt = :updatedAt
        WHERE categoryId = :categoryId AND isLocked = 0 AND isDeleted = 0
        """
    )
    suspend fun lockNotesByCategory(categoryId: Long, updatedAt: Long): Int

    @Query(
        """
        UPDATE notes SET isLocked = 0, updatedAt = :updatedAt
        WHERE categoryId = :categoryId AND isLocked = 1 AND isDeleted = 0
        """
    )
    suspend fun unlockNotesByCategory(categoryId: Long, updatedAt: Long): Int

    @Query("SELECT COUNT(*) FROM notes WHERE categoryId = :categoryId AND isDeleted = 0")
    suspend fun countNotesForCategory(categoryId: Long): Int

    @Query("SELECT COUNT(*) FROM notes WHERE categoryId = :categoryId AND isLocked = 1 AND isDeleted = 0")
    suspend fun countLockedNotesForCategory(categoryId: Long): Int

    @Query(
        """
        SELECT COUNT(*) FROM notes
        WHERE isFavorite = 1 AND isArchived = 0 AND isDeleted = 0
        """
    )
    fun observeFavoriteNotesCount(): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM notes
        WHERE isArchived = 1 AND isDeleted = 0
        """
    )
    fun observeArchivedNotesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notes WHERE isDeleted = 1")
    fun observeDeletedNotesCount(): Flow<Int>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: Long): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("UPDATE notes SET isPinned = :isPinned, updatedAt = :updatedAt WHERE id = :noteId")
    suspend fun updatePinned(noteId: Long, isPinned: Boolean, updatedAt: Long)

    @Query("UPDATE notes SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :noteId")
    suspend fun updateFavorite(noteId: Long, isFavorite: Boolean, updatedAt: Long)

    @Query("UPDATE notes SET isArchived = :isArchived, updatedAt = :updatedAt WHERE id = :noteId")
    suspend fun updateArchived(noteId: Long, isArchived: Boolean, updatedAt: Long)

    @Query(
        """
        UPDATE notes SET isDeleted = :isDeleted, deletedAt = :deletedAt, updatedAt = :updatedAt
        WHERE id = :noteId
        """
    )
    suspend fun updateDeleted(noteId: Long, isDeleted: Boolean, deletedAt: Long, updatedAt: Long)

    @Query("UPDATE notes SET isLocked = :isLocked, updatedAt = :updatedAt WHERE id = :noteId")
    suspend fun updateLocked(noteId: Long, isLocked: Boolean, updatedAt: Long)

    @Query(
        """
        UPDATE notes SET isLocked = 0, updatedAt = :updatedAt
        WHERE isLocked = 1 AND isDeleted = 0
        """
    )
    suspend fun unlockAllNotes(updatedAt: Long): Int

    @Query(
        """
        UPDATE notes SET
            reminderEnabled = :reminderEnabled,
            reminderTimeMillis = :reminderTimeMillis,
            repeatType = :repeatType,
            notificationId = :notificationId,
            updatedAt = :updatedAt
        WHERE id = :noteId
        """
    )
    suspend fun updateReminder(
        noteId: Long,
        reminderEnabled: Boolean,
        reminderTimeMillis: Long,
        repeatType: String,
        notificationId: Int,
        updatedAt: Long
    )

    @Query(
        """
        SELECT * FROM notes
        WHERE reminderEnabled = 1
        AND reminderTimeMillis > 0
        AND isDeleted = 0
        """
    )
    suspend fun getNotesWithActiveReminders(): List<Note>
}
