package com.notebook.notes.checklist.helper

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    enum class PermissionStatus {
        GRANTED,
        DENIED,
        PERMANENTLY_DENIED
    }

    /**
     * Check Phone Call Permission
     */
    fun checkPhoneCallPermission(activity: Activity): PermissionStatus {
        return getPermissionStatus(activity, Manifest.permission.CALL_PHONE)
    }

    /**
     * Check Phone State Permission
     */
    fun checkPhoneStatePermission(activity: Activity): PermissionStatus {
        return getPermissionStatus(activity, Manifest.permission.READ_PHONE_STATE)
    }

    /**
     * Check Notification Permission (Android 13+)
     */
    fun checkNotificationPermission(activity: Activity): PermissionStatus {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getPermissionStatus(activity, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            PermissionStatus.GRANTED
        }
    }

    /**
     * Check Schedule Exact Alarm Permission (Android 12+)
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }
    }

    /**
     * True when notification + exact-alarm permissions needed for reminders are granted.
     */
    fun hasReminderPermissions(activity: Activity): Boolean {
        return checkNotificationPermission(activity) == PermissionStatus.GRANTED &&
            canScheduleExactAlarms()
    }

    /**
     * Check Display Over Other Apps (Overlay)
     */
    fun isOverlayPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * Check Storage Permission (Handles Android 13+ Media Permissions)
     */
    fun checkStoragePermission(activity: Activity): PermissionStatus {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // On Android 13+, check READ_MEDIA_IMAGES as a representative for storage
            getPermissionStatus(activity, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            getPermissionStatus(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    /**
     * Check Microphone Permission
     */
    fun checkMicrophonePermission(activity: Activity): PermissionStatus {
        return getPermissionStatus(activity, Manifest.permission.RECORD_AUDIO)
    }

    /**
     * Check Camera Permission
     */
    fun checkCameraPermission(activity: Activity): PermissionStatus {
        return getPermissionStatus(activity, Manifest.permission.CAMERA)
    }

    /**
     * Helper to determine the status of a specific permission
     */
    private fun getPermissionStatus(activity: Activity, permission: String): PermissionStatus {
        return when {
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED -> {
                PermissionStatus.GRANTED
            }
            else -> {
                PermissionStatus.DENIED
            }
        }
    }

    /**
     * Open App Settings to allow user to manually enable permissions
     */
    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", context.packageName, null)
        intent.data = uri
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Open Overlay Settings specifically for "Display Over Other Apps"
     */
    fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    /**
     * Intent to request Schedule Exact Alarm permission (Android 12+).
     */
    fun createExactAlarmSettingsIntent(): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:${context.packageName}")
        )
    }
}
