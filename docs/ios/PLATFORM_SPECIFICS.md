# LifePath iOS Platform Specifics

iOS-specific requirements and considerations for the LifePath app.

---

## Deployment Target

| Setting | Value |
|---------|-------|
| Minimum iOS | 16.0 |
| Target | Latest (e.g., iOS 18) |

Rationale: iOS 16 covers most devices; supports WidgetKit, modern SwiftUI, BackgroundTasks.

---

## Background App Refresh

### Capabilities

- Enable "Background Modes" → "Background fetch."
- Register task identifier in Info.plist.
- Schedule in `application(_:didFinishLaunchingWithOptions:)` or after first login.

### Constraints

- **Timing**: System decides when to run; not guaranteed.
- **Frequency**: Throttled by system; typically a few times per day.
- **Battery**: Low Power Mode may reduce or disable.
- **Best practice**: Sync on foreground; use background as supplement.

---

## Widget Constraints (WidgetKit)

### Update Budget

- System limits refresh frequency.
- **Small widget**: ~4–5 updates per hour typical.
- **Timeline**: Provide 3–5 entries; system interpolates.

### Size Limits

- **Small**: ~158pt width (approx).
- **Medium**: ~364pt width.
- **Large**: Use sparingly; more complex.
- **Content**: Minimal text; avoid images that exceed size.

### Data Sharing

- **App Group**: Required for widget to read app data.
- **Container**: Shared UserDefaults or small SQLite/Core Data store. Include quests_completed_today, habits_completed_today (from GET /stats/today) for "X quests done today" in Lock Screen widget.
- **Security**: App Group is sandboxed to app + widget; not other apps.

---

## Haptics

- **Complete action**: `UIImpactFeedbackGenerator(style: .light)` or `.medium`.
- **Block action**: Light impact.
- **Skip action**: Light impact (same as block).
- **Avoid**: Strong or long; no celebration burst for every completion (optional, user setting).

### Respect User Settings

- Check "Reduce Motion" and "Haptic" in Accessibility.
- Honor in-app "Haptic feedback" setting.

---

## App Store Requirements

### Privacy

- **Privacy Manifest**: Required for certain APIs (if used).
- **App Tracking Transparency**: Only if tracking; LifePath does not require by default.
- **Data collection**: Declare what is collected (account, quests, habits); minimal.

### Data Handling

- **Export**: Support user data export (GDPR).
- **Deletion**: Support account deletion; purge data from server and local.

### Review Guidelines

- **Guideline 4.2**: Minimum functionality; ensure app is complete.
- **Guideline 5.1**: Privacy; clear privacy policy.

---

## Secure Storage

### Keychain

- **Tokens**: Store access_token, refresh_token in Keychain.
- **Service**: Use unique service name (e.g., `com.lifepath.tokens`).
- **Access**: `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` or similar.
- **Sharing**: Do not share tokens with widget; widget uses cached read-only data.

### Sensitive Data

- Never in UserDefaults (plain).
- Never in plist files in Documents.

---

## SwiftUI vs UIKit

- **Recommendation**: SwiftUI for new screens; supports iOS 16+.
- **Fallback**: UIKit if targeting older iOS or specific needs.
- **Mix**: Use UIViewControllerRepresentable for UIKit in SwiftUI when needed.

---

## Localization

- **Base**: English.
- **Future**: Use `LocalizedStringKey`; `.strings` files or String Catalog.
- **Layout**: Support RTL if adding Arabic/Hebrew later.
