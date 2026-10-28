package com.notebook.notes.checklist.iap

import com.notebook.notes.checklist.iap.model.ProductInfo
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.max

/**
 * Formats Play Billing micros into localized currency and derives paywall
 * weekly-equivalent / comparison ("old") prices from loaded products.
 */
object PaywallPricing {

    private const val MICROS = 1_000_000.0
    private const val WEEKS_PER_MONTH = 4.34524
    private const val WEEKS_PER_YEAR = 52.0

    fun formatMoney(amountMicros: Long, currencyCode: String): String {
        if (amountMicros <= 0L || currencyCode.isBlank()) return ""
        return try {
            val format = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
                currency = Currency.getInstance(currencyCode)
                maximumFractionDigits = 2
                minimumFractionDigits = if (amountMicros % 1_000_000L == 0L) 0 else 2
            }
            format.format(amountMicros / MICROS)
        } catch (_: Exception) {
            "%.2f".format(Locale.US, amountMicros / MICROS)
        }
    }

    /** Weekly-equivalent micros for subscription plans (0 for lifetime). */
    fun weeklyEquivalentMicros(product: ProductInfo, plan: PremiumPlan): Long {
        if (product.priceAmountMicros <= 0L) return 0L
        val weeks = weeksForPeriod(product.billingPeriod, plan)
        if (weeks <= 0.0) return 0L
        return (product.priceAmountMicros / weeks).toLong()
    }

    /**
     * Marketing "was" price for strikethrough:
     * - Monthly → weeks-in-month × weekly (if weekly loaded), else ~25% above monthly
     * - Yearly → 12 × monthly (if monthly loaded)
     * - Lifetime → 2 × current lifetime price (same currency as the live product)
     * - Weekly → none
     */
    fun oldPriceMicros(
        plan: PremiumPlan,
        products: List<ProductInfo>,
    ): Long? {
        val weekly = products.find { it.productId == PremiumPlan.WEEKLY.productId }
        val monthly = products.find { it.productId == PremiumPlan.MONTHLY.productId }
        val lifetime = products.find { it.productId == PremiumPlan.LIFETIME.productId }
        return when (plan) {
            PremiumPlan.MONTHLY -> {
                val fromWeekly = weekly?.priceAmountMicros
                    ?.takeIf { it > 0L }
                    ?.let { (it * WEEKS_PER_MONTH).toLong() }
                if (fromWeekly != null) return fromWeekly
                val m = monthly?.priceAmountMicros ?: return null
                if (m <= 0L) null else m * 4 / 3
            }
            PremiumPlan.YEARLY -> {
                val m = monthly?.priceAmountMicros ?: return null
                if (m <= 0L) null else m * 12
            }
            PremiumPlan.LIFETIME -> {
                val amount = lifetime?.priceAmountMicros ?: return null
                if (amount <= 0L) null else amount * 2
            }
            else -> null
        }
    }

    fun currencyCodeFor(plan: PremiumPlan, products: List<ProductInfo>): String {
        products.find { it.productId == plan.productId }?.priceCurrencyCode
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }
        return products.firstOrNull { it.priceCurrencyCode.isNotBlank() }?.priceCurrencyCode
            .orEmpty()
    }

    private fun weeksForPeriod(billingPeriod: String?, plan: PremiumPlan): Double {
        parseIso8601Weeks(billingPeriod)?.let { return max(it, 0.0001) }
        return when (plan) {
            PremiumPlan.WEEKLY -> 1.0
            PremiumPlan.MONTHLY -> WEEKS_PER_MONTH
            PremiumPlan.YEARLY -> WEEKS_PER_YEAR
            PremiumPlan.LIFETIME -> 0.0
        }
    }

    /** Best-effort ISO-8601 period → weeks (e.g. P1W, P1M, P1Y, P3M). */
    private fun parseIso8601Weeks(period: String?): Double? {
        if (period.isNullOrBlank() || !period.startsWith("P")) return null
        val body = period.removePrefix("P")
        val year = Regex("""(\d+)Y""").find(body)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val month = Regex("""(\d+)M""").find(body.substringBefore("T"))?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val week = Regex("""(\d+)W""").find(body)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val day = Regex("""(\d+)D""").find(body)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val total = year * WEEKS_PER_YEAR + month * WEEKS_PER_MONTH + week + day / 7.0
        return total.takeIf { it > 0.0 }
    }
}
