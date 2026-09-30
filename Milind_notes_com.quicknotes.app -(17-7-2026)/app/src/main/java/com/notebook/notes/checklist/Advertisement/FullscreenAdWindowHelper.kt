package com.notebook.notes.checklist.Advertisement

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.notebook.notes.checklist.R
import java.lang.ref.WeakReference

/**
 * Edge-to-edge (transparent status bar) causes AdMob Interstitial / Rewarded ads to show a
 * blank black strip in the status-bar area. Temporarily exit edge-to-edge before show,
 * restore after dismiss/fail.
 */
object FullscreenAdWindowHelper {

    @JvmStatic
    @Volatile
    private var preparedCount = 0

    @JvmStatic
    private var activityRef: WeakReference<Activity>? = null

    @JvmStatic
    fun prepare(activity: Activity?) {
        if (activity == null || activity.isFinishing || activity.isDestroyed) return
        activityRef = WeakReference(activity)
        preparedCount++
        if (preparedCount > 1) return

        activity.runOnUiThread {
            if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
            val window = activity.window
            WindowCompat.setDecorFitsSystemWindows(window, true)
            window.statusBarColor = Color.BLACK
            window.navigationBarColor = Color.BLACK
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes = window.attributes.apply {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
            @Suppress("DEPRECATION")
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

            WindowCompat.getInsetsController(window, window.decorView).apply {
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.statusBars())
                isAppearanceLightStatusBars = false
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility =
                    (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            }
        }
    }

    @JvmStatic
    fun restore(activity: Activity?) {
        val target = activity
            ?: activityRef?.get()
            ?: return
        if (preparedCount > 0) preparedCount--
        if (preparedCount > 0) return
        preparedCount = 0

        if (target.isFinishing || target.isDestroyed) return
        target.runOnUiThread {
            if (target.isFinishing || target.isDestroyed) return@runOnUiThread
            val window = target.window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor =
                target.getColor(R.color.bg_color)

            WindowCompat.getInsetsController(window, window.decorView).apply {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                show(WindowInsetsCompat.Type.statusBars())
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = true
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
            window.decorView.requestApplyInsets()
        }
        activityRef = null
    }
}
