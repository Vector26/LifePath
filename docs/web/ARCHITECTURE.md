# LifePath Web Architecture

Architecture guidance for the LifePath web application. Tech-agnostic; applicable to SPA, SSR, or hybrid approaches.

---

## Core Principles

- **Phone as progress tracker**: Web is a secondary surface; minimal, glanceable UX.
- **Single-objective HUD**: One quest at a time; no dashboard of guilt.
- **Offline-capable**: Support basic usage when network is unavailable.
- **No AI**: Deterministic, fast; no LLM integration.

---

## Rendering Model

### SPA vs SSR

| Approach | Pros | Cons |
|----------|------|------|
| **SPA** | Rich interactivity; fast after load; simpler deploy | Slower first paint; SEO needs meta/pre-render |
| **SSR** | Better first paint; SEO | More complex; hydration cost |
| **Hybrid** | Best of both | Highest complexity |

**Recommendation**: SPA is sufficient for an authenticated productivity app. Use SSR only if landing/marketing pages need SEO.

---

## State Management

### Global State

| State | Description | Persistence |
|-------|-------------|-------------|
| Current quest | Single active objective from API | Memory; refetch on mount |
| User | Profile and settings | Memory + optional localStorage cache |
| Sim state | Currency, stage, milestone | Memory; refetch on mount |
| Streaks | Per-habit streak data | Memory; refetch on mount |
| Daily stats | quests_completed_today, habits_completed_today (GET /stats/today) | Memory; refetch on mount |
| Context | focus \| quick \| low_energy for algorithm | Session; passed to GET /quests/current |
| Auth | Access/refresh tokens | Secure storage (httpOnly cookie or memory only) |

### Offline Queue

When offline, queue mutations locally:

- `completeQuest(id)`
- `blockQuest(id, payload)` — includes unblock_trigger, unblock_at, unblock_location, reason
- `unblockQuest(id)`
- `skipQuest(id)` — implemented as blockQuest(id, { unblock_trigger: "manual", reason: "Skipped for now" })
- `completeHabit(id)`
- `createQuest(payload)`
- `createHabit(payload)`
- `deleteQuest(id)`
- `deleteHabit(id)`

**Sync on reconnect**: Process queue in FIFO order; attach `Idempotency-Key: <uuid>` per mutation to prevent duplicate processing on retry; handle conflicts (see EDGE_CASES.md).

**Storage**: IndexedDB or localStorage for queue; keep minimal.

### Sync Status

Track:

- `online: boolean` (navigator.onLine)
- `syncing: boolean` (actively pushing queue)
- `lastSyncAt: timestamp`
- `pendingCount: number` (items in queue)

Display subtle indicator (e.g., dot) when offline or syncing. No guilt-inducing alerts.

---

## Route Structure

| Route | Purpose |
|-------|---------|
| `/` | Home; redirect to current quest or onboarding |
| `/quest` | Single-quest HUD (primary view) |
| `/quests` | Quest list; filter by status (pending, blocked, completed). Blocked = "Waiting" view for unblock flow. |
| `/quests/new` | Create quest form |
| `/quests/:id/edit` | Edit quest form |
| `/habits` | Habit list; complete actions |
| `/habits/new` | Create habit form |
| `/habits/:id/edit` | Edit habit form |
| `/sim` | Sim companion view; progress to next milestone |
| `/settings` | Profile, notifications, theme, companion type change |
| `/login` | Auth (if not using separate auth app) |
| `/register` | Registration (includes companion_type selection) |

**Deep linking**: Support `/quest`, `/quests`, `/quests?status=blocked`, `/habits`, `/habits/:id`, `/sim` for direct navigation.

---

## Service Worker / Offline Strategy

### Caching

- **Static assets**: Cache-first for JS, CSS, fonts.
- **API responses**: Network-first with fallback; cache `GET /quests/current`, `GET /sim/state`, `GET /habits`, `GET /stats/today` for offline read.
- **Mutations**: Never cache; queue when offline.

### Sync

- Use Background Sync API (where supported) to push queue when back online.
- Fallback: Push queue on `online` event and on visibility change (user returns to tab).

### Offline UX

- Show cached current quest if available.
- Disable or queue mutations; show "Offline – will sync when connected."
- Avoid blocking the UI; allow navigation to cached views.

---

## API Client

- **Base URL**: Configurable (`/api/v1` or full host).
- **Auth**: Attach `Authorization: Bearer <token>` from auth store.
- **Refresh**: Intercept 401; call refresh endpoint; retry original request.
- **Retry**: Exponential backoff for 5xx and network errors; no retry for 4xx (except 429).
- **Timeout**: 30s for normal requests; 10s for critical paths.
- **GET /quests/current**: Pass `?context=focus|quick|low_energy` when user has set context. Handle 204 No Content → currentQuest = null (no eligible quest).
- **Idempotency**: For offline queue replay, generate UUID per mutation; send `Idempotency-Key: <uuid>` header to prevent duplicate processing.

---

## Component Structure (Conceptual)

```
App
  AuthProvider
    Router
      Layout
        Sidebar/Nav (minimal)
        Main
          Route: /quest        → QuestHUD (single quest + complete/block/skip)
          Route: /quests       → QuestList (optional; filter: pending/blocked/completed)
          Route: /quests/new   → CreateQuestForm
          Route: /quests/:id/edit → EditQuestForm
          Route: /habits       → HabitList
          Route: /habits/new   → CreateHabitForm
          Route: /habits/:id/edit → EditHabitForm
          Route: /sim          → SimView
          Route: /settings     → Settings
```

**QuestHUD**: Large, centered card; one quest; three gestures: Complete (primary), Block (secondary), Skip (tertiary). Skip = block with reason "Skipped for now." Display `reasons` from API as expandable "Why this quest?" Optional context selector (focus/quick/low_energy) passed to GET /quests/current. Stats row: streak, XP, daily counts (GET /stats/today). No backlog visible.

**BlockQuestModal/Drawer**: When user taps Block, show modal: unblock_trigger (manual | time | location), unblock_at (if time), unblock_location (if location), reason (optional). Submit → POST /quests/:id/block.

**Blocked Quests View**: Under /quests?status=blocked or dedicated "Waiting" section. List blocked quests; tap to unblock (POST /quests/:id/unblock). Needed because user explicitly blocked these; they must be able to unblock.

**QuestList**: Filter by status. Delete from overflow/long-press; confirm; handle 409 if quest has children.

**HabitList**: Complete on tap. Create/edit/delete via forms and overflow menu.

**CreateQuestForm / EditQuestForm**: title (required), notes, priority_level (P0–P4), time_of_day (morning | afternoon | evening | any), context_tag (focus | quick | low_energy). Edit prefills; delete button on edit (with 409 handling).

**CreateHabitForm / EditHabitForm**: title (required), currency_per_completion (default 10), is_streak_tracked (default true). Edit prefills; delete in overflow.

**SimView**: Companion visual; progress bar; next milestone. Optional: change companion type (PATCH /sim/state) in footer or Settings.

**Register**: Include companion_type selection (plant | creature) after email/password/timezone. Default plant.

---

## Performance

- **Lazy load**: Routes; Sim view can be lazy (secondary screen).
- **Debounce**: Search/filter if list views exist.
- **Virtualization**: Only if quest/habit lists exceed ~100 items.
- **No heavy animations**: 200–300ms transitions max; respect `prefers-reduced-motion`.
