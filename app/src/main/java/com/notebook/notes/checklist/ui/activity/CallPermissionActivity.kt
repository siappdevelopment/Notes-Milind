package com.notebook.notes.checklist.ui.activity

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.notebook.notes.checklist.ads.AdRemoteConfig
import com.notebook.notes.checklist.ads.AdsManager
import com.notebook.notes.checklist.ads.loadScreenAd
import com.notebook.notes.checklist.ads.other_screen
import com.notebook.notes.checklist.Advertisement.ADSConsentManager
import com.notebook.notes.checklist.Advertisement.ADSMainClass
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.ActivityCallPermissionBinding
import com.notebook.notes.checklist.flow.FlowPermissionHost
import com.notebook.notes.checklist.flow.PermissionManager
import com.notebook.notes.checklist.helper.PermissionHelper
import com.notebook.notes.checklist.helper.PreferenceHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CallPermissionActivity : BaseActivity(), FlowPermissionHost {
    lateinit var binding: ActivityCallPermissionBinding

    @Inject
    lateinit var preferenceHelper: PreferenceHelper

    @Inject
    lateinit var permissionHelper: PermissionHelper

    @Inject
    lateinit var permissionManager: PermissionManager

    private var notificationCallback: (() -> Unit)? = null
    private var finishedStep = false

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            notificationCallback?.invoke()
            notificationCallback = null
        }

    private val requestPhoneStatePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            finishPermissionStep()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(
                left = systemBars.left,
                top = systemBars.top,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            insets
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finishPermissionStep()
                }
            }
        )

        if (permissionHelper.checkPhoneStatePermission(this) ==
            PermissionHelper.PermissionStatus.GRANTED
        ) {
            finishPermissionStep()
            return
        }
        initClick()
        loadOtherScreenAds()
    }

    private fun loadOtherScreenAds() {
        AdsManager.loadScreenAd(this, AdRemoteConfig.other_screen)
    }


    private fun initClick() {
        binding.btnAllow.setOnClickListener {
            requestPhoneStatePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        }
    }

    private fun finishPermissionStep() {
        if (finishedStep) return
        finishedStep = true
        permissionManager.onPermissionStepFinished(this)
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
