package com.notebook.notes.checklist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.notebook.notes.checklist.model.NoteBlock
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteBlockDao {

    @Query("SELECT * FROM note_blocks WHERE noteId = :noteId ORDER BY position ASC")
    fun getBlocksForNote(noteId: Long): Flow<List<NoteBlock>>

    @Query("SELECT * FROM note_blocks WHERE noteId = :noteId ORDER BY position ASC")
    suspend fun getBlocksForNoteOnce(noteId: Long): List<NoteBlock>

    @Query("SELECT content FROM note_blocks WHERE noteId = :noteId AND type IN ('TEXT', 'CHECKBOX') ORDER BY position ASC LIMIT 1")
    suspend fun getPreviewForNote(noteId: Long): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlocks(blocks: List<NoteBlock>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlock(block: NoteBlock): Long

    @Query("DELETE FROM note_blocks WHERE noteId = :noteId")
    suspend fun deleteBlocksForNote(noteId: Long)
}
