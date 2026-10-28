package com.notebook.notes.checklist.iap

import androidx.annotation.StringRes
import com.notebook.notes.checklist.R

/**
 * Premium-gated features. Add new entries here for future upsells.
 */
enum class PremiumFeature(
    @param:StringRes val titleRes: Int,
    @param:StringRes val messageRes: Int,
) {
    LOCK_NOTE(
        titleRes = R.string.premium_feature_title,
        messageRes = R.string.premium_feature_lock_note_message,
    ),
    CUSTOM_BACKGROUND(
        titleRes = R.string.premium_feature_title,
        messageRes = R.string.premium_feature_custom_background_message,
    ),
    DRAWING_SAVE(
        titleRes = R.string.save_your_drawing,
        messageRes = R.string.save_your_drawing_desc,
    ),
}
