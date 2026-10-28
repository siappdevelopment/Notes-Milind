package com.notebook.notes.checklist.ads

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.notebook.notes.checklist.BuildConfig
import com.notebook.notes.checklist.ads.AppConstants.DEFAULT_TIME_DELAY_SHOW_LANGUAGE_DONE_BUTTON
import com.notebook.notes.checklist.data.ForceUpdateConfig
import com.notebook.notes.checklist.di.GlobalApp

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlin.collections.set

object RemoteConfigUtils {
    private const val TAG = "RemoteConfigUtils"
    var completed = false
    private const val ON_SHOW_NAVIGATION_BUTTON = "on_show_navigation_button"
    private const val ON_SHOW_DIALOG_CONSENT = "on_show_dialog_consent"
    private const val DELAY_SHOW_LANGUAGE_DONE_BUTTON = "delay_button_done_language"
    private const val TIME_DELAY_SHOW_LANGUAGE_DONE_BUTTON = "time_delay_show_language_done_button"
    private const val AD_REMOTE_CONFIG = "ad_remote_config"
    private const val FORCE_UPDATE_CONFIG = "force_update_config"

    private const val ON_ENABLE_UNINSTALL_WIDGET = "on_enable_uninstall_widget"

    private val mapConditionForAd: HashMap<String, Any> = hashMapOf(
        ON_SHOW_DIALOG_CONSENT to true,
        ON_SHOW_NAVIGATION_BUTTON to false,
        DELAY_SHOW_LANGUAGE_DONE_BUTTON to true,
        ON_ENABLE_UNINSTALL_WIDGET to false,
        TIME_DELAY_SHOW_LANGUAGE_DONE_BUTTON to DEFAULT_TIME_DELAY_SHOW_LANGUAGE_DONE_BUTTON,
    )
    
    //Default layout
    private const val AD_LANGUAGE_LAYOUT_FILE = "ad_language_layout.json"
    private const val AD_REMOTE_CONFIG_FILE_DEBUG = "ad_config_debug.json"
    private const val AD_REMOTE_CONFIG_FILE_RELEASE = "ad_config.json"
    private const val FORCE_UPDATE_CONFIG_FILE = "force_update_config.json"

    fun getOnShowNavigationButton(): Boolean = getBoolean(ON_SHOW_NAVIGATION_BUTTON)
    fun getOnShowDialogConsent(): Boolean = getBoolean(ON_SHOW_DIALOG_CONSENT)
    fun getOnEnableUninstallWidget(): Boolean = getBoolean(ON_ENABLE_UNINSTALL_WIDGET, false)

    interface Listener {
        fun loadSuccess()
    }

    lateinit var listener: Listener
    private lateinit var remoteConfig: FirebaseRemoteConfig
    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val adUnitConfigAdapter = moshi.adapter(AdUnitConfig::class.java)
    private val forceUpdateAdapter = moshi.adapter(ForceUpdateConfig::class.java)

    fun init(context: Context, mListener: Listener, extraDefaults: Map<String, Any> = emptyMap()) {
        listener = mListener
        remoteConfig = getFirebaseRemoteConfig(context, extraDefaults)
    }

    private fun getDefaultsFromAdConfig(context: Context): Map<String, Any> {
        val defaults = mutableMapOf<String, Any>()
        try {
            if (!AdRemoteConfig.isInitialized()) {
                AdRemoteConfig.initializeFromAssets(context)
            }
            val adConfig = AdRemoteConfig.getInstance()
            adConfig.ads.forEach { (key, adUnitConfig) ->
                val jsonString = adUnitConfigAdapter.toJson(adUnitConfig)
                defaults[key] = jsonString
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return defaults
    }

    private fun getFirebaseRemoteConfig(context: Context, extraDefaults: Map<String, Any> = emptyMap()): FirebaseRemoteConfig {
        remoteConfig = Firebase.remoteConfig

        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) {
                0
            } else {
                60 * 60
            }
        }
        val defaults = extraDefaults.toMutableMap().apply { putAll(getDefaultsFromAdConfig(context)) }
        remoteConfig.apply {
            setConfigSettingsAsync(configSettings)
            mapConditionForAd.forEach { (key, value) ->
                defaults[key] = value
            }
            setDefaultsAsync(defaults)
            Log.d(TAG, "Fetching Firebase Remote Config (minFetchInterval=${configSettings.minimumFetchIntervalInSeconds}s)")
            fetchAndActivate().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val source = remoteConfig.getValue(AD_REMOTE_CONFIG).source
                    Log.d(TAG, "Fetch complete: activated=${task.result}, $AD_REMOTE_CONFIG source=${sourceName(source)}")
                    logFetchedData()
                } else {
                    Log.w(TAG, "Fetch failed; using defaults/cached values", task.exception)
                }
                listener.loadSuccess()
                completed = true
            }
        }
        return remoteConfig
    }

    private fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return try {
            if (!completed) defaultValue
            else remoteConfig.getBoolean(key)
        } catch (ex: Exception) {
            ex.printStackTrace()
            defaultValue
        }
    }


    private fun getLong(key: String, defaultValue: Long = 0): Long {
        return try {
            if (!completed) defaultValue
            else remoteConfig.getLong(key)
        } catch (ex: Exception) {
            ex.printStackTrace()
            defaultValue
        }
    }


    fun getAdRemoteConfig(): String {
        val defaultValue = loadDefaultAdRemoteConfig()
        if (!completed) {
            Log.d(TAG, "$AD_REMOTE_CONFIG: fetch not completed, using bundled asset (${defaultValue.length} chars)")
            return defaultValue
        } else {
            val configValue = remoteConfig.getString(AD_REMOTE_CONFIG)
            val result = configValue.ifBlank { defaultValue }
            val origin = if (configValue.isBlank()) "bundled asset (Firebase value blank)" else "Firebase"
            Log.d(TAG, "$AD_REMOTE_CONFIG: using $origin (${result.length} chars)")
            return result
        }
    }

    /** Logs every fetched key with its source, then the full `ad_remote_config` JSON (chunked: logcat cuts long lines). */
    private fun logFetchedData() {
        try {
            remoteConfig.all.forEach { (key, value) ->
                if (key == AD_REMOTE_CONFIG) {
                    Log.d(TAG, "[${sourceName(value.source)}] $key = (${value.asString().length} chars, printed below)")
                } else {
                    Log.d(TAG, "[${sourceName(value.source)}] $key = ${value.asString().take(200)}")
                }
            }
            val json = remoteConfig.getString(AD_REMOTE_CONFIG)
            Log.d(TAG, "$AD_REMOTE_CONFIG json (${json.length} chars):")
            json.chunked(3000).forEachIndexed { i, part -> Log.d(TAG, "json[$i]: $part") }
        } catch (e: Exception) {
            Log.w(TAG, "Could not log fetched data", e)
        }
    }

    private fun sourceName(source: Int): String = when (source) {
        FirebaseRemoteConfig.VALUE_SOURCE_REMOTE -> "REMOTE"
        FirebaseRemoteConfig.VALUE_SOURCE_DEFAULT -> "DEFAULT"
        else -> "STATIC"
    }


    fun getForceUpdateConfig(): ForceUpdateConfig? {
        val defaultJson = loadDefaultForceUpdateConfig()
        val json = if (!completed) {
            defaultJson
        } else {
            val configValue = remoteConfig.getString(FORCE_UPDATE_CONFIG)
            configValue.ifBlank { defaultJson }
        }

        return try {
            forceUpdateAdapter.fromJson(json)
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }

    private fun loadDefaultAdLanguageLayout(): String {
        return try {
            GlobalApp.instance.assets.open(AD_LANGUAGE_LAYOUT_FILE).bufferedReader().use { reader ->
                reader.readText()
            }
        } catch (ex: Exception) {
            "{}"
        }
    }

    private fun loadDefaultForceUpdateConfig(): String {
        return try {
            GlobalApp.instance.assets.open(FORCE_UPDATE_CONFIG_FILE).bufferedReader().use { reader ->
                reader.readText()
            }
        } catch (ex: Exception) {
            "{}"
        }
    }

    private fun loadDefaultAdRemoteConfig(): String {
        val fileName = if (BuildConfig.DEBUG) {
            AD_REMOTE_CONFIG_FILE_DEBUG
        } else {
            AD_REMOTE_CONFIG_FILE_RELEASE
        }
        return try {
            GlobalApp.instance.assets.open(fileName).bufferedReader().use { reader ->
                reader.readText()
            }
        } catch (ex: Exception) {
            "{}"
        }
    }


    fun shouldDelayLanguageDoneButton(): Boolean = getBoolean(DELAY_SHOW_LANGUAGE_DONE_BUTTON, true)
    fun getTimeDelayButtonDoneLanguage(): Long = getLong(
        TIME_DELAY_SHOW_LANGUAGE_DONE_BUTTON,
        AppConstants.DEFAULT_TIME_DELAY_SHOW_LANGUAGE_DONE_BUTTON
    )
}