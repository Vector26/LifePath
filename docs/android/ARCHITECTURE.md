# LifePath Android Architecture

Architecture guidance for the LifePath Android application. Tech-agnostic; applicable to Kotlin, Java, or cross-platform frameworks.

---

## Core Principles

- **Phone as progress tracker**: App is for glancing at progress, not heavy interaction.
- **Minimal UI**: Few elements; low cognitive load.
- **Offline-first**: Local data; sync when connected.
- **No guilt mechanics**: Calm, supportive UX.

---

## Layered Architecture

```
UI Layer (Activities/Fragments/Compose)
    ↓
ViewModel / State Holder
    ↓
Repository (single source of truth)
    ↓
Data Sources (Local DB, API)
```

### UI Layer

- Displays current quest, habits, Sim state.
- Handles user input: Complete (primary), Block (secondary), Skip (tertiary or swipe). Skip = block with `unblock_trigger: manual`, `reason: "Skipped for now"`.
- Displays "Why this quest?" reasons (expandable) when API provides them.
- Optional context selector (focus | quick | low_energy) above quest card; passed to GET /quests/current.
- Subscribes to ViewModel state; does not fetch directly.

### ViewModel / State

- Holds UI state: current quest, loading, error.
- Exposes one-time events (navigation, toasts).
- Survives configuration changes; does not hold Android context.

### Repository

- Abstracts data source (Room DB + API).
- Implements offline-first: read from DB; write to DB + queue for sync.
- Resolves conflicts per backend EDGE_CASES_AND_ERRORS.md.

### Data Sources

- **Local**: Room database for quests, habits, Sim state, sync queue, daily stats (quests_completed_today, habits_completed_today from GET /stats/today).
- **Remote**: REST API client (Retrofit or equivalent). GET /quests/current: pass `?context=focus|quick|low_energy` when user has set context; handle 204 No Content → currentQuest = null.
- **Sync**: WorkManager for background sync; optional Foreground Service for critical sync.

---

## Offline-First Flow

1. **Read**: Always read from local DB. DB is populated by initial sync and subsequent syncs.
2. **Write**: Write to local DB immediately (optimistic); enqueue for sync. Generate UUID per mutation for `Idempotency-Key` header on replay.
3. **Sync**: WorkManager periodic work or on network change; push queue to API in FIFO order; attach `Idempotency-Key: <uuid>` per request.
4. **Conflict**: Server wins for conflicts; update local DB; no user prompt unless data loss.

**Queue mutations**: completeQuest, blockQuest, unblockQuest, skipQuest (= blockQuest with "Skipped"), completeHabit, createQuest, createHabit, deleteQuest, deleteHabit.

---

## Widget Support

### Home Screen Widget

- **Content**: Current quest title; streak count; Sim stage.
- **Update**: On sync; max every 15 min (battery).
- **Tap**: Open app to quest or Sim view.
- **Size**: Small (1x1) to medium (2x2). Minimal; no clutter.

### Lock Screen Widget (Android 12+)

- **Content**: Streak or "X quests done today."
- **Data source**: App fetches GET /stats/today on sync; stores quests_completed_today in Room (e.g., DailyStats entity). Widget reads from same DB.
- **Constraints**: Limited update frequency; small size.

---

## Background Sync

### WorkManager

- **Periodic**: Every 15–60 min when idle (battery-aware).
- **One-time**: On network reconnect; on app foreground.
- **Constraints**: Network connected; battery not low (optional).

### Foreground Service

- **Use case**: Critical sync during active use (e.g., user completes quest; ensure sync before they leave).
- **UX**: Brief notification "Syncing…"; dismiss when done.
- **Avoid**: Long-running unless necessary.

### Doze and App Standby

- **Doze**: WorkManager uses doze-resistant scheduling where possible.
- **App Standby**: Sync may be delayed when app is in background long-term.
- **Mitigation**: Sync on next foreground; show "Last synced X ago" if stale.

---

## Navigation

- **Primary**: Single main screen (quest HUD); bottom nav or drawer for Habits, Sim, Settings.
- **Blocked/Waiting**: Tab or filter for blocked quests (GET /quests?status=blocked); tap to unblock.
- **Add/Edit**: Create quest (POST /quests), Create habit (POST /habits); Edit screens for PATCH; Delete via overflow/long-press with confirm; handle 409 if quest has children.
- **Back stack**: Simple; avoid deep nesting.
- **Deep links**: `lifepath://quest`, `lifepath://habits`, `lifepath://sim`, `lifepath://quests?status=blocked`.

## Screen and Component Specs

**Quest HUD**: Complete (primary FAB), Block (opens BlockQuestBottomSheet), Skip (one-tap; no sheet). Stats row: streak, XP, "X quests · Y habits today" (GET /stats/today).

**BlockQuestBottomSheet**: unblock_trigger (Manual | Time | Location); if Time → DatePicker/TimePicker for unblock_at; if Location → TextInput for unblock_location; reason (optional). Submit → POST /quests/:id/block.

**Blocked Quests List**: GET /quests?status=blocked; list items with tap-to-unblock (POST /quests/:id/unblock).

**CreateQuestScreen / EditQuestScreen**: title (required), notes, priority_level (P0–P4), time_of_day, context_tag. Edit prefills; Delete with confirm; handle 409 QUEST_HAS_CHILDREN.

**CreateHabitScreen / EditHabitScreen**: title (required), currency_per_completion (default 10), is_streak_tracked. Edit prefills; Delete in overflow.

**Register**: Include companion_type selection (Plant | Creature) after email/password/timezone. Default plant.

---

## Dependency Direction

- UI → ViewModel → Repository → API/DB
- No UI → API directly.
- No Repository → UI.

---

## Testing

- **Unit**: Repository, ViewModel, sync logic.
- **UI**: Compose previews or Espresso for critical flows.
- **Integration**: Mock API; verify offline queue and sync behavior.
