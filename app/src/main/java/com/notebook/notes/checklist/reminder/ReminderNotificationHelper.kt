package com.notebook.notes.checklist.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.model.Note
import com.notebook.notes.checklist.ui.activity.CreateNotesActivity
import com.notebook.notes.checklist.ui.activity.SetPasswordActivity
import com.notebook.notes.checklist.util.DateTimeUtils
import com.notebook.notes.checklist.utils.JsonUtils
import com.notebook.notes.checklist.viewmodel.NoteEditViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_notification_channel),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.reminder_notification_channel_desc)
            enableVibration(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    fun showReminderNotification(note: Note, preview: String) {
        ensureChannel()
        DateTimeUtils.refresh(context)

        val notificationId = ReminderScheduler.requestCodeFor(note)
        val contentIntent = createContentIntent(note)
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
        )

        val title: String
        val bodyWithTime: String
        if (note.isLocked) {
            title = context.getString(R.string.locked_note)
            bodyWithTime = context.getString(R.string.locked_reminder_notification_message)
        } else {
            // Titles are stored as rich-text JSON; show plain text in the notification.
            title = JsonUtils.blockContentPlainText(note.title)
                .ifBlank { context.getString(R.string.untitled) }
            val body = preview.ifBlank { context.getString(R.string.reminder_notification_fallback) }
            val timeLabel = note.reminderTimeMillis.takeIf { it > 0L }
                ?.let { DateTimeUtils.formatDateTime(it) }
                .orEmpty()
            bodyWithTime = if (timeLabel.isNotEmpty()) {
                "$body\n$timeLabel"
            } else {
                body
            }
        }

        // Wake the display when the device screen is off so the reminder is visible.
        wakeDisplayIfOff()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_reminder)
            .setContentTitle(title)
            .setContentText(bodyWithTime)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyWithTime))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    /**
     * Turns the screen on briefly when a reminder fires while the display is off.
     */
    private fun wakeDisplayIfOff() {
        val powerManager = context.getSystemService(PowerManager::class.java) ?: return
        if (powerManager.isInteractive) return

        @Suppress("DEPRECATION")
        val wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE,
            WAKE_LOCK_TAG
        )
        wakeLock.setReferenceCounted(false)
        wakeLock.acquire(WAKE_DISPLAY_MS)
    }

    private fun createContentIntent(note: Note): Intent {
        return if (note.isLocked) {
            SetPasswordActivity.createVerifyAndOpenNoteIntent(context, note.id)
        } else {
            Intent(context, CreateNotesActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(NoteEditViewModel.EXTRA_NOTE_ID, note.id)
            }
        }
    }

    private fun immutableFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }

    companion object {
        const val CHANNEL_ID = "note_reminders"
        private const val WAKE_LOCK_TAG = "quicknotes:reminder_wake"
        private const val WAKE_DISPLAY_MS = 5_000L
    }
}
