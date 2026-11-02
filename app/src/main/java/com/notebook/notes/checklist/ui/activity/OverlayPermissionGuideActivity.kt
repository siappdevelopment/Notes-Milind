package com.notebook.notes.checklist.ui.activity

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.notebook.notes.checklist.Advertisement.ADSAppManage
import com.notebook.notes.checklist.R
import java.lang.ref.WeakReference

/**
 * Small guide card shown over the system "Display over other apps" Settings screen: app icon +
 * name and an animated hand turning the toggle on.
 *
 * The window is not focusable and taps outside it go straight through to Settings underneath;
 * any tap also closes the card, as do the timeout and the overlay grant.
 */
class OverlayPermissionGuideActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var toggleAnimator: ValueAnimator? = null

    private val autoHide = Runnable { if (!isFinishing && !isDestroyed) finish() }

    private val grantCheck = object : Runnable {
        override fun run() {
            if (isFinishing || isDestroyed) return
            if (isOverlayGranted(this@OverlayPermissionGuideActivity)) {
                finish()
            } else {
                handler.postDelayed(this, CHECK_INTERVAL_MS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_overlay_permission_guide)

        bindAppInfo(window.decorView)
        // A newer card replaces any older, covered one.
        current?.get()?.takeIf { it !== this }?.finish()
        current = WeakReference(this)

        // Taps outside the card still reach Settings; they also arrive here as ACTION_OUTSIDE.
        window.addFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
        )
        window.attributes = window.attributes.apply {
            gravity = Gravity.BOTTOM
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
        }

        toggleAnimator = buildToggleAnimator(window.decorView)
        handler.postDelayed(autoHide, AUTO_HIDE_MS)
        handler.postDelayed(grantCheck, CHECK_INTERVAL_MS)
    }

    /** singleInstance: later start attempts land here instead of creating a new card; keep it up. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handler.removeCallbacks(autoHide)
        handler.postDelayed(autoHide, AUTO_HIDE_MS)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_OUTSIDE || ev.actionMasked == MotionEvent.ACTION_DOWN) {
            finish()
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onDestroy() {
        // The user is still in Settings; keep App Open blocked for their return.
        ADSAppManage.blockAppOpenAd(APP_OPEN_BLOCK_MS)
        toggleAnimator?.cancel()
        handler.removeCallbacksAndMessages(null)
        if (current?.get() === this) current = null
        super.onDestroy()
    }

    private fun bindAppInfo(root: View) {
        root.findViewById<ImageView>(R.id.ivGuideIcon)
            .setImageDrawable(applicationInfo.loadIcon(packageManager))
        root.findViewById<TextView>(R.id.tvGuideName)
            .text = applicationInfo.loadLabel(packageManager)
    }

    /** Looping "tap" on the toggle: hand rises while the thumb slides right and the track fills. */
    private fun buildToggleAnimator(root: View): ValueAnimator {
        val hand = root.findViewById<View>(R.id.overlayGuideHand)
        val trackOn = root.findViewById<View>(R.id.overlayGuideTrackOn)
        val thumb = root.findViewById<View>(R.id.overlayGuideThumb)
        val density = root.resources.displayMetrics.density
        val rise = 30f * density
        // Track 56dp - thumb 24dp - 2 x 4dp margin.
        val travel = 24f * density
        val direction = if (root.layoutDirection == View.LAYOUT_DIRECTION_RTL) -1f else 1f
        return ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 900L
            repeatCount = ValueAnimator.INFINITE
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val p = animation.animatedValue as Float
                hand.translationY = rise * (1f - p)
                trackOn.alpha = p
                thumb.translationX = direction * travel * p
            }
            start()
        }
    }

    companion object {
        private const val AUTO_HIDE_MS = 4000L
        private const val TAG = "OverlayGuide"
        private const val CHECK_INTERVAL_MS = 500L
        private val SHOW_ATTEMPT_DELAYS_MS = longArrayOf(150L, 300L, 450L)
        private const val APP_OPEN_BLOCK_MS = 120_000L

        private var current: WeakReference<OverlayPermissionGuideActivity>? = null

        private fun isOverlayGranted(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

        /**
         * Own task (distinct affinity) so the card can land on top of Settings instead of
         * sitting behind it in the app's task.
         */
        private fun launchIntent(context: Context): Intent =
            Intent(context, OverlayPermissionGuideActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION,
            )

        /**
         * Call right after opening the system overlay permission settings.
         *
         * The first start runs immediately, from the caller's (still foreground) Activity: Android 10+
         * blocks background activity starts, so once Settings is in front a start from the application
         * context is refused and no card ever appears. It is queued after Settings, so it lands on top.
         * The delayed starts stay as a fallback; a newer card replaces the older one.
         * No-op once overlay is granted.
         */
        @JvmStatic
        fun showDelayed(context: Context) {
            val appContext = context.applicationContext
            ADSAppManage.blockAppOpenAd(APP_OPEN_BLOCK_MS)
            if (isOverlayGranted(appContext)) return
            startCard(context, "immediate")
            val handler = Handler(Looper.getMainLooper())
            SHOW_ATTEMPT_DELAYS_MS.forEach { delay ->
                handler.postDelayed({
                    if (!isOverlayGranted(appContext)) startCard(appContext, "delayed ${delay}ms")
                }, delay)
            }
        }

        private fun startCard(context: Context, attempt: String) {
            runCatching { context.startActivity(launchIntent(context)) }
                .onFailure { Log.w(TAG, "Guide card start refused ($attempt)", it) }
        }
    }
}
