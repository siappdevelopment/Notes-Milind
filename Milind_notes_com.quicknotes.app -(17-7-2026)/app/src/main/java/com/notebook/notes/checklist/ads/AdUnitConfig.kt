package com.notebook.notes.checklist.ads

import androidx.annotation.Keep
@Keep
data class AdUnitConfig(
    val id: String,
    val isEnable: Boolean,
    val enableUaCheck: Boolean = false,
    val reloadIntervalSeconds: Int? = null,
    val colorCTA: String = "default",
    val heightCTA: Int = 45,
    val positionCTA: String = "BOTTOM",
    val components: List<String> = listOf("icon_headline", "body", "media", "cta"),
    /** For count-gated interstitials: show on every Nth trigger (1 = every time). */
    val clickCount: Int = 1,
    /** For slots that can be either format: "native" or "banner". */
    val type: String = "native",
    /** Optional per-format unit IDs for slots that switch [type]; blank falls back to [id]. */
    val bannerId: String = "",
    val nativeId: String = ""
) {
    /** Config with [id] resolved for the banner format. */
    fun forBanner(): AdUnitConfig = copy(id = bannerId.ifBlank { id })

    /** Config with [id] resolved for the native format. */
    fun forNative(): AdUnitConfig = copy(id = nativeId.ifBlank { id })
}
