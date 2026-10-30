# Web Coverage Audit

Audit of web documentation against the product plan, backend API, and UI implementation requirements. Ensures nothing is missing from a planning perspective and that a fully implemented web UI would have complete spec coverage.

---

## 1. Product Vision → Web Coverage

| Product Requirement | Web Coverage | Status |
|---------------------|--------------|--------|
| Single-objective HUD (one quest) | ARCHITECTURE: QuestHUD; RESPONSIVE: single card | Covered |
| Algorithmic funnel | Backend chooses; web displays | Covered |
| IO-multiplexing (block/unblock) | ARCHITECTURE: blockQuest, unblockQuest in queue | Partial: block UI not specified |
| Block with trigger (manual/time/location) | — | Gap: Block modal/drawer not documented |
| Unblock flow | — | Gap: Where to view/unblock blocked quests |
| Complete, block, skip gestures | ARCHITECTURE: complete/block only | Gap: Skip not documented |
| "Why this quest?" (reasons) | — | Gap: No component spec for reasons display |
| Sims-style rewards | ARCHITECTURE: SimView; RESPONSIVE: Sim layout | Covered |
| Companion type at setup | — | Gap: Register form, companion selection |
| Habits → currency → Sim | HabitList, complete habit | Covered |
| XP on complete/block | — | Gap: XP display optional but not specified |
| Streaks (additive) | Streaks in state; SimView | Covered |
| Minimal UI, 40% white space | RESPONSIVE: layout, typography, palette | Covered |
| No guilt, calm colors | RESPONSIVE: color palette | Covered |
| Offline-capable | ARCHITECTURE: queue, Service Worker | Covered |
| Touch targets 44×44px | RESPONSIVE: Touch Targets | Covered |
| Reduced motion | RESPONSIVE, EDGE_CASES | Covered |
| Accessibility | RESPONSIVE: contrast, labels, skip link | Covered |
| Context match (focus/quick/low_energy) | — | Gap: Context selector not documented |
| Delete quest/habit | — | Gap: No delete UI specified |

---

## 2. Backend API → Web UI Mapping

| Endpoint | Web Screen/Component | Current Doc | Status |
|----------|----------------------|-------------|--------|
| GET /quests/current | QuestHUD | ARCHITECTURE | Partial: context param, 204, reasons not specified |
| POST /quests/:id/complete | QuestHUD Complete button | ARCHITECTURE | Covered |
| POST /quests/:id/block | Block action → modal/drawer | — | Gap: Block UI with unblock_trigger, unblock_at |
| POST /quests/:id/unblock | Blocked quest list item | — | Gap: Blocked list route/component |
| GET /quests | Quest list (optional); Blocked list | ARCHITECTURE | Partial: blocked filter usage not clear |
| POST /quests | Add quest form | — | Gap: Create form fields not specified |
| PATCH /quests/:id | Edit quest form | — | Gap: Edit UI, when shown |
| DELETE /quests/:id | — | — | Gap: Delete from where? |
| GET /habits | HabitList | ARCHITECTURE | Covered |
| POST /habits/:id/complete | HabitList item tap | ARCHITECTURE | Covered |
| POST /habits | Add habit form | — | Gap: Create form not specified |
| PATCH /habits/:id | Edit habit | — | Gap: Edit UI |
| DELETE /habits/:id | — | — | Gap: Delete UI |
| GET /sim/state | SimView | ARCHITECTURE | Covered |
| PATCH /sim/state | Companion type change | — | Gap: Where? Settings or Sim view |
| GET /sim/milestones | SimView | ARCHITECTURE | Covered |
| GET /streaks | QuestHUD stats; SimView | ARCHITECTURE | Covered |
| GET /stats/today | QuestHUD or header | — | Gap: Daily stats display |
| POST /auth/register | Register screen | Route only | Gap: companion_type in form |
| POST /auth/login | Login screen | Route only | Covered |
| GET /users/me | Profile, header | — | Implicit |
| PATCH /users/me | Settings profile | — | Implicit |
| GET /users/me/settings | Settings screen | — | Implicit |

---

## 3. Web Docs Cross-Check

### ARCHITECTURE.md

| Section | Covers | Gaps |
|---------|--------|------|
| Global State | Current quest, user, Sim, streaks, auth | Daily stats (GET /stats/today) |
| Offline Queue | complete, block, unblock, completeHabit, createQuest, createHabit | createHabit; deleteQuest, deleteHabit; Idempotency-Key |
| Routes | /, /quest, /quests, /habits, /sim, /settings, /login, /register | /quests/blocked or how to access blocked; add-quest, edit-quest |
| QuestHUD | Complete/block | Skip; reasons; block modal |
| SimView | Progress, milestone | Companion type change UI |

### RESPONSIVE_DESIGN.md

| Section | Covers | Gaps |
|---------|--------|------|
| Breakpoints | Mobile, tablet, desktop | — |
| Quest HUD layout | Card, buttons, stats | "Stats" = streak + XP + daily count? |
| Sim View | Layout, progress bar | — |
| Touch targets | 44×44 | — |
| Typography, colors | Full palette | — |
| Accessibility | Focus, contrast, labels | — |

### EDGE_CASES.md

| Section | Covers | Gaps |
|---------|--------|------|
| Offline queue, sync | Full | 204 from GET /quests/current; Idempotency-Key |
| Empty states | No quests, no habits, no current | Quest has children on delete (409) |
| Auth | Token expiry, logout other tab | — |
| Form validation | Inline, rate limit | — |

---

## 4. Gaps and Recommendations

### 4.1 Skip Gesture (High)

**Gap:** Product: "complete, block, skip" — one gesture each. Web ARCHITECTURE only mentions complete and block.

**Recommendation:** Document skip = block with `unblock_trigger: manual`, `reason: "Skipped for now"`. Add third action to QuestHUD: Complete (primary), Block (secondary), Skip (tertiary or swipe). One-tap each; no modal for skip.

**Doc change:** ARCHITECTURE.md — add skip to QuestHUD and offline queue.

---

### 4.2 Block Modal/Drawer (High)

**Gap:** Block requires `unblock_trigger` (manual/time/location), optional `unblock_at`, `unblock_location`. No UI spec.

**Recommendation:** Add BlockQuestModal/BlockQuestDrawer spec:
- Trigger: manual | time | location
- If time: datetime picker for unblock_at
- If location: text input for unblock_location (or future geo)
- Reason: optional text field
- Submit → POST /quests/:id/block

**Doc change:** New section in ARCHITECTURE or add COMPONENT_SPECS.md.

---

### 4.3 Blocked Quests View (High)

**Gap:** User must unblock quests. Where do they see the blocked list? Product says backlog hidden — but blocked = "waiting," different from backlog.

**Recommendation:** Add route or section for blocked quests:
- **Option A:** `/quests?status=blocked` — "Waiting" tab or filter in quest list.
- **Option B:** Dedicated `/blocked` or "Waiting" in nav — minimal list; tap to unblock.
- Product allows "hidden by default" for full backlog; blocked list should be accessible (user explicitly blocked them; they need to unblock).

**Doc change:** ARCHITECTURE routes — add /quests with status=blocked or /blocked. RESPONSIVE: Blocked list layout.

---

### 4.4 Context Selector (Medium)

**Gap:** GET /quests/current accepts `?context=focus|quick|low_energy`. Algorithm uses it. No web spec for setting context.

**Recommendation:** Add context selector: subtle control (segmented control, dropdown, or quick chips) on QuestHUD or in header. Default null. Persist in session or let user change per session.

**Doc change:** ARCHITECTURE — context state; RESPONSIVE — context selector placement.

---

### 4.5 "Why This Quest?" Reasons Display (Medium)

**Gap:** API returns `reasons` array. No component spec.

**Recommendation:** Add expandable "Why this quest?" section or tooltip on QuestHUD. Display reasons as human-readable labels (map `high_priority` → "High priority", `matches_morning` → "Good for morning").

**Doc change:** ARCHITECTURE — QuestHUD reasons; RESPONSIVE — typography for reasons.

---

### 4.6 Companion Type at Registration (Medium)

**Gap:** Backend accepts companion_type in register. No form spec.

**Recommendation:** Add to register flow: after email/password/timezone, show "Choose your companion" — Plant or Creature with visuals. Default plant. Persist in register payload.

**Doc change:** ARCHITECTURE — Register flow; optional COMPONENT_SPECS.

---

### 4.7 Daily Stats Display (Medium)

**Gap:** GET /stats/today returns quests_completed_today, habits_completed_today. Web could show "3 quests done today" on HUD or header.

**Recommendation:** Add to QuestHUD stats row or app header: "X quests · Y habits today" (muted, small). Fetch GET /stats/today with quest data or as separate call. Cache for session.

**Doc change:** ARCHITECTURE — stats in state; RESPONSIVE — stats in HUD layout.

---

### 4.8 Delete Quest / Habit UI (Medium)

**Gap:** Backend has DELETE. No web UI for remove.

**Recommendation:**
- **Quest:** Edit view or long-press on quest card → Delete. Confirm dialog. Handle 409 (has children).
- **Habit:** Habit list item → overflow menu → Delete. Confirm.

**Doc change:** ARCHITECTURE — delete actions; EDGE_CASES — 409 handling.

---

### 4.9 Quest Create/Edit Form (Medium)

**Gap:** POST/PATCH quest fields: title, notes, priority_level, time_of_day, context_tag, parent_id. No form spec.

**Recommendation:** Document CreateQuestForm/EditQuestForm:
- title (required), notes (optional textarea)
- priority_level: P0–P4 (required; default P2)
- time_of_day: morning | afternoon | evening | any (optional)
- context_tag: focus | quick | low_energy (optional)
- parent_id: optional for dependencies (future)
Route: /quests/new, /quests/:id/edit. Or modal from empty state / quest list.

**Doc change:** ARCHITECTURE or COMPONENT_SPECS.

---

### 4.10 Habit Create/Edit Form (Medium)

**Gap:** POST/PATCH habit: title, currency_per_completion, is_streak_tracked. No form spec.

**Recommendation:** CreateHabitForm: title (required), currency (default 10), streak toggle. Edit: same. Route: /habits/new or modal from habit list.

**Doc change:** ARCHITECTURE.

---

### 4.11 Companion Type Change (Low)

**Gap:** PATCH /sim/state for companion_type. Where to change after setup?

**Recommendation:** Settings screen or Sim view footer: "Change companion" → Plant/Creature picker → PATCH. Low priority; can defer.

**Doc change:** ARCHITECTURE — Settings or SimView.

---

### 4.12 GET /quests/current 204 Handling (Low)

**Gap:** Backend returns 204 when no eligible quest. Client must handle empty body.

**Recommendation:** API client: 204 → set currentQuest = null, show empty state. Document in EDGE_CASES.

**Doc change:** EDGE_CASES — API response handling.

---

### 4.13 XP Display (Low)

**Gap:** Complete/block return xp_earned. Optional celebration or subtle +N feedback.

**Recommendation:** Brief toast or inline "+10 XP" on complete. Or aggregate in header (level from GET /users/me). Defer to polish phase.

---

### 4.14 Idempotency-Key for Offline Queue (Low)

**Gap:** Backend supports Idempotency-Key. Offline queue retries should use it.

**Recommendation:** Generate UUID per queued mutation; attach as Idempotency-Key on replay. Prevents duplicate credit on retry.

**Doc change:** ARCHITECTURE — offline queue; EDGE_CASES — retry semantics.

---

## 5. Coverage Summary

| Category | Covered | Gaps |
|----------|---------|------|
| Core quest HUD | 90% | Skip, reasons, context, block modal |
| Block/unblock flow | 60% | Block UI, blocked list |
| Habits | 85% | Create/edit form, delete |
| Sim view | 90% | Companion change |
| Auth | 80% | Register + companion |
| Settings | 70% | Companion, profile edit |
| Offline | 90% | Idempotency-Key, 204 |
| Forms (quest, habit) | 40% | Full field specs |
| Lifecycle (CRUD) | 70% | Delete, edit UI |

---

## 6. Action Items for Full Web Coverage

**Before implementation:**

1. Add skip gesture to QuestHUD (document).
2. Add BlockQuestModal spec (unblock_trigger, unblock_at, unblock_location, reason).
3. Add Blocked quests view (route + component).
4. Add context selector (focus/quick/low_energy).
5. Add reasons display for "Why this quest?"
6. Add companion_type to register form.
7. Add daily stats (GET /stats/today) to HUD or header.
8. Add delete UI for quest and habit.
9. Add CreateQuestForm and CreateHabitForm field specs.
10. Add EditQuestForm, EditHabitForm (or reuse create with prefilled).

**Doc changes:**

- ARCHITECTURE.md: Skip; block modal; blocked view; context; reasons; register flow; stats; delete; form routes.
- RESPONSIVE_DESIGN.md: Block modal layout; context selector; stats in HUD; form layouts.
- EDGE_CASES.md: 204 handling; 409 on delete; Idempotency-Key.
- Optional: COMPONENT_SPECS.md for forms and modals.

---

## 7. Audit Completion Status

Doc updates have been implemented:

| Doc | Changes |
|-----|---------|
| ARCHITECTURE.md | Skip gesture; block modal; blocked view; context selector; reasons; daily stats; Idempotency-Key; delete in queue; form routes; CreateQuestForm, CreateHabitForm, EditQuestForm, EditHabitForm; register + companion_type; API client 204, context param |
| RESPONSIVE_DESIGN.md | QuestHUD: three actions, stats row, context selector, reasons; Block modal layout; Form layouts |
| EDGE_CASES.md | 204 handling; Idempotency-Key; 409 delete-quest-with-children |
