package com.notebook.notes.checklist.flow

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.data.remote.FeedbackApiConfig
import com.notebook.notes.checklist.helper.PreferenceHelper
import com.notebook.notes.checklist.utils.Constant
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for Remote Config–driven flow settings.
 *
 * [com.notebook.notes.checklist.Advertisement.activity.BActivity] performs the one
 * Firebase [FirebaseRemoteConfig.fetchAndActivate] (ads + flow keys together), then
 * calls [cacheFromActivatedRemoteConfig]. All other screens read typed cached APIs only.
 *
 * Always seeds from [R.xml.remote_config_defaults] when Firebase values are missing.
 */
@Singleton
class RemoteConfigManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
    private val preferenceHelper: PreferenceHelper
) {

    @Volatile
    private var cachedConfig: FlowConfig = FlowConfig.defaults()

    @Volatile
    private var fetchCompleted: Boolean = false

    /** Full cached snapshot (prefer typed getters below). */
    fun getFlowConfig(): FlowConfig = cachedConfig

    fun getScreenFlow(): List<FlowStep> = cachedConfig.screenFlow

    fun getSplashPermissionMode(): SplashPermissionMode = cachedConfig.splashPermissionMode

    fun getPermissionScreenPermissions(): List<PermissionType> =
        cachedConfig.permissionScreenPermissions

    fun getOnboardingCount(): Int = cachedConfig.onboardingCount

    /** Show onboarding again every N normal launches; 0 = once only. */
    fun getOnboardingRepeatCount(): Int = cachedConfig.onboardingRepeatCount

    fun shouldShowPaywallAfterOnboarding(): Boolean =
        cachedConfig.showPaywallAfterOnboarding

    /** Cold-launch interval for session paywall; 0 = disabled. */
    fun getPaywallSessionInterval(): Int = cachedConfig.paywallSessionInterval

    /** True after BActivity cached flow values (or XML-only fallback). */
    fun isFetchCompleted(): Boolean = fetchCompleted

    /**
     * Ads XML + flow XML merged for [FirebaseRemoteConfig.setDefaultsAsync].
     * Call from BActivity before fetchAndActivate so flow keys are never STATIC-missing.
     */
    fun buildMergedDefaultsMap(): Map<String, Any> {
        val merged = readDefaultsXmlMap(R.xml.default_config) +
            readDefaultsXmlMap(R.xml.remote_config_defaults)
        return merged.mapValues { (_, value) -> value as Any }
    }

    /**
     * Called from BActivity after its single fetchAndActivate (success or failure).
     * Parses flow keys into typed cache — no network call.
     */
    fun cacheFromActivatedRemoteConfig(
        remoteConfig: FirebaseRemoteConfig? = obtainRemoteConfig()
    ): FlowConfig {
        val defaults = loadXmlDefaults()
        if (remoteConfig == null) {
            cachedConfig = defaults
            fetchCompleted = true
            Log.d(TAG, "No Firebase; cached XML defaults: $cachedConfig")
            return cachedConfig
        }

        return try {
            val showPaywallAfterOnboarding = remoteBooleanOrNull(
                remoteConfig,
                KEY_SHOW_PAYWALL_AFTER_ONBOARDING
            ) ?: defaults.showPaywallAfterOnboarding

            cachedConfig = parseConfig(
                screenFlowRaw = remoteStringOrEmpty(remoteConfig, KEY_SCREEN_FLOW),
                splashModeRaw = remoteStringOrEmpty(remoteConfig, KEY_SPLASH_PERMISSION_MODE),
                permissionRaw = remoteStringOrEmpty(remoteConfig, KEY_PERMISSION_SCREEN_PERMISSIONS),
                onboardingCountRaw = remoteLongOrNull(remoteConfig, RC_ONBOARDING_COUNT),
                showPaywallAfterOnboardingRaw = showPaywallAfterOnboarding,
                paywallSessionIntervalRaw = remoteLongOrNull(
                    remoteConfig,
                    KEY_PAYWALL_SESSION_INTERVAL
                ),
                onboardingRepeatCountRaw = remoteLongOrNull(remoteConfig, KEY_ONBOARDING_REPEAT_COUNT),
                fallback = defaults
            )
            cacheFeedbackUrl(adsJson = null, remoteConfig = remoteConfig)
            fetchCompleted = true
            Log.d(TAG, "Cached flow RC from activated Firebase: $cachedConfig")
            cachedConfig
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache activated RC; using XML defaults", e)
            cachedConfig = defaults
            fetchCompleted = true
            cachedConfig
        }
    }

    /**
     * Primary path: parse the flow keys from the big versioned ads JSON that BActivity
     * already fetched (keys are nested inside it, not stored as separate RC parameters).
     *
     * Lookup order per key: ads JSON (recursive) → separate REMOTE param → XML default.
     * Never throws.
     */
    fun cacheFromAdsJson(
        adsJson: JSONObject?,
        remoteConfig: FirebaseRemoteConfig? = obtainRemoteConfig()
    ): FlowConfig {
        val defaults = loadXmlDefaults()
        return try {
            cachedConfig = parseConfig(
                screenFlowRaw = resolveRawString(adsJson, remoteConfig, KEY_SCREEN_FLOW),
                splashModeRaw = resolveRawString(adsJson, remoteConfig, KEY_SPLASH_PERMISSION_MODE),
                permissionRaw = resolveRawString(adsJson, remoteConfig, KEY_PERMISSION_SCREEN_PERMISSIONS),
                onboardingCountRaw = resolveRawLong(adsJson, remoteConfig, RC_ONBOARDING_COUNT),
                showPaywallAfterOnboardingRaw = resolveRawBoolean(
                    adsJson,
                    remoteConfig,
                    KEY_SHOW_PAYWALL_AFTER_ONBOARDING,
                    defaults.showPaywallAfterOnboarding
                ),
                paywallSessionIntervalRaw = resolveRawLong(
                    adsJson,
                    remoteConfig,
                    KEY_PAYWALL_SESSION_INTERVAL
                ),
                onboardingRepeatCountRaw = resolveRawLong(adsJson, remoteConfig, KEY_ONBOARDING_REPEAT_COUNT),
                fallback = defaults
            )
            cacheFeedbackUrl(adsJson, remoteConfig)
            fetchCompleted = true
            Log.d(TAG, "Cached flow from ads JSON: $cachedConfig")
            cachedConfig
        } catch (e: Exception) {
            Log.e(TAG, "cacheFromAdsJson failed; using XML defaults", e)
            cachedConfig = defaults
            fetchCompleted = true
            cachedConfig
        }
    }

    /**
     * Offline / no-network path: store XML defaults and mark ready.
     */
    fun cacheXmlDefaultsOnly(): FlowConfig {
        cachedConfig = loadXmlDefaults()
        fetchCompleted = true
        Log.d(TAG, "Cached XML defaults only: $cachedConfig")
        return cachedConfig
    }

    /**
     * Safety fallback if Splash starts before BActivity cached values.
     * Does not perform a network fetch — BActivity owns fetchAndActivate.
     */
    fun ensureCached(): FlowConfig {
        if (fetchCompleted) return cachedConfig
        return cacheFromActivatedRemoteConfig()
    }

    /**
     * Plain feedback API base URL from Firebase `feedback_url` / `APi.feedback_url` (no encrypt).
     * Example: http://139.59.79.157:3002
     */
    fun getFeedbackUrl(): String {
        val url = preferenceHelper.getString(Constant.PREF_FEEDBACK_URL).orEmpty().trim().trimEnd('/')
        require(url.isNotBlank()) {
            "feedback_url not found in prefs. Remote Config must load on Splash."
        }
        require(url.startsWith("http", ignoreCase = true)) {
            "Invalid feedback_url: $url"
        }
        return url
    }

    fun buildFeedbackUploadUrl(): String {
        val base = getFeedbackUrl()
        val endpoint = FeedbackApiConfig.UPLOAD_ENDPOINT
        return if (endpoint.startsWith("/")) "$base$endpoint" else "$base/$endpoint"
    }

    /** Saves plain `feedback_url` from ads JSON or separate RC key into prefs. */
    private fun cacheFeedbackUrl(adsJson: JSONObject?, remoteConfig: FirebaseRemoteConfig?) {
        val url = resolveRawString(adsJson, remoteConfig, KEY_FEEDBACK_URL)
            .ifBlank {
                (adsJson?.optJSONObject("APi")?.optString("feedback_url").orEmpty())
            }
            .trim()
            .trimEnd('/')
        if (url.isNotBlank()) {
            preferenceHelper.putString(Constant.PREF_FEEDBACK_URL, url)
            Log.d(TAG, "Cached feedback_url")
        }
    }

    /**
     * Parses [R.xml.remote_config_defaults] so splash / flow settings work offline
     * and without a Firebase project.
     */
    fun loadXmlDefaults(): FlowConfig {
        return try {
            val map = readDefaultsXmlMap(R.xml.remote_config_defaults)
            parseConfig(
                screenFlowRaw = map[KEY_SCREEN_FLOW].orEmpty(),
                splashModeRaw = map[KEY_SPLASH_PERMISSION_MODE].orEmpty(),
                permissionRaw = map[KEY_PERMISSION_SCREEN_PERMISSIONS].orEmpty(),
                onboardingCountRaw = map[RC_ONBOARDING_COUNT]?.toLongOrNull(),
                showPaywallAfterOnboardingRaw = parseBoolean(
                    map[KEY_SHOW_PAYWALL_AFTER_ONBOARDING],
                    fallback = FlowConfig.defaults().showPaywallAfterOnboarding
                ),
                paywallSessionIntervalRaw = map[KEY_PAYWALL_SESSION_INTERVAL]?.toLongOrNull(),
                onboardingRepeatCountRaw = map[KEY_ONBOARDING_REPEAT_COUNT]?.toLongOrNull(),
                fallback = FlowConfig.defaults()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse remote_config_defaults.xml", e)
            FlowConfig.defaults()
        }
    }

    private fun readDefaultsXmlMap(xmlResId: Int): Map<String, String> {
        val map = linkedMapOf<String, String>()
        val parser = context.resources.getXml(xmlResId)
        try {
            var eventType = parser.eventType
            var currentTag: String? = null
            var currentKey: String? = null
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> currentTag = parser.name
                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim().orEmpty()
                        if (text.isEmpty()) {
                            // ignore whitespace between tags
                        } else when (currentTag) {
                            "key" -> currentKey = text
                            "value" -> {
                                val key = currentKey
                                if (!key.isNullOrEmpty()) {
                                    map[key] = text
                                    currentKey = null
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> currentTag = null
                }
                eventType = parser.next()
            }
        } finally {
            parser.close()
        }
        return map
    }

    private fun obtainRemoteConfig(): FirebaseRemoteConfig? {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val app = FirebaseApp.initializeApp(context)
                if (app == null) {
                    Log.i(
                        TAG,
                        "Firebase not configured (missing google-services.json); using XML defaults"
                    )
                    return null
                }
            }
            FirebaseRemoteConfig.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Unable to obtain FirebaseRemoteConfig", e)
            null
        }
    }

    /**
     * Depth-first search for [key] anywhere in [root] (handles the flow keys being nested
     * inside the big ads JSON under any parent object). Returns the first non-null match.
     */
    private fun findJsonValue(root: JSONObject, key: String): Any? {
        if (root.has(key)) {
            val direct = root.opt(key)
            if (direct != null && direct != JSONObject.NULL) return direct
        }
        val keys = root.keys()
        while (keys.hasNext()) {
            when (val child = root.opt(keys.next())) {
                is JSONObject -> findJsonValue(child, key)?.let { return it }
                is JSONArray -> {
                    for (i in 0 until child.length()) {
                        (child.opt(i) as? JSONObject)?.let { obj ->
                            findJsonValue(obj, key)?.let { return it }
                        }
                    }
                }
            }
        }
        return null
    }

    private fun resolveRawString(
        adsJson: JSONObject?,
        remoteConfig: FirebaseRemoteConfig?,
        key: String
    ): String {
        val fromJson = adsJson?.let { findJsonValue(it, key) }
        if (fromJson != null) return fromJson.toString()
        if (remoteConfig != null) {
            val value = remoteConfig.getValue(key)
            if (value.source != FirebaseRemoteConfig.VALUE_SOURCE_STATIC) return value.asString()
        }
        return ""
    }

    private fun resolveRawLong(
        adsJson: JSONObject?,
        remoteConfig: FirebaseRemoteConfig?,
        key: String
    ): Long? {
        when (val fromJson = adsJson?.let { findJsonValue(it, key) }) {
            is Number -> return fromJson.toLong()
            null -> Unit
            else -> fromJson.toString().trim().toLongOrNull()?.let { return it }
        }
        if (remoteConfig != null) {
            val value = remoteConfig.getValue(key)
            if (value.source != FirebaseRemoteConfig.VALUE_SOURCE_STATIC) return value.asLong()
        }
        return null
    }

    private fun resolveRawBoolean(
        adsJson: JSONObject?,
        remoteConfig: FirebaseRemoteConfig?,
        key: String,
        fallback: Boolean
    ): Boolean {
        when (val fromJson = adsJson?.let { findJsonValue(it, key) }) {
            is Boolean -> return fromJson
            is Number -> return fromJson.toInt() != 0
            null -> Unit
            else -> parseBooleanOrNull(fromJson.toString())?.let { return it }
        }
        if (remoteConfig != null) {
            remoteBooleanOrNull(remoteConfig, key)?.let { return it }
        }
        return fallback
    }

    private fun parseBooleanOrNull(raw: String?): Boolean? {
        if (raw.isNullOrBlank()) return null
        return when (raw.trim().lowercase()) {
            "true", "1", "yes" -> true
            "false", "0", "no" -> false
            else -> null
        }
    }

    private fun parseConfig(
        screenFlowRaw: String,
        splashModeRaw: String,
        permissionRaw: String,
        onboardingCountRaw: Long?,
        showPaywallAfterOnboardingRaw: Boolean,
        paywallSessionIntervalRaw: Long?,
        onboardingRepeatCountRaw: Long?,
        fallback: FlowConfig
    ): FlowConfig {
        val screenFlow = parseStringList(screenFlowRaw)
            .mapNotNull { FlowStep.fromRemoteValue(it) }
            .ifEmpty { fallback.screenFlow }

        val splashMode = if (splashModeRaw.isBlank()) {
            fallback.splashPermissionMode
        } else {
            SplashPermissionMode.fromRemoteValue(splashModeRaw)
        }

        val permissionTypes = parseStringList(permissionRaw)
            .mapNotNull { PermissionType.fromRemoteValue(it) }
            .ifEmpty { fallback.permissionScreenPermissions }

        val onboardingCount = parseOnboardingCount(onboardingCountRaw, fallback.onboardingCount)
        val paywallSessionInterval = parsePaywallSessionInterval(
            paywallSessionIntervalRaw,
            fallback.paywallSessionInterval
        )

        return FlowConfig(
            screenFlow = screenFlow,
            splashPermissionMode = splashMode,
            permissionScreenPermissions = permissionTypes,
            onboardingCount = onboardingCount,
            showPaywallAfterOnboarding = showPaywallAfterOnboardingRaw,
            paywallSessionInterval = paywallSessionInterval,
            // 0 = once only; negative / missing values fall back safely.
            onboardingRepeatCount = (onboardingRepeatCountRaw?.toInt() ?: fallback.onboardingRepeatCount)
                .coerceAtLeast(0)
        )
    }

    private fun parseOnboardingCount(raw: Long?, fallback: Int): Int {
        if (raw == null) return fallback
        return raw.toInt().coerceIn(
            FlowConfig.MIN_ONBOARDING_COUNT,
            FlowConfig.MAX_ONBOARDING_COUNT
        )
    }

    /** 0 = disabled; negative / invalid remote values fall back safely. */
    private fun parsePaywallSessionInterval(raw: Long?, fallback: Int): Int {
        if (raw == null) return fallback.coerceAtLeast(0)
        return raw.toInt().coerceAtLeast(0)
    }

    private fun remoteLongOrNull(remoteConfig: FirebaseRemoteConfig, key: String): Long? {
        val value = remoteConfig.getValue(key)
        return if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_STATIC) null else value.asLong()
    }

    private fun remoteStringOrEmpty(remoteConfig: FirebaseRemoteConfig, key: String): String {
        val value = remoteConfig.getValue(key)
        return if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_STATIC) "" else value.asString()
    }

    private fun remoteBooleanOrNull(remoteConfig: FirebaseRemoteConfig, key: String): Boolean? {
        val value = remoteConfig.getValue(key)
        if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_STATIC) return null
        val raw = value.asString().trim()
        if (raw.isNotEmpty()) {
            when (raw.lowercase()) {
                "true", "1", "yes" -> return true
                "false", "0", "no" -> return false
            }
        }
        return value.asBoolean()
    }

    private fun parseBoolean(raw: String?, fallback: Boolean): Boolean {
        if (raw.isNullOrBlank()) return fallback
        return when (raw.trim().lowercase()) {
            "true", "1", "yes" -> true
            "false", "0", "no" -> false
            else -> fallback
        }
    }

    private fun parseStringList(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(raw, type).orEmpty()
        } catch (_: Exception) {
            raw.split(',', '|', ';')
                .map { it.trim().removeSurrounding("\"") }
                .filter { it.isNotEmpty() }
        }
    }

    companion object {
        private const val TAG = "RemoteConfigManager"
        const val KEY_SCREEN_FLOW = "screen_flow"
        const val KEY_SPLASH_PERMISSION_MODE = "splash_permission_mode"
        const val KEY_PERMISSION_SCREEN_PERMISSIONS = "permission_screen_permissions"
        const val RC_ONBOARDING_COUNT = "onboarding_count"
        const val KEY_SHOW_PAYWALL_AFTER_ONBOARDING = "show_paywall_after_onboarding"
        const val KEY_PAYWALL_SESSION_INTERVAL = "paywall_session_interval"
        const val KEY_ONBOARDING_REPEAT_COUNT = "onboarding_repeat_count"
        /** Plain feedback API base URL (also under nested APi.feedback_url). */
        const val KEY_FEEDBACK_URL = "feedback_url"
    }
}
