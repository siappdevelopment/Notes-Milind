package com.notebook.notes.checklist.helper

import com.notebook.notes.checklist.utils.Constant
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotePasswordManager @Inject constructor(
    private val preferenceHelper: PreferenceHelper
) {

    enum class PasswordType {
        PATTERN,
        PIN
    }

    fun hasPassword(): Boolean =
        !preferenceHelper.getString(KEY_HASH).isNullOrBlank()

    fun isAppLockEnabled(): Boolean =
        preferenceHelper.getBoolean(KEY_APP_LOCK_ENABLED, false)

    fun setAppLockEnabled(enabled: Boolean) {
        preferenceHelper.putBoolean(KEY_APP_LOCK_ENABLED, enabled)
    }

    fun isFingerprintEnabled(): Boolean =
        preferenceHelper.getBoolean(Constant.PREF_FINGERPRINT_ENABLED, false)

    fun setFingerprintEnabled(enabled: Boolean) {
        preferenceHelper.putBoolean(Constant.PREF_FINGERPRINT_ENABLED, enabled)
    }

    fun getPasswordType(): PasswordType? {
        return when (preferenceHelper.getString(KEY_TYPE)) {
            TYPE_PATTERN -> PasswordType.PATTERN
            TYPE_PIN -> PasswordType.PIN
            else -> null
        }
    }

    fun savePattern(pattern: List<Int>) {
        preferenceHelper.putString(KEY_TYPE, TYPE_PATTERN)
        preferenceHelper.putString(KEY_HASH, hash(pattern.joinToString("-")))
    }

    fun savePin(pin: String) {
        preferenceHelper.putString(KEY_TYPE, TYPE_PIN)
        preferenceHelper.putString(KEY_HASH, hash(pin))
    }

    fun verifyPattern(pattern: List<Int>): Boolean {
        if (getPasswordType() != PasswordType.PATTERN) return false
        return preferenceHelper.getString(KEY_HASH) == hash(pattern.joinToString("-"))
    }

    fun verifyPin(pin: String): Boolean {
        if (getPasswordType() != PasswordType.PIN) return false
        return preferenceHelper.getString(KEY_HASH) == hash(pin)
    }

    fun clearPassword() {
        preferenceHelper.remove(KEY_TYPE)
        preferenceHelper.remove(KEY_HASH)
        clearSecurityQuestion()
        setFingerprintEnabled(false)
        setAppLockEnabled(false)
    }

    fun hasSecurityQuestion(): Boolean =
        !preferenceHelper.getString(KEY_SECURITY_QUESTION).isNullOrBlank() &&
            !preferenceHelper.getString(KEY_SECURITY_ANSWER_HASH).isNullOrBlank()

    fun getSecurityQuestion(): String? =
        preferenceHelper.getString(KEY_SECURITY_QUESTION)

    fun saveSecurityQuestion(question: String, answer: String) {
        preferenceHelper.putString(KEY_SECURITY_QUESTION, question.trim())
        preferenceHelper.putString(KEY_SECURITY_ANSWER_HASH, hash(normalizeAnswer(answer)))
    }

    fun verifySecurityAnswer(answer: String): Boolean {
        val saved = preferenceHelper.getString(KEY_SECURITY_ANSWER_HASH) ?: return false
        return saved == hash(normalizeAnswer(answer))
    }

    fun clearSecurityQuestion() {
        preferenceHelper.remove(KEY_SECURITY_QUESTION)
        preferenceHelper.remove(KEY_SECURITY_ANSWER_HASH)
    }

    private fun normalizeAnswer(answer: String): String =
        answer.trim().lowercase()

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_TYPE = "note_password_type"
        private const val KEY_HASH = "note_password_hash"
        private const val KEY_SECURITY_QUESTION = "note_security_question"
        private const val KEY_SECURITY_ANSWER_HASH = "note_security_answer_hash"
        private const val KEY_APP_LOCK_ENABLED = "pref_app_lock_enabled"
        private const val TYPE_PATTERN = "pattern"
        private const val TYPE_PIN = "pin"
        const val MIN_PATTERN_LENGTH = 4
        const val PIN_LENGTH = 4
    }
}
