# Requirement: Reusable "onboarding-ads-firebase-kit" Skill

## Goal
Analyze this app's full onboarding flow (Splash -> Language -> Onboarding 1/2/3 (+ full-native ad page) -> Set Default -> Home), its Firebase config and its ads placement/dependencies, and package everything into ONE global Claude skill (`~/.claude/skills/onboarding-ads-firebase-kit/`) usable in any other Android project.

## Requirements
1. Flow map: screen order, routing, which screen opens when (Splash, Language, Onboarding1-3, OnboardingFullAd, SetDefault).
2. Firebase: google-services setup, Remote Config defaults/keys, fetch/timeout logic, `ad_config.json` / `ad_config_debug.json` structure.
3. Ads placement table: screen -> ad type (banner/native/full native/interstitial) -> ad-unit key -> show/skip rules.
4. Ads dependency: gradle plugins, libraries + exact versions (Firebase BOM, AdMob, UMP consent, etc.), manifest entries.
5. UI assets copied verbatim into the skill: layout XML, drawables/icons, colors, strings, themes, fonts.
6. Code templates (Kotlin/Java) with package-name placeholder: Activities, adapters, AdsManager, RemoteConfig loader, consent, onboarding store.
7. SKILL.md with step-by-step instructions + `templates/` folder; Claude copies files and replaces package name/keys in the target project.
8. All ad unit IDs in templates are Google TEST IDs (no real IDs, no secrets).

## Edge cases
- Target app has different package/theme/ad IDs -> placeholders.
- Optional screens (Set Default) can be skipped.

## Out of scope
- Home screen ads and other features; any change to this app's code.

## Open questions
- None.
