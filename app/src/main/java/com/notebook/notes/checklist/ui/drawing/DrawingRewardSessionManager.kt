package com.notebook.notes.checklist.ui.drawing

/**
 * In-memory session state for rewarded drawing saves.
 *
 * Reward counts and temporary save authorization live only for the process
 * lifetime — they reset on app restart / process death and are never persisted.
 *
 * Authorization applies only to the current drawing save; it does not unlock
 * Premium, the Drawing feature, or future drawings.
 */
object DrawingRewardSessionManager {

    const val MAX_REWARD_UNLOCKS_PER_SESSION = 3

    private var rewardUnlockCount: Int = 0
    private var pendingSaveAuthorized: Boolean = false

    fun canWatchReward(): Boolean =
        rewardUnlockCount < MAX_REWARD_UNLOCKS_PER_SESSION

    fun remainingRewardUnlocks(): Int =
        (MAX_REWARD_UNLOCKS_PER_SESSION - rewardUnlockCount).coerceAtLeast(0)

    fun isSaveAuthorized(): Boolean = pendingSaveAuthorized

    /**
     * Grants a one-shot save authorization for the current drawing and increments
     * the session reward counter. Returns false if the session limit was reached.
     */
    fun authorizeNextSave(): Boolean {
        if (pendingSaveAuthorized) return true
        if (!canWatchReward()) return false
        rewardUnlockCount++
        pendingSaveAuthorized = true
        return true
    }

    /**
     * Consumes the temporary save authorization after a successful save
     * (or when explicitly abandoning the unlock).
     */
    fun consumeSaveAuthorization() {
        pendingSaveAuthorized = false
    }
}
