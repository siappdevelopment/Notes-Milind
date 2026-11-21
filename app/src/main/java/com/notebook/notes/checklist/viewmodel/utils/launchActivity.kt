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
    AdsManager.showInterHome(this, navigate)
}

fun Fragment.openWithHomeInterAd(navigate: () -> Unit) {
    requireActivity().openWithHomeInterAd(navigate)
}

fun Activity.openWithCreateNotesInterAd(navigate: () -> Unit) {
    navigate()
}

fun Activity.finishWithCreateNotesBackInterAd() {
    finish()
}

fun Activity.openWithSetPasswordInterAd(navigate: () -> Unit) {
    navigate()
}

fun Activity.finishWithSetPasswordBackInterAd() {
    finish()
}

fun Activity.openWithDrawingInterAd(navigate: () -> Unit) {
    navigate()
}

fun Activity.finishWithDrawingBackInterAd() {
    finish()
}

fun Activity.openWithImageViewerInterAd(navigate: () -> Unit) {
    navigate()
}

fun Activity.finishWithImageViewerBackInterAd() {
    finish()
}

fun Activity.openWithRecycleBinInterAd(navigate: () -> Unit) {
    navigate()
}

fun Activity.finishWithRecycleBinBackInterAd() {
    finish()
}

fun Activity.openWithEditLabelInterAd(navigate: () -> Unit) {
    navigate()
}

fun Activity.finishWithEditLabelBackInterAd() {
    finish()
}

fun Activity.openWithNoteBackgroundPickerInterAd(navigate: () -> Unit) {
    navigate()
}

fun Activity.finishWithNoteBackgroundPickerBackInterAd() {
    finish()
}

fun Activity.openWithSettingInterAd(navigate: () -> Unit) {
    navigate()
}

fun Activity.finishWithSettingBackInterAd() {
    finish()
}


fun Activity.finishWithVideoPlayerBackInterAd() {
    finish()
}

