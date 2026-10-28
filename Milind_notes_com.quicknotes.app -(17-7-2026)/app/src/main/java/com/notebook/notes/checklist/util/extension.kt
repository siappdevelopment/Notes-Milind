package com.notebook.notes.checklist.util

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import androidx.core.view.WindowCompat

object extension {

    fun Activity.changeNavigationBarToWhite() {
        val window = this.window

        // 1. Set the navigation bar background color to White
        window.navigationBarColor = Color.WHITE

        // 2. Make the navigation bar icons dark (gray/black)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Modern approach (API 30+)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.isAppearanceLightNavigationBars = true
        } else {
            // Legacy approach (API 26 to 29)
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }
}