package com.notebook.notes.checklist.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.notebook.notes.checklist.model.RepeatType
import com.notebook.notes.checklist.repository.NoteRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var noteRepository: NoteRepository
    @Inject lateinit var reminderScheduler: ReminderScheduler
    @Inject lateinit var notificationHelper: ReminderNotificationHelper

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_REMINDER) return
        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
        if (noteId <= 0L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val note = noteRepository.getNoteById(noteId) ?: return@launch
                if (!note.reminderEnabled) return@launch

                // Never load/show note content for locked notes in the notification path.
                val preview = if (note.isLocked) "" else noteRepository.getPreview(noteId)
                notificationHelper.showReminderNotification(note, preview)

                val repeatType = RepeatType.fromStorage(note.repeatType)
                when (repeatType) {
                    RepeatType.NEVER -> {
                        noteRepository.clearReminder(noteId, cancelAlarm = false)
                    }
                    RepeatType.DAILY, RepeatType.WEEKLY, RepeatType.MONTHLY -> {
                        val next = reminderScheduler.nextOccurrenceAfter(
                            reminderTimeMillis = note.reminderTimeMillis,
                            repeatType = repeatType
                        )
                        if (next != null) {
                            noteRepository.setReminder(
                                noteId = noteId,
                                timeMillis = next,
                                repeatType = repeatType
                            )
                        } else {
                            noteRepository.clearReminder(noteId, cancelAlarm = false)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REMINDER = "com.notebook.notes.checklist.action.NOTE_REMINDER"
        const val EXTRA_NOTE_ID = "extra_reminder_note_id"
    }
}
