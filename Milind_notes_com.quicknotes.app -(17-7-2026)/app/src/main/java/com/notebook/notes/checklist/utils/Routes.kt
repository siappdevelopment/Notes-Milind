package com.notebook.notes.checklist.utils

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.notebook.notes.checklist.ads.AppConstants
import com.notebook.notes.checklist.ui.activity.HomeActivity
import com.notebook.notes.checklist.ui.activity.LanguageActivity
import com.notebook.notes.checklist.ui.activity.Onboarding1Activity
import com.notebook.notes.checklist.ui.activity.SplashActivity

import kotlin.jvm.java

object Routes {
    fun startMainActivity(fromActivity: Activity) =
        Intent(fromActivity, HomeActivity::class.java).apply {
            putExtra(AppConstants.KEY_TRACKING_SCREEN_FROM, fromActivity::class.java.simpleName)
            fromActivity.startActivity(this)
        }
    fun startOnBoardingActivity(fromActivity: Activity) =
        Intent(fromActivity, Onboarding1Activity::class.java).apply {
            putExtra(AppConstants.KEY_TRACKING_SCREEN_FROM, fromActivity::class.java.simpleName)
            fromActivity.startActivity(this)

        }

    fun startLanguageActivity(fromActivity: Activity, bundle: Bundle?) =
        Intent(fromActivity, LanguageActivity::class.java).apply {
            putExtra(AppConstants.KEY_TRACKING_SCREEN_FROM, fromActivity::class.java.simpleName)
            bundle?.let { putExtras(it) }
            fromActivity.startActivity(this)

        }



}