package com.notebook.notes.checklist.ui.activity

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.notebook.notes.checklist.Advertisement.ADSBannerSmall
import com.notebook.notes.checklist.Advertisement.ADSConsentManager
import com.notebook.notes.checklist.Advertisement.ADSMainClass
import com.notebook.notes.checklist.Advertisement.ADSNativeDisplay
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.ActivityWelcomeBinding
import com.notebook.notes.checklist.flow.FlowPermissionHost
import com.notebook.notes.checklist.flow.PermissionManager
import com.notebook.notes.checklist.helper.PermissionHelper
import com.notebook.notes.checklist.helper.PreferenceHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WelcomeActivity : BaseActivity(), FlowPermissionHost {
    lateinit var binding: ActivityWelcomeBinding

    @Inject
    lateinit var permissionHelper: PermissionHelper

    @Inject
    lateinit var preferenceHelper: PreferenceHelper

    @Inject
    lateinit var permissionManager: PermissionManager

    private var notificationCallback: (() -> Unit)? = null

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            notificationCallback?.invoke()
            notificationCallback = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityWelcomeBinding.inflate(layoutInflater).also { binding = it }.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initClick()
        loadOtherScreenAds()
    }

    private fun loadOtherScreenAds() {
        if (!ADSMainClass.getOtherAdsShow()) {
            hideOtherScreenAdPlaceholders()
            return
        }

        if (!ADSConsentManager.canRequestAds(this) || ADSMainClass.getAds_Free()) {
            hideOtherScreenAdPlaceholders()
            return
        }
        if (ADSMainClass.getOtherAdsType().equals("native")) {
            ADSNativeDisplay.loadAdmobNativeAdBig(
                ADSMainClass.getStringValue(ADSMainClass.OTHER_SCREEN_NATIVE),
                findViewById(R.id.flNativeSmallPlaceholder),
                findViewById(R.id.shimmer_container_banner),
                "small",
                this
            )
        } else {
            ADSBannerSmall.loadAdMobBanner(
                ADSMainClass.getStringValue(ADSMainClass.OTHER_SCREEN_BANNER),
                findViewById(R.id.flBannerSmallPlaceholder),
                findViewById(R.id.shimmer_container_banner),
                this
            )
        }
    }

    private fun hideOtherScreenAdPlaceholders() {
        findViewById<android.view.View>(R.id.shimmer_container_banner).visibility = android.view.View.GONE
        findViewById<android.view.View>(R.id.flNativeSmallPlaceholder).visibility = android.view.View.GONE
        findViewById<android.view.View>(R.id.flBannerSmallPlaceholder).visibility = android.view.View.GONE
    }

    private fun initClick() {
        binding.btnGetstart.setOnClickListener {
            permissionManager.startPermissionScreenFlow(this)
        }
    }

    override fun requestNotificationPermission(onComplete: () -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            permissionHelper.checkNotificationPermission(this) ==
            PermissionHelper.PermissionStatus.GRANTED
        ) {
            onComplete()
            return
        }
        notificationCallback = onComplete
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
