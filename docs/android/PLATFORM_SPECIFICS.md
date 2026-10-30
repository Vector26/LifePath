# LifePath Android Platform Specifics

Android-specific requirements and considerations for the LifePath app.

---

## SDK Versions

| Setting | Value |
|---------|-------|
| minSdk | 24 (Android 7.0) |
| targetSdk | 34 (Android 14) |
| compileSdk | 34 |

Rationale: minSdk 24 covers ~95% of devices; targetSdk 34 for Play Store requirements.

---

## Permissions

| Permission | Use |
|------------|-----|
| INTERNET | API calls |
| RECEIVE_BOOT_COMPLETED | Restart WorkManager after reboot |
| FOREGROUND_SERVICE | Sync notification (if used) |
| VIBRATE | Haptic feedback (optional) |

**No**: Location (unless location-based unblock trigger); storage (use app-specific storage).

---

## Foreground Service

If used for critical sync:

- **Type**: `dataSync` or `shortService`.
- **Notification**: Required; minimal: "LifePath syncing."
- **Duration**: Short (< 30s typical).
- **Target SDK 34**: Declare appropriate foreground service type in manifest.

---

## Doze and App Standby

### Doze

- **Behavior**: Delays network, alarms, jobs when device is idle and unplugged.
- **WorkManager**: Uses `setExpedited()` or `setImportantWhileForeground()` for time-sensitive work.
- **Fallback**: Sync on next app launch.

### App Standby (Buckets)

- **Active**: User recently used app; normal restrictions.
- **Working set**: Occasional use; some restrictions.
- **Rare**: Rarely used; heavy restrictions.
- **Mitigation**: Request exemption only if justified; prefer sync on launch.

---

## Widget Constraints

| Constraint | Limit |
|------------|-------|
| Update frequency | System may throttle; 15–30 min realistic |
| Data source | Read from DB; Widget receives data via RemoteViews or Glance |
| Size | Small 1x1, Medium 2x2; avoid large complex layouts |

### Widget Update Strategy

1. App writes to DB.
2. App triggers widget update (same process).
3. WorkManager sync runs; on success, fetch GET /stats/today; store quests_completed_today, habits_completed_today in Room; trigger widget update.
4. Widget reads current quest, streaks, Sim stage, daily stats from DB. Avoid widget directly calling API.

---

## Haptics

- **Complete action**: Light impact (`HapticFeedbackConstants.CONFIRM` or equivalent).
- **Block action**: Neutral or light.
- **Skip action**: Light impact (same as block).
- **Avoid**: Strong or long vibrations; no "celebration" burst for every completion (optional, user setting).

### Respect User Settings

- Check if haptics disabled in system; respect.
- Honor in-app "Haptic feedback" setting.

---

## Notifications

- **Sync**: Minimal; "Syncing…" during foreground sync; auto-dismiss.
- **Reminders**: Optional; user-configured. Calm tone; no guilt ("You have a quest waiting" vs "You missed 5 quests!").
- **Channels**: Separate channel for sync vs reminders; low importance for sync.

---

## Secure Storage

- **Tokens**: Use EncryptedSharedPreferences or Android Keystore.
- **Sensitive data**: Never in plain SharedPreferences or external storage.

---

## ProGuard / R8

- **API models**: Keep for reflection if using Gson/Moshi.
- **Retrofit**: Add keep rules for API interfaces if needed.
- **Test**: Verify release build; no obfuscation of critical paths.

---

## App Bundle

- **Format**: AAB (Android App Bundle) for Play Store.
- **Splits**: Use resource/config splits if app supports many locales (unlikely for MVP).
