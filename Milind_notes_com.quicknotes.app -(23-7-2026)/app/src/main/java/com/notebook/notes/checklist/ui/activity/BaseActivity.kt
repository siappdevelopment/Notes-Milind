package com.notebook.notes.checklist.ui.activity

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.notebook.notes.checklist.R

abstract class BaseActivity : AppCompatActivity() {

    @get:ColorRes
    protected open val statusBarColorRes: Int = R.color.white

    @get:ColorRes
    protected open val navigationBarColorRes: Int = R.color.white

    protected open val useLightStatusBarIcons: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        setupSystemBars()
        super.onCreate(savedInstanceState)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Re-apply even when focus is lost (e.g. PopupWindow / Dialog), otherwise the
        // system navigation bar stays visible for the lifetime of that overlay.
        hideNavigationBar()
    }

    protected fun setupSystemBars() {
        val statusColor = ContextCompat.getColor(this, statusBarColorRes)
        val navigationColor = ContextCompat.getColor(this, navigationBarColorRes)

        val statusBarStyle = if (useLightStatusBarIcons) {
            SystemBarStyle.light(statusColor, statusColor)
        } else {
            SystemBarStyle.dark(statusColor)
        }
        val navigationBarStyle = if (useLightStatusBarIcons) {
            SystemBarStyle.light(navigationColor, navigationColor)
        } else {
            SystemBarStyle.dark(navigationColor)
        }

        enableEdgeToEdge(
            statusBarStyle = statusBarStyle,
            navigationBarStyle = navigationBarStyle
        )

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = useLightStatusBarIcons
            isAppearanceLightNavigationBars = useLightStatusBarIcons
            show(WindowInsetsCompat.Type.statusBars())
        }
        hideNavigationBar()
    }

    protected fun hideNavigationBar() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
