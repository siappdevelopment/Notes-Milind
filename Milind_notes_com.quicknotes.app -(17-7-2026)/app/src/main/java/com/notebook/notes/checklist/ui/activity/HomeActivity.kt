package com.notebook.notes.checklist.ui.activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.content.res.ColorStateList
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.doAfterTextChanged
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.notebook.notes.checklist.Advertisement.ADSBannerSmall
import com.notebook.notes.checklist.Advertisement.ADSConsentManager
import com.notebook.notes.checklist.Advertisement.ADSHomeInterDisplayClick
import com.notebook.notes.checklist.Advertisement.ADSInterDisplayClick
import com.notebook.notes.checklist.Advertisement.ADSMainClass
import com.notebook.notes.checklist.Advertisement.ADSNativeDisplay
import com.notebook.notes.checklist.Advertisement.ADSUtilitis
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.ui.activity.SettingActivity
import com.notebook.notes.checklist.viewmodel.utils.launchActivity
import com.notebook.notes.checklist.viewmodel.utils.openWithHomeInterAd
import com.notebook.notes.checklist.databinding.LayoutHomeTopBarBinding
import com.notebook.notes.checklist.databinding.ActivityHomeBinding
import com.notebook.notes.checklist.databinding.ItemNavDrawerRowBinding
import com.notebook.notes.checklist.databinding.LayoutNavDrawerBinding
import com.notebook.notes.checklist.model.Category
import com.notebook.notes.checklist.model.CategoryWithCount
import com.notebook.notes.checklist.repository.NoteRepository
import com.notebook.notes.checklist.sort.NoteSortPreference
import com.notebook.notes.checklist.ui.adapter.NavDrawerCategoryAdapter
import com.notebook.notes.checklist.ui.category.CategorySelectorPopup
import com.notebook.notes.checklist.ui.category.CreateLabelBottomSheet
import com.notebook.notes.checklist.ui.home.HomeMoreOptionsPopup
import com.notebook.notes.checklist.ui.fragment.ArchiveFragment
import com.notebook.notes.checklist.ui.fragment.BookMarkFragment
import com.notebook.notes.checklist.ui.fragment.HomeFragment
import com.notebook.notes.checklist.ui.fragment.MoreFragment
import com.notebook.notes.checklist.ui.fragment.ReminderFragment
import com.notebook.notes.checklist.util.DateTimeUtils
import com.notebook.notes.checklist.viewmodel.CategoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.text.equals

@AndroidEntryPoint
class HomeActivity : BaseActivity() {

    companion object {

        private const val TAG_HOME = "home"
        private const val TAG_REMINDER = "reminder"
        private const val TAG_BOOKMARK = "bookmark"
        private const val TAG_ARCHIVE = "archive"
        private const val TAG_MORE = "more"
        private const val MAX_VISIBLE_CATEGORY_ROWS = 3
        private const val SELECTION_LAYOUT_ANIM_MS = 220L
    }

    private lateinit var binding: ActivityHomeBinding
    private lateinit var topBarBinding: LayoutHomeTopBarBinding
    private lateinit var drawerBinding: LayoutNavDrawerBinding
    @Inject
    lateinit var noteRepository: NoteRepository

    @Inject
    lateinit var sortPreference: NoteSortPreference

    @Inject
    lateinit var premiumManager: com.notebook.notes.checklist.iap.PremiumManager

    @Inject
    lateinit var sessionManager: com.notebook.notes.checklist.flow.SessionManager

    private val categoryViewModel: CategoryViewModel by viewModels()
    private lateinit var drawerCategoryAdapter: NavDrawerCategoryAdapter
    private var categoryItemHeightPx = 0

    private var selectedTab = BottomTab.HOME
    private var navIndicatorAnimator: ValueAnimator? = null
    private var hasPositionedNavIndicator = false
    private var wasImeVisible = false
    private var isNoteSelectionMode = false

    private enum class BottomTab(val tag: String) {
        HOME(TAG_HOME),
        REMINDER(TAG_REMINDER),
        BOOKMARK(TAG_BOOKMARK),
        ARCHIVE(TAG_ARCHIVE),
        MORE(TAG_MORE);
    }
    private lateinit var appUpdateManager: AppUpdateManager
    private val APP_UPDATE_REQUEST_CODE = 1001


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        topBarBinding = binding.layoutTopBar
        drawerBinding = binding.navDrawer
        preloadInterAds()
        appUpdateManager = AppUpdateManagerFactory.create(this)
        if (ADSMainClass.shouldShowAppUpdate()) {
            checkForUpdates()
        }


        ViewCompat.setOnApplyWindowInsetsListener(binding.drawerLayout) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.main.updatePadding(
                left = systemBars.left,
                top = systemBars.top,
                right = systemBars.right
            )
            // Keep XML bottom padding so surround shadow is not clipped by the nav bar.
            val shadowPadBottom = resources.getDimensionPixelSize(R.dimen.bottom_nav_shadow_padding)
            binding.bottomNavContainer.updatePadding(bottom = systemBars.bottom + shadowPadBottom)
            drawerBinding.drawerHeader.root.updatePadding(top = systemBars.top)

            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            if (wasImeVisible && !imeVisible && topBarBinding.etSearch.hasFocus()) {
                topBarBinding.etSearch.clearFocus()
                binding.main.requestFocus()
            }
            wasImeVisible = imeVisible

            insets
        }

        setupNavigationDrawer()
        setupTopBar()
        setupBottomNavigation()
        setupSelectionToolbar()
        setupBackPressHandler()
        setupDrawerFocusGuard()
        if (savedInstanceState == null) {
            showTab(BottomTab.HOME, countInterAd = false)
        } else {
            selectedTab = BottomTab.entries.firstOrNull {
                supportFragmentManager.findFragmentByTag(it.tag)?.isVisible == true
            } ?: BottomTab.HOME
            updateBottomNavSelection()
            binding.root.post {
                applyTopBarForCurrentTab()
                getTopBarCallback()?.syncTopBarNotesCount()
            }
        }
        loadAds()

    }

    private fun loadAds() {
        if (!ADSMainClass.getHomeScreenBottomShow()) {
            hideOtherScreenAdPlaceholders()
            return
        }

        if (!ADSConsentManager.canRequestAds(this) || ADSMainClass.getAds_Free()) {
            hideOtherScreenAdPlaceholders()
            return
        }

        if (ADSMainClass.getHomeScreenAdsType().equals("native")) {
            ADSNativeDisplay.loadAdmobNativeAdBig(
                ADSMainClass.getStringValue(ADSMainClass.HOME_SCREEN_NATIVE),
                findViewById(R.id.flNativeSmallPlaceholder),
                findViewById(R.id.shimmer_container_banner),
                "small",
                this
            )
        } else {
            ADSBannerSmall.loadAdMobBanner(
                ADSMainClass.getStringValue(ADSMainClass.HOME_SCREEN_BANNER),
                findViewById(R.id.flBannerSmallPlaceholder),
                findViewById(R.id.shimmer_container_banner),
                this
            )
        }
    }

    private fun hideOtherScreenAdPlaceholders() {
        findViewById<View>(R.id.shimmer_container_banner)?.visibility = View.GONE
        findViewById<FrameLayout>(R.id.flNativeSmallPlaceholder)?.apply {
            removeAllViews()
            visibility = View.GONE
        }
        findViewById<FrameLayout>(R.id.flBannerSmallPlaceholder)?.apply {
            removeAllViews()
            visibility = View.GONE
        }
    }

    /** Hides home banner/native ads once the derived ads-free flag follows premium. */
    private fun refreshAdsForPremium() {
        if (ADSMainClass.getAds_Free()) {
            hideOtherScreenAdPlaceholders()
        }
    }

    override fun onResume() {
        super.onResume()
        DateTimeUtils.refresh(this)
        refreshVisibleListDateTimeFormat()
        // Back from Paywall after purchase — remove ads immediately.
        refreshAdsForPremium()
        // Warm app opens skip Splash — still evaluate session paywall here (5, 10, 15…).
        binding.root.post {
            if (!isFinishing) {
                sessionManager.maybePresentSessionPaywallFromHome(this)
            }
        }
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
        clearTopBarSearchFocus()
        setTopBarSearchFocusEnabled(false)
        binding.drawerLayout.openDrawer(GravityCompat.START)
        scrollDrawerCategoriesToSelected()
    }

    fun openArchiveTab() {
        showTab(BottomTab.ARCHIVE)
    }

    fun updateTopBarNotesCount(count: Int) {
        if (selectedTab == BottomTab.MORE) return
        topBarBinding.tvNotesCountNumber.text = count.toString()
    }

    /**
     * Only the currently selected tab may update the shared count.
     * Hidden list fragments stay STARTED (hide/show) and would otherwise overwrite it.
     */
    fun canUpdateTopBarNotesCount(fragment: Fragment): Boolean {
        if (selectedTab == BottomTab.MORE) return false
        return supportFragmentManager.findFragmentByTag(selectedTab.tag) === fragment
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

    /**
     * Opening/closing the drawer (or tapping upgrade) must not hand focus to the
     * home search field behind the drawer — that opens the keyboard unexpectedly.
     */
    private fun setupDrawerFocusGuard() {
        binding.main.isFocusableInTouchMode = true
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                if (slideOffset > 0f) clearTopBarSearchFocus()
            }

            override fun onDrawerOpened(drawerView: View) {
                setTopBarSearchFocusEnabled(false)
                clearTopBarSearchFocus()
            }

            override fun onDrawerClosed(drawerView: View) {
                setTopBarSearchFocusEnabled(true)
                clearTopBarSearchFocus()
                binding.main.requestFocus()
                resetDrawerToFreshState()
            }
        })
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            setTopBarSearchFocusEnabled(false)
            clearTopBarSearchFocus()
        }
    }

    private fun resetDrawerToFreshState() {
        drawerBinding.drawerScroll.scrollTo(0, 0)
        drawerBinding.rvDrawerCategories.scrollToPosition(0)
    }

    private fun setTopBarSearchFocusEnabled(enabled: Boolean) {
        topBarBinding.etSearch.isFocusable = enabled
        topBarBinding.etSearch.isFocusableInTouchMode = enabled
        topBarBinding.searchContainer.isFocusable = false
        topBarBinding.searchContainer.isFocusableInTouchMode = false
    }

    private fun clearTopBarSearchFocus() {
        if (topBarBinding.etSearch.hasFocus() || topBarBinding.searchContainer.hasFocus()) {
            topBarBinding.etSearch.clearFocus()
            topBarBinding.searchContainer.clearFocus()
            ViewCompat.getWindowInsetsController(topBarBinding.etSearch)
                ?.hide(WindowInsetsCompat.Type.ime())
        }
    }

    private fun updateDrawerCategoriesHeight(itemCount: Int) {
        val recyclerView = drawerBinding.rvDrawerCategories
        val canScrollCategories = itemCount > MAX_VISIBLE_CATEGORY_ROWS
        val targetHeight = if (!canScrollCategories) {
            ViewGroup.LayoutParams.WRAP_CONTENT
        } else {
            val itemHeight = resolveCategoryItemHeight(recyclerView)
            if (itemHeight <= 0) {
                recyclerView.post { updateDrawerCategoriesHeight(itemCount) }
                return
            }
            itemHeight * MAX_VISIBLE_CATEGORY_ROWS +
                recyclerView.paddingTop +
                recyclerView.paddingBottom
        }

        val params = recyclerView.layoutParams
        if (params.height != targetHeight) {
            params.height = targetHeight
            recyclerView.layoutParams = params
        }
        recyclerView.setHasFixedSize(canScrollCategories)
        // Clip only when the list scrolls so rows cannot paint over section headers.
        // Keep clipping off for short lists so the selected-row shadow can still show.
        drawerBinding.categoryListContainer.clipChildren = canScrollCategories
        drawerBinding.categoryListContainer.clipToPadding = canScrollCategories
        recyclerView.clipChildren = canScrollCategories
        recyclerView.post { updateCategoryFadeOverlays() }
    }

    private fun updateCategoryFadeOverlays() {
        val recyclerView = drawerBinding.rvDrawerCategories
        val hasOverflow = drawerCategoryAdapter.itemCount > MAX_VISIBLE_CATEGORY_ROWS
        drawerBinding.categoryFadeTop.isVisible =
            hasOverflow && recyclerView.canScrollVertically(-1)
        drawerBinding.categoryFadeBottom.isVisible =
            hasOverflow && recyclerView.canScrollVertically(1)
    }

    private fun resolveCategoryItemHeight(recyclerView: RecyclerView): Int {
        if (categoryItemHeightPx > 0) return categoryItemHeightPx

        val laidOutChild = recyclerView.getChildAt(0)
        if (laidOutChild != null && laidOutChild.height > 0) {
            categoryItemHeightPx = laidOutChild.height
            return categoryItemHeightPx
        }

        if (drawerCategoryAdapter.itemCount == 0) return 0

        val viewHolder = drawerCategoryAdapter.createViewHolder(
            recyclerView,
            drawerCategoryAdapter.getItemViewType(0)
        )
        drawerCategoryAdapter.onBindViewHolder(viewHolder, 0)

        val width = when {
            recyclerView.width > 0 -> recyclerView.width
            recyclerView.measuredWidth > 0 -> recyclerView.measuredWidth
            else -> resources.getDimensionPixelSize(R.dimen.nav_drawer_width)
        }
        val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        viewHolder.itemView.measure(widthSpec, heightSpec)
        val measuredHeight = viewHolder.itemView.measuredHeight
        if (measuredHeight <= 0) return 0

        categoryItemHeightPx = measuredHeight
        return categoryItemHeightPx
    }

    private fun scrollDrawerCategoriesToSelected() {
        val position = drawerCategoryAdapter.indexOfSelected()
        if (position < 0) return

        drawerBinding.rvDrawerCategories.post {
            val layoutManager =
                drawerBinding.rvDrawerCategories.layoutManager as? LinearLayoutManager
                    ?: return@post
            layoutManager.scrollToPositionWithOffset(position, 0)
            drawerBinding.rvDrawerCategories.post { updateCategoryFadeOverlays() }
        }
    }

    private fun setupNavigationDrawer() {
        drawerBinding.sectionCategories.tvSectionTitle.text = getString(R.string.drawer_section_categories)
        drawerBinding.sectionOthers.tvSectionTitle.text = getString(R.string.drawer_section_others)
        drawerBinding.sectionAbout.tvSectionTitle.text = getString(R.string.drawer_section_about)

        drawerBinding.upgradeCard.root.setOnClickListener {
            clearTopBarSearchFocus()
            closeDrawer()
            startActivity(Intent(this, com.notebook.notes.checklist.iap.PaywallActivity::class.java))
        }
        drawerBinding.upgradeCard.root.isVisible = !premiumManager.isPremium()

        // Observe only. Entitlement syncing belongs to the process lifecycle (see MyApplication);
        // triggering a restore from here ran one on every return to Home — including the return from
        // the paywall, whose response then raced the just-completed purchase.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                premiumManager.isPremiumFlow.collect { isPremium ->
                    drawerBinding.upgradeCard.root.isVisible = !isPremium
                    applyTopBarForCurrentTab()
                    if (isPremium) {
                        refreshAdsForPremium()
                    }
                }
            }
        }

        drawerCategoryAdapter = NavDrawerCategoryAdapter { category ->
            categoryViewModel.selectCategory(category.id)
            showTab(BottomTab.HOME)
            closeDrawer()
        }
        drawerBinding.rvDrawerCategories.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = drawerCategoryAdapter
            itemAnimator = null
            isNestedScrollingEnabled = true
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    updateCategoryFadeOverlays()
                }
            })
        }

        bindDrawerRow(drawerBinding.rowBookmark.root, R.drawable.ic_drawer_bookmark, R.string.drawer_bookmark) {
            showTab(BottomTab.BOOKMARK)
        }
        bindDrawerRow(drawerBinding.rowArchive.root, R.drawable.ic_drawer_archive, R.string.drawer_archive) {
            showTab(BottomTab.ARCHIVE)
        }
        bindDrawerRow(drawerBinding.rowRecycleBin.root, R.drawable.ic_recylerbin, R.string.drawer_recycle_bin) {
            openActivityWithInterAd {
                launchActivity<RecycleBinActivity>()
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    categoryViewModel.uiState,
                    noteRepository.observeFavoriteNotesCount(),
                    noteRepository.observeArchivedNotesCount(),
                    noteRepository.observeDeletedNotesCount()
                ) { state, favoriteCount, archivedCount, deletedCount ->
                    DrawerCountsSnapshot(
                        categoriesWithCount = state.categoriesWithCount,
                        selectedCategoryId = state.selectedCategoryId,
                        favoriteCount = favoriteCount,
                        archivedCount = archivedCount,
                        deletedCount = deletedCount
                    )
                }.collect { snapshot ->
                    drawerCategoryAdapter.submitCategories(
                        categories = snapshot.categoriesWithCount,
                        selectedCategoryId = snapshot.selectedCategoryId
                    ) {
                        updateDrawerCategoriesHeight(snapshot.categoriesWithCount.size)
                        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                            scrollDrawerCategoriesToSelected()
                        }
                    }
                    updateDrawerRowCount(drawerBinding.rowBookmark.root, snapshot.favoriteCount)
                    updateDrawerRowCount(drawerBinding.rowArchive.root, snapshot.archivedCount)
                    updateDrawerRowCount(drawerBinding.rowRecycleBin.root, snapshot.deletedCount)
                }
            }
        }
        bindDrawerRow(drawerBinding.rowSettings.root, R.drawable.ic_drawer_setting, R.string.drawer_settings) {
            openActivityWithInterAd {
                launchActivity<SettingActivity>()
            }
        }
        bindDrawerRow(drawerBinding.rowPrivacyPolicy.root, R.drawable.ic_privacy_policy, R.string.drawer_privacy_policy) {
            showComingSoon()
        }
        bindDrawerRow(drawerBinding.rowRateUs.root, R.drawable.ic_rate_us, R.string.drawer_rate_us) {
            openRateUs()
        }
        bindDrawerRow(drawerBinding.rowShareApp.root, R.drawable.ic_drawer_share, R.string.drawer_share_app) {
            shareApp()
        }
        bindDrawerRow(drawerBinding.rowFeedback.root, R.drawable.ic_drawer_feedback, R.string.drawer_feedback) {
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

    private fun updateDrawerRowCount(rowRoot: View, count: Int) {
        val rowBinding = ItemNavDrawerRowBinding.bind(rowRoot)
        val purple = ContextCompat.getColor(this, R.color.app_primary_color)
        rowBinding.tvCount.visibility = View.VISIBLE
        rowBinding.tvCount.text = count.toString()
        rowBinding.tvCount.setBackgroundResource(R.drawable.bg_nav_drawer_count_badge_unselected)
        rowBinding.tvCount.setTextColor(purple)
    }

    private data class DrawerCountsSnapshot(
        val categoriesWithCount: List<CategoryWithCount>,
        val selectedCategoryId: Long?,
        val favoriteCount: Int,
        val archivedCount: Int,
        val deletedCount: Int
    )

    private fun openActivityWithInterAd(navigate: () -> Unit) {
        openWithHomeInterAd(navigate)
    }

    private fun showComingSoon() {
        Toast.makeText(this, R.string.drawer_coming_soon, Toast.LENGTH_SHORT).show()
    }

    private fun openRateUs() {
        com.notebook.notes.checklist.ui.dialog.RatingBottomSheet().apply {
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
                    openActivityWithInterAd {
                        launchActivity<FeedbackActivity>()
                    }
                }
            }
        }.show(supportFragmentManager, com.notebook.notes.checklist.ui.dialog.RatingBottomSheet.TAG)
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
        openActivityWithInterAd {
            launchActivity<FeedbackActivity>()
        }
    }

    private fun setupTopBar() {
        topBarBinding.btnMenu.setOnClickListener { openDrawer() }
        topBarBinding.btnPro.setOnClickListener {
            startActivity(Intent(this, com.notebook.notes.checklist.iap.PaywallActivity::class.java))
        }
        topBarBinding.btnSettings.setOnClickListener {
            openActivityWithInterAd {
                launchActivity<SettingActivity> { }
            }
        }
        topBarBinding.btnMore.setOnClickListener { anchor ->
            showTopBarMoreOptions(anchor)
        }
        topBarBinding.etSearch.doAfterTextChanged { editable ->
            getTopBarCallback()?.onTopBarSearchQuery(editable?.toString().orEmpty())
        }
        topBarBinding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            ) {
                topBarBinding.etSearch.clearFocus()
                ViewCompat.getWindowInsetsController(topBarBinding.etSearch)
                    ?.hide(WindowInsetsCompat.Type.ime())
                binding.main.requestFocus()
                true
            } else {
                false
            }
        }
    }

    fun onNoteSelectionModeChanged(inSelectionMode: Boolean, selectedCount: Int) {
        if (!supportsSelectionToolbar(selectedTab)) return

        if (isNoteSelectionMode != inSelectionMode) {
            isNoteSelectionMode = inSelectionMode
            TransitionManager.beginDelayedTransition(
                binding.main,
                AutoTransition().apply {
                    duration = SELECTION_LAYOUT_ANIM_MS
                    interpolator = DecelerateInterpolator()
                }
            )
            binding.bottomNavCard.isVisible = !inSelectionMode
            binding.selectionToolbar.root.isVisible = inSelectionMode
            topBarBinding.selectionTopBar.isVisible = inSelectionMode
            topBarBinding.homeToolbar.isVisible = !inSelectionMode
            topBarBinding.homeStatusRow.isVisible = !inSelectionMode
            topBarBinding.searchContainer.isVisible = !inSelectionMode
            if (inSelectionMode) {
                clearTopBarSearchFocus()
            }
        }

        topBarBinding.tvSelectionCount.text = getString(R.string.notes_selected, selectedCount)
        updateSelectionActionsVisibility(selectedCount)
        updateSelectionPinState()
        updateSelectionLockState()
        updateSelectionArchiveState()
        val host = getSelectionHost()
        val allSelected = host?.isAllNotesSelected() == true
        topBarBinding.btnSelectAllNotes.setText(
            if (allSelected) R.string.deselect_all else R.string.select_all
        )
    }

    private fun supportsSelectionToolbar(tab: BottomTab): Boolean = when (tab) {
        BottomTab.HOME, BottomTab.REMINDER, BottomTab.BOOKMARK, BottomTab.ARCHIVE -> true
        BottomTab.MORE -> false
    }

    private fun updateSelectionActionsVisibility(selectedCount: Int) {
        val toolbar = binding.selectionToolbar
        val host = getSelectionHost()
        val singleSelected = selectedCount == 1
        val showReminder = singleSelected && host?.showsSelectionReminderAction() != false
        toolbar.btnSelectionReminder.visibility =
            if (showReminder) View.VISIBLE else View.GONE
        toolbar.btnSelectionCopy.visibility =
            if (singleSelected) View.VISIBLE else View.GONE
    }

    private fun updateSelectionPinState() {
        val toolbar = binding.selectionToolbar
        val allPinned = getSelectionHost()?.areAllSelectedNotesPinned() == true
        if (allPinned) {
            toolbar.ivSelectionPin.setImageResource(R.drawable.ic_unpin)
            toolbar.tvSelectionPin.setText(R.string.unpin_note)
            toolbar.ivSelectionPin.contentDescription = getString(R.string.unpin_note)
        } else {
            toolbar.ivSelectionPin.setImageResource(R.drawable.ic_op_pin)
            toolbar.tvSelectionPin.setText(R.string.pin_note)
            toolbar.ivSelectionPin.contentDescription = getString(R.string.pin_note)
        }
    }

    private fun updateSelectionLockState() {
        val toolbar = binding.selectionToolbar
        val allLocked = getSelectionHost()?.areAllSelectedNotesLocked() == true
        if (allLocked) {
            toolbar.ivSelectionLock.setImageResource(R.drawable.ic_unlock)
            toolbar.tvSelectionLock.setText(R.string.unlock_note)
            toolbar.ivSelectionLock.contentDescription = getString(R.string.unlock_note)
        } else {
            toolbar.ivSelectionLock.setImageResource(R.drawable.ic_lock)
            toolbar.tvSelectionLock.setText(R.string.lock_note)
            toolbar.ivSelectionLock.contentDescription = getString(R.string.lock_note)
        }
    }

    private fun updateSelectionArchiveState() {
        val allArchived = getSelectionHost()?.areAllSelectedNotesArchived() == true
        if (allArchived) {
            topBarBinding.btnSelectionArchive.setImageResource(R.drawable.ic_un_arhive)
            topBarBinding.btnSelectionArchive.contentDescription = getString(R.string.unarchive_note)
        } else {
            topBarBinding.btnSelectionArchive.setImageResource(R.drawable.ic_op_archive)
            topBarBinding.btnSelectionArchive.contentDescription = getString(R.string.archive_note)
        }
    }

    private fun setupSelectionToolbar() {
        val toolbar = binding.selectionToolbar
        toolbar.btnSelectionLock.setOnClickListener {
            getSelectionHost()?.handleBulkLock()
        }
        toolbar.btnSelectionPin.setOnClickListener {
            getSelectionHost()?.handleBulkPin()
        }
        toolbar.btnSelectionReminder.setOnClickListener {
            getSelectionHost()?.handleBulkReminder()
        }
        toolbar.btnSelectionCopy.setOnClickListener {
            getSelectionHost()?.handleBulkCopy()
        }
        toolbar.btnSelectionDelete.setOnClickListener {
            getSelectionHost()?.handleBulkDelete()
        }

        topBarBinding.btnCancelSelection.setOnClickListener {
            getSelectionHost()?.exitSelectionMode()
        }
        topBarBinding.btnSelectionArchive.setOnClickListener {
            getSelectionHost()?.handleBulkArchive()
        }
        topBarBinding.btnSelectAllNotes.setOnClickListener {
            getSelectionHost()?.selectAllNotes()
        }
    }

    private fun getSelectionHost(): NoteSelectionCallback? {
        return supportFragmentManager.findFragmentByTag(selectedTab.tag) as? NoteSelectionCallback
    }

    private fun getTopBarCallback(): HomeTopBarCallback? {
        val fragment = supportFragmentManager.findFragmentByTag(selectedTab.tag)
        return fragment as? HomeTopBarCallback
    }

    private fun showTopBarMoreOptions(anchor: View) {
        val callback = getTopBarCallback() ?: return
        HomeMoreOptionsPopup.show(
            context = this,
            anchor = anchor,
            isGridView = callback.isNotesGridView(),
            selectedSort = sortPreference.current,
            showReminderSort = selectedTab != BottomTab.REMINDER,
            onViewModeClick = { callback.toggleNotesViewMode() },
            onSelectClick = { getSelectionHost()?.enterSelectionSelectingAll() },
            onSortSelected = { order -> sortPreference.setSortOrder(order) }
        )
    }

    private fun applyTopBarForCurrentTab() {
        val showTopBar = selectedTab != BottomTab.MORE
        topBarBinding.root.isVisible = showTopBar
        if (!showTopBar) return

        val config = getTopBarCallback()?.getTopBarConfiguration()
            ?: defaultTopBarConfiguration(selectedTab)

        topBarBinding.tvHomeTitle.setText(config.titleRes)
        topBarBinding.btnPro.isVisible = config.showProBadge && !premiumManager.isPremium()
        topBarBinding.btnSettings.isVisible = config.showSettings
        topBarBinding.autoSavedBadge.isVisible = config.showAutoSavedBadge
    }

    private fun defaultTopBarConfiguration(tab: BottomTab): HomeTopBarConfiguration = when (tab) {
        BottomTab.HOME -> HomeTopBarConfiguration()
        BottomTab.REMINDER -> HomeTopBarConfiguration(
            titleRes = R.string.nav_reminders,
            showAutoSavedBadge = false
        )
        BottomTab.BOOKMARK -> HomeTopBarConfiguration(
            titleRes = R.string.nav_bookmark,
            showAutoSavedBadge = false
        )
        BottomTab.ARCHIVE -> HomeTopBarConfiguration(
            titleRes = R.string.nav_archive,
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
                val selectionHost = getSelectionHost()
                if (selectionHost?.exitSelectionModeIfActive() == true) {
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

    private fun showTab(tab: BottomTab, countInterAd: Boolean = true) {
        if (selectedTab == tab && supportFragmentManager.findFragmentByTag(tab.tag)?.isVisible == true) {
            updateBottomNavSelection()
            return
        }

        if (!countInterAd) {
            performShowTab(tab)
            return
        }


        ADSInterDisplayClick.ADSBottomNavDisplayInterstitial(
            this,
            ADSMainClass.getStringValue(ADSMainClass.INTER_FIRST_TIME),
            {
                performShowTab(tab)
            }
        )
    }

    private fun performShowTab(tab: BottomTab) {
        getSelectionHost()?.exitSelectionModeIfActive()

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
        binding.root.post {
            applyTopBarForCurrentTab()
            getTopBarCallback()?.syncTopBarNotesCount()
        }
        updateBottomNavSelection()
    }

    private fun createFragment(tab: BottomTab): Fragment = when (tab) {
        BottomTab.HOME -> HomeFragment()
        BottomTab.REMINDER -> ReminderFragment()
        BottomTab.BOOKMARK -> BookMarkFragment()
        BottomTab.ARCHIVE -> ArchiveFragment()
        BottomTab.MORE -> MoreFragment()
    }

    private fun updateBottomNavSelection(animateIndicator: Boolean = true) {
        val horizontalInset = (2 * resources.displayMetrics.density).toInt()
        val tabs = bottomNavItems()
        val selectedPill = tabs.first { it.tab == selectedTab }.pill
        val unselectedTint = ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.bottom_nav_icon_unselected)
        )
        val selectedTint = ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.white)
        )

        tabs.forEach { item ->
            val isSelected = selectedTab == item.tab
            item.slot.isSelected = isSelected
            item.label.visibility = if (isSelected) View.VISIBLE else View.GONE
            applyBottomNavIcon(item, isSelected, unselectedTint, selectedTint)

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
                height = resources.getDimensionPixelSize(R.dimen.bottom_nav_selected_pill_height)
                gravity = android.view.Gravity.CENTER
                marginStart = if (isSelected) horizontalInset else 0
                marginEnd = if (isSelected) horizontalInset else 0
            }
        }

        binding.bottomNavCustom.post {
            animateNavIndicatorTo(selectedPill, animate = animateIndicator && hasPositionedNavIndicator)
        }
    }

    private fun applyBottomNavIcon(
        item: NavItemViews,
        isSelected: Boolean,
        unselectedTint: ColorStateList,
        selectedTint: ColorStateList
    ) {
        val iconRes = if (isSelected) item.selectedIconRes else item.unselectedIconRes
        item.icon.setImageResource(iconRes)
        // Selected assets already include white/purple colors; clear tint so accents stay.
        ImageViewCompat.setImageTintList(
            item.icon,
            when {
                isSelected && item.clearTintWhenSelected -> null
                isSelected -> selectedTint
                else -> unselectedTint
            }
        )
    }

    private fun bottomNavItems(): List<NavItemViews> = listOf(
        NavItemViews(
            tab = BottomTab.HOME,
            slot = binding.navHome,
            pill = binding.navHomeContainer,
            label = binding.navHomeText,
            icon = binding.navHomeIcon,
            unselectedIconRes = R.drawable.ic_nv_home,
            selectedIconRes = R.drawable.ic_nv_selected_home,
            clearTintWhenSelected = true
        ),
        NavItemViews(
            tab = BottomTab.REMINDER,
            slot = binding.navReminder,
            pill = binding.navReminderContainer,
            label = binding.navReminderText,
            icon = binding.navReminderIcon,
            unselectedIconRes = R.drawable.ic_nv_reminder,
            selectedIconRes = R.drawable.ic_selected_nv_reminder,
            clearTintWhenSelected = true
        ),
        NavItemViews(
            tab = BottomTab.BOOKMARK,
            slot = binding.navBookmark,
            pill = binding.navBookmarkContainer,
            label = binding.navBookmarkText,
            icon = binding.navBookmarkIcon,
            unselectedIconRes = R.drawable.ic_nv_bookmark,
            selectedIconRes = R.drawable.ic_selected_nv_bookmark,
            clearTintWhenSelected = true
        ),
        NavItemViews(
            tab = BottomTab.ARCHIVE,
            slot = binding.navArchive,
            pill = binding.navArchiveContainer,
            label = binding.navArchiveText,
            icon = binding.navArchiveIcon,
            unselectedIconRes = R.drawable.ic_nv_archive,
            selectedIconRes = R.drawable.ic_selected_nv_archive,
            clearTintWhenSelected = true
        ),
        NavItemViews(
            tab = BottomTab.MORE,
            slot = binding.navMore,
            pill = binding.navMoreContainer,
            label = binding.navMoreText,
            icon = binding.navMoreIcon,
            unselectedIconRes = R.drawable.ic_nv_more,
            selectedIconRes = R.drawable.ic_selected_nv_more,
            clearTintWhenSelected = true
        )
    )

    private fun animateNavIndicatorTo(target: View, animate: Boolean) {
        val indicator = binding.navIndicator
        val parent = indicator.parent as? ViewGroup ?: return
        if (target.width <= 0 || target.height <= 0) {
            target.post { animateNavIndicatorTo(target, animate) }
            return
        }

        val targetLoc = IntArray(2)
        val parentLoc = IntArray(2)
        target.getLocationInWindow(targetLoc)
        parent.getLocationInWindow(parentLoc)

        val endX = (targetLoc[0] - parentLoc[0]).toFloat()
        val endY = (targetLoc[1] - parentLoc[1]).toFloat()
        val endWidth = target.width
        val endHeight = target.height

        navIndicatorAnimator?.let { running ->
            navIndicatorAnimator = null
            running.cancel()
        }

        if (!animate || indicator.isInvisible || !hasPositionedNavIndicator) {
            indicator.translationX = endX
            indicator.translationY = endY
            indicator.updateLayoutParams<FrameLayout.LayoutParams> {
                width = endWidth
                height = endHeight
                gravity = android.view.Gravity.START or android.view.Gravity.TOP
                marginStart = 0
                topMargin = 0
            }
            indicator.isInvisible = false
            hasPositionedNavIndicator = true
            return
        }

        val startX = indicator.translationX
        val startY = indicator.translationY
        val startWidth = indicator.width
        val startHeight = indicator.height

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 280L
            interpolator = DecelerateInterpolator()
            addUpdateListener { valueAnimator ->
                val fraction = valueAnimator.animatedFraction
                indicator.translationX = startX + (endX - startX) * fraction
                indicator.translationY = startY + (endY - startY) * fraction
                indicator.updateLayoutParams<FrameLayout.LayoutParams> {
                    width = (startWidth + (endWidth - startWidth) * fraction).toInt().coerceAtLeast(1)
                    height = (startHeight + (endHeight - startHeight) * fraction).toInt().coerceAtLeast(1)
                    gravity = android.view.Gravity.START or android.view.Gravity.TOP
                }
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (navIndicatorAnimator !== animation) return
                    indicator.translationX = endX
                    indicator.translationY = endY
                    indicator.updateLayoutParams<FrameLayout.LayoutParams> {
                        width = endWidth
                        height = endHeight
                        gravity = android.view.Gravity.START or android.view.Gravity.TOP
                    }
                    navIndicatorAnimator = null
                }
            })
        }
        navIndicatorAnimator = animator
        animator.start()
        hasPositionedNavIndicator = true
    }

    private data class NavItemViews(
        val tab: BottomTab,
        val slot: View,
        val pill: View,
        val label: View,
        val icon: ImageView,
        @DrawableRes val unselectedIconRes: Int,
        @DrawableRes val selectedIconRes: Int,
        val clearTintWhenSelected: Boolean
    )

    fun preloadInterAds(){
        if (!ADSMainClass.getInterAdsLoadType().equals("Load") && ADSUtilitis.IsNetworkConnected(this@HomeActivity)) {
            ADSHomeInterDisplayClick.AdmobInterstitialAd(
                this@HomeActivity,
                ADSMainClass.getStringValue(ADSMainClass.INTER_FIRST_TIME)
            )
        }
    }
    private fun checkForUpdates() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            val updateType = if (ADSMainClass.getInAppUpdateType().equals("Immediate", ignoreCase = true)) {
                AppUpdateType.IMMEDIATE
            } else {
                AppUpdateType.FLEXIBLE
            }

            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(updateType)
            ) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        updateType,
                        this,
                        APP_UPDATE_REQUEST_CODE
                    )
                    ADSMainClass.updateAppUpdateShowCount()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

            appUpdateManager.registerListener(installStateUpdatedListener)
    }
    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            showUpdateCompletedSnackbar()
        }
    }
    private fun showUpdateCompletedSnackbar() {
//        Snackbar.make(
//            findViewById(R.id.drawer),
//            "An update has just been downloaded.",
//            Snackbar.LENGTH_INDEFINITE
//        ).apply {
//            setAction("RESTART") { appUpdateManager.completeUpdate() }
//            show()
//        }
    }


}
