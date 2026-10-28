package com.notebook.notes.checklist.di

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Process lifecycle hook. Coming back from the background deliberately does nothing: the app resumes on
 * the screen the user left, and never pushes Home (or a "welcome back" screen) on top of it.
 */
class AppLifecycleObserver : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {}

    override fun onStop(owner: LifecycleOwner) {}
}
