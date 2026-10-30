# [Feature] LifePath Android App — Full Port with Notifications & Home-Screen Widgets

## Summary

Port the complete LifePath web application to a native Android app, including a push notification system and three home-screen widget sizes that surface metrics and the current objective.

## Scope

- **Full app port:** All web screens and features (auth, quests, habits, calendar, city game, settings)
- **Notification system:** Background delivery of productivity reminders, objective nudges, and habit prompts
- **Home-screen widgets:** Three sizes (small, medium, large) displaying metrics and the current objective only

## Context

LifePath is a gamified productivity app (React/Vite web) with:
- Backend API: Spring Boot, JWT auth, REST
- Features: quests, habits, streaks, calendar sync, city game (Phaser), objectives algorithm
- Existing API: `GET /objectives/current`, `GET /stats/today`, `GET /city/state` (coins/XP)

The Android app must replicate the web experience while adding native notifications and glanceable widgets.

---

## Acceptance Criteria

### 1. Full App Port

- [ ] Android app targets SDK 24+ (or project minimum)
- [ ] All routes/screens from web are present: Login, Register, Quest HUD, Quest List, Habits, Calendar, City, Settings
- [ ] Auth flow works (JWT storage, refresh, logout)
- [ ] API client uses same endpoints; configurable base URL
- [ ] City game (Phaser) runs in Android WebView or equivalent (or native re-implementation if feasible)
- [ ] Design system preserved: tap targets ≥44dp, themes (light/dark)
- [ ] **Visual parity:** The in-app UI must look exactly like the web app when viewed on a mobile phone. Same layout, spacing, typography, colors, and component placement as the responsive web viewport. Reference: load `web/` build in Chrome DevTools mobile view or on a physical phone; the Android app should be visually indistinguishable.

### 2. Notification System

- [ ] Push notifications configured (FCM or equivalent)
- [ ] Notification types:
  - **Objective reminder:** "Your current focus: [quest title]"
  - **Habit prompt:** "Don’t forget: [habit title]"
  - **Daily summary (optional):** "X quests, Y habits completed today"
- [ ] User can enable/disable notifications in Settings (mirror `notifications_enabled` from backend)
- [ ] Tapping a notification opens the app to the relevant screen (quest, habit, or home)

### 3. Home-Screen Widgets (3 Sizes)

All widgets must show:

- **Metrics:** `quests_completed_today`, `habits_completed_today`, and optionally coins/XP
- **Current objective only:** Title and type (quest / habit / calendar event) from `GET /objectives/current`

| Widget Size | Dimensions (dp) | Content |
|-------------|-----------------|---------|
| **Small**   | 2×2 (≈110×110)  | Current objective title (truncated) + one metric (e.g. "3/5 today") |
| **Medium**  | 4×2 (≈250×110)  | Current objective + today’s quest/habit counts + tap to open app |
| **Large**   | 4×4 (≈250×250)  | Current objective (full), today stats, recent streak or mini-summary, tap targets |

- [ ] Widgets use AppWidgetProvider; update on configurable interval (e.g. 30 min) or manual refresh
- [ ] Tapping widget opens app to Quest HUD (or deep link to objective)
- [ ] Widget respects system theme (light/dark) if supported

---

## Technical Notes

### Widget Data

- **Metrics:** `GET /stats/today` → `quests_completed_today`, `habits_completed_today`, `date`
- **Current objective:** `GET /objectives/current?context=focus` → `type`, `payload.title` (or equivalent), `reasons`
- Widgets need auth token; consider WorkManager + periodic sync or foreground service for token refresh

### Notification Channels

- Create channels: `objectives`, `habits`, `summary` (or similar)
- Allow user to customize per channel in app settings

### Visual Parity: Match Web on Mobile

To achieve "look exactly like the webview on mobile phone":

- **Option A (recommended for fastest parity):** Wrap the web app in a fullscreen WebView. Load the production or dev build URL; ensure viewport meta, `100dvh` / `100vw` work correctly; hide browser chrome. The app is then a native shell (nav, notifications, widgets) + WebView content. Visually identical by definition.
- **Option B (native UI):** Replicate the web design tokens in Android. Reference: `web/src/theme/theme.ts`, `web/src/index.css` (CSS variables for spacing, colors, tap targets). Use the same font (Inter), base unit (8px → 8dp), and component layouts. Compare against Chrome mobile viewport (e.g. 412×915 or 390×844) as the design target.

### Stack Recommendation

- **Kotlin + Jetpack Compose** (or XML views) for UI
- **Retrofit/OkHttp** for API
- **Hilt/Koin** for DI
- **Firebase Cloud Messaging** for push (or alternative)
- **Phaser:** Evaluate WebView with hardware acceleration for city game; fallback to simplified or placeholder if performance is poor

---

## Validation Points

- [ ] App installs and runs on Android device/emulator
- [ ] User can log in, complete a quest, and complete a habit
- [ ] City game loads and tap-to-move works
- [ ] Notifications are received when triggered (test payload)
- [ ] All three widget sizes render correctly and show current objective + metrics
- [ ] Tapping widget opens app
- [ ] Settings persist (notifications on/off, theme)

---

## Dependencies

- Backend must expose notification registration endpoint if server-triggered push is desired (optional for v1; can use local alarms for reminders)
- Existing docs: `docs/android/ARCHITECTURE.md`, `docs/android/PLATFORM_SPECIFICS.md`

---

## Out of Scope (This Ticket)

- iOS port
- Offline-first / sync queue (separate ticket)
- Widget editing/configuration UI (use default sizing only)

---

## Agent Onboarding: Information a New Developer Needs

### Repository Structure

```

├── backend/          # Spring Boot (Java 21), JWT auth, REST API
│   └── src/main/java/com/lifepath/
│       ├── controller/   # AuthController, QuestController, HabitController, CityController, etc.
│       ├── service/      # QuestService, HabitService, CityService, ObjectiveService
│       └── config/       # SecurityConfig (JWT, no CSRF, stateless)
├── web/              # React 19 + Vite + TypeScript (reference implementation)
│   └── src/
│       ├── api/          # client.ts, endpoints.ts, types.ts — API contract
│       ├── screens/      # Login, Register, QuestHUD, HabitList, CalendarToday, CityView, Settings
│       └── game/         # Phaser 3 city game (tap-to-move, shop, decor decay)
├── docs/
│   └── android/     # ARCHITECTURE.md, PLATFORM_SPECIFICS.md, EDGE_CASES.md, COVERAGE_AUDIT.md
└── openapi/
    └── lifepath-api.yaml # OpenAPI 3 spec (may be partial; web src is source of truth)
```

### API Base URL & Auth

- **Base path:** `/api/v1` (backend `context-path` in `application.yml`)
- **Auth:** JWT Bearer. All endpoints except `/auth/login`, `/auth/register`, `/auth/refresh`, `/calendar/callback` require `Authorization: Bearer <access_token>`.
- **Token refresh:** `POST /auth/refresh` with body `{ "refresh_token": "<refresh_token>" }`. Returns `{ "access_token", "refresh_token?" }`. On 401, retry request after refresh; if refresh fails, clear tokens and redirect to login.
- **Idempotency:** For `POST /quests/:id/complete`, `POST /quests/:id/block`, `POST /habits/:id/complete`, send `Idempotency-Key: <uuid>` header. Client generates UUID when queuing the mutation.

### Key Endpoints for Widgets & Notifications

| Endpoint | Method | Auth | Response (widget-relevant) |
|----------|--------|------|----------------------------|
| `/stats/today` | GET | Yes | `{ quests_completed_today, habits_completed_today, date }` |
| `/objectives/current?context=focus` | GET | Yes | `{ type: "quest"\|"habit"\|"calendar_event", payload: { title?, quest?, ... }, reasons }` or 204 No Content |
| `/users/me` | GET | Yes | `{ total_xp, level, ... }` |
| `/city/state` | GET | Yes | `{ coins, city_name, ... }` |

**Objective payload shape by type:**
- `quest`: `payload.quest.title`, `payload.quest.id`
- `habit`: `payload.title`, `payload.id`
- `calendar_event`: `payload.title`, `payload.start_time`, `payload.end_time`

### Web Screens to Port (Routes)

| Route | Component | Purpose |
|-------|-----------|---------|
| `/login` | Login | Email + password |
| `/register` | Register | Email, password, timezone |
| `/quest` | QuestHUD | Current objective, Complete/Block/Skip, context selector, stats |
| `/quests` | QuestList | List quests, filters |
| `/quests/new`, `/quests/:id/edit` | CreateQuest, EditQuest | CRUD forms |
| `/habits` | HabitList | List habits, tap to complete |
| `/habits/new`, `/habits/:id/edit` | CreateHabit, EditHabit | CRUD forms |
| `/calendar` | CalendarToday | Today’s calendar events |
| `/city` | CityView | Phaser city game (WebView or native) |
| `/settings` | Settings | User profile, notifications, theme |

### City Game (Phaser)

- **Location:** `web/src/game/` (launcher.ts, scenes/Main.tsx, sprites/Player.ts, utils/pathfinding.ts)
- **Tech:** Phaser 3 + phaser-jsx. Tilemap from `tuxemon-town.json`, tileset `tuxemon-sample-32px-extruded.png`.
- **Android:** Run in WebView with hardware acceleration, or evaluate Capacitor/Ionic for hybrid. Fallback: simplified or placeholder screen if performance is poor.

### Backend Run (Local)

```bash
cd backend && mvn spring-boot:run
```

- Default port: 8081. API at `http://localhost:8081/api/v1`.
- Requires PostgreSQL (or H2 for dev; check `application.yml` / `application-dev.yml`).

### Web Run (Local)

```bash
cd web && npm install && npm run dev
```

- Dev server proxies `/api` to `http://localhost:8081`.
- Default: `http://localhost:5173`.

### Android Docs to Read First

1. **`docs/android/ARCHITECTURE.md`** — Layered design, offline-first, WorkManager, widget data flow (DB → Widget).
2. **`docs/android/PLATFORM_SPECIFICS.md`** — SDK versions (min 24, target 34), permissions, Doze, widget constraints.
3. **`docs/android/EDGE_CASES.md`** — Sync failures, process death, idempotency.
4. **`docs/android/COVERAGE_AUDIT.md`** — Gaps between product plan and existing Android docs.

### Context Tag

- Objectives and quests use `context`: `focus` | `quick` | `low_energy`.
- Pass as query param: `GET /objectives/current?context=focus`.
- Web stores selection in sessionStorage; Android should persist in SharedPreferences or DB.

### Error Handling

- **401:** Refresh token; if refresh fails, logout.
- **404:** `NoSuchElementException`-style messages from backend.
- **409:** Conflict (e.g. `CELL_OCCUPIED`, `QUEST_HAS_CHILDREN`).
- **422:** Unprocessable (e.g. `INSUFFICIENT_COINS`, `QUEST_ALREADY_COMPLETED`).
- Error body shape: `{ message, code?, details? }`.

### User Settings (Backend)

- `GET /users/me/settings` → `{ notifications_enabled, haptic_feedback, theme }`
- Use `notifications_enabled` to gate notification delivery.
