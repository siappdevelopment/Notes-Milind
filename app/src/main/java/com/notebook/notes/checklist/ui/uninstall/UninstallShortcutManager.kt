package com.notebook.notes.checklist.ui.uninstall

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.ads.AppConstants
import com.notebook.notes.checklist.ui.activity.SplashActivity

/**
 * Long-press "Uninstall" launcher shortcut. It never opens its target directly: it launches
 * [SplashActivity] with [AppConstants.FROM_SHORTCUT], so ads/config/consent initialise first and
 * Splash then routes to [ConfirmUninstallActivity].
 */
object UninstallShortcutManager {

    private const val TAG = "UninstallShortcut"

    /** Replaces all dynamic shortcuts with the uninstall one. No-op below API 25. */
    fun initShortcut(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        try {
            val appContext = context.applicationContext
            val intent = Intent(appContext, SplashActivity::class.java).apply {
                action = ACTION_SHORTCUT_UNINSTALL
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(AppConstants.FROM_SHORTCUT, AppConstants.ACTION_OPEN_UNINSTALL)
            }
            val shortcut = ShortcutInfoCompat.Builder(appContext, AppConstants.ACTION_OPEN_UNINSTALL)
                .setShortLabel(appContext.getString(R.string.txt_uninstall))
                .setLongLabel(appContext.getString(R.string.txt_uninstall))
                .setIcon(IconCompat.createWithResource(appContext, R.drawable.ic_trash))
                .setRank(1)
                .setIntent(intent)
                .build()
            ShortcutManagerCompat.setDynamicShortcuts(appContext, listOf(shortcut))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register uninstall shortcut", e)
        }
    }

    private const val ACTION_SHORTCUT_UNINSTALL = "android.intent.action.SHORTCUT_UNINSTALL_APP"
}
