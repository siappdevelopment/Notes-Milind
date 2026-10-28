package com.notebook.notes.checklist.ui.richtext

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.notebook.notes.checklist.R

/**
 * Smooth show/hide for editor bottom panels without layout zigzag.
 */
object EditorPanelAnimator {
    private const val DURATION_MS = 220L
    /** Typical IME animation length; used as fallback if onEnd is missed. */
    private const val IME_ANIM_FALLBACK_MS = 420L
    private val interpolator = FastOutSlowInInterpolator()

    /**
     * Hides the soft keyboard with the system IME slide animation, waits until
     * [settleView] (usually the rich-text toolbar) has finished moving down,
     * then runs [onReady].
     */
    fun hideKeyboardThen(
        activity: Activity,
        anchor: View,
        settleView: View = resolveToolbar(anchor) ?: anchor,
        onReady: () -> Unit
    ) {
        val imeVisible = ViewCompat.getRootWindowInsets(anchor)
            ?.isVisible(WindowInsetsCompat.Type.ime()) == true

        if (!imeVisible) {
            waitUntilSettled(settleView, onReady)
            return
        }

        var finished = false
        fun complete() {
            if (finished) return
            finished = true
            waitUntilSettled(settleView, onReady)
        }

        val content = activity.findViewById<View>(android.R.id.content) ?: anchor
        ViewCompat.setWindowInsetsAnimationCallback(
            content,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat = insets

                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    if (animation.typeMask and WindowInsetsCompat.Type.ime() == 0) return
                    ViewCompat.setWindowInsetsAnimationCallback(content, null)
                    complete()
                }
            }
        )

        hideKeyboardAnimated(activity, anchor)

        content.postDelayed({
            ViewCompat.setWindowInsetsAnimationCallback(content, null)
            complete()
        }, IME_ANIM_FALLBACK_MS)
    }

    /**
     * Hides the IME using the system animated path when available.
     * Avoids [InputMethodManager.hideSoftInputFromWindow] on API 30+ because
     * it often dismisses instantly and cancels the insets animation.
     */
    fun hideKeyboardAnimated(activity: Activity, anchor: View) {
        val controller = WindowCompat.getInsetsController(activity.window, anchor)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            controller.hide(WindowInsetsCompat.Type.ime())
            return
        }
        val imm = activity.getSystemService(InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(anchor.windowToken, 0)
    }

    /**
     * Shows the IME after a prior [hideKeyboardAnimated] call.
     * On API 30+, [WindowInsetsController] hide can suppress IME until explicitly shown again.
     */
    fun showKeyboard(activity: Activity, target: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.getInsetsController(activity.window, target)
                .show(WindowInsetsCompat.Type.ime())
        }
        val imm = activity.getSystemService(InputMethodManager::class.java)
        imm?.showSoftInput(target, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun waitUntilSettled(view: View, onSettled: () -> Unit) {
        var lastBottom = Int.MIN_VALUE
        var stableFrames = 0
        val maxFrames = 36

        val check = object : Runnable {
            private var frames = 0
            override fun run() {
                val location = IntArray(2)
                view.getLocationOnScreen(location)
                val bottom = location[1] + view.height
                frames++

                if (bottom == lastBottom) {
                    stableFrames++
                } else {
                    stableFrames = 0
                    lastBottom = bottom
                }

                val imeGone = ViewCompat.getRootWindowInsets(view)
                    ?.isVisible(WindowInsetsCompat.Type.ime()) != true

                if ((imeGone && stableFrames >= 2) || frames >= maxFrames) {
                    onSettled()
                } else {
                    view.postOnAnimation(this)
                }
            }
        }
        view.postOnAnimation(check)
    }

    private fun resolveToolbar(anchor: View): View? {
        val parent = anchor.parent as? ViewGroup ?: return null
        return parent.findViewById(R.id.richTextToolbar)
    }

    fun show(panel: View, animate: Boolean = true, onShown: (() -> Unit)? = null) {
        panel.animate().cancel()
        if (panel.isVisible && panel.alpha >= 1f && panel.translationY == 0f) {
            onShown?.invoke()
            return
        }

        if (!animate) {
            panel.alpha = 1f
            panel.translationY = 0f
            panel.visibility = View.VISIBLE
            onShown?.invoke()
            return
        }

        val parent = panel.parent as? ViewGroup
        if (parent != null) {
            TransitionManager.beginDelayedTransition(parent, layoutTransition())
        }

        panel.alpha = 0f
        panel.translationY = slideDistance(panel)
        panel.visibility = View.VISIBLE
        panel.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(DURATION_MS)
            .setInterpolator(interpolator)
            .withEndAction { onShown?.invoke() }
            .start()
    }

    fun hide(
        panel: View,
        animate: Boolean = true,
        onHidden: (() -> Unit)? = null
    ) {
        panel.animate().cancel()
        if (!panel.isVisible) {
            reset(panel)
            onHidden?.invoke()
            return
        }

        if (!animate) {
            reset(panel)
            panel.visibility = View.GONE
            onHidden?.invoke()
            return
        }

        val parent = panel.parent as? ViewGroup
        if (parent != null) {
            TransitionManager.beginDelayedTransition(parent, layoutTransition())
        }

        panel.animate()
            .alpha(0f)
            .translationY(slideDistance(panel))
            .setDuration(DURATION_MS)
            .setInterpolator(interpolator)
            .withEndAction {
                reset(panel)
                panel.visibility = View.GONE
                onHidden?.invoke()
            }
            .start()
    }

    private fun reset(panel: View) {
        panel.alpha = 1f
        panel.translationY = 0f
    }

    private fun slideDistance(panel: View): Float =
        20f * panel.resources.displayMetrics.density

    private fun layoutTransition() = AutoTransition().apply {
        duration = DURATION_MS
        interpolator = EditorPanelAnimator.interpolator
        excludeTarget(RecyclerView::class.java, true)
    }
}
