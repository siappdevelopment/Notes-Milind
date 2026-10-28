package com.notebook.notes.checklist.ui.activity

import android.Manifest
import android.app.AppOpsManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.notebook.notes.checklist.Advertisement.ADSAppManage
import com.notebook.notes.checklist.Advertisement.ADSBannerSmall
import com.notebook.notes.checklist.Advertisement.ADSConsentManager
import com.notebook.notes.checklist.Advertisement.ADSMainClass
import com.notebook.notes.checklist.Advertisement.ADSNativeDisplay
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.ActivityPermissionFlowBinding
import com.notebook.notes.checklist.databinding.ItemPermissionCardBinding
import com.notebook.notes.checklist.databinding.ItemPermissionIllustrationBinding
import com.notebook.notes.checklist.flow.FlowManager
import com.notebook.notes.checklist.flow.FlowPermissionHost
import com.notebook.notes.checklist.flow.PermissionManager
import com.notebook.notes.checklist.helper.PermissionHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Entry for [com.notebook.notes.checklist.flow.FlowStep.PERMISSION].
 * ViewPager only swaps illustrations; page content toggles VISIBLE/GONE.
 * Call / Notification cards appear only when that permission is still missing.
 */
@AndroidEntryPoint
class PermissionFlowActivity : BaseActivity(), FlowPermissionHost {

    private lateinit var binding: ActivityPermissionFlowBinding
    private lateinit var permissionCardsAdapter: PermissionCardsAdapter

    @Inject
    lateinit var permissionHelper: PermissionHelper

    @Inject
    lateinit var permissionManager: PermissionManager

    @Inject
    lateinit var flowManager: FlowManager

    private var notificationCallback: (() -> Unit)? = null
    private var awaitingOverlaySettings = false
    private var flowFinished = false

    private val overlayCheckHandler = Handler(Looper.getMainLooper())
    private val overlayCheckRunnable = object : Runnable {
        override fun run() {
            if (!awaitingOverlaySettings || flowFinished) return
            if (permissionHelper.isOverlayPermissionGranted()) {
                onOverlayGrantedFromSettings()
            } else {
                overlayCheckHandler.postDelayed(this, OVERLAY_POLL_MS)
            }
        }
    }

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            stopOverlayPolling()
            // Back from Settings without grant — still continue the flow.
            finishFlow()
        }

    private val illustrationDrawables = intArrayOf(
        R.drawable.li_permission_1,
        R.drawable.li_permission_2
    )

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            refreshMissingPermissions()
            // Granted or denied — continue to the Overlay page.
            if (binding.viewPager.currentItem == 0) {
                showOverlayOnly()
            }
            notificationCallback?.invoke()
            notificationCallback = null
        }

    private val phonePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            refreshMissingPermissions()
            // Granted or denied — request notification next, or move on.
            continueAfterPhonePermissionResult()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionFlowBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupPermissionList()
        setupViewPager()
        setupButtons()

        // Call + Notification already granted → show Overlay layout only (no Required page).
        if (areRequiredPermissionsGranted()) {
            showOverlayOnly()
        } else {
            updateUIForPage(0)
            refreshMissingPermissions()
        }
        loadPermissionAds()
    }

    private fun showPermissionAdsLayout() {
        findViewById<View>(R.id.llOtherScreenAds)?.visibility = View.VISIBLE
        findViewById<View>(R.id.shimmer_container_banner)?.visibility = View.VISIBLE
        findViewById<View>(R.id.flNativeSmallPlaceholder)?.visibility = View.GONE
        findViewById<View>(R.id.flBannerSmallPlaceholder)?.visibility = View.GONE
    }

    private fun loadPermissionAds() {
        showPermissionAdsLayout()

        if (!ADSMainClass.getPermissionSmallAdsShow() || ADSMainClass.getAds_Free()) {
            hidePermissionAds()
            return
        }

        if (!ADSConsentManager.canRequestAds(this)) {
            // Keep shimmer visible by default until consent allows load
            return
        }

        val adsType = ADSMainClass.getPermissionAdsType()
        var nativeId = ADSMainClass.getStringValue(ADSMainClass.PERMISSION_SCREEN_NATIVE)
        var bannerId = ADSMainClass.getStringValue(ADSMainClass.PERMISSION_SCREEN_BANNER)
        if (nativeId.isNullOrEmpty()) {
            nativeId = ADSMainClass.getStringValue(ADSMainClass.OTHER_SCREEN_NATIVE)
        }
        if (bannerId.isNullOrEmpty()) {
            bannerId = ADSMainClass.getStringValue(ADSMainClass.OTHER_SCREEN_BANNER)
        }

        if (adsType.equals("native", ignoreCase = true)) {
            findViewById<View>(R.id.shimmer_container_banner)?.visibility = View.VISIBLE
            findViewById<View>(R.id.flNativeSmallPlaceholder)?.visibility = View.GONE
            findViewById<View>(R.id.flBannerSmallPlaceholder)?.visibility = View.GONE
            ADSNativeDisplay.LoadingCheck = true
            ADSNativeDisplay.loadAdmobNativeAdBig(
                nativeId,
                findViewById(R.id.flNativeSmallPlaceholder),
                findViewById(R.id.shimmer_container_banner),
                "small",
                this
            )
        } else {

            findViewById<View>(R.id.shimmer_container_banner)?.visibility = View.GONE
            findViewById<View>(R.id.flNativeSmallPlaceholder)?.visibility = View.GONE
            findViewById<View>(R.id.flBannerSmallPlaceholder)?.visibility = View.VISIBLE
            ADSBannerSmall.loadAdMobBanner(
                bannerId,
                findViewById(R.id.flBannerSmallPlaceholder),
                findViewById(R.id.shimmer_container_banner),
                this
            )
        }
    }

    private fun hidePermissionAds() {
        findViewById<View>(R.id.llOtherScreenAds)?.visibility = View.GONE
        findViewById<View>(R.id.shimmer_container_banner)?.visibility = View.GONE
        findViewById<View>(R.id.flNativeSmallPlaceholder)?.visibility = View.GONE
        findViewById<View>(R.id.flBannerSmallPlaceholder)?.visibility = View.GONE
    }

    private fun showOverlayOnly() {
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.setCurrentItem(1, false)
        updateUIForPage(1)
        // Single-page mode: hide the inactive first indicator dot.
        if (binding.indicatorOverlay.childCount >= 2) {
            binding.indicatorOverlay.getChildAt(0).visibility = View.GONE
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupPermissionList() {
        permissionCardsAdapter = PermissionCardsAdapter()
        binding.rvPermissions.apply {
            layoutManager = LinearLayoutManager(this@PermissionFlowActivity)
            adapter = permissionCardsAdapter
            // Let NestedScrollView own vertical scrolling of the permission cards.
            isNestedScrollingEnabled = false
        }
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = PermissionIllustrationAdapter(illustrationDrawables)
        binding.viewPager.isUserInputEnabled = true

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateUIForPage(position)
            }
        })
    }

    private fun updateUIForPage(position: Int) {
        val isRequired = position == 0
        binding.layoutRequired.visibility = if (isRequired) View.VISIBLE else View.GONE
        binding.layoutOverlay.visibility = if (isRequired) View.GONE else View.VISIBLE
        binding.tvContinue.text = if (isRequired) "Continue" else "Get Started"
    }

    private fun setupButtons() {
        binding.btnContinue.setOnClickListener {
            if (binding.viewPager.currentItem == 0) {
                handlePage1Click()
            } else {
                handlePage2Click()
            }
        }
    }

    private fun refreshMissingPermissions() {
        val items = mutableListOf<PermissionCardItem>()

        if (permissionHelper.checkPhoneStatePermission(this) !=
            PermissionHelper.PermissionStatus.GRANTED
        ) {
            items += PermissionCardItem(
                type = PermissionCardType.PHONE,
                iconRes = R.drawable.ic_call_permission,
                title = getString(R.string.phone_call),
                description = getString(R.string.grant_call_permission_to_identity_callers_and_improve_calling_experience)
            )
        }

        val notifMissing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                permissionHelper.checkNotificationPermission(this) !=
                PermissionHelper.PermissionStatus.GRANTED
        if (notifMissing) {
            items += PermissionCardItem(
                type = PermissionCardType.NOTIFICATION,
                iconRes = R.drawable.ic_permission_notification,
                title = getString(R.string.notification),
                description = getString(R.string.enable_reminders_to_get_timely_alerts_and_never_miss_important_notes)
            )
        }

        permissionCardsAdapter.submit(items)
        binding.rvPermissions.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun areRequiredPermissionsGranted(): Boolean {
        val phoneGranted = permissionHelper.checkPhoneStatePermission(this) ==
                PermissionHelper.PermissionStatus.GRANTED
        val notifGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                permissionHelper.checkNotificationPermission(this) ==
                PermissionHelper.PermissionStatus.GRANTED
        return phoneGranted && notifGranted
    }

    private fun handlePage1Click() {
        val phoneStatus = permissionHelper.checkPhoneStatePermission(this)
        val notifStatus = permissionHelper.checkNotificationPermission(this)

        if (phoneStatus != PermissionHelper.PermissionStatus.GRANTED) {
            phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
            return
        }

        if (notifStatus != PermissionHelper.PermissionStatus.GRANTED &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }

        showOverlayOnly()
    }

    private fun continueAfterPhonePermissionResult() {
        if (binding.viewPager.currentItem != 0) return

        val notifStatus = permissionHelper.checkNotificationPermission(this)
        if (notifStatus != PermissionHelper.PermissionStatus.GRANTED &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }

        showOverlayOnly()
    }

    private fun handlePage2Click() {
        if (permissionHelper.isOverlayPermissionGranted()) {
            finishFlow()
            return
        }
        openOverlaySettings()
    }

    private fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            finishFlow()
            return
        }
        awaitingOverlaySettings = true
        ADSAppManage.isAppOpenBlocked = true
        overlayCheckHandler.removeCallbacks(overlayCheckRunnable)
        overlayCheckHandler.postDelayed(overlayCheckRunnable, OVERLAY_POLL_MS)

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
     * Overlay was enabled in Settings — bring this screen back and continue
     * without asking the user to press Back or tap Get Started again.
     */
    private fun onOverlayGrantedFromSettings() {
        if (flowFinished) return
        stopOverlayPolling()
        val bringToFront = Intent(this, PermissionFlowActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(bringToFront)
        finishFlow()
    }

    private fun stopOverlayPolling() {
        awaitingOverlaySettings = false
        overlayCheckHandler.removeCallbacks(overlayCheckRunnable)
    }

    override fun onResume() {
        super.onResume()
        ADSAppManage.isAppOpenBlocked = false
        refreshMissingPermissions()
        if (awaitingOverlaySettings && permissionHelper.isOverlayPermissionGranted()) {
            onOverlayGrantedFromSettings()
        } else if (binding.viewPager.currentItem == 1 && permissionHelper.isOverlayPermissionGranted()) {
            finishFlow()
        }

    }

    override fun onDestroy() {
        stopOverlayPolling()
        super.onDestroy()
    }

    private fun finishFlow() {
        if (flowFinished) return
        flowFinished = true
        stopOverlayPolling()
        flowManager.markPermissionCompleted()
        flowManager.next(this)
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

    private enum class PermissionCardType { PHONE, NOTIFICATION }

    private data class PermissionCardItem(
        val type: PermissionCardType,
        val iconRes: Int,
        val title: String,
        val description: String
    )

    private class PermissionCardsAdapter : RecyclerView.Adapter<PermissionCardsAdapter.CardVH>() {

        private val items = mutableListOf<PermissionCardItem>()

        fun submit(newItems: List<PermissionCardItem>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardVH {
            val binding = ItemPermissionCardBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return CardVH(binding)
        }

        override fun onBindViewHolder(holder: CardVH, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        class CardVH(
            private val binding: ItemPermissionCardBinding
        ) : RecyclerView.ViewHolder(binding.root) {
            fun bind(item: PermissionCardItem) {
                binding.ivIcon.setImageResource(item.iconRes)
                binding.tvTitle.text = item.title
                binding.tvDesc.text = item.description
            }
        }
    }

    private class PermissionIllustrationAdapter(
        private val drawables: IntArray
    ) : RecyclerView.Adapter<PermissionIllustrationAdapter.IllustrationViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IllustrationViewHolder {
            val binding = ItemPermissionIllustrationBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return IllustrationViewHolder(binding)
        }

        override fun onBindViewHolder(holder: IllustrationViewHolder, position: Int) {
            holder.ivIllustration.setImageResource(drawables[position])
        }

        override fun getItemCount(): Int = drawables.size

        class IllustrationViewHolder(
            binding: ItemPermissionIllustrationBinding
        ) : RecyclerView.ViewHolder(binding.root) {
            val ivIllustration: ImageView = binding.ivIllustration
        }
    }

    companion object {
        private const val OVERLAY_POLL_MS = 400L
    }
}
