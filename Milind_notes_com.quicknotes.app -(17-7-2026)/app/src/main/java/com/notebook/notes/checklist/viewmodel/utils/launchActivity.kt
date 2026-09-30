package com.notebook.notes.checklist.viewmodel.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.notebook.notes.checklist.Advertisement.ADSInterDisplayClick
import com.notebook.notes.checklist.Advertisement.ADSMainClass
import com.notebook.notes.checklist.Advertisement.ADSUtilitis



inline fun <reified T> Context.launchActivity(
    noinline intentBuilder: Intent.() -> Unit = {}
) {
    val intent = Intent(this, T::class.java)
    intent.intentBuilder()
    startActivity(intent)
}

fun Activity.openWithHomeInterAd(navigate: () -> Unit) {
    if (ADSMainClass.getHomeInterAdsShow() && ADSUtilitis.IsNetworkConnected(this)) {
        ADSInterDisplayClick.ADSInterstitialShowing(
            this,
            ADSMainClass.getStringValue(ADSMainClass.INTER_FIRST_TIME),
            ADSInterDisplayClick.OnFinishAds {
                navigate()
            }
        )
    } else {
        navigate()
    }
}

fun Fragment.openWithHomeInterAd(navigate: () -> Unit) {
    requireActivity().openWithHomeInterAd(navigate)
}

fun Activity.openWithCreateNotesInterAd(navigate: () -> Unit) {
    if (ADSMainClass.getCreateNotesInterAdsShow() && ADSUtilitis.IsNetworkConnected(this)) {
        ADSInterDisplayClick.ADSInterstitialShowing(
            this,
            ADSMainClass.getStringValue(ADSMainClass.CREATE_NOTES_SCREEN_INTER),
            ADSInterDisplayClick.OnFinishAds {
                navigate()

            }
        )
    } else {
        navigate()
    }
}

fun Activity.finishWithCreateNotesBackInterAd() {
    if (ADSMainClass.getCreateNotesInterAdsShow() && ADSUtilitis.IsNetworkConnected(this)) {
        ADSInterDisplayClick.ADSBackDisplayInterstitial(
            this,
            ADSMainClass.getStringValue(ADSMainClass.CREATE_NOTES_SCREEN_INTER),
            ADSInterDisplayClick.OnFinishAds {
                finish()
            }
        )
    } else {
        finish()
    }
}

fun Activity.openWithSetPasswordInterAd(navigate: () -> Unit) {
    if (ADSMainClass.getSetPasswordInterAdsShow() && ADSUtilitis.IsNetworkConnected(this)) {
        ADSInterDisplayClick.ADSInterstitialShowing(
            this,
            ADSMainClass.getStringValue(ADSMainClass.INTER_FIRST_TIME),
            ADSInterDisplayClick.OnFinishAds {
                navigate()
            }
        )
    } else {
        navigate()
    }
}

fun Activity.finishWithSetPasswordBackInterAd() {
    if (ADSMainClass.getSetPasswordInterAdsShow() && ADSUtilitis.IsNetworkConnected(this)) {
        ADSInterDisplayClick.ADSBackDisplayInterstitial(
            this,
            ADSMainClass.getStringValue(ADSMainClass.INTER_SECOND_TIME),
            ADSInterDisplayClick.OnFinishAds {
                finish()
            }
        )
    } else {
        finish()
    }
}

fun Activity.openWithDrawingInterAd(navigate: () -> Unit) {
    if (ADSMainClass.getDrawingInterAdsShow() && ADSUtilitis.IsNetworkConnected(this)) {
        ADSInterDisplayClick.ADSInterstitialShowing(
            this,
            ADSMainClass.getStringValue(ADSMainClass.INTER_FIRST_TIME),
            ADSInterDisplayClick.OnFinishAds {
                navigate()
            }
        )
    } else {
        navigate()
    }
}

fun Activity.finishWithDrawingBackInterAd() {
    if (ADSMainClass.getDrawingInterAdsShow() && ADSUtilitis.IsNetworkConnected(this)) {
        ADSInterDisplayClick.ADSBackDisplayInterstitial(
            this,
            ADSMainClass.getStringValue(ADSMainClass.INTER_SECOND_TIME),
            ADSInterDisplayClick.OnFinishAds {
                finish()
            }
        )
    } else {
        finish()
    }
}

fun Activity.openWithImageViewerInterAd(navigate: () -> Unit) {
    if (ADSMainClass.getImageViewerInterAdsShow() && ADSUtilitis.IsNetworkConnected(this)) {
        ADSInterDisplayClick.ADSInterstitialShowing(
            this,
            ADSMainClass.getStringValue(ADSMainClass.INTER_FIRST_TIME),
            ADSInterDisplayClick.OnFinishAds {
                navigate()
            }
        )
    } else {
        navigate()
    }
}

fun Activity.finishWithImageViewerBackInterAd() {
    if (ADSMainClass.getImageViewerInterAdsShow() && ADSUtilitis.IsNetworkConnected(this)) {
        ADSInterDisplayClick.ADSBackDisplayInterstitial(
            this,
            ADSMainClass.getStringValue(ADSMainClass.INTER_SECOND_TIME),
            ADSInterDisplayClick.OnFinishAds {
                finish()
            }
        )
    } else {
        finish()
    }
}

fun Activity.openWithRecycleBinInterAd(navigate: () -> Unit) {
    if (ADSMainClass.getRecycleBinInterAdsShow() && ADSUtilitis.IsNetworkConnected(this)) {
        ADSInterDisplayClick.ADSInterstitialShowing(
            this,
            ADSMainClass.getStringValue(ADSMainClass.INTER_FIRST_TIME),
            ADSInterDisplayClick.OnFinishAds {
                navigate()
            }
        )
    } else {
        navigate()
    }
}

fun Activity.finishWithRecycleBinBackInterAd() {
    if (ADSMainClass.getRecycleBinInterAdsShow() && ADSUtilitis.IsNetworkConnected(this)) {
        ADSInterDisplayClick.ADSBackDisplayInterstitial(
            this,
            ADSMainClass.getStringValue(ADSMainClass.INTER_SECOND_TIME),
            ADSInterDisplayClick.OnFinishAds {
                finish()
            }
        )
    } else {
        finish()
    }
}

fun Activity.openWithEditLabelInterAd(navigate: () -> Unit) {
    if (ADSMainClass.getEditLabelInterAdsShow() && ADSUtilitis.IsNetworkConnected(this)) {
        ADSInterDisplayClick.ADSInterstitialShowing(
            this,
            ADSMainClass.getStringValue(ADSMainClass.INTER_FIRST_TIME),
            ADSInterDisplayClick.OnFinishAds {
                navigate()
            }
        )
    } else {
        navigate()
    }
}

fun Activity.finishWithEditLabelBackInterAd() {
    if (ADSMainClass.getEditLabelInterAdsShow() && ADSUtilitis.IsNetworkConnected(this)) {
        ADSInterDisplayClick.ADSBackDisplayInterstitial(
            this,
            ADSMainClass.getStringValue(ADSMainClass.INTER_SECOND_TIME),
            ADSInterDisplayClick.OnFinishAds {
                finish()
            }
        )
    } else {
        finish()
    }
}

fun Activity.openWithNoteBackgroundPickerInterAd(navigate: () -> Unit) {
    if (ADSMainClass.getNoteBackgroundPickerInterAdsShow() && ADSUtilitis.IsNetworkConnected(this)) {
        ADSInterDisplayClick.ADSInterstitialShowing(
            this,
            ADSMainClass.getStringValue(ADSMainClass.INTER_FIRST_TIME),
            ADSInterDisplayClick.OnFinishAds {
                navigate()
            }
        )
    } else {
        navigate()
    }
}

fun Activity.finishWithNoteBackgroundPickerBackInterAd() {
    if (ADSMainClass.getNoteBackgroundPickerInterAdsShow() && ADSUtilitis.IsNetworkConnected(this)) {
        ADSInterDisplayClick.ADSBackDisplayInterstitial(
            this,
            ADSMainClass.getStringValue(ADSMainClass.INTER_SECOND_TIME),
            ADSInterDisplayClick.OnFinishAds {
                finish()
            }
        )
    } else {
        finish()
    }
}

fun Activity.openWithSettingInterAd(navigate: () -> Unit) {
    if (ADSMainClass.getSettingInterShow() && ADSUtilitis.IsNetworkConnected(this)) {
        ADSInterDisplayClick.ADSInterstitialShowing(
            this,
            ADSMainClass.getStringValue(ADSMainClass.INTER_FIRST_TIME),
            ADSInterDisplayClick.OnFinishAds {
                navigate()
            }
        )
    } else {
        navigate()
    }
}

fun Activity.finishWithSettingBackInterAd() {
    if (ADSMainClass.getSettingInterShow() && ADSUtilitis.IsNetworkConnected(this)) {
        ADSInterDisplayClick.ADSBackDisplayInterstitial(
            this,
            ADSMainClass.getStringValue(ADSMainClass.INTER_SECOND_TIME),
            ADSInterDisplayClick.OnFinishAds {
                finish()
            }
        )
    } else {
        finish()
    }
}


fun Activity.finishWithVideoPlayerBackInterAd() {
    if (ADSMainClass.getVideoPlayerInterAdsShow() && ADSUtilitis.IsNetworkConnected(this)) {
        ADSInterDisplayClick.ADSBackDisplayInterstitial(
            this,
            ADSMainClass.getStringValue(ADSMainClass.INTER_SECOND_TIME),
            ADSInterDisplayClick.OnFinishAds {
                finish()
            }
        )
    } else {
        finish()
    }
}

