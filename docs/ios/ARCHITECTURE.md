# LifePath iOS Architecture

Architecture guidance for the LifePath iOS application. Tech-agnostic; applicable to Swift, SwiftUI, or UIKit.

---

## Core Principles

- **Phone as progress tracker**: App is for glancing at progress, not heavy interaction.
- **Minimal UI**: Few elements; low cognitive load.
- **Offline-first**: Local data; sync when connected.
- **No guilt mechanics**: Calm, supportive UX.

---

## Layered Architecture

```
View Layer (SwiftUI / UIKit)
    ↓
ViewModel / ObservableObject
    ↓
Repository (single source of truth)
    ↓
Data Sources (Core Data / SwiftData, API)
```

### View Layer

- Displays current quest, habits, Sim state.
- Handles user input: Complete (primary), Block (secondary), Skip (tertiary or swipe). Skip = block with `unblock_trigger: manual`, `reason: "Skipped for now"`.
- Displays "Why this quest?" reasons (expandable DisclosureGroup or info button) when API provides them.
- Optional context selector (focus | quick | low_energy) above quest card; passed to GET /quests/current.
- Subscribes to ViewModel/ObservableObject state; does not fetch directly.

### ViewModel / ObservableObject

- Holds UI state: current quest, loading, error.
- Exposes one-time events (navigation, alerts).
- Does not hold UIView/UIViewController reference.
- Survives view recreation (SwiftUI) or tied to lifecycle (UIKit).

### Repository

- Abstracts data source (Core Data/SwiftData + API).
- Implements offline-first: read from local store; write to local + queue for sync.
- Resolves conflicts per backend EDGE_CASES_AND_ERRORS.md.

### Data Sources

- **Local**: Core Data or SwiftData for quests, habits, Sim state, sync queue. App Group store for daily stats (quests_completed_today from GET /stats/today) shared with widget.
- **Remote**: URLSession or Alamofire for REST API. GET /quests/current: pass `?context=focus|quick|low_energy` when user has set context; handle 204 No Content → currentQuest = nil.
- **Sync**: BGAppRefreshTask or BGProcessingTask for background sync.

---

## Offline-First Flow

1. **Read**: Always read from local store. Store is populated by initial sync and subsequent syncs.
2. **Write**: Write to local store immediately (optimistic); enqueue for sync. Generate UUID per mutation for `Idempotency-Key` header on replay.
3. **Sync**: Background task or on app foreground; push queue to API in FIFO order; attach `Idempotency-Key: <uuid>` per request.
4. **Conflict**: Server wins for conflicts; update local store; no user prompt unless data loss.

**Queue mutations**: completeQuest, blockQuest, unblockQuest, skipQuest (= blockQuest with "Skipped"), completeHabit, createQuest, createHabit, deleteQuest, deleteHabit.

---

## Widget Support (WidgetKit)

### Home Screen Widget

- **Content**: Current quest title; streak count; Sim stage.
- **Size**: Small, Medium. Minimal; no clutter.
- **Update**: Timeline provider; system controls refresh frequency.
- **Tap**: Deep link to app (quest or Sim view).

### Lock Screen Widget (iOS 16+)

- **Content**: Streak or "X quests done today."
- **Data source**: App fetches GET /stats/today on sync; writes quests_completed_today to App Group shared store; widget timeline provider reads.
- **Constraints**: Limited size; system controls update budget.

### Widget Data

- **Source**: App Group shared container; app writes to shared store (quests, habits, Sim state, daily stats); widget reads.
- **Refresh**: Use `WidgetCenter.shared.reloadTimelines(ofKind:)` after sync or key user action.

---

## Background Tasks

### BGAppRefreshTask

- **Use**: Periodic sync when app is suspended.
- **Schedule**: `BGTaskScheduler`; system chooses execution time.
- **Duration**: Short (~30s); complete or request more time.

### BGProcessingTask

- **Use**: Heavier sync (e.g., full reconciliation).
- **Schedule**: Less frequent; runs when device is idle and charging.
- **Duration**: Minutes; use sparingly.

### App Launch Sync

- **Primary**: Sync on foreground; do not rely solely on background tasks.

---

## Navigation

- **SwiftUI**: NavigationStack; simple push/pop.
- **UIKit**: UINavigationController.
- **Primary**: Single main screen (quest HUD); tab bar or sidebar for Habits, Sim, Settings.
- **Blocked/Waiting**: Tab or filter for blocked quests (GET /quests?status=blocked); tap to unblock.
- **Add/Edit**: Create quest, Create habit; Edit screens for PATCH; Delete via swipe or context menu with confirm; handle 409 if quest has children.
- **Deep links**: `lifepath://quest`, `lifepath://habits`, `lifepath://sim`, `lifepath://quests?status=blocked`.

## Screen and Component Specs

**Quest HUD**: Complete (primary), Block (opens BlockQuestSheet), Skip (one-tap; no sheet). Stats row: streak, XP, "X quests · Y habits today" (GET /stats/today).

**BlockQuestSheet**: Picker for unblock_trigger (Manual | Time | Location); if Time → DatePicker for unblock_at; if Location → TextField for unblock_location; reason (optional). Submit → POST /quests/:id/block.

**Blocked Quests List**: GET /quests?status=blocked; list items with tap-to-unblock (POST /quests/:id/unblock).

**CreateQuestView / EditQuestView**: title (required), notes, priority_level (P0–P4), time_of_day, context_tag. Edit prefills; Delete with confirm; handle 409 QUEST_HAS_CHILDREN.

**CreateHabitView / EditHabitView**: title (required), currency_per_completion (default 10), is_streak_tracked. Edit prefills; Delete in context menu.

**Register**: Include companion_type selection (Plant | Creature) after email/password/timezone. Default plant.

---

## Dependency Direction

- View → ViewModel → Repository → API/Store
- No View → API directly.
- No Repository → View.

---

## State Management

- **SwiftUI**: @StateObject, @ObservedObject; @Published for reactive updates.
- **UIKit**: Combine or closure-based callbacks from ViewModel.
- **Single source**: Repository; ViewModel observes Repository.

---

## Testing

- **Unit**: Repository, ViewModel, sync logic.
- **UI**: SwiftUI Previews; XCTest UI tests for critical flows.
- **Integration**: Mock API; verify offline queue and sync behavior.
