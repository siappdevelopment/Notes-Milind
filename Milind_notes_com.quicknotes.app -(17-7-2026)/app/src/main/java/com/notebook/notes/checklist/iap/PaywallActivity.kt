package com.notebook.notes.checklist.iap

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.notebook.notes.checklist.Advertisement.ADSMainClass
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.ActivityPaywallBinding
import com.notebook.notes.checklist.databinding.ItemPaywallPlanBinding
import com.notebook.notes.checklist.flow.FlowManager
import com.notebook.notes.checklist.flow.FlowStep
import com.notebook.notes.checklist.iap.model.BillingError
import com.notebook.notes.checklist.iap.model.ProductInfo
import com.notebook.notes.checklist.iap.model.PurchaseEvent
import com.notebook.notes.checklist.ui.activity.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Premium upgrade screen with weekly / monthly / yearly subscriptions + lifetime.
 */
@AndroidEntryPoint
class PaywallActivity : BaseActivity() {

    @Inject lateinit var premiumManager: PremiumManager
    @Inject lateinit var flowManager: FlowManager

    private lateinit var binding: ActivityPaywallBinding
    private var purchaseInFlight = false
    /** Premium state when this screen opened, used to detect a newly granted entitlement. */
    private var wasPremiumOnEntry = false
    private var premiumCompleted = false
    private var selectedPlan: PremiumPlan = PremiumPlan.YEARLY
    private var loadedProducts: List<ProductInfo> = emptyList()
    private val bannerIndicatorDots = mutableListOf<View>()
    private var autoSwipeJob: Job? = null
    private var userDraggingBanner = false
    private var continueZoomAnimator: Animator? = null
    private val closeButtonBaseMarginTop by lazy {
        (12 * resources.displayMetrics.density).toInt()
    }

    private val bannerPages = listOf(
        PaywallBannerPage(
            imageRes = R.drawable.paywall_banner_1,
            backgroundRes = R.drawable.bg_banner_1,
            captionRes = R.string.iap_banner_caption_backgrounds,
        ),
        PaywallBannerPage(
            imageRes = R.drawable.paywall_banner_2,
            backgroundRes = R.drawable.bg_banner_2,
            captionRes = R.string.iap_banner_caption_security,
        ),
        PaywallBannerPage(
            imageRes = R.drawable.paywall_banner_3,
            backgroundRes = R.drawable.bg_banner_3,
            captionRes = R.string.ads_free_experience,
        ),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaywallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        wasPremiumOnEntry = premiumManager.isPremium()
        premiumManager.start()
        setupBannerPager()
        startContinueButtonZoom()
        bindPlanLabels()
        setupClicks()
        setupBackNavigation()
        scheduleCloseButtonReveal()
        observeState()
        loadProducts()

        if (premiumManager.isPremium()) {
            showAlreadyPremium()
        }
    }

    /**
     * System back matches close: from onboarding/session PREMIUM flow → Home;
     * otherwise finish back to the caller (e.g. HomeActivity).
     */
    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    dismissPaywall()
                }
            },
        )
    }

    private fun scheduleCloseButtonReveal() {
        binding.btnClose.isVisible = false
        lifecycleScope.launch {
            delay(CLOSE_BUTTON_DELAY_MS)
            if (!isFinishing) {
                binding.btnClose.isVisible = true
            }
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.paywallRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            // No top padding — status bar overlaps the banner layout
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)

            val closeParams = binding.btnClose.layoutParams as
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            closeParams.topMargin = statusBars.top + closeButtonBaseMarginTop
            binding.btnClose.layoutParams = closeParams

            insets
        }
        ViewCompat.requestApplyInsets(binding.paywallRoot)
    }

    private fun setupBannerPager() {
        binding.bannerPager.adapter = PaywallBannerAdapter(bannerPages)
        binding.bannerPager.offscreenPageLimit = 1
        binding.bannerPager.post {
            (binding.bannerPager.getChildAt(0) as? RecyclerView)?.apply {
                isNestedScrollingEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
            }
        }
        setupBannerIndicators(bannerPages.size)
        updateBannerPage(0)

        binding.bannerPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateBannerPage(position)
            }

            override fun onPageScrollStateChanged(state: Int) {
                userDraggingBanner = state == ViewPager2.SCROLL_STATE_DRAGGING
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    restartAutoSwipe()
                }
            }
        })
        restartAutoSwipe()
    }

    private fun restartAutoSwipe() {
        autoSwipeJob?.cancel()
        if (bannerPages.size <= 1) return
        autoSwipeJob = lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    delay(AUTO_SWIPE_INTERVAL_MS)
                    if (userDraggingBanner || purchaseInFlight) continue
                    val count = bannerPages.size
                    val next = (binding.bannerPager.currentItem + 1) % count
                    binding.bannerPager.setCurrentItem(next, true)
                }
            }
        }
    }

    private fun startContinueButtonZoom() {
        stopContinueButtonZoom()
        val button = binding.btnUpgrade
        button.post {
            if (isFinishing) return@post
            button.pivotX = button.width / 2f
            button.pivotY = button.height / 2f
            continueZoomAnimator = ObjectAnimator.ofPropertyValuesHolder(
                button,
                PropertyValuesHolder.ofFloat(View.SCALE_X, CONTINUE_ZOOM_MIN, CONTINUE_ZOOM_MAX),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, CONTINUE_ZOOM_MIN, CONTINUE_ZOOM_MAX),
            ).apply {
                duration = CONTINUE_ZOOM_DURATION_MS
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
        }
    }

    private fun stopContinueButtonZoom() {
        continueZoomAnimator?.cancel()
        continueZoomAnimator = null
        if (::binding.isInitialized) {
            binding.btnUpgrade.scaleX = 1f
            binding.btnUpgrade.scaleY = 1f
        }
    }

    private fun updateContinueButtonZoom(enabled: Boolean) {
        if (enabled) {
            if (continueZoomAnimator?.isRunning != true) {
                startContinueButtonZoom()
            }
        } else {
            stopContinueButtonZoom()
        }
    }

    override fun onDestroy() {
        autoSwipeJob?.cancel()
        stopContinueButtonZoom()
        super.onDestroy()
    }

    private fun setupBannerIndicators(count: Int) {
        binding.bannerIndicator.removeAllViews()
        bannerIndicatorDots.clear()

        repeat(count) { index ->
            val dot = View(this)
            val params = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(
                    if (index == 0) R.dimen.paywall_dot_active_width else R.dimen.paywall_dot_size,
                ),
                resources.getDimensionPixelSize(R.dimen.paywall_dot_size),
            )
            if (index > 0) {
                params.marginStart = resources.getDimensionPixelSize(R.dimen.paywall_dot_spacing)
            }
            dot.layoutParams = params
            dot.background = ContextCompat.getDrawable(
                this,
                if (index == 0) R.drawable.bg_paywall_dot_active else R.drawable.bg_paywall_dot_inactive,
            )
                binding.bannerIndicator.addView(dot)
            bannerIndicatorDots.add(dot)
        }
    }

    private fun updateBannerPage(position: Int) {
        val page = bannerPages.getOrNull(position) ?: return
        binding.tvBannerCaption.setText(page.captionRes)

        bannerIndicatorDots.forEachIndexed { index, dot ->
            val active = index == position
            val params = dot.layoutParams as LinearLayout.LayoutParams
            params.width = resources.getDimensionPixelSize(
                if (active) R.dimen.paywall_dot_active_width else R.dimen.paywall_dot_size,
            )
            params.height = resources.getDimensionPixelSize(R.dimen.paywall_dot_size)
            if (index > 0) {
                params.marginStart = resources.getDimensionPixelSize(R.dimen.paywall_dot_spacing)
            }
            dot.layoutParams = params
            dot.background = ContextCompat.getDrawable(
                this,
                if (active) R.drawable.bg_paywall_dot_active else R.drawable.bg_paywall_dot_inactive,
            )
        }
    }

    private fun bindPlanLabels() {
        binding.planWeekly.tvPlanName.setText(R.string.iap_plan_weekly)
        binding.planWeekly.tvPlanPrice.setText(R.string.iap_price_loading)

        binding.planMonthly.tvPlanName.setText(R.string.iap_plan_monthly)
        binding.planMonthly.tvPlanPrice.setText(R.string.iap_price_loading)

        binding.planYearly.tvPlanName.setText(R.string.iap_plan_yearly)
        binding.planYearly.tvPlanPrice.setText(R.string.iap_price_loading)

        binding.planLifetime.tvPlanName.setText(R.string.iap_plan_lifetime)
        binding.planLifetime.tvPlanPrice.setText(R.string.iap_price_loading)

        updatePlanSelectionUi()
    }

    private fun setupClicks() {
        binding.btnClose.setOnClickListener { dismissPaywall() }

        binding.planWeekly.root.setOnClickListener { selectPlan(PremiumPlan.WEEKLY) }
        binding.planMonthly.root.setOnClickListener { selectPlan(PremiumPlan.MONTHLY) }
        binding.planYearly.root.setOnClickListener { selectPlan(PremiumPlan.YEARLY) }
        binding.planLifetime.root.setOnClickListener { selectPlan(PremiumPlan.LIFETIME) }

        binding.btnUpgrade.setOnClickListener {
            if (premiumManager.isPremium() && premiumManager.currentPlan() == PremiumPlan.LIFETIME) {
                toast(getString(R.string.iap_already_premium))
                dismissPaywall()
                return@setOnClickListener
            }
            startPurchase()
        }

        binding.btnRestore.setOnClickListener { restorePurchase() }

        binding.btnPrivacyPolicy.setOnClickListener {
            openUrl(ADSMainClass.getPrivacyPolicy())
        }

        binding.btnTermsOfUse.setOnClickListener {
            openUrl(ADSMainClass.getTermsOfUse())
        }
    }

    private fun openUrl(rawUrl: String?) {
        val url = rawUrl.orEmpty().trim()
        if (url.isBlank()) return
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun selectPlan(plan: PremiumPlan) {
        selectedPlan = plan
        updatePlanSelectionUi()
    }

    private fun updatePlanSelectionUi() {
        stylePlan(binding.planWeekly, selectedPlan == PremiumPlan.WEEKLY, PremiumPlan.WEEKLY)
        stylePlan(binding.planMonthly, selectedPlan == PremiumPlan.MONTHLY, PremiumPlan.MONTHLY)
        stylePlan(binding.planYearly, selectedPlan == PremiumPlan.YEARLY, PremiumPlan.YEARLY)
        stylePlan(binding.planLifetime, selectedPlan == PremiumPlan.LIFETIME, PremiumPlan.LIFETIME)
    }

    private fun stylePlan(planBinding: ItemPaywallPlanBinding, selected: Boolean, plan: PremiumPlan) {
        val ctx = planBinding.root.context
        planBinding.planRoot.background = ContextCompat.getDrawable(
            ctx,
            if (selected) R.drawable.bg_paywall_plan_selected else R.drawable.bg_paywall_plan_unselected,
        )
        // Clip content so the /Week footer follows the card’s rounded bottom curve.
        planBinding.planRoot.clipToOutline = true
        planBinding.tvPlanHint.background = ContextCompat.getDrawable(
            ctx,
            if (selected) R.drawable.bg_paywall_plan_footer_selected else R.drawable.bg_paywall_plan_footer_unselected,
        )

        val hintColor = if (selected) {
            ContextCompat.getColor(ctx, R.color.white)
        } else {
            ContextCompat.getColor(ctx, R.color.app_primary_color)
        }
        planBinding.tvPlanHint.setTextColor(hintColor)

        val priceColor = if (selected) {
            ContextCompat.getColor(ctx, R.color.app_primary_color)
        } else {
            ContextCompat.getColor(ctx, R.color.text_black_2)
        }
        planBinding.tvPlanPrice.setTextColor(priceColor)

        planBinding.tvHotBadge.isVisible = plan == PremiumPlan.YEARLY
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    premiumManager.products.collect { products ->
                        Log.e("PaywallActivity", "products=$products")
                        applyProductsToUi(products)
                    }
                }
                launch {
                    premiumManager.isPremiumFlow.collect { isPremium ->
                        if (isPremium && premiumManager.currentPlan() == PremiumPlan.LIFETIME) {
                            binding.btnUpgrade.text = getString(R.string.iap_already_premium)
                        }
                        // Premium state is the source of truth: one-shot purchase events are lost
                        // while the store sheet keeps this screen stopped.
                        if (isPremium && !wasPremiumOnEntry) {
                            completePremium(getString(R.string.iap_purchase_success))
                        }
                    }
                }
            }
        }
        // Purchase outcomes are one-shot and arrive while the Play sheet still covers this screen,
        // so this collector must outlive STOPPED. Inside repeatOnLifecycle a cancelled purchase was
        // dropped and the progress bar stayed up forever.
        lifecycleScope.launch {
            premiumManager.events.collect { event ->
                handlePurchaseEvent(event)
            }
        }
    }

    private fun applyProductsToUi(products: List<ProductInfo>) {
        loadedProducts = products
        products.forEach { plan ->
            android.util.Log.d(
                "PaywallPlan",
                "PLAN → id=${plan.productId}, type=${plan.productType}, price=${plan.formattedPrice}",
            )
        }

        bindPlanPrice(binding.planWeekly, PremiumPlan.WEEKLY)
        bindPlanPrice(binding.planMonthly, PremiumPlan.MONTHLY)
        bindPlanPrice(binding.planYearly, PremiumPlan.YEARLY)
        bindPlanPrice(binding.planLifetime, PremiumPlan.LIFETIME)

        // Prefer first available plan if selected is missing.
        if (products.none { it.productId == selectedPlan.productId }) {
            val fallback = listOf(
                PremiumPlan.YEARLY,
                PremiumPlan.MONTHLY,
                PremiumPlan.WEEKLY,
                PremiumPlan.LIFETIME,
            ).firstOrNull { plan -> products.any { it.productId == plan.productId } }
            if (fallback != null) selectPlan(fallback)
        }
    }

    private fun bindPlanPrice(planBinding: ItemPaywallPlanBinding, plan: PremiumPlan) {
        val product = loadedProducts.find { it.productId == plan.productId }
        // Weekly is optional — hide until Play returns that product.
        if (plan == PremiumPlan.WEEKLY) {
            planBinding.root.isVisible = product != null
            if (product == null) return
        }

        planBinding.tvPlanPrice.text = product?.formattedPrice
            ?: getString(R.string.iap_price_unavailable)

        planBinding.tvPlanHint.text = planHintText(plan, product)
        bindOldPrice(planBinding, plan, product)

        planBinding.root.alpha = if (product != null) 1f else 0.45f
        planBinding.root.isEnabled = product != null
        planBinding.planRoot.isEnabled = product != null
    }

    private fun planHintText(plan: PremiumPlan, product: ProductInfo?): String {
        if (plan == PremiumPlan.LIFETIME) {
            return getString(R.string.iap_price_pay_once)
        }
        if (product == null) {
            return getString(R.string.iap_price_per_week, getString(R.string.iap_price_unavailable))
        }
        val weeklyMicros = PaywallPricing.weeklyEquivalentMicros(product, plan)
        val formatted = PaywallPricing.formatMoney(weeklyMicros, product.priceCurrencyCode)
            .ifBlank { product.formattedPrice }
        return getString(R.string.iap_price_per_week, formatted)
    }

    private fun bindOldPrice(
        planBinding: ItemPaywallPlanBinding,
        plan: PremiumPlan,
        product: ProductInfo?,
    ) {
        val oldMicros = if (product != null) {
            PaywallPricing.oldPriceMicros(plan, loadedProducts)
        } else {
            null
        }
        // Always format with this plan's live currency so dummy offers match Play.
        val currency = product?.priceCurrencyCode
            ?.takeIf { it.isNotBlank() }
            ?: PaywallPricing.currencyCodeFor(plan, loadedProducts)
        val oldFormatted = oldMicros
            ?.takeIf { it > (product?.priceAmountMicros ?: 0L) }
            ?.let { PaywallPricing.formatMoney(it, currency) }
            ?.takeIf { it.isNotBlank() }

        if (!oldFormatted.isNullOrBlank()) {
            planBinding.tvOldPrice.isInvisible = false
            planBinding.tvOldPrice.paintFlags =
                planBinding.tvOldPrice.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            planBinding.tvOldPrice.text = oldFormatted
        } else {
            // Keep INVISIBLE (not GONE) so all plan cards stay the same height.
            planBinding.tvOldPrice.isInvisible = true
            planBinding.tvOldPrice.text = ""
        }
    }

    private fun loadProducts() {
        setLoading(true)
        lifecycleScope.launch {
            val products = premiumManager.refreshProducts()
            setLoading(false)
            applyProductsToUi(products)

        }
    }

    private fun startPurchase() {
        if (purchaseInFlight) return
        purchaseInFlight = true
        setLoading(true)

        lifecycleScope.launch {
            val products = premiumManager.refreshProducts()
            applyProductsToUi(products)
            if (products.none { it.productId == selectedPlan.productId }) {
                setLoading(false)
                purchaseInFlight = false
                showError(BillingError.ProductNotFound)
                return@launch
            }

            val error = premiumManager.purchasePlan(this@PaywallActivity, selectedPlan.productId)
            if (error != null) {
                setLoading(false)
                purchaseInFlight = false
                showError(error)
            }
        }
    }

    private fun restorePurchase() {
        if (purchaseInFlight) return
        purchaseInFlight = true
        setLoading(true)


        lifecycleScope.launch {
            when (val event = premiumManager.restorePurchases()) {
                is PurchaseEvent.Restored -> completePremium(getString(R.string.iap_restore_success))
                is PurchaseEvent.NoneFound -> {
                    setLoading(false)
                    purchaseInFlight = false
                    toast(getString(R.string.iap_restore_none))

                }
                is PurchaseEvent.Failed -> {
                    setLoading(false)
                    purchaseInFlight = false
                    showError(event.error)
                }
                else -> {
                    setLoading(false)
                    purchaseInFlight = false
                }
            }
        }
    }

    private fun handlePurchaseEvent(event: PurchaseEvent) {
        when (event) {
            is PurchaseEvent.Success -> completePremium(getString(R.string.iap_purchase_success))
            is PurchaseEvent.Restored -> {
                if (!isFinishing && purchaseInFlight) {
                    completePremium(getString(R.string.iap_restore_success))
                }
            }
            is PurchaseEvent.Failed -> {
                if (!purchaseInFlight) return
                setLoading(false)
                purchaseInFlight = false
                if (event.error is BillingError.UserCancelled) {
                    toast(event.error.toUserMessage(this))

                } else {
                    showError(event.error)
                }
            }
            PurchaseEvent.NoneFound -> Unit
        }
    }

    private fun completePremium(message: String) {
        if (isFinishing || premiumCompleted) return
        premiumCompleted = true
        setLoading(false)
        purchaseInFlight = false
        toast(message)
        setResult(RESULT_OK)
        dismissPaywall()
    }

    private fun dismissPaywall() {
        if (flowManager.currentStep == FlowStep.PREMIUM) {
            flowManager.next(this)
        } else {
            finish()
        }
    }

    private fun showAlreadyPremium() {
        val plan = premiumManager.currentPlan()
        binding.btnUpgrade.text = getString(R.string.iap_already_premium)

        if (plan != null) {
            selectPlan(plan)
        }
    }

    private fun showError(error: BillingError) {
        val message = error.toUserMessage(this)

        toast(message)
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.isVisible = loading
        binding.btnUpgrade.isEnabled = !loading
        binding.btnRestore.isEnabled = !loading
        binding.btnRestore.alpha = if (loading) 0.5f else 1f
        updateContinueButtonZoom(!loading)
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private data class PaywallBannerPage(
        val imageRes: Int,
        val backgroundRes: Int,
        val captionRes: Int,
    )

    private class PaywallBannerAdapter(
        private val pages: List<PaywallBannerPage>,
    ) : RecyclerView.Adapter<PaywallBannerAdapter.BannerVH>() {

        private val fadedBannerCache = SparseArray<Bitmap>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerVH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_paywall_banner, parent, false)
            return BannerVH(view)
        }

        override fun onBindViewHolder(holder: BannerVH, position: Int) {
            val page = pages[position]
            holder.background.setImageResource(page.backgroundRes)
            holder.image.setImageBitmap(fadedBannerBitmap(holder.image, page.imageRes))
        }

        override fun getItemCount(): Int = pages.size

        /**
         * Soft-fades the top of the banner into transparency so [bg_banner_*] shows
         * through and the hard gradient seam disappears — bg itself stays.
         */
        private fun fadedBannerBitmap(imageView: ImageView, @DrawableRes imageRes: Int): Bitmap {
            fadedBannerCache.get(imageRes)?.let { return it }

            val drawable = ContextCompat.getDrawable(imageView.context, imageRes)
                ?: error("Missing banner drawable $imageRes")
            val src = drawable.toBitmap()
            val out = src.copy(Bitmap.Config.ARGB_8888, true)
            val fadeHeight = (out.height * BANNER_TOP_FADE_FRACTION).toInt().coerceAtLeast(1)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f,
                    0f,
                    0f,
                    fadeHeight.toFloat(),
                    Color.TRANSPARENT,
                    Color.BLACK,
                    Shader.TileMode.CLAMP,
                )
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            }
            Canvas(out).drawRect(0f, 0f, out.width.toFloat(), fadeHeight.toFloat(), paint)

            fadedBannerCache.put(imageRes, out)
            return out
        }

        class BannerVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val background: ImageView = itemView.findViewById(R.id.ivBannerBackground)
            val image: ImageView = itemView.findViewById(R.id.ivBanner)
        }
    }

    companion object {
        private const val AUTO_SWIPE_INTERVAL_MS = 3500L
        private const val CLOSE_BUTTON_DELAY_MS = 2000L
        private const val CONTINUE_ZOOM_MIN = 1f
        private const val CONTINUE_ZOOM_MAX = 1.05f
        private const val CONTINUE_ZOOM_DURATION_MS = 400L
        private const val BANNER_TOP_FADE_FRACTION = 0.18f
    }
}
