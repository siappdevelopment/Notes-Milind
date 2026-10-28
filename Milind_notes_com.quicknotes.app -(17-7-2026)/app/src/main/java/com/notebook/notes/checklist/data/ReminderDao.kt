package com.notebook.notes.checklist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.notebook.notes.checklist.model.Reminder

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders WHERE noteId = :noteId LIMIT 1")
    suspend fun getReminderForNote(noteId: Long): Reminder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Query("DELETE FROM reminders WHERE noteId = :noteId")
    suspend fun deleteReminderForNote(noteId: Long)
}
