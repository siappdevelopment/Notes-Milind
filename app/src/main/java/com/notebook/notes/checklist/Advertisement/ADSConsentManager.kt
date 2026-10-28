package com.notebook.notes.checklist.Advertisement

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import com.notebook.notes.checklist.BuildConfig

import java.util.concurrent.atomic.AtomicBoolean

object ADSConsentManager {

    private const val TAG = "ADSConsentManager"

    /**
     * Debug builds simulate EEA geography so the consent form can be tested from India.
     * Set false only if you want production-like behavior on a debug APK.
     */
    private val useDebugEeaGeography: Boolean = BuildConfig.DEBUG

    /** From Logcat: UserMessagingPlatform → addTestDeviceHashedId(...) */
    private const val DEBUG_TEST_DEVICE_HASHED_ID = "E53C829561998ECFE30BD664EE155557"

    @JvmStatic
    fun gatherConsent(activity: Activity, onComplete: () -> Unit) {
        gatherConsent(activity, showForm = true) { onComplete() }
    }

    /**
     * @param showForm When false, only refreshes consent info (no form). Use on splash when
     *                 Firebase targets the language screen.
     * @param onComplete Called with true when the consent form was shown and dismissed.
     */
    @JvmStatic
    fun gatherConsent(activity: Activity, showForm: Boolean, onComplete: (Boolean) -> Unit) {
        val completed = AtomicBoolean(false)
        val finish: (Boolean) -> Unit = { formShown ->
            if (completed.compareAndSet(false, true)) {
                onComplete(formShown)
            }
        }

        if (activity.isFinishing || activity.isDestroyed) {
            finish(false)
            return
        }

        // Safe point: UMP needs the network, so offline it can never deliver a callback.
        if (!ADSUtilitis.IsNetworkConnected(activity)) {
            finish(false)
            return
        }

        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        val paramsBuilder = ConsentRequestParameters.Builder()
//        Log.d(TAG, "gatherConsent: showForm=$showForm, debugEea=$useDebugEeaGeography")
        if (useDebugEeaGeography) {
            val debugSettings = ConsentDebugSettings.Builder(activity)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .addTestDeviceHashedId(DEBUG_TEST_DEVICE_HASHED_ID)
                .addTestDeviceHashedId("F2F0412B2E13A3561318DC9EF6EFD068")
                .build()
            paramsBuilder.setConsentDebugSettings(debugSettings)
//            Log.d(TAG, "Debug EEA geography enabled for test device $DEBUG_TEST_DEVICE_HASHED_ID")
        }

        try {
            consentInformation.requestConsentInfoUpdate(
                activity,
                paramsBuilder.build(),
                {
                    logConsentState(consentInformation, "after requestConsentInfoUpdate")

                    if (activity.isFinishing || activity.isDestroyed) {
                        finish(false)
                        return@requestConsentInfoUpdate
                    }

                    if (!showForm) {
                        finish(false)
                        return@requestConsentInfoUpdate
                    }

                    if (!consentInformation.isConsentFormAvailable) {
//                    Log.w(
//                        TAG,
//                        "Consent form not available. Create and publish a GDPR message in " +
//                            "AdMob → Privacy & messaging → GDPR."
//                    )
                        finish(false)
                        return@requestConsentInfoUpdate
                    }

                    val consentRequired =
                        consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED
                    if (!consentRequired) {
                        finish(false)
                        return@requestConsentInfoUpdate
                    }

                    // Safe point: a throw here would otherwise leave the caller waiting forever.
                    try {
                        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                            if (formError != null) {
                                logFormError("loadAndShowConsentFormIfRequired", formError)
                                finish(false)
                            } else {
                                logConsentState(consentInformation, "after consent form")
                                if (consentInformation.canRequestAds()) {
                                    ADSUtilitis.trackPermissionAllowOnce(activity, "ConsentIn_Allow")
                                }
                                finish(true)
                            }
                        }
                    } catch (t: Throwable) {
                        t.printStackTrace()
                        finish(false)
                    }
                },
                { formError ->
                    logFormError("requestConsentInfoUpdate", formError)
                    finish(false)
                }
            )
        } catch (t: Throwable) {
            // Safe point: consent SDK failed to even start the request.
            t.printStackTrace()
            finish(false)
        }
    }

    /** Splash was configured but the form did not appear — retry on language. */
    @JvmStatic
    fun shouldRetryConsentOnLanguage(context: Context): Boolean {
        if (canRequestAds(context)) {
            return false
        }
        val consentInformation = UserMessagingPlatform.getConsentInformation(context)
        return consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED ||
            consentInformation.isConsentFormAvailable
    }

    @JvmStatic
    fun canRequestAds(context: Context): Boolean {
        return UserMessagingPlatform.getConsentInformation(context).canRequestAds()
    }

    @JvmStatic
    fun isConsentFormAvailable(context: Context): Boolean {
        return UserMessagingPlatform.getConsentInformation(context).isConsentFormAvailable
    }

    @JvmStatic
    fun showPrivacyOptionsForm(activity: Activity, onDismiss: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { onDismiss() }
    }

    @JvmStatic
    fun resetConsentForTesting(context: Context) {
        UserMessagingPlatform.getConsentInformation(context).reset()
    }

    @JvmStatic
    fun consentStatus(context: Context): Int {
        return UserMessagingPlatform.getConsentInformation(context).consentStatus
    }

    private fun logConsentState(consentInformation: ConsentInformation, stage: String) {
//        Log.d(
//            TAG,
//            "$stage | status=${consentInformation.consentStatus}, " +
//                "formAvailable=${consentInformation.isConsentFormAvailable}, " +
//                "canRequestAds=${consentInformation.canRequestAds()}"
//        )
    }

    private fun logFormError(stage: String, formError: FormError) {
//        Log.e(
//            TAG,
//            "$stage failed: code=${formError.errorCode}, message=${formError.message}"
//        )
    }
}
