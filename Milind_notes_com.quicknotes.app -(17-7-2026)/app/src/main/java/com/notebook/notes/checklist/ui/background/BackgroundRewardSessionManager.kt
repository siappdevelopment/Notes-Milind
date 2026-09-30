package com.notebook.notes.checklist.ui.background

import com.notebook.notes.checklist.iap.PremiumFeatureGate

/**
 * In-memory session state for rewarded background unlocks.
 *
 * Unlock keys and reward counts live only for the process lifetime —
 * they reset on app restart / process death and are never persisted.
 */
object BackgroundRewardSessionManager {

    /** First N backgrounds in each category are free. Override for remote config later. */
    @JvmField
    var freeBackgroundCount: Int = DEFAULT_FREE_BACKGROUND_COUNT

    const val DEFAULT_FREE_BACKGROUND_COUNT = 2
    const val MAX_REWARD_UNLOCKS_PER_SESSION = 3

    private val unlockedKeys = mutableSetOf<String>()
    private var rewardUnlockCount: Int = 0

    fun isFreeIndex(indexInCategory: Int): Boolean =
        indexInCategory >= 0 && indexInCategory < freeBackgroundCount.coerceAtLeast(0)

    fun isTemporarilyUnlocked(key: String): Boolean =
        key.isNotBlank() && unlockedKeys.contains(key)

    fun canWatchReward(): Boolean =
        rewardUnlockCount < MAX_REWARD_UNLOCKS_PER_SESSION

    fun remainingRewardUnlocks(): Int =
        (MAX_REWARD_UNLOCKS_PER_SESSION - rewardUnlockCount).coerceAtLeast(0)

    /**
     * Temporarily unlocks [key] for this session and increments the reward counter.
     * Returns false if the session reward limit was already reached.
     */
    fun unlockForSession(key: String): Boolean {
        if (key.isBlank()) return false
        if (unlockedKeys.contains(key)) return true
        if (!canWatchReward()) return false
        unlockedKeys.add(key)
        rewardUnlockCount++
        return true
    }

    fun isAccessible(key: String, indexInCategory: Int): Boolean {
        if (PremiumFeatureGate.isPremiumSafe()) return true
        if (isFreeIndex(indexInCategory)) return true
        if (isTemporarilyUnlocked(key)) return true
        return false
    }

    fun isLocked(key: String, indexInCategory: Int): Boolean =
        !isAccessible(key, indexInCategory)
}
