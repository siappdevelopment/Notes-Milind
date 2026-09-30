package com.notebook.notes.checklist.flow

import android.app.Activity
import android.util.Log
import com.notebook.notes.checklist.helper.PermissionHelper
import com.notebook.notes.checklist.ui.activity.CallPermissionActivity
import com.notebook.notes.checklist.ui.activity.HomeActivity
import com.notebook.notes.checklist.ui.activity.OverLayPermissionActivity
import com.notebook.notes.checklist.ui.activity.PermissionFlowActivity
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton
import android.Manifest
import android.os.Build

/**
 * Sequences splash and permission-screen permission requests from Remote Config.
 * Already-granted permissions are skipped. Allow and deny both continue the flow.
 */
@Singleton
class PermissionManager @Inject constructor(
    private val remoteConfigManager: RemoteConfigManager,
    private val permissionHelper: PermissionHelper,
    private val flowManager: FlowManager,
    private val navigationCoordinator: NavigationCoordinator
) {

    private val screenQueue: ArrayDeque<PermissionType> = ArrayDeque()

    fun splashPermissionsToRequest(
        mode: SplashPermissionMode = remoteConfigManager.getSplashPermissionMode()
    ): List<String> {
        val permissions = mutableListOf<String>()
        when (mode) {
            SplashPermissionMode.NONE -> Unit
            SplashPermissionMode.NOTIFICATION -> addNotification(permissions)
            SplashPermissionMode.CALL_STATE ->
                permissions.add(Manifest.permission.READ_PHONE_STATE)
            SplashPermissionMode.BOTH -> {
                addNotification(permissions)
                permissions.add(Manifest.permission.READ_PHONE_STATE)
            }
        }
        return permissions
    }

    /**
     * Starts the permission-screen sequence (no Welcome).
     * Order matches [FlowConfig.permissionScreenPermissions]; granted ones are skipped.
     */
    fun startPermissionScreenFlow(from: Activity) {
        screenQueue.clear()
        remoteConfigManager.getPermissionScreenPermissions()
            .filter { !isGranted(from, it) }
            .forEach { screenQueue.addLast(it) }
        Log.d(TAG, "Permission screen queue: $screenQueue")
        proceed(from)
    }

    /**
     * Call after the user finishes a permission UI step (allow or deny).
     */
    fun onPermissionStepFinished(from: Activity) {
        proceed(from)
    }

    private fun proceed(from: Activity) {
        while (screenQueue.isNotEmpty() && isGranted(from, screenQueue.first())) {
            screenQueue.removeFirst()
        }

        val next = screenQueue.pollFirst()
        if (next == null) {
            flowManager.markPermissionCompleted()
            flowManager.next(from)
            return
        }

        when (next) {
            PermissionType.CALL_STATE -> {
                navigationCoordinator.openCallPermission(from)
                finishAfterLaunch(from)
            }

            PermissionType.OVERLAY -> {
                navigationCoordinator.openOverlayPermission(from)
                finishAfterLaunch(from)
            }

            PermissionType.NOTIFICATION -> {
                if (from is FlowPermissionHost) {
                    from.requestNotificationPermission {
                        onPermissionStepFinished(from)
                    }
                } else {
                    Log.w(TAG, "No FlowPermissionHost for NOTIFICATION; skipping")
                    proceed(from)
                }
            }
        }
    }

    fun isGranted(activity: Activity, type: PermissionType): Boolean = when (type) {
        PermissionType.NOTIFICATION ->
            permissionHelper.checkNotificationPermission(activity) ==
                PermissionHelper.PermissionStatus.GRANTED

        PermissionType.CALL_STATE ->
            permissionHelper.checkPhoneStatePermission(activity) ==
                PermissionHelper.PermissionStatus.GRANTED

        PermissionType.OVERLAY -> permissionHelper.isOverlayPermissionGranted()
    }

    private fun addNotification(permissions: MutableList<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * Finish the sender when leaving for a dedicated permission Activity.
     * Keep the host alive when requesting an in-place notification dialog.
     */
    private fun finishAfterLaunch(from: Activity) {
        when (from) {
            is HomeActivity -> Unit
            is PermissionFlowActivity,
            is CallPermissionActivity,
            is OverLayPermissionActivity -> from.finish()
            else -> from.finish()
        }
    }

    companion object {
        private const val TAG = "PermissionManager"
    }
}
