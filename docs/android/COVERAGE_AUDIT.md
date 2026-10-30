# Android Coverage Audit

Audit of Android documentation against the product plan, backend API, and UI implementation requirements. Ensures nothing is missing from a planning perspective and that a fully implemented Android app would have complete spec coverage.

---

## 1. Product Vision → Android Coverage

| Product Requirement | Android Coverage | Status |
|---------------------|------------------|--------|
| Single-objective HUD | ARCHITECTURE: quest HUD; UI complete/block/swipe | Partial: skip not documented |
| Algorithmic funnel | Backend chooses; app displays | Covered |
| IO-multiplexing | blockQuest, unblockQuest in queue | Partial: block UI, blocked list not specified |
| Block with trigger (manual/time/location) | — | Gap: Block bottom sheet/dialog not documented |
| Unblock flow | — | Gap: Where to view/unblock blocked quests |
| Complete, block, skip gestures | ARCHITECTURE: complete, block | Gap: Skip not documented |
| "Why this quest?" (reasons) | — | Gap: No spec for reasons display |
| Sims-style rewards | Sim view | Covered |
| Companion type at setup | — | Gap: Register screen, companion selection |
| Habits → currency → Sim | HabitList | Covered |
| Widget: "X quests done today" | PLATFORM: Lock screen widget content | Gap: GET /stats/today; widget data source |
| Streaks (additive) | Widget, Sim | Covered |
| Offline-first | Room, queue, WorkManager | Covered |
| Haptics | PLATFORM: complete, block | Partial: skip haptic not specified |
| Accessibility | EDGE_CASES: TalkBack, font scaling | Covered |
| Context match (focus/quick/low_energy) | — | Gap: Context selector not documented |
| Delete quest/habit | — | Gap: No delete UI specified |

---

## 2. Backend API → Android UI Mapping

| Endpoint | Android Screen/Component | Current Doc | Status |
|----------|-------------------------|-------------|--------|
| GET /quests/current | Quest HUD | ARCHITECTURE | Partial: context, 204, reasons |
| POST /quests/:id/complete | Quest HUD Complete | ARCHITECTURE | Covered |
| POST /quests/:id/block | Block action → sheet | — | Gap |
| POST /quests/:id/unblock | Blocked list item | — | Gap |
| GET /quests | Quest list; Blocked filter | — | Gap |
| POST /quests | Add quest form | — | Gap |
| PATCH /quests/:id | Edit quest form | — | Gap |
| DELETE /quests/:id | — | — | Gap |
| GET /habits | HabitList | ARCHITECTURE | Covered |
| POST /habits/:id/complete | Habit tap | ARCHITECTURE | Covered |
| POST /habits | Add habit form | — | Gap |
| PATCH /habits/:id | Edit habit | — | Gap |
| DELETE /habits/:id | — | — | Gap |
| GET /sim/state | SimView; Widget | ARCHITECTURE | Covered |
| PATCH /sim/state | Companion change | — | Gap |
| GET /stats/today | Widget; HUD stats | — | Gap: widget needs this |
| GET /streaks | HUD; Widget | ARCHITECTURE | Covered |
| POST /auth/register | Register | — | Gap: companion_type |
| Deep links | lifepath://quest, habits, sim | ARCHITECTURE | Partial: add blocked, new |

---

## 3. Android Docs Cross-Check

### ARCHITECTURE.md

| Section | Covers | Gaps |
|---------|--------|------|
| UI Layer | complete, block, swipe | Skip; block sheet; blocked list |
| Offline Queue | write to DB, enqueue | deleteQuest, deleteHabit; Idempotency-Key; skipQuest |
| Widget | Current quest, streak, Sim stage | "X quests done today" requires GET /stats/today |
| Navigation | Quest HUD, Habits, Sim, Settings | Blocked list; Add quest/habit; Edit forms |

### PLATFORM_SPECIFICS.md

| Section | Covers | Gaps |
|---------|--------|------|
| Widget | Update strategy, size | Data: quests_completed_today from API |
| Haptics | Complete, block | Skip gesture haptic |

### EDGE_CASES.md

| Section | Covers | Gaps |
|---------|--------|------|
| Sync | Idempotency, queue | Idempotency-Key header explicitly |
| Network | Offline, retry | 204 from GET /quests/current |
| — | — | 409 delete quest with children |

---

## 4. Gaps and Recommendations

### 4.1 Skip Gesture (High)

**Gap:** Product: "complete, block, skip." ARCHITECTURE only complete/block.

**Recommendation:** Skip = block with `unblock_trigger: manual`, `reason: "Skipped for now"`. Add third action: Complete (primary FAB), Block (secondary), Skip (tertiary or swipe). One-tap each. Light haptic for skip.

**Doc change:** ARCHITECTURE, PLATFORM_SPECIFICS (haptics).

---

### 4.2 Block Bottom Sheet / Dialog (High)

**Gap:** Block requires unblock_trigger, unblock_at, unblock_location, reason. No UI spec.

**Recommendation:** BlockQuestBottomSheet or Dialog: unblock_trigger (ChipGroup: Manual | Time | Location); if Time → DatePicker/TimePicker for unblock_at; if Location → TextInput for unblock_location; reason optional. Submit → POST /quests/:id/block.

**Doc change:** ARCHITECTURE.

---

### 4.3 Blocked Quests View (High)

**Gap:** User must unblock. Where to see blocked list?

**Recommendation:** "Waiting" tab in bottom nav, or filter in quest list. GET /quests?status=blocked. List items: tap to unblock (POST /quests/:id/unblock). Deep link: lifepath://quests?status=blocked.

**Doc change:** ARCHITECTURE navigation.

---

### 4.4 Widget: "X quests done today" (High)

**Gap:** Lock screen widget shows "X quests done today." Backend has GET /stats/today. Widget reads from DB; app must fetch and store quests_completed_today.

**Recommendation:** Sync flow: Fetch GET /stats/today; store in Room (e.g., DailyStats entity or cached in prefs); widget reads from same. Trigger widget update after sync. PLATFORM_SPECIFICS: widget data schema.

**Doc change:** ARCHITECTURE (Repository fetches stats); PLATFORM_SPECIFICS (widget content).

---

### 4.5 Context Selector (Medium)

**Gap:** GET /quests/current accepts ?context=focus|quick|low_energy. No spec.

**Recommendation:** Segmented control or chips above quest card. Persist in session. Pass to API.

**Doc change:** ARCHITECTURE.

---

### 4.6 "Why This Quest?" Reasons (Medium)

**Gap:** API returns reasons. No display spec.

**Recommendation:** Expandable section or info icon; map reason codes to strings (high_priority → "High priority").

**Doc change:** ARCHITECTURE.

---

### 4.7 Companion Type at Register (Medium)

**Gap:** Backend accepts companion_type. Register screen needs selection.

**Recommendation:** After email/password/timezone: "Choose companion" — Plant or Creature with visual. Default plant.

**Doc change:** ARCHITECTURE.

---

### 4.8 Delete Quest / Habit UI (Medium)

**Gap:** DELETE endpoints exist. No UI.

**Recommendation:** Quest: long-press or overflow → Delete; confirm dialog; handle 409. Habit: overflow → Delete; confirm.

**Doc change:** ARCHITECTURE; EDGE_CASES (409).

---

### 4.9 Quest / Habit Create and Edit Forms (Medium)

**Gap:** POST/PATCH fields not specified. No form screens.

**Recommendation:** CreateQuestScreen: title, notes, priority_level (P0–P4), time_of_day, context_tag. CreateHabitScreen: title, currency_per_completion, is_streak_tracked. Edit screens prefilled; delete option. Navigation: Add FAB or empty-state CTA.

**Doc change:** ARCHITECTURE.

---

### 4.10 Idempotency-Key (Low)

**Gap:** Offline queue replay should send Idempotency-Key per mutation.

**Recommendation:** Generate UUID when queuing; attach header on sync. Prevents duplicate credit on retry.

**Doc change:** ARCHITECTURE, EDGE_CASES.

---

### 4.11 GET /quests/current 204 (Low)

**Gap:** 204 No Content when no eligible quest. Client handling not explicit.

**Recommendation:** 204 → currentQuest = null; show empty state.

**Doc change:** EDGE_CASES.

---

## 5. Coverage Summary

| Category | Covered | Gaps |
|----------|---------|------|
| Core quest HUD | 85% | Skip, reasons, context, block sheet |
| Block/unblock | 50% | Block UI, blocked list |
| Widget | 80% | Daily stats data source |
| Habits | 80% | Create/edit/delete |
| Sim | 90% | Companion change |
| Auth | 75% | Register + companion |
| Offline | 90% | Idempotency-Key |
| Forms | 30% | Full specs |

---

## 6. Action Items

**Before implementation:**

1. Add skip gesture to quest HUD.
2. Add BlockQuestBottomSheet spec.
3. Add Blocked/Waiting view and navigation.
4. Add GET /stats/today to sync; widget reads from DB.
5. Add context selector.
6. Add reasons display.
7. Add companion_type to register.
8. Add delete UI (quest, habit).
9. Add Create/EditQuest and Create/EditHabit screen specs.
10. Add Idempotency-Key to sync queue.
11. Document 204 handling, 409 handling.

---

## 7. Audit Completion Status

Doc updates implemented:

| Doc | Changes |
|-----|---------|
| ARCHITECTURE.md | Skip gesture; BlockQuestBottomSheet; Blocked list; context selector; reasons; daily stats; Idempotency-Key; queue mutations; Create/Edit screens; Register + companion; API 204, context |
| PLATFORM_SPECIFICS.md | Skip haptic; widget data from GET /stats/today |
| EDGE_CASES.md | Idempotency-Key; 204 handling; 409 delete |
