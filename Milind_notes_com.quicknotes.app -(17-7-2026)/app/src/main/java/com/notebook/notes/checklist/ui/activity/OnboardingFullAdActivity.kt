package com.notebook.notes.checklist.ui.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ads.module.ads.wrapper.ApNativeAd
import com.notebook.notes.checklist.ads.AdsManager
import com.notebook.notes.checklist.ads.populateNativeAdView
import com.notebook.notes.checklist.databinding.ActivityOnboardingFullAdBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Full-screen native ad page shown between onboarding page 2 and 3. The ad is loaded by
 * [Onboarding2Activity]; this screen only renders it. If no ad is available it skips straight to page 3.
 */
@AndroidEntryPoint
class OnboardingFullAdActivity : BaseActivity() {

    private lateinit var binding: ActivityOnboardingFullAdBinding
    private val handler = Handler(Looper.getMainLooper())
    private val showCloseRunnable = Runnable { binding.imgCloseAdsFull.visibility = View.VISIBLE }
    private val loadTimeoutRunnable = Runnable { goNext() }
    private var adShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingFullAdBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Back is blocked like on the other onboarding pages; the close button continues.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })
        binding.imgCloseAdsFull.setOnClickListener { goNext() }

        // The ad may still be loading when this page opens: show the shimmer, render when it arrives,
        // and move on if it fails or takes too long.
        var skippedInitialNull = true
        AdsManager.nativeAdOnBoardingFullLive.observe(this) { ad ->
            if (ad != null) {
                if (!adShown) showAd(ad)
            } else if (!skippedInitialNull) {
                goNext()
            }
            skippedInitialNull = false
        }
        handler.postDelayed(loadTimeoutRunnable, LOAD_TIMEOUT_MS)
    }

    private fun showAd(ad: ApNativeAd) {
        adShown = true
        handler.removeCallbacks(loadTimeoutRunnable)
        populateNativeAdView(this, ad, binding.flNativeFullPlaceholder, binding.shimmerAdsFull)
        binding.flNativeFullPlaceholder.visibility = View.VISIBLE
        handler.postDelayed(showCloseRunnable, CLOSE_DELAY_MS)
    }

    private fun goNext() {
        if (isFinishing) return
        startActivity(Intent(this, Onboarding3Activity::class.java))
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        // The ad is single-use: clear it so it is never rendered twice.
        AdsManager.nativeAdOnBoardingFullLive.value = null
        super.onDestroy()
    }

    companion object {
        private const val CLOSE_DELAY_MS = 1500L
        private const val LOAD_TIMEOUT_MS = 5000L
    }
}
