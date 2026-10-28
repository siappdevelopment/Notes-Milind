package com.notebook.notes.checklist.flow

import android.app.Activity

/**
 * Activities that can host a runtime notification permission request
 * as part of the Remote Config permission sequence.
 */
interface FlowPermissionHost {
    fun requestNotificationPermission(onComplete: () -> Unit)
}
