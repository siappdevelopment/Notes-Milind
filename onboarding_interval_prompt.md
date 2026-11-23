# Prompt for AI: Onboarding show-control (every launch / every N launches / every time interval)

## Context (already in this Android project, package `com.notebook.notes.checklist`)
Onboarding is already launch-count controlled. Do NOT rebuild it, extend it.

| Piece | File | What it does today |
|---|---|---|
| Decision logic | `flow/OnboardingManager.kt` | `shouldShowOnboarding()`: first launch = true; else `repeat > 0 && launchesSinceOnboarding + 1 >= repeat`. `onOnboardingCompleted()` sets `hasSeenOnboarding=true`, counter=0. `recordNormalLaunch()` counter+1. |
| Storage | `flow/PreferenceOnboardingStore.kt` | SharedPreferences keys `has_seen_onboarding`, `launches_since_onboarding` (+ legacy flags `ONBOARDING_COMPLETED`, `IS_ONBOARDING_COMPLTED` count as "seen"). |
| Remote key | `flow/RemoteConfigManager.kt` | `onboarding_repeat_count` (Int, 0 = once only), read from the big ads JSON (nested, recursive lookup) -> separate RC param -> XML default. `onboarding_count` = number of pages (0-3). |
| Where decided | `ui/activity/SplashActivity.kt` `decideOnboardingForThisLaunch()` | Only on cold launch (`savedInstanceState == null`); sets `flowManager.onboardingDueThisLaunch`; if not due -> `recordNormalLaunch()`. |
| Where completed | `flow/FlowManager.kt` (~line 141, 162) | Calls `onOnboardingCompleted()` when user finishes/skips; builds the flow with ONBOARDING only if due and `onboardingCount > 0`. |
| Defaults | `res/xml/remote_config_defaults.xml`, `assets/ad_config.json`, `assets/ad_config_debug.json` | `onboarding_repeat_count` currently 2 in the JSONs, 1 in the XML. |
| Tests | `src/test/.../flow/OnboardingManagerTest.kt` | JVM unit tests with a `FakeOnboardingStore`. |

Current behaviour: N=0 once only, N=1 every launch, N=2 every 2nd launch, N=3 every 3rd launch... Only launch COUNT is supported. There is NO time-based interval.
Note: the comment in `remote_config_defaults.xml` says "1 = every other launch" which is wrong (1 = every launch). Fix that comment.

## Goal
Let me control, from Firebase Remote Config without an app update, when onboarding is shown:
1. **Once only** (first install)
2. **Every launch**
3. **Every N launches** (already works)
4. **Every X hours / minutes** (NEW) - show onboarding again only if at least X time has passed since onboarding was last completed.

## Requirements
1. Add a new Remote Config key `onboarding_repeat_interval_minutes` (Long, default 0 = disabled). Resolve it exactly like `onboarding_repeat_count` (ads JSON recursive -> RC param -> XML default), add to `FlowConfig`, `RemoteConfigManager` (const, getter, parse, `loadXmlDefaults`), `remote_config_defaults.xml`, `ad_config.json`, `ad_config_debug.json`.
2. Add `onboarding_repeat_mode` (String): `ONCE | EVERY_LAUNCH | EVERY_N_LAUNCHES | EVERY_INTERVAL`. If missing/invalid, fall back to the current behaviour derived from `onboarding_repeat_count` so existing configs keep working.
3. Extend `OnboardingStore` with `lastOnboardingCompletedAt: Long` (epoch ms, 0 = never) persisted in `PreferenceOnboardingStore`. Set it inside `onOnboardingCompleted()`.
4. Extend `OnboardingManager.shouldShowOnboarding()` to take the mode, repeat count, interval minutes and a `now: () -> Long` clock (injectable for tests):
   - never seen -> always true
   - ONCE -> false after first completion
   - EVERY_LAUNCH -> true
   - EVERY_N_LAUNCHES -> existing `>=` counter logic
   - EVERY_INTERVAL -> `now - lastOnboardingCompletedAt >= intervalMinutes * 60_000`; if `lastOnboardingCompletedAt == 0` for a user who has legacy "seen" flags, treat as due only after the interval from first app run (set it to `now` once, do not force onboarding immediately).
   - If the device clock goes backwards (`now < lastCompletedAt`), treat as not due and reset `lastCompletedAt = now`.
5. Keep existing rules: decide once per cold launch only (rotation/recreation ignored), `shouldShowOnboarding()` stays a pure read, onboarding started but not completed is shown again, `onboarding_count == 0` skips onboarding regardless of mode.
6. Do not change which screens/ads are in the onboarding flow, and do not touch paywall logic.
7. Update `OnboardingManagerTest.kt`: add cases for each mode, interval boundary (exactly X, X-1), clock moved backwards, legacy-seen user with `lastCompletedAt == 0`, unknown mode fallback. Existing tests must still pass.
8. Fix the wrong comment in `remote_config_defaults.xml` and document the new keys there.

## Edge cases
- Remote Config fetch fails/offline -> use XML defaults, never crash, never show onboarding on every launch by accident.
- Negative values / huge values for count or minutes -> `coerceAtLeast(0)`; interval 0 in EVERY_INTERVAL mode = disabled (treated as ONCE).
- App updated from an older version (legacy flags only) must NOT be pushed through onboarding on first launch of the new version.
- Fresh install after uninstall -> first launch always shows onboarding.

## Out of scope
- Onboarding UI/layouts/ads, language screen, permission screen, paywall, splash ads.
- Any other Remote Config key.

## Done when
- `./gradlew testDebugUnitTest` passes, `./gradlew assembleDebug` builds.
- Changing only the Firebase value (`onboarding_repeat_mode` + `onboarding_repeat_count` / `onboarding_repeat_interval_minutes`) changes the behaviour on next cold launch.
- Report max 15 lines: files changed, new keys, tests added. Read only the files listed above and `git diff`; no repo-wide exploration.

## Remote Config examples
```json
{ "onboarding_repeat_mode": "ONCE" }
{ "onboarding_repeat_mode": "EVERY_LAUNCH" }
{ "onboarding_repeat_mode": "EVERY_N_LAUNCHES", "onboarding_repeat_count": 3 }
{ "onboarding_repeat_mode": "EVERY_INTERVAL", "onboarding_repeat_interval_minutes": 1440 }
```
