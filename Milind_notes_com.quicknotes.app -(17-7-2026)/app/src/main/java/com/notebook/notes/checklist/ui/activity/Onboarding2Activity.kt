package com.notebook.notes.checklist.ui.activity

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.ActivityOnboarding2Binding
import com.notebook.notes.checklist.flow.FlowManager
import com.notebook.notes.checklist.flow.RemoteConfigManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class Onboarding2Activity : BaseActivity() {

    private lateinit var binding: ActivityOnboarding2Binding

    @Inject
    lateinit var flowManager: FlowManager

    @Inject
    lateinit var remoteConfigManager: RemoteConfigManager

    private val indicatorDots = mutableListOf<View>()

    private val onboardingCount: Int
        get() = remoteConfigManager.getOnboardingCount()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboarding2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        if (onboardingCount < 2) {
            finishOnboarding()
            return
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        bindPageContent()
        setupIndicators()
        updateIndicators(1)
        setupContinue()
        loadOnboardingAds()
    }

    private fun loadOnboardingAds() {
        OnboardingAdsHelper.load(
            this,
            binding.llAdsContainer,
            binding.cardView,
            binding.shimmerContainerBanner,
            binding.flNativeSmallPlaceholder,
            binding.flBannerSmallPlaceholder
        )
    }

    override fun onDestroy() {
        OnboardingAdsHelper.resetNativeLoadState()
        super.onDestroy()
    }

    private fun bindPageContent() {
        val prefix = getString(R.string.onboarding_title_2_prefix)
        val suffix = getString(R.string.onboarding_title_2_suffix)
        val full = "$prefix $suffix"
        val spannable = SpannableString(full)

        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            full.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.black)),
            0,
            prefix.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.onboarding_title_accent)),
            prefix.length + 1,
            full.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.tvOnboardingTitle.text = spannable
    }

    private fun setupIndicators() {
        binding.indicatorContainer.removeAllViews()
        indicatorDots.clear()

        repeat(onboardingCount) {
            val dot = View(this)
            val params = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.onboarding_dot_size),
                resources.getDimensionPixelSize(R.dimen.onboarding_dot_size)
            )
            if (it > 0) {
                params.marginStart =
                    resources.getDimensionPixelSize(R.dimen.onboarding_dot_spacing)
            }
            dot.layoutParams = params
            binding.indicatorContainer.addView(dot)
            indicatorDots.add(dot)
        }
    }

    private fun updateIndicators(position: Int) {
        indicatorDots.forEachIndexed { index, dot ->
            val active = index == position
            val params = dot.layoutParams as ViewGroup.MarginLayoutParams
            params.width = resources.getDimensionPixelSize(
                if (active) R.dimen.onboarding_dot_active_width else R.dimen.onboarding_dot_size
            )
            params.height = resources.getDimensionPixelSize(R.dimen.onboarding_dot_size)
            if (index > 0) {
                params.marginStart =
                    resources.getDimensionPixelSize(R.dimen.onboarding_dot_spacing)
            }
            dot.layoutParams = params
            dot.background = ContextCompat.getDrawable(
                this,
                if (active) R.drawable.bg_onboarding_dot_active
                else R.drawable.bg_onboarding_dot_inactive
            )
        }
    }

    private fun setupContinue() {
        binding.btnContinue.setOnClickListener {
            if (onboardingCount > 2) {
                startActivity(Intent(this, Onboarding3Activity::class.java))
                finish()
            } else {
                finishOnboarding()
            }
        }
    }

    private fun finishOnboarding() {
        flowManager.markOnboardingCompleted()
        flowManager.next(this)
    }
}
