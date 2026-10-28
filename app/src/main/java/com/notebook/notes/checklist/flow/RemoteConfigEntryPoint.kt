package com.notebook.notes.checklist.flow

import com.notebook.notes.checklist.iap.PremiumManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Access [RemoteConfigManager] from non-Hilt types (e.g. [com.notebook.notes.checklist.Advertisement.activity.BActivity]).
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface RemoteConfigEntryPoint {
    fun remoteConfigManager(): RemoteConfigManager
    fun premiumManager(): PremiumManager
}
