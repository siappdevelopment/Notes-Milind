package com.notebook.notes.checklist.utils

object Constant {
    const val IS_ONBOARDING_COMPLTED = "is_onboarding"
    const val IS_LANGUAGE_SELECTED = "is_language_selected"
    const val IS_PERMISSION_COMPLETED = "permission_completed"
    /** Skill alias; kept in sync with [IS_LANGUAGE_SELECTED]. */
    const val LANGUAGE_COMPLETED = "language_completed"
    /** Skill alias; kept in sync with [IS_ONBOARDING_COMPLTED]. */
    const val ONBOARDING_COMPLETED = "onboarding_completed"
    /** One-time flag: post-flow paywall (before Home) already shown or dismissed. */
    const val POST_ONBOARDING_PAYWALL_SHOWN = "post_onboarding_paywall_shown"
    /** Cold-launch session counter for session-based paywall. */
    const val TOTAL_SESSION_COUNT = "total_session_count"
    /** Session number when the last session-based paywall was shown. */
    const val LAST_PAYWALL_SESSION = "last_paywall_session"
    const val PREF_SELECTED_LANGUAGE_CODE = "pref_selected_language_code"
    const val PREF_SELECTED_LANGUAGE_NAME = "pref_selected_language_name"
    const val PENDING_LANGUAGE_NAVIGATION = "pending_language_navigation"
    const val PENDING_LANGUAGE_FROM_SETTINGS = "pending_language_from_settings"
    const val DEFAULT_LANGUAGE_CODE = "en"
    const val DEFAULT_LANGUAGE_NAME = "English"
    const val PREF_DEFAULT_FONT_SIZE = "pref_default_font_size"
    const val PREF_DEFAULT_FONT_FAMILY = "pref_default_font_family"
    const val PREF_LIST_ITEM_HEIGHT = "pref_list_item_height"
    const val PREF_DEFAULT_NOTE_BACKGROUND = "pref_default_note_background"
    const val PREF_TIME_FORMAT = "pref_time_format"
    const val PREF_DATE_FORMAT = "pref_date_format"
    const val PREF_APP_LOCK_ENABLED = "pref_app_lock_enabled"
    const val PREF_FINGERPRINT_ENABLED = "pref_fingerprint_enabled"
    /** Plain feedback API base URL from Remote Config `feedback_url` (no encrypt). */
    const val PREF_FEEDBACK_URL = "pref_feedback_url"
    
    /** Existing rich-text / body editor default (matches `RichTextSpan` and item layouts). */
    const val DEFAULT_FONT_SIZE = 16
    const val DEFAULT_FONT_FAMILY = "default"
    const val DEFAULT_ITEM_HEIGHT = 20
    const val TIME_FORMAT_12 = "12"
    const val TIME_FORMAT_24 = "24"
    const val DEFAULT_DATE_FORMAT = "dd/MM/yyyy"
}