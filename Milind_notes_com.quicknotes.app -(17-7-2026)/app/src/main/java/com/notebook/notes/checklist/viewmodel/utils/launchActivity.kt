package com.notebook.notes.checklist.viewmodel.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.notebook.notes.checklist.ads.AdsManager



inline fun <reified T> Context.launchActivity(
    noinline intentBuilder: Intent.() -> Unit = {}
) {
    val intent = Intent(this, T::class.java)
    intent.intentBuilder()
    startActivity(intent)
}

fun Activity.openWithHomeInterAd(navigate: () -> Unit) {
    AdsManager.showInterScreenOpen(this, navigate)
}

fun Fragment.openWithHomeInterAd(navigate: () -> Unit) {
    requireActivity().openWithHomeInterAd(navigate)
}

fun Activity.openWithCreateNotesInterAd(navigate: () -> Unit) {
    AdsManager.showInterScreenOpen(this, navigate)
}

fun Activity.finishWithCreateNotesBackInterAd() {
    AdsManager.showInterScreenOpen(this) { finish() }
}

fun Activity.openWithSetPasswordInterAd(navigate: () -> Unit) {
    AdsManager.showInterScreenOpen(this, navigate)
}

fun Activity.finishWithSetPasswordBackInterAd() {
    AdsManager.showInterScreenOpen(this) { finish() }
}

fun Activity.openWithDrawingInterAd(navigate: () -> Unit) {
    AdsManager.showInterScreenOpen(this, navigate)
}

fun Activity.finishWithDrawingBackInterAd() {
    AdsManager.showInterScreenOpen(this) { finish() }
}

fun Activity.openWithImageViewerInterAd(navigate: () -> Unit) {
    AdsManager.showInterScreenOpen(this, navigate)
}

fun Activity.finishWithImageViewerBackInterAd() {
    AdsManager.showInterScreenOpen(this) { finish() }
}

fun Activity.openWithRecycleBinInterAd(navigate: () -> Unit) {
    AdsManager.showInterScreenOpen(this, navigate)
}

fun Activity.finishWithRecycleBinBackInterAd() {
    AdsManager.showInterScreenOpen(this) { finish() }
}

fun Activity.openWithEditLabelInterAd(navigate: () -> Unit) {
    AdsManager.showInterScreenOpen(this, navigate)
}

fun Activity.finishWithEditLabelBackInterAd() {
    AdsManager.showInterScreenOpen(this) { finish() }
}

fun Activity.openWithNoteBackgroundPickerInterAd(navigate: () -> Unit) {
    AdsManager.showInterScreenOpen(this, navigate)
}

fun Activity.finishWithNoteBackgroundPickerBackInterAd() {
    AdsManager.showInterScreenOpen(this) { finish() }
}

fun Activity.openWithSettingInterAd(navigate: () -> Unit) {
    AdsManager.showInterScreenOpen(this, navigate)
}

fun Activity.finishWithSettingBackInterAd() {
    AdsManager.showInterScreenOpen(this) { finish() }
}


fun Activity.finishWithVideoPlayerBackInterAd() {
    AdsManager.showInterScreenOpen(this) { finish() }
}

