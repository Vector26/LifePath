# LifePath Android Edge Cases

Edge cases and failure handling for the LifePath Android application.

---

## App Killed During Sync

### Scenario

User backgrounds app; system kills process while sync is in progress.

### Mitigation

- **Atomic writes**: Write to DB in transactions; avoid partial state.
- **Queue persistence**: Sync queue in Room; survives process death.
- **On next launch**: Load queue from DB; resume sync.
- **Idempotency**: Client sends `Idempotency-Key: <uuid>` per queued mutation on replay (generate UUID when queuing). Server handles duplicate completes; client retries safely.

### Data Integrity

- **Never**: Delete local data without successful API confirmation for critical mutations.
- **Rollback**: If sync fails after local write, mark item "sync failed"; retry later. Do not revert optimistic UI state aggressively (user may have left screen).

---

## Orientation and Configuration Changes

### ViewModel retention

- ViewModel survives configuration change by default.
- Avoid storing heavy or non-serializable data in ViewModel.

### Process death

- **Scenario**: System kills app under memory pressure; user returns later.
- **Behavior**: Activity recreated; ViewModel recreated; Repository loads from DB.
- **State restore**: No special handling if state is in DB. For in-memory state (e.g., scroll position), use SavedStateHandle.

### Fragment recreation

- Use ViewModel for Fragment state; avoid putExtra for complex data.

---

## Low Storage

### Scenario

Device storage nearly full; DB write or download fails.

### Mitigation

- **Catch**: IO exceptions on DB or file operations.
- **UI**: "Storage full. Free up space and try again." Do not crash.
- **Reduce**: Limit local cache size; prune old data if needed.
- **Avoid**: Aggressive preloading or large downloads.

---

## Background Execution Limits

### Android 8+ background limits

- **Background service**: Restricted; use WorkManager for deferred work.
- **Foreground service**: Allowed with notification.
- **Exact alarms**: Need SCHEDULE_EXACT_ALARM permission (Android 12+); avoid unless required for reminders.

### WorkManager constraints

- Use `NetworkType.CONNECTED` for sync.
- Use `BatteryNotLowConstraint` (optional) to avoid sync when battery critical.
- Do not rely on exact timing; system may delay.

---

## Screen Densities and Sizes

### Density

- Use dp for dimensions; avoid px.
- Test on ldpi, mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi; provide drawables for key densities or use vector.

### Screen sizes

- **Phone**: 320dp–480dp width.
- **Tablet**: 600dp+.
- **Layout**: Use adaptable layouts (ConstraintLayout, Compose); avoid fixed widths.
- **Edge-to-edge**: Support display cutouts; use WindowInsets for padding.

---

## Accessibility

### TalkBack

- **Labels**: All tappable elements have contentDescription.
- **Focus order**: Logical; complete/block actions reachable.
- **Announcements**: Use LiveData or Snackbar for completion feedback; ensure announced.

### Font scaling

- Use sp for text; support large fonts (1.5x–2x).
- Test at 130%, 150%, 200% scale.
- Avoid fixed heights that clip text.

### Reduce motion

- Check `Settings.Global.ANIMATOR_DURATION_SCALE`.
- Shorten or disable animations when reduced.
- Respect system "Remove animations" if available.

---

## API Response Handling

### GET /quests/current returns 204

- **Scenario**: No eligible quest (all blocked, none pending, or dependencies unsatisfied).
- **Behavior**: 204 No Content with empty body → set currentQuest = null; show empty state: "No quest ready. Unblock a quest or add a new one."

### Delete quest returns 409 (QUEST_HAS_CHILDREN)

- **Scenario**: User deletes quest that has dependent quests (parent_id references it).
- **Behavior**: Show message: "Delete or reparent dependent quests first." Navigate to quest list or children.

---

## Network Edge Cases

### No network on launch

- **Behavior**: Load from DB; show cached data.
- **Indicator**: Subtle offline icon.
- **Sync**: Queue mutations; sync when connected.

### Network change mid-request

- **Behavior**: Request may fail; retry with exponential backoff.
- **UX**: "Connection lost. Retrying…" or silent retry.

### SSL / certificate errors

- **Never**: Bypass certificate validation for production.
- **Handle**: Show error; do not expose stack trace.

---

## Battery and Performance

### Battery drain

- **Sync**: Limit frequency; use WorkManager batching.
- **Wake locks**: Avoid; use WorkManager.
- **Location**: Do not use unless location-based unblock is implemented.

### ANR avoidance

- **Rule**: No heavy work on main thread.
- **DB**: Use Kotlin coroutines, RxJava, or LiveData with background executor.
- **Network**: Always on background thread.
