package com.notebook.notes.checklist.ui.activity

import android.Manifest
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.postDelayed
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.firebase.messaging.FirebaseMessaging
import com.notebook.notes.checklist.Advertisement.ADSMainClass
import com.notebook.notes.checklist.Advertisement.ADSUtilitis
import com.notebook.notes.checklist.Advertisement.activity.BActivity
import com.notebook.notes.checklist.flow.FlowManager
import com.notebook.notes.checklist.flow.PermissionManager
import com.notebook.notes.checklist.flow.RemoteConfigManager
import com.notebook.notes.checklist.flow.SessionManager
import com.notebook.notes.checklist.helper.NotePasswordManager
import com.notebook.notes.checklist.helper.PreferenceHelper
import com.notebook.notes.checklist.iap.BillingConstants
import com.notebook.notes.checklist.iap.PremiumManager
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.iap.model.PurchaseEvent
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.restorePurchasesWith
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Continues after [BActivity] ads bootstrap. Flow Remote Config is already cached in
 * [RemoteConfigManager] by BActivity's single fetchAndActivate — this screen only
 * builds [FlowManager] and navigates (splash permissions → screen_flow).
 */
@AndroidEntryPoint
class SplashActivity : BActivity() {

    @Inject
    lateinit var preferenceHelper: PreferenceHelper

    @Inject
    lateinit var passwordManager: NotePasswordManager

    @Inject
    lateinit var remoteConfigManager: RemoteConfigManager

    @Inject
    lateinit var flowManager: FlowManager

    @Inject
    lateinit var permissionManager: PermissionManager

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var premiumManager: PremiumManager

    private var pendingSplashPermissions: List<String> = emptyList()
    private var splashPermissionIndex = 0
    private var hasStartedAppFlow = false

    private val splashPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            val lastPermission = pendingSplashPermissions.getOrNull(splashPermissionIndex - 1)
            when (lastPermission) {
                Manifest.permission.POST_NOTIFICATIONS ->
                    ADSUtilitis.trackScreen(this, "Notification_Allow")
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_PHONE_STATE ->
                    ADSUtilitis.trackScreen(this, "Call_Phone_Allow")
            }
            // Continue regardless of allow / deny.
            requestNextSplashPermission()
        }

    private val verifyPasswordLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            flowManager.next(this)
        } else {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        // BActivity: layout + ads remote config → consent → Paywall-identical restore → ads → initActivity()
        super.onCreate(savedInstanceState)

        applyFullscreenEdgeToEdge()
        fcmtoken()

    }

    override fun initActivity() {
        if (hasStartedAppFlow) return
        hasStartedAppFlow = true

        applyFullscreenEdgeToEdge()
        ADSMainClass.updateConsecutiveStreak(this)
        ADSMainClass.initAppContext(this)

        bootstrapFlow()
    }

    override fun onResume() {
        super.onResume()
        applyFullscreenEdgeToEdge()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyFullscreenEdgeToEdge()
    }

    /**
     * Restore already ran in [BActivity] before ads, using the same [PremiumManager.restorePurchases]
     * path as PaywallActivity. Wait here only so the screen flow is built from the settled Boolean
     * ([PremiumManager.isPurchaseRestoreChecked] / [PremiumManager.isPremium]).
     */
    private fun bootstrapFlow() {

        lifecycleScope.launch {
            val synced = premiumManager.awaitInitialSync()
            if (isFinishing || isDestroyed) return@launch
            Log.d(
                "SplashActivity",
                "Entitlement settled=$synced " +
                    "premium=${premiumManager.isPremium()} " +
                    "restoreChecked=${premiumManager.isPurchaseRestoreChecked()}",
            )
            continueBootstrapFlow()
        }
    }

    private fun continueBootstrapFlow() {
        // BActivity already fetched RC and cached flow keys — never fetch again here.
        remoteConfigManager.ensureCached()
        // Session count is incremented in MyApplication on foreground (before Splash reaches here).
        try {
            ADSUtilitis.trackScreen(this, "Session_Increment")
        } catch (_: Exception) {
            // analytics must never block startup
        }
        flowManager.buildFlow(remoteConfigManager.getFlowConfig())

        val splashMode = remoteConfigManager.getSplashPermissionMode()
        val needed = permissionManager.splashPermissionsToRequest(splashMode)
            .filter { permission ->
                ContextCompat.checkSelfPermission(this, permission) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        Log.d(
            "SplashActivity",
            "RC flow=${remoteConfigManager.getScreenFlow()} " +
                "splashMode=$splashMode onboarding=${remoteConfigManager.getOnboardingCount()} " +
                "paywall=${remoteConfigManager.shouldShowPaywallAfterOnboarding()} " +
                "sessionInterval=${remoteConfigManager.getPaywallSessionInterval()} " +
                "session=${sessionManager.getTotalSessionCount()} requesting=$needed"
        )
        if (needed.isEmpty()) {
            flowManager.start(this)
        } else {
            pendingSplashPermissions = needed
            splashPermissionIndex = 0
            requestNextSplashPermission()
        }
    }


    private fun requestNextSplashPermission() {
        if (splashPermissionIndex >= pendingSplashPermissions.size) {
            flowManager.start(this)
            return
        }
        val permission = pendingSplashPermissions[splashPermissionIndex++]
        if (permission == Manifest.permission.POST_NOTIFICATIONS &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
            requestNextSplashPermission()
            return
        }
        splashPermissionLauncher.launch(permission)
    }

    /** Draws content under status/nav/cutout and hides system bars (swipe to reveal). */
    private fun applyFullscreenEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    fun fcmtoken(){
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.e("FCM_TOKEN", "Startup FCM Token: $token")
                } else {
                    Log.e(
                        "FCM_TOKEN", "Startup FCM Token fetch failed", task.exception
                    )
                }
            }
    }
}
