# LifePath iOS Edge Cases

Edge cases and failure handling for the LifePath iOS application.

---

## App Suspension During Sync

### Scenario

User switches away; system suspends app while sync is in progress.

### Mitigation

- **Completion handler**: Use `URLSession` with completion handlers or async/await; ensure task completes or is cancelled gracefully.
- **Background task**: Wrap sync in `beginBackgroundTask`; call `endBackgroundTask` when done.
- **Queue persistence**: Sync queue in Core Data/SwiftData; survives suspension.
- **Idempotency**: Client sends `Idempotency-Key: <uuid>` per queued mutation on replay (generate UUID when queuing).
- **On next foreground**: Resume sync; process queue.

### Data Integrity

- **Atomic writes**: Use Core Data save in a single context; avoid partial state.
- **Rollback**: If save fails, do not update UI; retry on next opportunity.

---

## Low Power Mode

### Behavior

- **Background refresh**: May be disabled or severely limited.
- **Network**: May throttle.
- **Battery**: System may kill background tasks early.

### Mitigation

- **Detect**: `ProcessInfo.processInfo.isLowPowerModeEnabled`.
- **Adjust**: Skip non-critical background sync; reduce frequency.
- **Foreground**: Always attempt sync when app is active; Low Power Mode does not block foreground networking.

---

## iPad vs iPhone Layout

### Size Classes

- **Compact**: iPhone portrait; some iPhone landscape.
- **Regular**: iPad; iPhone plus landscape.
- **Layout**: Use size classes; provide alternate layouts for regular width.

### iPad-Specific

- **Split view**: Support multitasking; adapt to 1/3, 1/2, 2/3 width.
- **Popovers**: Use for secondary actions instead of full-screen push.
- **Keyboard**: May have different shortcuts; support Cmd+key if applicable.

### Phone-Only Features

- **Widget**: Same on both; ensure content scales.
- **Haptics**: Available on both; skip on older iPad without Taptic Engine (graceful).

---

## Dynamic Type

### Support

- Use `.dynamicTypeSize` or `UIFont.preferredFont(forTextStyle:)`.
- **SwiftUI**: `.font(.body)` etc.; scales automatically.
- **UIKit**: Use `preferredFont`; observe `UIContentSizeCategory.didChangeNotification`.

### Layout

- **Flexible**: Allow text to wrap; avoid fixed heights.
- **Max scale**: Test at Accessibility sizes (e.g., XXL).
- **Images**: Ensure layout does not break when text grows.

---

## VoiceOver

### Labels

- All interactive elements: `accessibilityLabel`.
- **Buttons**: "Complete quest" not "Button."
- **Images**: Descriptive or `accessibilityHidden = true` if decorative.

### Focus

- Logical order; ensure Complete and Block are reachable.
- **Custom controls**: Implement `accessibilityActivate` if needed.

### Announcements

- Use `UIAccessibility.post(notification: .announcement, argument: "Quest completed")` for completion feedback.
- Brief; avoid long strings.

---

## Reduce Motion

### Detection

- `UIAccessivity.isReduceMotionEnabled`.
- **SwiftUI**: `@Environment(\.accessibilityReduceMotion)`.

### Behavior

- Disable or shorten animations.
- Skip parallax and decorative motion.
- Transitions: instant or minimal.

---

## Keychain and Secure Storage

### Migration

- If changing keychain structure, migrate existing keys on first launch.
- Use version or migration flag to avoid duplicate keys.

### Multi-Device

- Keychain can sync via iCloud (optional); document if tokens sync across devices.
- **Security**: Prefer `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` for tokens; no sync.

### Failure

- **Add**: May fail (e.g., keychain locked); handle error; retry or prompt.
- **Read**: May return errSecItemNotFound; treat as logged out; redirect to login.

---

## API Response Handling

### GET /quests/current returns 204

- **Scenario**: No eligible quest (all blocked, none pending, or dependencies unsatisfied).
- **Behavior**: 204 No Content with empty body → set currentQuest = nil; show empty state: "No quest ready. Unblock a quest or add a new one."

### Delete quest returns 409 (QUEST_HAS_CHILDREN)

- **Scenario**: User deletes quest that has dependent quests (parent_id references it).
- **Behavior**: Show alert: "Delete or reparent dependent quests first." Navigate to quest list or children.

---

## Network Edge Cases

### No network on launch

- **Behavior**: Load from local store; show cached data.
- **Indicator**: Subtle offline icon in nav bar or status area.
- **Sync**: Queue mutations; sync when reachable.

### Reachability

- Use `NWPathMonitor` or `Network` framework.
- **Caveat**: Do not block UI on reachability; attempt request and handle failure.
- **Optimization**: Skip sync attempt when path is unsatisfied; save battery.

### SSL / Certificate errors

- **Never**: Disable certificate validation for production.
- **Handle**: Show user-friendly error; log for debugging.
