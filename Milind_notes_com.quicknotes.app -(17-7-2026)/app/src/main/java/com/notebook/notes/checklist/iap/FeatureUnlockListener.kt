package com.notebook.notes.checklist.iap

/**
 * Listener for FeatureUnlockDialog.
 */
interface FeatureUnlockListener {
    fun onRewardUnlock()
    fun onUpgrade()
    fun onCancel()
}
