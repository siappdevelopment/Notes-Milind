package com.notebook.notes.checklist.helper

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.notebook.notes.checklist.R

object BiometricAuthHelper {

    /** Biometric-only authenticators (no device PIN), so lock-screen fallback stays available. */
    private const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_WEAK

    fun canAuthenticate(context: Context): Boolean {
        return availabilityStatus(context) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Checks strong first (typical fingerprint), then weak (face/fingerprint class 2/3).
     * Some OEMs report success for only one of these flags.
     */
    fun availabilityStatus(context: Context): Int {
        val biometricManager = BiometricManager.from(context)

        val strong = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        )
        if (strong == BiometricManager.BIOMETRIC_SUCCESS) return strong

        val weak = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        if (weak == BiometricManager.BIOMETRIC_SUCCESS) return weak

        // Prefer a non-"no hardware" result so "none enrolled" surfaces correctly.
        return when {
            strong == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ||
                weak == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
            }
            strong == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ||
                weak == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
            }
            strong != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> strong
            else -> weak
        }
    }

    fun unavailableMessageRes(status: Int): Int {
        return when (status) {
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                R.string.fingerprint_none_enrolled
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                R.string.fingerprint_hw_unavailable
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                R.string.fingerprint_not_available
            else -> R.string.fingerprint_not_available
        }
    }

    /**
     * Opens the device screen where the user can enroll a fingerprint (not face unlock).
     * Returns false if no suitable settings activity was found.
     */
    fun openEnrollmentSettings(context: Context): Boolean {
        val candidates = buildList {
            // Prefer the fingerprint-only enrollment screen (API 28+).
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                add(Intent(Settings.ACTION_FINGERPRINT_ENROLL))
            }
            // API 30+ generic biometric enroll defaults to face on many devices unless
            // fingerprint-only is requested (AOSP Settings extra).
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(
                    Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                        putExtra(EXTRA_FINGERPRINT_ENROLLMENT_ONLY, true)
                        putExtra(
                            Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                            BiometricManager.Authenticators.BIOMETRIC_STRONG
                        )
                    }
                )
            }
            add(Intent(Settings.ACTION_SECURITY_SETTINGS))
            add(Intent(Settings.ACTION_SETTINGS))
        }
        for (intent in candidates) {
            if (intent.resolveActivity(context.packageManager) != null) {
                try {
                    context.startActivity(intent)
                    return true
                } catch (_: Exception) {
                    // Some OEM builds expose the activity but crash on launch; try next.
                }
            }
        }
        return false
    }

    /** AOSP Settings: skip face and other biometrics during [Settings.ACTION_BIOMETRIC_ENROLL]. */
    private const val EXTRA_FINGERPRINT_ENROLLMENT_ONLY = "fingerprint_enrollment_only"

    fun showPrompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButtonText: String,
        onSuccess: () -> Unit,
        onError: ((errorCode: Int, errString: CharSequence) -> Unit)? = null,
        onFailed: (() -> Unit)? = null
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            createCallback(onSuccess, onError, onFailed)
        )
        prompt.authenticate(buildPromptInfo(title, subtitle, negativeButtonText))
    }

    fun showPrompt(
        fragment: Fragment,
        title: String,
        subtitle: String,
        negativeButtonText: String,
        onSuccess: () -> Unit,
        onError: ((errorCode: Int, errString: CharSequence) -> Unit)? = null,
        onFailed: (() -> Unit)? = null
    ) {
        val executor = ContextCompat.getMainExecutor(fragment.requireContext())
        val prompt = BiometricPrompt(
            fragment,
            executor,
            createCallback(onSuccess, onError, onFailed)
        )
        prompt.authenticate(buildPromptInfo(title, subtitle, negativeButtonText))
    }

    private fun buildPromptInfo(
        title: String,
        subtitle: String,
        negativeButtonText: String
    ): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()
    }

    private fun createCallback(
        onSuccess: () -> Unit,
        onError: ((errorCode: Int, errString: CharSequence) -> Unit)?,
        onFailed: (() -> Unit)?
    ): BiometricPrompt.AuthenticationCallback {
        return object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError?.invoke(errorCode, errString)
            }

            override fun onAuthenticationFailed() {
                onFailed?.invoke()
            }
        }
    }
}
