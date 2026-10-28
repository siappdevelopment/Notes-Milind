package com.notebook.notes.checklist.Advertisement.activity

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.notebook.notes.checklist.Advertisement.ADSAppManage

abstract class BaseActivity : AppCompatActivity() {
    var isMaterialActivity = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        lastLanguage = baseConfig.appLanguage

        if (savedInstanceState != null) {
            ADSAppManage.isAppOpenBlocked = true
        }
    }

    @SuppressLint("NewApi")
    override fun onResume() {
//        if (baseConfig.appLanguage != lastLanguage) {
//            lastLanguage = baseConfig.appLanguage
//            recreate()
//        }
        super.onResume()
//        AppCompatDelegate.setDefaultNightMode(getAppNightMode())
//        updateNavigationBarColor()
//        val backgroundColor = getProperBackgroundColor()
//        updateStatusbarColor(backgroundColor)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
