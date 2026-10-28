package com.notebook.notes.checklist.ui.activity

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.notebook.notes.checklist.ads.AdRemoteConfig
import com.notebook.notes.checklist.ads.AdsManager
import com.notebook.notes.checklist.ads.loadScreenAd
import com.notebook.notes.checklist.ads.other_screen
import com.notebook.notes.checklist.Advertisement.ADSConsentManager
import com.notebook.notes.checklist.Advertisement.ADSMainClass
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
        AdsManager.loadScreenAd(this, AdRemoteConfig.other_screen)
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
