package com.notebook.notes.checklist.util

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.Window
import android.widget.PopupWindow
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

object SystemBars {

    fun hideNavigationBar(window: Window) {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    fun hideNavigationBar(view: View) {
        ViewCompat.getWindowInsetsController(view)?.apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    fun hideNavigationBar(context: Context) {
        context.findActivity()?.window?.let(::hideNavigationBar)
    }

    fun hideNavigationBar(dialog: Dialog) {
        dialog.window?.let(::hideNavigationBar)
    }

    /** Call from [androidx.fragment.app.DialogFragment.onStart]. */
    fun onDialogFragmentStarted(dialog: Dialog?) {
        dialog?.window?.let(::hideNavigationBar)
    }

    /** Call from [androidx.fragment.app.DialogFragment.onDismiss]. */
    fun onDialogFragmentDismissed(activityWindow: Window?) {
        activityWindow?.let(::hideNavigationBar)
    }
}

/**
 * Focusable [PopupWindow]s create a new window that exits immersive mode.
 * Re-hides the nav bar on the popup and restores it on the host when dismissed.
 *
 * Call this before [PopupWindow.showAsDropDown] / [PopupWindow.showAtLocation].
 */
fun PopupWindow.keepNavigationBarHidden(context: Context) {
    val hostWindow = context.findActivity()?.window
    val applyHide = Runnable {
        contentView?.let { content ->
            SystemBars.hideNavigationBar(content)
            SystemBars.hideNavigationBar(content.rootView)
        }
        hostWindow?.let(SystemBars::hideNavigationBar)
    }

    contentView?.addOnAttachStateChangeListener(
        object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.post(applyHide)
            }

            override fun onViewDetachedFromWindow(v: View) {
                hostWindow?.let(SystemBars::hideNavigationBar)
            }
        }
    )

    setOnDismissListener {
        hostWindow?.let(SystemBars::hideNavigationBar)
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}
