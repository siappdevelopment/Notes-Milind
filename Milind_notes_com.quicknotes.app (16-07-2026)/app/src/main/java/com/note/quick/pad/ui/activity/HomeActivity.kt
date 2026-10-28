package com.note.quick.pad.ui.activity

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.note.quick.pad.R
import androidx.core.view.isVisible
import com.note.quick.pad.ui.activity.SettingActivity
import com.note.quick.pad.viewmodel.utils.launchActivity
import com.note.quick.pad.databinding.LayoutHomeTopBarBinding
import com.note.quick.pad.databinding.ActivityHomeBinding
import com.note.quick.pad.databinding.ItemNavDrawerRowBinding
import com.note.quick.pad.databinding.LayoutNavDrawerBinding
import com.note.quick.pad.helper.PermissionHelper
import com.note.quick.pad.model.Category
import com.note.quick.pad.ui.adapter.NavDrawerCategoryAdapter
import com.note.quick.pad.ui.category.CategorySelectorPopup
import com.note.quick.pad.ui.category.CreateLabelBottomSheet
import com.note.quick.pad.ui.fragment.ArchiveFragment
import com.note.quick.pad.ui.fragment.BookMarkFragment
import com.note.quick.pad.ui.fragment.HomeFragment
import com.note.quick.pad.ui.fragment.MoreFragment
import com.note.quick.pad.ui.fragment.ReminderFragment
import com.note.quick.pad.ui.fragment.ReminderPermissionBottomSheet
import com.note.quick.pad.util.DateTimeUtils
import com.note.quick.pad.viewmodel.CategoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : BaseActivity() {

    companion object {


        private const val TAG_HOME = "home"
        private const val TAG_REMINDER = "reminder"
        private const val TAG_BOOKMARK = "bookmark"
        private const val TAG_ARCHIVE = "archive"
        private const val TAG_MORE = "more"
    }

    private lateinit var binding: ActivityHomeBinding
    private lateinit var topBarBinding: LayoutHomeTopBarBinding
    private lateinit var drawerBinding: LayoutNavDrawerBinding

    @Inject
    lateinit var permissionHelper: PermissionHelper

    private val categoryViewModel: CategoryViewModel by viewModels()
    private lateinit var drawerCategoryAdapter: NavDrawerCategoryAdapter

    private var selectedTab = BottomTab.HOME

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                requestExactAlarmPermissionIfNeeded()
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private val requestExactAlarmPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // User returned from exact-alarm settings; no further action required on home.
        }

    private enum class BottomTab(val tag: String) {
        HOME(TAG_HOME),
        REMINDER(TAG_REMINDER),
        BOOKMARK(TAG_BOOKMARK),
        ARCHIVE(TAG_ARCHIVE),
        MORE(TAG_MORE);
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        topBarBinding = binding.layoutTopBar
        drawerBinding = binding.navDrawer

        ViewCompat.setOnApplyWindowInsetsListener(binding.drawerLayout) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.main.updatePadding(
                left = systemBars.left,
                top = systemBars.top,
                right = systemBars.right
            )
            binding.bottomNavContainer.updatePadding(bottom = systemBars.bottom)
            drawerBinding.drawerHeader.root.updatePadding(top = systemBars.top)
            insets
        }

        setupNavigationDrawer()
        setupTopBar()
        setupBottomNavigation()
        setupBackPressHandler()
        if (savedInstanceState == null) {
            showTab(BottomTab.HOME)
        } else {
            selectedTab = BottomTab.entries.firstOrNull {
                supportFragmentManager.findFragmentByTag(it.tag)?.isVisible == true
            } ?: BottomTab.HOME
            updateBottomNavSelection()
            binding.root.post { applyTopBarForCurrentTab() }
        }
        checkReminderPermissions()
    }

    override fun onResume() {
        super.onResume()
        DateTimeUtils.refresh(this)
        refreshVisibleListDateTimeFormat()
    }

    /** Re-applies Settings → Date/Time Format to the currently visible notes list. */
    private fun refreshVisibleListDateTimeFormat() {
        when (val fragment = supportFragmentManager.findFragmentByTag(selectedTab.tag)) {
            is HomeFragment -> fragment.refreshTimeFormat()
            is ReminderFragment -> fragment.refreshTimeFormat()
            is BookMarkFragment -> fragment.refreshTimeFormat()
            is ArchiveFragment -> fragment.refreshTimeFormat()
        }
    }

    fun openDrawer() {
        binding.drawerLayout.openDrawer(GravityCompat.START)
    }

    fun openArchiveTab() {
        showTab(BottomTab.ARCHIVE)
    }

    fun updateTopBarNotesCount(count: Int) {
        if (selectedTab == BottomTab.MORE) return
        topBarBinding.tvNotesCountNumber.text = count.toString()
    }

    fun onFragmentTopBarReady() {
        applyTopBarForCurrentTab()
    }



    fun showAddCategorySheet(onCategorySelected: (Category) -> Unit) {
        val categories = categoryViewModel.uiState.value.categories
        CreateLabelBottomSheet.show(
            activity = this,
            existingCategories = categories,
            onLabelAdded = onCategorySelected,
            onCreateLabel = { name, onSuccess ->
                categoryViewModel.createCategory(name, onSuccess)
            }
        )
    }

    private fun closeDrawer() {
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun setupNavigationDrawer() {
        drawerBinding.sectionCategories.root.text = getString(R.string.drawer_section_categories)
        drawerBinding.sectionOthers.root.text = getString(R.string.drawer_section_others)
        drawerBinding.sectionAbout.root.text = getString(R.string.drawer_section_about)

        drawerCategoryAdapter = NavDrawerCategoryAdapter { category ->
            categoryViewModel.selectCategory(category.id)
            showTab(BottomTab.HOME)
            closeDrawer()
        }
        drawerBinding.rvDrawerCategories.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = drawerCategoryAdapter
            itemAnimator = null
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                categoryViewModel.uiState.collect { state ->
                    drawerCategoryAdapter.submitCategories(
                        categories = state.categories,
                        selectedCategoryId = state.selectedCategoryId
                    )
                }
            }
        }

        bindDrawerRow(drawerBinding.rowBookmark.root, R.drawable.ic_bookmark, R.string.drawer_bookmark, "8") {
            showTab(BottomTab.BOOKMARK)
        }
        bindDrawerRow(drawerBinding.rowArchive.root, R.drawable.ic_archive, R.string.drawer_archive) {
            showTab(BottomTab.ARCHIVE)
        }
        bindDrawerRow(drawerBinding.rowRecycleBin.root, R.drawable.ic_recylerbin, R.string.drawer_recycle_bin) {
            showComingSoon()
        }
        bindDrawerRow(drawerBinding.rowBackupRestore.root, R.drawable.ic_backup, R.string.drawer_backup_restore) {
            showComingSoon()
        }
        bindDrawerRow(drawerBinding.rowSettings.root, R.drawable.ic_setting, R.string.drawer_settings) {
            showComingSoon()
        }
        bindDrawerRow(drawerBinding.rowPrivacyPolicy.root, R.drawable.ic_privacy_policy, R.string.drawer_privacy_policy) {
            showComingSoon()
        }
        bindDrawerRow(drawerBinding.rowRateUs.root, R.drawable.ic_rate_us, R.string.drawer_rate_us) {
            openRateUs()
        }
        bindDrawerRow(drawerBinding.rowShareApp.root, R.drawable.ic_share, R.string.drawer_share_app) {
            shareApp()
        }
        bindDrawerRow(drawerBinding.rowFeedback.root, R.drawable.ic_feedback, R.string.drawer_feedback) {
            sendFeedback()
        }
    }

    private fun bindDrawerRow(
        rowRoot: View,
        iconRes: Int,
        titleRes: Int,
        count: String? = null,
        onClick: () -> Unit
    ) {
        val rowBinding = ItemNavDrawerRowBinding.bind(rowRoot)
        val purple = ContextCompat.getColor(this, R.color.app_primary_color)
        rowBinding.ivIcon.setImageResource(iconRes)
        rowBinding.ivIcon.setColorFilter(purple)
        rowBinding.tvTitle.setText(titleRes)
        rowBinding.rowContainer.isSelected = false
        rowBinding.viewSelectedIndicator.visibility = View.GONE
        rowBinding.rowContainer.elevation = 0f

        if (count != null) {
            rowBinding.tvCount.visibility = View.VISIBLE
            rowBinding.tvCount.text = count
            rowBinding.tvCount.setBackgroundResource(R.drawable.bg_nav_drawer_count_badge_unselected)
            rowBinding.tvCount.setTextColor(purple)
        } else {
            rowBinding.tvCount.visibility = View.GONE
        }

        rowRoot.setOnClickListener {
            onClick()
            closeDrawer()
        }
    }

    private fun showComingSoon() {
        Toast.makeText(this, R.string.drawer_coming_soon, Toast.LENGTH_SHORT).show()
    }

    private fun openRateUs() {
        com.note.quick.pad.ui.dialog.RatingDialog(this).apply {
            setOnRatingSubmittedListener { rating ->
                if (rating >= 4) {
                    val packageName = packageName
                    val marketIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=$packageName")
                    )
                    if (marketIntent.resolveActivity(packageManager) != null) {
                        startActivity(marketIntent)
                    } else {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                            )
                        )
                    }
                } else {
                    Toast.makeText(this@HomeActivity, R.string.drawer_feedback, Toast.LENGTH_SHORT).show()
                }
            }
        }.show()
    }

    private fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                getString(R.string.drawer_share_message, getString(R.string.drawer_app_title))
            )
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.drawer_share_app)))
    }

    private fun sendFeedback() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.drawer_feedback))
            putExtra(Intent.EXTRA_TEXT, getString(R.string.drawer_app_title))
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            showComingSoon()
        }
    }

    private fun setupTopBar() {
        topBarBinding.btnMenu.setOnClickListener { openDrawer() }
        topBarBinding.btnPro.setOnClickListener { showComingSoon() }
        topBarBinding.btnSettings.setOnClickListener {
            launchActivity<SettingActivity> { }
        }
        topBarBinding.btnMore.setOnClickListener { anchor ->
            getTopBarCallback()?.onTopBarMoreClick(anchor)
        }
        topBarBinding.etSearch.doAfterTextChanged { editable ->
            getTopBarCallback()?.onTopBarSearchQuery(editable?.toString().orEmpty())
        }
    }

    fun onNoteSelectionModeChanged(inSelectionMode: Boolean, selectedCount: Int) {
        if (selectedTab != BottomTab.HOME) return
        binding.bottomNavContainer.isVisible = !inSelectionMode
    }

    private fun getTopBarCallback(): HomeTopBarCallback? {
        val fragment = supportFragmentManager.findFragmentByTag(selectedTab.tag)
        return fragment as? HomeTopBarCallback
    }

    private fun applyTopBarForCurrentTab() {
        val showTopBar = selectedTab != BottomTab.MORE
        topBarBinding.root.isVisible = showTopBar
        if (!showTopBar) return

        val config = getTopBarCallback()?.getTopBarConfiguration()
            ?: defaultTopBarConfiguration(selectedTab)

        topBarBinding.tvHomeTitle.setText(config.titleRes)
        topBarBinding.btnPro.isVisible = config.showProBadge
        topBarBinding.btnSettings.isVisible = config.showSettings
        topBarBinding.autoSavedBadge.isVisible = config.showAutoSavedBadge
    }

    private fun defaultTopBarConfiguration(tab: BottomTab): HomeTopBarConfiguration = when (tab) {
        BottomTab.HOME -> HomeTopBarConfiguration()
        BottomTab.REMINDER -> HomeTopBarConfiguration(
            titleRes = R.string.nav_reminders,
            showProBadge = false,
            showSettings = false,
            showAutoSavedBadge = false
        )
        BottomTab.BOOKMARK -> HomeTopBarConfiguration(
            titleRes = R.string.nav_bookmark,
            showProBadge = false,
            showSettings = false,
            showAutoSavedBadge = false
        )
        BottomTab.ARCHIVE -> HomeTopBarConfiguration(
            titleRes = R.string.nav_archive,
            showProBadge = false,
            showSettings = false,
            showAutoSavedBadge = false
        )
        BottomTab.MORE -> HomeTopBarConfiguration()
    }

    private fun clearTopBarSearch() {
        topBarBinding.etSearch.setText("")
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    closeDrawer()
                    return
                }
                val homeFragment = supportFragmentManager.findFragmentByTag(TAG_HOME) as? HomeFragment
                if (homeFragment?.exitSelectionModeIfActive() == true) {
                    return
                }
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })
    }

    private fun setupBottomNavigation() {
        binding.navHome.setOnClickListener { showTab(BottomTab.HOME) }
        binding.navReminder.setOnClickListener { showTab(BottomTab.REMINDER) }
        binding.navBookmark.setOnClickListener { showTab(BottomTab.BOOKMARK) }
        binding.navArchive.setOnClickListener { showTab(BottomTab.ARCHIVE) }
        binding.navMore.setOnClickListener { showTab(BottomTab.MORE) }
        updateBottomNavSelection()
    }

    private fun showTab(tab: BottomTab) {
        if (selectedTab == tab && supportFragmentManager.findFragmentByTag(tab.tag)?.isVisible == true) {
            updateBottomNavSelection()
            return
        }

        selectedTab = tab
        val fragment = supportFragmentManager.findFragmentByTag(tab.tag) ?: createFragment(tab)

        supportFragmentManager.beginTransaction().apply {
            BottomTab.entries.forEach { currentTab ->
                val currentFragment = supportFragmentManager.findFragmentByTag(currentTab.tag)
                if (currentFragment != null && currentTab != tab) {
                    hide(currentFragment)
                }
            }
            if (fragment.isAdded) {
                show(fragment)
            } else {
                add(R.id.fragmentContainer, fragment, tab.tag)
            }
        }.commit()

        clearTopBarSearch()
        binding.root.post { applyTopBarForCurrentTab() }
        updateBottomNavSelection()
    }

    private fun createFragment(tab: BottomTab): Fragment = when (tab) {
        BottomTab.HOME -> HomeFragment()
        BottomTab.REMINDER -> ReminderFragment()
        BottomTab.BOOKMARK -> BookMarkFragment()
        BottomTab.ARCHIVE -> ArchiveFragment()
        BottomTab.MORE -> MoreFragment()
    }

    private fun updateBottomNavSelection() {
        val horizontalInset = (2 * resources.displayMetrics.density).toInt()
        val tabs = listOf(
            NavItemViews(BottomTab.HOME, binding.navHome, binding.navHomeContainer, binding.navHomeText),
            NavItemViews(BottomTab.REMINDER, binding.navReminder, binding.navReminderContainer, binding.navReminderText),
            NavItemViews(BottomTab.BOOKMARK, binding.navBookmark, binding.navBookmarkContainer, binding.navBookmarkText),
            NavItemViews(BottomTab.ARCHIVE, binding.navArchive, binding.navArchiveContainer, binding.navArchiveText),
            NavItemViews(BottomTab.MORE, binding.navMore, binding.navMoreContainer, binding.navMoreText)
        )

        tabs.forEach { item ->
            val isSelected = selectedTab == item.tab
            item.slot.isSelected = isSelected
            item.label.visibility = if (isSelected) View.VISIBLE else View.GONE

            // Selected tab sizes to full label; others share leftover space.
            item.slot.updateLayoutParams<LinearLayout.LayoutParams> {
                if (isSelected) {
                    width = ViewGroup.LayoutParams.WRAP_CONTENT
                    weight = 0f
                } else {
                    width = 0
                    weight = 1f
                }
            }

            item.pill.updateLayoutParams<FrameLayout.LayoutParams> {
                width = ViewGroup.LayoutParams.WRAP_CONTENT
                height = (52 * resources.displayMetrics.density).toInt()
                gravity = android.view.Gravity.CENTER
                marginStart = if (isSelected) horizontalInset else 0
                marginEnd = if (isSelected) horizontalInset else 0
            }
        }
    }

    private data class NavItemViews(
        val tab: BottomTab,
        val slot: View,
        val pill: View,
        val label: View
    )

    private fun checkReminderPermissions() {
        if (permissionHelper.hasReminderPermissions(this)) return
        ReminderPermissionBottomSheet.show(
            fragmentManager = supportFragmentManager,
            onAllow = { requestReminderPermissions() }
        )
    }

    private fun requestReminderPermissions() {
        if (permissionHelper.checkNotificationPermission(this) != PermissionHelper.PermissionStatus.GRANTED &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        requestExactAlarmPermissionIfNeeded()
    }

    private fun requestExactAlarmPermissionIfNeeded() {
        if (permissionHelper.canScheduleExactAlarms()) return
        permissionHelper.createExactAlarmSettingsIntent()?.let { intent ->
            requestExactAlarmPermissionLauncher.launch(intent)
        }
    }
}
