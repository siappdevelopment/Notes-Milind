package com.notebook.notes.checklist.ui.activity

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.ActivityOverLayPermissionBinding
import com.notebook.notes.checklist.flow.FlowPermissionHost
import com.notebook.notes.checklist.flow.PermissionManager
import com.notebook.notes.checklist.helper.PermissionHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OverLayPermissionActivity : BaseActivity(), FlowPermissionHost {
    lateinit var binding: ActivityOverLayPermissionBinding

    @Inject
    lateinit var permissionHelper: PermissionHelper

    @Inject
    lateinit var permissionManager: PermissionManager

    private var notificationCallback: (() -> Unit)? = null
    private var finishedStep = false
    private var awaitingOverlaySettings = false

    private val overlayCheckHandler = Handler(Looper.getMainLooper())
    private val overlayCheckRunnable = object : Runnable {
        override fun run() {
            if (!awaitingOverlaySettings || finishedStep) return
            if (permissionHelper.isOverlayPermissionGranted()) {
                onOverlayGrantedFromSettings()
            } else {
                overlayCheckHandler.postDelayed(this, OVERLAY_POLL_MS)
            }
        }
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            notificationCallback?.invoke()
            notificationCallback = null
        }

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            stopOverlayPolling()
            // User pressed back from Settings — continue either way.
            finishPermissionStep()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOverLayPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    stopOverlayPolling()
                    finishPermissionStep()
                }
            }
        )

        if (permissionHelper.isOverlayPermissionGranted()) {
            finishPermissionStep()
            return
        }
        initClick()
    }

    override fun onResume() {
        super.onResume()
        if (awaitingOverlaySettings && permissionHelper.isOverlayPermissionGranted()) {
            onOverlayGrantedFromSettings()
        }
    }

    override fun onDestroy() {
        stopOverlayPolling()
        super.onDestroy()
    }

    private fun initClick() {
        binding.btnAllow.setOnClickListener {
            openOverlaySettings()
        }
    }

    private fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            finishPermissionStep()
            return
        }
        awaitingOverlaySettings = true
        overlayCheckHandler.removeCallbacks(overlayCheckRunnable)
        overlayCheckHandler.postDelayed(overlayCheckRunnable, OVERLAY_POLL_MS)

        // No package URI → full "Display over other apps" app list (sorted by label, e.g. #Notes).
        val showListIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        try {
            overlayPermissionLauncher.launch(showListIntent)
        } catch (_: Exception) {
            overlayPermissionLauncher.launch(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }

    /**
     * When the user enables the toggle in Settings, bring this app back and continue
     * without requiring them to press Back.
     */
    private fun onOverlayGrantedFromSettings() {
        if (finishedStep) return
        stopOverlayPolling()
        val bringToFront = Intent(this, OverLayPermissionActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(bringToFront)
        finishPermissionStep()
    }

    private fun stopOverlayPolling() {
        awaitingOverlaySettings = false
        overlayCheckHandler.removeCallbacks(overlayCheckRunnable)
    }

    private fun finishPermissionStep() {
        if (finishedStep) return
        finishedStep = true
        stopOverlayPolling()
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

    companion object {
        private const val OVERLAY_POLL_MS = 400L
    }
}
