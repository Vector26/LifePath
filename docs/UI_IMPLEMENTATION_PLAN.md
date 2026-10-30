# LifePath UI Implementation Plan: Cross-Platform Unified Design

A single design system and screen specification for Web, Android, and iOS. The experience is identical across all platforms—same pages, same components, same flows.

---

## 1. Tech Stack Recommendation

| Platform | Technology | Rationale |
|----------|------------|-----------|
| **Web** | React (Vite) | Fast, ecosystem, SSR optional |
| **Android** | React Native / Expo | Shared logic and components with web via React Native Web |
| **iOS** | React Native / Expo | Same codebase as Android |

**Alternative:** Flutter—single codebase for all three, but different ecosystem from existing React-centric docs.

**Recommended:** **Expo (React Native)** with **React Native Web** for web. One component library (e.g., Tamagui, NativeWind, or custom) renders to web DOM and native views. Shared API client, state, and business logic.

---

## 2. Design Principles (All Platforms)

- **Single-objective HUD:** One quest at a time; no dashboard clutter
- **No guilt mechanics:** No overdue badges, no punishing visuals
- **Touch-first:** Large tap targets (min 44pt); works on phone, tablet, desktop
- **Offline-aware:** Subtle status; queue mutations; no blocking
- **Accessible:** WCAG 2.1 AA; `prefers-reduced-motion` respected
- **Dark/light theme:** System preference with manual override

---

## 3. Shared Component Library

Components used across pages. Implement once; reuse on web, Android, iOS.

| Component | Purpose |
|------------|---------|
| **AppLayout** | Shell: safe area, nav bar, content area |
| **NavBar** | Bottom nav (mobile) or sidebar (tablet/desktop): Quest, Habits, Sim, Settings |
| **Button** | Primary, secondary, tertiary variants; loading state |
| **Card** | Elevated surface; padding; optional press feedback |
| **Input** | Text, email, password; validation feedback |
| **Select** | Dropdown/picker for enums (priority, time, context, companion) |
| **Chip** | Inline tag (P0, P1, focus, morning) |
| **Avatar** | User or companion icon |
| **ProgressBar** | Linear; used for XP, Sim currency, milestone |
| **Toast** | Transient feedback (success, error); auto-dismiss |
| **Modal** | Centered overlay; backdrop tap to dismiss |
| **Sheet** | Bottom drawer (mobile); side panel (desktop) |
| **EmptyState** | Illustration + message + CTA when no data |
| **LoadingSpinner** | Full-screen or inline |
| **SyncIndicator** | Dot/badge: online, syncing, offline, pending count |

---

## 4. Pages / Screens (Count: 14)

### 4.1 Authentication (2)

#### **Login**
- **Route:** `/login`
- **Layout:** Centered form, max-width card (400px on web)
- **Content:**
  - Logo / app name
  - Email input
  - Password input
  - "Log in" primary button
  - "Create account" text link → Register
- **Behavior:** Submit → POST /auth/login; store tokens; redirect to Home
- **Validation:** Client-side email format, password non-empty; server errors as Toast

#### **Register**
- **Route:** `/register`
- **Layout:** Scrollable form, max-width card
- **Content:**
  - Email, password, confirm password
  - Display name (optional)
  - Timezone selector (IANA)
  - Companion type: plant | creature (radio or cards)
  - "Create account" primary button
  - "Already have an account? Log in" link
- **Behavior:** Submit → POST /auth/register; store tokens; redirect to Home
- **Validation:** Min 8 chars password; timezone required

---

### 4.2 Main App (12)

#### **Home / Current Quest**
- **Route:** `/` or `/quest`
- **Layout:** Full viewport; centered content; stats bar at top
- **Content:**
  - **Stats row:** XP badge (e.g., "Lv 2 • 150 XP"), today's completions ("3 quests, 2 habits"), streak summary
  - **Current quest card (large):**
    - Title (prominent)
    - Notes (collapsible)
    - Priority chip (P0–P4)
    - Optional "Why this quest?" expandable (reasons from API)
    - Context selector: focus | quick | low_energy (chips or dropdown)
  - **Actions:** Complete (primary), Block (secondary), Skip (tertiary)
  - **Empty state:** "No quest ready. Add quests or unblock waiting ones." + CTA to Quest List or Add Quest
- **Behavior:**
  - On mount: GET /quests/current?context={userContext}
  - Complete → POST /quests/:id/complete; show XP toast; refresh current
  - Block → open BlockQuestModal
  - Skip → POST /quests/:id/block with unblock_trigger=manual, reason="Skipped for now"; refresh
- **204 response:** Show empty state

#### **BlockQuestModal**
- **Type:** Modal (web/tablet) or Sheet (mobile)
- **Content:**
  - Unblock trigger: manual | time | location (radio/segmented)
  - If time: datetime picker (unblock_at)
  - If location: text input (unblock_location)
  - Reason (optional textarea)
  - "Block" primary button; "Cancel" secondary
- **Behavior:** Submit → POST /quests/:id/block; close; refresh current quest; show next_quest if returned

#### **Quest List**
- **Route:** `/quests`
- **Layout:** List; filter tabs or chips at top
- **Content:**
  - Filter: All | Pending | Blocked | Completed
  - List of quest cards (title, priority, status, time-of-day, parent indicator if any)
  - FAB or header button: "Add Quest"
  - Long-press / overflow: Edit, Delete
- **Behavior:**
  - GET /quests?status={filter}&limit=50
  - Delete → confirm → DELETE /quests/:id; handle 409 (has children)
  - Tap card → Edit Quest (or Blocked detail if blocked)
- **Empty state:** "No quests yet. Add your first quest."

#### **Create Quest**
- **Route:** `/quests/new`
- **Layout:** Form in card or full-screen form
- **Content:**
  - Title (required, 1–500 chars)
  - Notes (optional, max 5000)
  - Priority: P0 | P1 | P2 | P3 | P4 (required)
  - Parent quest (optional; dropdown of existing quests)
  - Time of day: morning | afternoon | evening | any
  - Context: focus | quick | low_energy (optional)
  - "Create" primary button
- **Behavior:** POST /quests; redirect to Home or Quest List; Toast success

#### **Edit Quest**
- **Route:** `/quests/:id/edit`
- **Layout:** Same as Create; prefill from GET /quests/:id
- **Content:** Same fields + "Delete" (destructive, in overflow or footer)
- **Behavior:** PATCH /quests/:id; DELETE with confirm; handle 409 on delete

#### **Blocked / Waiting**
- **Route:** `/quests?status=blocked` or `/waiting`
- **Layout:** Subset of Quest List; filtered to blocked only
- **Content:** List of blocked quests; each shows reason, unblock trigger, unblock_at/location
- **Behavior:** Tap "Unblock" → POST /quests/:id/unblock; remove from list; refresh current quest

#### **Habit List**
- **Route:** `/habits`
- **Layout:** List of habit cards
- **Content:**
  - Each card: title, currency per completion, streak (current/longest), "Complete" button or tap target
  - FAB or header: "Add Habit"
  - Long-press / overflow: Edit, Delete
- **Behavior:**
  - GET /habits
  - Complete → POST /habits/:id/complete; show currency + streak toast; update list
  - Idempotent: same-day second tap = no change, 200
- **Empty state:** "No habits. Add your first habit."

#### **Create Habit**
- **Route:** `/habits/new`
- **Layout:** Form
- **Content:**
  - Title (required, 1–200 chars)
  - Currency per completion (number, default 10)
  - Streak tracked (toggle, default on)
  - "Create" button
- **Behavior:** POST /habits; redirect to Habit List; Toast

#### **Edit Habit**
- **Route:** `/habits/:id/edit`
- **Layout:** Same as Create; prefill
- **Content:** Same + Delete in overflow
- **Behavior:** PATCH /habits/:id; DELETE with confirm

#### **Streaks**
- **Route:** `/streaks` or embedded in Habit List
- **Layout:** List or section
- **Content:**
  - Per habit: habit title, current_streak_days, longest_streak_days, last_completed_date
  - GET /streaks
- **Behavior:** Read-only; can be a section on Habits or Sim

#### **Sim / Companion**
- **Route:** `/sim`
- **Layout:** Companion visual (illustration or simple graphic); progress area
- **Content:**
  - Companion graphic (plant or creature)
  - total_currency, current_stage (starter | stable | thriving | flourishing)
  - Progress bar to next milestone
  - next_milestone: title, description, currency_threshold, currency_to_go
  - Milestone list (all; unlocked vs locked)
  - "Change companion" link or button → Settings or inline
- **Behavior:** GET /sim/state, GET /sim/milestones; PATCH /sim/state for companion_type in Settings

#### **Settings**
- **Route:** `/settings`
- **Layout:** Grouped list of options
- **Content:**
  - **Profile:** Display name, email (read-only), timezone
  - **Preferences:** notifications_enabled, haptic_feedback, theme (system | light | dark)
  - **Companion:** companion_type (plant | creature)
  - **Account:** Log out
- **Behavior:** GET /users/me, GET /users/me/settings; PATCH /users/me, PATCH /sim/state; POST /auth/logout

---

## 5. Navigation Structure

| Platform | Primary nav |
|----------|-------------|
| **Phone** | Bottom tab bar: Quest (Home), Habits, Sim, Settings |
| **Tablet** | Side nav or bottom bar (same 4 tabs) |
| **Web** | Sidebar (collapsible) or top nav with same 4 sections |

**Deep links:** Support `/quest`, `/quests`, `/quests?status=blocked`, `/habits`, `/habits/:id`, `/sim`, `/settings`, `/login`, `/register`

---

## 6. Component Count Summary

| Category | Count |
|----------|-------|
| **Layout** | AppLayout, NavBar |
| **Form elements** | Button, Input, Select, Chip |
| **Feedback** | Card, Toast, Modal, Sheet, LoadingSpinner, EmptyState, SyncIndicator |
| **Progress** | ProgressBar, Avatar |
| **Pages** | 14 (2 auth + 12 main) |
| **Modals/Sheets** | BlockQuestModal |
| **Reusable sections** | StatsRow, QuestCard, HabitCard, SimProgress |

**Total unique components (excluding pages):** ~18 shared + 4 section components.

---

## 7. Per-Page Component Breakdown

| Page | Components Used |
|------|-----------------|
| Login | AppLayout, Input×2, Button, Card |
| Register | AppLayout, Input×4, Select, Button, Card |
| Home | AppLayout, NavBar, StatsRow, QuestCard, Button×3, EmptyState, SyncIndicator |
| BlockQuestModal | Modal/Sheet, Select, Input, Button×2 |
| Quest List | AppLayout, NavBar, Chip (filter), QuestCard[], FAB, EmptyState |
| Create Quest | AppLayout, NavBar, Input×2, Select×3, Button |
| Edit Quest | Same + Delete |
| Habit List | AppLayout, NavBar, HabitCard[], FAB, EmptyState |
| Create Habit | AppLayout, NavBar, Input×2, Toggle, Button |
| Edit Habit | Same + Delete |
| Streaks | AppLayout, NavBar, HabitCard or StreakCard[] |
| Sim | AppLayout, NavBar, Avatar, ProgressBar, Card (milestones) |
| Settings | AppLayout, NavBar, Input, Toggle, Select, Button |

---

## 8. Data Flow (All Platforms)

1. **Auth:** Store access_token, refresh_token (secure storage). On 401, call POST /auth/refresh; retry.
2. **Current quest:** Fetch on Home mount; pass context from user selection.
3. **Lists:** Fetch on mount; refetch after mutation or pull-to-refresh.
4. **Offline:** Queue mutations; show SyncIndicator; replay with Idempotency-Key on reconnect.

---

## 9. Responsive Breakpoints

| Breakpoint | Width | Layout |
|------------|-------|--------|
| Phone | &lt; 600px | Single column; bottom nav; sheet modals |
| Tablet | 600–1024px | Two columns optional; bottom/side nav |
| Desktop | &gt; 1024px | Sidebar; max-width content; modal dialogs |

---

## 10. Implementation Order

1. **Design system:** Tokens (colors, typography, spacing), base components
2. **Auth:** Login, Register; token storage; protected routes
3. **Home:** Current quest, complete/block/skip; BlockQuestModal
4. **Quest List + Create + Edit**
5. **Habit List + Create + Edit**
6. **Sim**
7. **Streaks** (can embed in Habits)
8. **Settings**
9. **Offline queue + SyncIndicator**
10. **Polish:** Animations, accessibility, theme

---

## 11. File Structure (Conceptual)

```
src/
  components/       # Shared design system
    ui/             # Button, Input, Card, etc.
    layout/         # AppLayout, NavBar
    feedback/       # Toast, Modal, EmptyState
  screens/          # Page-level components
    auth/
    home/
    quests/
    habits/
    sim/
    settings/
  hooks/            # useAuth, useCurrentQuest, useOfflineQueue
  api/              # Client, endpoints
  theme/            # Tokens, dark/light
  navigation/       # Router config
```

---

*Document version: 1.0 | Aligned with LifePath backend API v1*
