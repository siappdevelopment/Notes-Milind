# Remote Config JSON - Gujlish Explanation Sheet

Badha ad IDs `ca-app-pub-3940256099942544/...` Google na **TEST IDs** chhe. Release pehla real IDs nakho.

## 1. General / Flow
| Key | Value | Matlab |
|---|---|---|
| ads_enabled | true | Puri app ma ads no master switch. false = badha ads band |
| onboarding_count | 3 | Onboarding na 3 page |
| onboarding_repeat_count | 2 | Onboarding flow 2 round (native_onboarding_{round}_{page}) |
| screen_flow | LANGUAGE, ONBOARDING, PERMISSION, HOME | Screens no order |
| permission_screen_permissions | CALL_STATE, NOTIFICATION, OVERLAY | Permission screen par kaya permission puchva |
| App_PrivacyPolicy / App_TermsOfUse | URL | Privacy ane Terms links (banne same URL) |

## 2. Common fields
| Field | Matlab |
|---|---|
| id / bannerId / nativeId | AdMob ad unit ID |
| isEnable | Aa ad show karvo ke nahi |
| enable_ua_check | true = User Acquisition check lagay. Atyare false |
| reloadIntervalSeconds | Banner reload time (30s) |
| colorCTA | CTA button color ("default" = theme no) |
| heightCTA | CTA button height (45) |
| positionCTA | CTA position (BOTTOM) |
| components | Native ad na parts ane order: icon_headline, body, media, cta |
| type | "banner" ke "native" - te screen par kayo ad |
| clickCount | Ketla click/open pachhi interstitial |

## 3. Splash
| Key | Value | Matlab |
|---|---|---|
| inter_splash | false | Normal splash par interstitial BAND |
| inter_splash_uninstall | true | Uninstall shortcut splash par interstitial CHALU |
| banner_splash | true, 30s | Splash banner |
| banner_splash_uninstall | true, 30s | Uninstall splash banner |
| open_resume | true | App background thi aave tyare App Open ad |

## 4. Language
native_language_1, native_language_1_click, native_language_2, native_language_2_click - Language screen na native ads (_click = language select karya pachhi badlay). Badha enabled.

## 5. Onboarding
- native_onboarding_{round}_{page}: round 1-2, page 1-3. Badha enabled.
- native_onboarding_fullscreen_1_3: **false** (band). fullscreen_2_3, 1_4, 2_4: true.
- inter_onboarding: onboarding interstitial, enabled.

## 6. Permission
native_permission - permission screen native, enabled.

## 7. Home / Notes
| Key | Matlab |
|---|---|
| home_screen | type=banner, 30s reload (nativeId che pan type banner etle use na thay) |
| create_note | type=native |
| native_drawing | Drawing screen native |
| banner_home | Alag home banner (home_screen sathe overlap - code ma kayu use thay te confirm karo) |

## 8. Interstitials
| Key | Matlab |
|---|---|
| inter_bottom_nav (clickCount 3) | Bottom nav par darek 3 click e interstitial |
| inter_screen_open (clickCount 2) | Darek 2 screen open e interstitial |
| inter_welcome | Welcome screen interstitial |
| reward_example | Rewarded ad example |

## 9. Other screens
native_survey, native_confirm_uninstall, native_welcome. setting_screen / set_password_screen / other_screen: type=native (banner id pan chhe, type "banner" karo to banner).

## 10. callend_screen
| Key | Value | Matlab |
|---|---|---|
| is_call_end_show | true | Call end screen chalu |
| app_logo_show | false | App logo na batavvo |
| is_callend_bottom_ad_show | true | Niche ad batavvo |
| callend_bottom_ads_type | native | native / banner / big_banner / mrec |
| callend_banner_ad_id / callend_native_ad_id | test | Ad unit IDs |
| notification_install_days | 1 | Install pachhi 1 divas pachhi FCM notification (matra notification permission hoy) |
| notification_call_install_days | 1 | Notification + Call permission hoy to |
| notification_call_overlay_install_days | 1 | Badhi 3 permission hoy to |
| All_allow_permission_show_fb_notification | true | Badhi permission hoy to Firebase notification batavvu |
| close_button_show_on_full_native_ads | false | Fullscreen native par close button na batavvo |
| country_get_with_ip | false | Country IP thi na lo |
| notification_country / _call_country / _call_overlay_country | [] | Khali = badha countries |

### call_end_bacK_inter
| Key | Value | Matlab |
|---|---|---|
| call_end_inter_ads_show | true | Back par interstitial chalu |
| notification_screen_back_ads_show | false | Notification flow ma back par interstitial na batavvo |
| callend_again_open_count | 2 | Banner/native load na thay to call end screen vadhumam vadhu 2 vaar fari khule |
| call_end_inter_ads_type | inter | Ad type |
| callend_inter_ad_id | test | Interstitial unit |
| call_end_inter_day_count | 1 | Install na 1 divas pachhi (day 1 thi) interstitial shuru. 0 hoy to band |
| call_end_inter_active_total_show_count | 3 | Max show count (shouldShowCallEndAd ma use). 0 hoy to band |
| call_end_inter_ads_show_country | [] | Khali = badha countries |
| call_end_inter_ads_exclude_country | [] | Kai exclude nathi |

## Dhyan ma rakhvu
1. Release pehla test IDs badlo.
2. home_screen ane banner_home overlap - confirm karo.
3. native_onboarding_fullscreen_1_3 false chhe - jaanbujhine hoy to thik.
4. day_count ke show_count 0 rakho to call end interstitial band thai jay.
