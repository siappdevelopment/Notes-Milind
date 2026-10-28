package com.notebook.notes.checklist.flow

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.notebook.notes.checklist.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.xmlpull.v1.XmlPullParser
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Single source of truth for Remote Config–driven flow settings.
 *
 * Always reads [R.xml.remote_config_defaults] first (works without Firebase).
 * When Firebase is available, remote values override those defaults after fetch.
 */
@Singleton
class RemoteConfigManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {

    @Volatile
    private var cachedConfig: FlowConfig = FlowConfig.defaults()

    fun getFlowConfig(): FlowConfig = cachedConfig

    /**
     * Loads XML defaults, then optionally fetches & activates Firebase Remote Config.
     * Never throws; always leaves [cachedConfig] in a usable state.
     */
    suspend fun fetchAndActivate(timeoutMs: Long = FETCH_TIMEOUT_MS): FlowConfig {
        // XML is the local source of truth — even when google-services.json is missing.
        val defaults = loadXmlDefaults()
        cachedConfig = defaults
        Log.e(TAG, "Loaded XML defaults: $defaults")

        val remoteConfig = obtainRemoteConfig() ?: return cachedConfig

        return try {
            remoteConfig.setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(MINIMUM_FETCH_INTERVAL_SECONDS)
                    .build()
            ).awaitCompleted()

            remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults).awaitCompleted()

            val activated = withTimeoutOrNull(timeoutMs) {
                remoteConfig.fetchAndActivate().awaitCompleted()
            } == true

            cachedConfig = parseConfig(
                screenFlowRaw = remoteConfig.getString(KEY_SCREEN_FLOW),
                splashModeRaw = remoteConfig.getString(KEY_SPLASH_PERMISSION_MODE),
                permissionRaw = remoteConfig.getString(KEY_PERMISSION_SCREEN_PERMISSIONS),
                onboardingCountRaw = remoteConfig.getLong(RC_ONBOARDING_COUNT),
                fallback = defaults
            ).also {
                if (activated) {
                    Log.e(TAG, "Remote Config activated: $it")
                } else {
                    Log.e(TAG, "Remote Config fetch failed/timed out; keeping XML/cache: $it")
                }
            }
            cachedConfig
        } catch (e: Exception) {
            Log.e(TAG, "Remote Config error; using XML defaults", e)
            cachedConfig = defaults
            cachedConfig
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

    private fun parseConfig(
        screenFlowRaw: String,
        splashModeRaw: String,
        permissionRaw: String,
        onboardingCountRaw: Long?,
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

        return FlowConfig(
            screenFlow = screenFlow,
            splashPermissionMode = splashMode,
            permissionScreenPermissions = permissionTypes,
            onboardingCount = onboardingCount
        )
    }

    private fun parseOnboardingCount(raw: Long?, fallback: Int): Int {
        if (raw == null) return fallback
        return raw.toInt().coerceIn(
            FlowConfig.MIN_ONBOARDING_COUNT,
            FlowConfig.MAX_ONBOARDING_COUNT
        )
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

    private suspend fun Task<*>.awaitCompleted(): Boolean =
        suspendCancellableCoroutine { cont ->
            addOnCompleteListener { task ->
                if (cont.isActive) {
                    cont.resume(task.isSuccessful)
                }
            }
        }

    companion object {
        private const val TAG = "RemoteConfigManager"
        const val KEY_SCREEN_FLOW = "screen_flow"
        const val KEY_SPLASH_PERMISSION_MODE = "splash_permission_mode"
        const val KEY_PERMISSION_SCREEN_PERMISSIONS = "permission_screen_permissions"
        const val RC_ONBOARDING_COUNT = "onboarding_count"
        private const val FETCH_TIMEOUT_MS = 8_000L
        private const val MINIMUM_FETCH_INTERVAL_SECONDS = 0L
    }
}
