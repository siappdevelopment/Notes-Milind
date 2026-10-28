package com.notebook.notes.checklist.util

import android.app.Activity
import android.app.Dialog
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.ui.richtext.EditorPanelAnimator

/**
 * Shared bottom-sheet presentation helpers for Create Notes.
 *
 * Hides the IME and waits until layout settles before showing a sheet, so the
 * sheet does not open above the keyboard and then jump to the bottom when the
 * keyboard dismisses.
 */
object BottomSheetHelper {

    fun showAfterImeHidden(activity: Activity, anchor: View, show: () -> Unit) {
        activity.currentFocus?.clearFocus()
        EditorPanelAnimator.hideKeyboardThen(activity, anchor) {
            if (activity.isFinishing) return@hideKeyboardThen
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed) {
                return@hideKeyboardThen
            }
            show()
        }
    }

    fun create(activity: Activity): BottomSheetDialog {
        return BottomSheetDialog(activity, R.style.ThemeOverlay_Notes_BottomSheetDialog)
    }

    /**
     * Transparent container, wrap-content height, and no system-bar bottom padding
     * so sheets hug their content with no empty white/transparent gap at the bottom.
     *
     * When [adjustForInput] is true, the sheet is lifted above the keyboard with a
     * bottom margin equal to the IME height. Do not use translationY —
     * [BottomSheetBehavior] owns it and will cover the sheet again.
     */
    fun prepare(dialog: Dialog?, adjustForInput: Boolean = false) {
        val sheetDialog = dialog ?: return
        SystemBars.hideNavigationBar(sheetDialog)

        sheetDialog.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        }

        if (sheetDialog is BottomSheetDialog) {
            sheetDialog.behavior.apply {
                isFitToContents = true
                skipCollapsed = true
                isGestureInsetBottomIgnored = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        val bottomSheet = sheetDialog.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        ) ?: return

        bottomSheet.setBackgroundResource(android.R.color.transparent)
        forceWrapContent(bottomSheet, bottomMargin = 0)
        stripBottomPadding(bottomSheet)
        sheetDialog.findViewById<View>(com.google.android.material.R.id.container)
            ?.let(::stripBottomPadding)
        sheetDialog.findViewById<View>(com.google.android.material.R.id.coordinator)
            ?.let(::stripBottomPadding)

        fun liftForIme(imeBottom: Int) {
            if (!adjustForInput) return
            applyImeBottomMargin(bottomSheet, imeBottom)
            if (sheetDialog is BottomSheetDialog) {
                sheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(bottomSheet) { view, insets ->
            stripBottomPadding(view)
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            liftForIme(imeBottom)

            WindowInsetsCompat.Builder(insets)
                .setInsets(
                    WindowInsetsCompat.Type.systemBars(),
                    Insets.of(bars.left, bars.top, bars.right, 0)
                )
                .setInsets(WindowInsetsCompat.Type.ime(), Insets.NONE)
                .build()
        }

        sheetDialog.window?.decorView?.let { decor ->
            ViewCompat.setOnApplyWindowInsetsListener(decor) { _, insets ->
                liftForIme(insets.getInsets(WindowInsetsCompat.Type.ime()).bottom)
                insets
            }
        }

        bottomSheet.post {
            stripBottomPadding(bottomSheet)
            val imeBottom = if (adjustForInput) currentImeBottom(bottomSheet, sheetDialog) else 0
            forceWrapContent(bottomSheet, bottomMargin = imeBottom)
            bottomSheet.requestLayout()
            ViewCompat.requestApplyInsets(bottomSheet)
            sheetDialog.window?.decorView?.let(ViewCompat::requestApplyInsets)
        }
        ViewCompat.requestApplyInsets(bottomSheet)
    }

    /** Public so input sheets can re-apply lift after showing the keyboard. */
    fun applyImeBottomMargin(bottomSheet: View, imeBottom: Int) {
        val lift = imeBottom.coerceAtLeast(0)
        val params = bottomSheet.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        if (params.bottomMargin == lift) return
        params.bottomMargin = lift
        bottomSheet.layoutParams = params
    }

    private fun currentImeBottom(bottomSheet: View, dialog: Dialog): Int {
        val fromSheet = ViewCompat.getRootWindowInsets(bottomSheet)
            ?.getInsets(WindowInsetsCompat.Type.ime())?.bottom
        if (fromSheet != null && fromSheet > 0) return fromSheet
        val decor = dialog.window?.decorView ?: return fromSheet ?: 0
        return ViewCompat.getRootWindowInsets(decor)
            ?.getInsets(WindowInsetsCompat.Type.ime())?.bottom
            ?: 0
    }

    private fun forceWrapContent(bottomSheet: View, bottomMargin: Int) {
        val params = bottomSheet.layoutParams ?: return
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        (params as? ViewGroup.MarginLayoutParams)?.apply {
            this.bottomMargin = bottomMargin
            leftMargin = 0
            rightMargin = 0
        }
        bottomSheet.layoutParams = params

        val content = (bottomSheet as? ViewGroup)?.getChildAt(0) ?: return
        val childParams = content.layoutParams
            ?: ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        childParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        content.layoutParams = childParams
    }

    private fun stripBottomPadding(view: View) {
        if (view.paddingBottom != 0) {
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, 0)
        }
    }
}
