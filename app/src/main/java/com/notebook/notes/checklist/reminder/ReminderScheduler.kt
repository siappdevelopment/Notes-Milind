package com.notebook.notes.checklist.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.notebook.notes.checklist.model.Note
import com.notebook.notes.checklist.model.RepeatType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(note: Note) {
        if (!note.reminderEnabled || note.reminderTimeMillis <= 0L || note.id <= 0L) return

        val triggerAt = resolveNextTrigger(
            reminderTimeMillis = note.reminderTimeMillis,
            repeatType = RepeatType.fromStorage(note.repeatType)
        ) ?: return

        cancel(note)

        val pendingIntent = createAlarmPendingIntent(
            noteId = note.id,
            requestCode = requestCodeFor(note),
            flags = PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlag()
        )
        setExactAlarm(triggerAt, pendingIntent)
    }

    fun cancel(note: Note) {
        if (note.id <= 0L) return
        val requestCode = requestCodeFor(note)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            alarmIntent(note.id),
            PendingIntent.FLAG_NO_CREATE or pendingIntentImmutableFlag()
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun resolveNextTrigger(
        reminderTimeMillis: Long,
        repeatType: RepeatType,
        now: Long = System.currentTimeMillis()
    ): Long? {
        if (reminderTimeMillis > now) return reminderTimeMillis
        return when (repeatType) {
            RepeatType.NEVER -> null
            RepeatType.DAILY -> advanceWhilePast(reminderTimeMillis, now) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            RepeatType.WEEKLY -> advanceWhilePast(reminderTimeMillis, now) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
            RepeatType.MONTHLY -> advanceWhilePast(reminderTimeMillis, now) {
                add(Calendar.MONTH, 1)
            }
        }
    }

    fun nextOccurrenceAfter(
        reminderTimeMillis: Long,
        repeatType: RepeatType
    ): Long? {
        val calendar = Calendar.getInstance().apply { timeInMillis = reminderTimeMillis }
        when (repeatType) {
            RepeatType.NEVER -> return null
            RepeatType.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            RepeatType.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            RepeatType.MONTHLY -> calendar.add(Calendar.MONTH, 1)
        }
        return calendar.timeInMillis
    }

    private fun advanceWhilePast(
        startMillis: Long,
        now: Long,
        advance: Calendar.() -> Unit
    ): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = startMillis }
        var guard = 0
        while (calendar.timeInMillis <= now && guard < 10_000) {
            calendar.advance()
            guard++
        }
        return calendar.timeInMillis
    }

    private fun setExactAlarm(triggerAt: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun createAlarmPendingIntent(
        noteId: Long,
        requestCode: Int,
        flags: Int
    ): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            alarmIntent(noteId),
            flags
        )
    }

    private fun alarmIntent(noteId: Long): Intent {
        return Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_REMINDER
            putExtra(ReminderAlarmReceiver.EXTRA_NOTE_ID, noteId)
        }
    }

    private fun pendingIntentImmutableFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }

    companion object {
        fun requestCodeFor(note: Note): Int {
            val stored = note.notificationId
            if (stored != 0) return stored
            return notificationIdFor(note.id)
        }

        fun notificationIdFor(noteId: Long): Int {
            val id = (noteId and 0x7FFFFFFF).toInt()
            return if (id == 0) 1 else id
        }
    }
}
