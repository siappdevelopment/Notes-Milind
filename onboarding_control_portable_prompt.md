# Prompt: Add remote-controlled "when to show onboarding" to an Android app

You are working in a DIFFERENT Android project from the one this prompt was written in. Do not assume any file, package or class name below exists. First find the equivalents (see Step 0), then implement. Replace `<pkg>` with the target project's package.

## Goal
Control, from Firebase Remote Config and without an app update, WHEN the onboarding flow is shown:
1. ONCE: only on first install
2. EVERY_LAUNCH: every cold launch
3. EVERY_N_LAUNCHES: every Nth cold launch
4. EVERY_INTERVAL: again only after X minutes since onboarding was last completed

## Step 0: Discover (read only what is needed, no repo-wide exploration)
Find in the target project and note the names:
- Splash/launcher Activity and where it routes to onboarding vs home.
- Existing "onboarding done" flag in SharedPreferences/DataStore (the legacy flag).
- How Firebase Remote Config is already fetched and parsed (or whether it is missing: then add `firebase-config` via the project's existing Firebase BOM, plus `res/xml/remote_config_defaults.xml`).
- Where the user finishes or skips onboarding (the single place to call `onOnboardingCompleted()`).
- DI style (Hilt/Koin/manual) and test setup (JUnit).
If the project has no onboarding, stop and tell me.

## Step 1: Pure decision logic (JVM-testable, no Android imports)
```kotlin
enum class OnboardingRepeatMode { ONCE, EVERY_LAUNCH, EVERY_N_LAUNCHES, EVERY_INTERVAL;
    companion object { fun parse(raw: String?): OnboardingRepeatMode? =
        entries.find { it.name.equals(raw?.trim(), ignoreCase = true) } } }

interface OnboardingStore {
    var hasSeenOnboarding: Boolean          // true after finish/skip (also true if legacy flag set)
    var launchesSinceOnboarding: Int        // normal cold launches since last completion
    var lastOnboardingCompletedAt: Long     // epoch ms, 0 = unknown
}

class OnboardingManager(
    private val store: OnboardingStore,
    private val mode: () -> OnboardingRepeatMode,
    private val repeatCount: () -> Int,
    private val intervalMinutes: () -> Long,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /** Pure read except the one-time legacy timestamp seed and clock-rollback reset. */
    fun shouldShowOnboarding(): Boolean {
        if (!store.hasSeenOnboarding) return true
        return when (mode()) {
            OnboardingRepeatMode.ONCE -> false
            OnboardingRepeatMode.EVERY_LAUNCH -> true
            OnboardingRepeatMode.EVERY_N_LAUNCHES -> {
                val n = repeatCount().coerceAtLeast(0)
                n > 0 && store.launchesSinceOnboarding + 1 >= n   // >= so a lowered N still triggers
            }
            OnboardingRepeatMode.EVERY_INTERVAL -> {
                val ms = intervalMinutes().coerceAtLeast(0) * 60_000L
                if (ms == 0L) return false                         // 0 = disabled = ONCE
                val t = now()
                val last = store.lastOnboardingCompletedAt
                when {
                    last == 0L -> { store.lastOnboardingCompletedAt = t; false } // legacy user: start clock now
                    t < last -> { store.lastOnboardingCompletedAt = t; false }   // clock went backwards
                    else -> t - last >= ms
                }
            }
        }
    }
    fun onOnboardingCompleted() {
        store.hasSeenOnboarding = true
        store.launchesSinceOnboarding = 0
        store.lastOnboardingCompletedAt = now()
    }
    /** Call once per cold launch that did NOT show onboarding. */
    fun recordNormalLaunch() { store.launchesSinceOnboarding += 1 }
}
```
Persist the store in SharedPreferences (keys: `has_seen_onboarding`, `launches_since_onboarding`, `last_onboarding_completed_at`). `hasSeenOnboarding` getter must also return true if the project's existing legacy "onboarding done" flag is true.

## Step 2: Remote Config keys
| Key | Type | Default | Meaning |
|---|---|---|---|
| `onboarding_repeat_mode` | String | derived (see below) | ONCE / EVERY_LAUNCH / EVERY_N_LAUNCHES / EVERY_INTERVAL |
| `onboarding_repeat_count` | Int | 0 | N for EVERY_N_LAUNCHES (0 = once, 1 = every launch, 2 = every 2nd...) |
| `onboarding_repeat_interval_minutes` | Long | 0 | X for EVERY_INTERVAL (0 = disabled) |

- Add all three to `remote_config_defaults.xml` with a comment explaining each (be correct: count 1 = every launch).
- Missing/invalid mode: if count > 0 -> EVERY_N_LAUNCHES, else ONCE. Negative numbers -> coerceAtLeast(0).
- If the project fetches Remote Config as one big JSON string, read these keys from the same place the other flow keys come from. Do not add a second fetch.
- Fetch failure/offline -> use defaults; never crash; never default to "every launch".

## Step 3: Wiring
- In the splash/launcher Activity, on COLD launch only (`savedInstanceState == null`; ignore rotation/recreate), AFTER Remote Config is cached, call `shouldShowOnboarding()` once, store the result for this launch, and if not due call `recordNormalLaunch()`.
- Route to onboarding only if due. Call `onOnboardingCompleted()` when the user finishes or skips (single place). Started-but-not-finished onboarding leaves state unchanged, so it shows again.
- If the project already has an onboarding page-count setting equal to 0, skip onboarding regardless of mode.
- Do not change onboarding UI, ads, or other screens.

## Step 4: Tests (JUnit, fake store + fake clock)
Cover: first launch always true; ONCE false after completion; EVERY_LAUNCH true; N=2 pattern (show, skip, show); lowered N still triggers; interval boundary (X-1 min false, exactly X true); clock moved backwards -> false; legacy-seen user with `lastCompletedAt == 0` -> false and clock seeded; unknown mode fallback; negative values; `shouldShowOnboarding()` called repeatedly does not advance the launch counter.

## Edge cases
- App updated from an older version with only the legacy flag: must NOT be forced through onboarding.
- Uninstall + reinstall = first launch -> onboarding shown.
- Only cold launches count; returning from background does not.

## Done when
- Unit tests pass and the project builds with its normal command (`./gradlew testDebugUnitTest assembleDebug`).
- Changing only the Firebase values changes behaviour on next cold launch.
- Final report max 15 lines: files changed, keys added, tests added, anything you could not verify. Max 2 fix rounds, then stop and ask me.

## Firebase examples
```
onboarding_repeat_mode = ONCE
onboarding_repeat_mode = EVERY_LAUNCH
onboarding_repeat_mode = EVERY_N_LAUNCHES , onboarding_repeat_count = 3
onboarding_repeat_mode = EVERY_INTERVAL    , onboarding_repeat_interval_minutes = 1440
```
