# iOS Coverage Audit

Audit of iOS documentation against the product plan, backend API, and UI implementation requirements. Ensures nothing is missing from a planning perspective and that a fully implemented iOS app would have complete spec coverage.

---

## 1. Product Vision → iOS Coverage

| Product Requirement | iOS Coverage | Status |
|---------------------|--------------|--------|
| Single-objective HUD | ARCHITECTURE: quest HUD; complete/block/swipe | Partial: skip not documented |
| Algorithmic funnel | Backend chooses; app displays | Covered |
| IO-multiplexing | blockQuest, unblockQuest in queue | Partial: block UI, blocked list not specified |
| Block with trigger (manual/time/location) | — | Gap: Block sheet not documented |
| Unblock flow | — | Gap: Where to view/unblock blocked quests |
| Complete, block, skip gestures | ARCHITECTURE: complete, block | Gap: Skip not documented |
| "Why this quest?" (reasons) | — | Gap: No spec for reasons display |
| Sims-style rewards | Sim view | Covered |
| Companion type at setup | — | Gap: Register screen, companion selection |
| Habits → currency → Sim | HabitList | Covered |
| Lock Screen Widget: "X quests done today" | PLATFORM: widget content | Gap: GET /stats/today; App Group data |
| Streaks (additive) | Widget | Covered |
| Offline-first | Core Data/SwiftData, queue | Covered |
| Haptics | PLATFORM: complete, block | Partial: skip haptic not specified |
| Accessibility | EDGE_CASES: VoiceOver, Dynamic Type | Covered |
| Context match | — | Gap: Context selector not documented |
| Delete quest/habit | — | Gap: No delete UI specified |

---

## 2. Backend API → iOS UI Mapping

| Endpoint | iOS Screen/Component | Current Doc | Status |
|----------|---------------------|-------------|--------|
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

## 3. iOS Docs Cross-Check

### ARCHITECTURE.md

| Section | Covers | Gaps |
|---------|--------|------|
| View Layer | complete, block, swipe | Skip; block sheet; blocked list |
| Offline Queue | write to store, enqueue | deleteQuest, deleteHabit; Idempotency-Key; skipQuest |
| Widget | Current quest, streak, Sim stage | "X quests done today" requires GET /stats/today |
| Navigation | Quest HUD, Habits, Sim | Blocked list; Add quest/habit; Edit forms |

### PLATFORM_SPECIFICS.md

| Section | Covers | Gaps |
|---------|--------|------|
| Widget | Update budget, data sharing | Data: quests_completed_today in App Group |
| Haptics | Complete, block | Skip gesture haptic |

### EDGE_CASES.md

| Section | Covers | Gaps |
|---------|--------|------|
| Sync | Queue persistence | Idempotency-Key header |
| Network | Offline | 204 from GET /quests/current |
| — | — | 409 delete quest with children |

---

## 4. Gaps and Recommendations

### 4.1 Skip Gesture (High)

**Gap:** Product: "complete, block, skip." ARCHITECTURE only complete/block.

**Recommendation:** Skip = block with `unblock_trigger: manual`, `reason: "Skipped for now"`. Third action: Complete (primary), Block (secondary), Skip (tertiary or swipe). Light haptic for skip.

**Doc change:** ARCHITECTURE, PLATFORM_SPECIFICS.

---

### 4.2 Block Sheet (High)

**Gap:** Block requires unblock_trigger, unblock_at, unblock_location, reason. No UI spec.

**Recommendation:** BlockQuestSheet (SwiftUI) or modal: Picker for trigger (Manual | Time | Location); if Time → DatePicker for unblock_at; if Location → TextField for unblock_location; reason optional. Submit → POST /quests/:id/block.

**Doc change:** ARCHITECTURE.

---

### 4.3 Blocked Quests View (High)

**Gap:** User must unblock. Where to see blocked list?

**Recommendation:** "Waiting" tab or filter in quest list. GET /quests?status=blocked. List: tap to unblock. Deep link: lifepath://quests?status=blocked.

**Doc change:** ARCHITECTURE.

---

### 4.4 Widget: "X quests done today" (High)

**Gap:** Lock screen widget shows "X quests done today." Backend: GET /stats/today. Widget reads from App Group.

**Recommendation:** App fetches GET /stats/today on sync; writes quests_completed_today to App Group shared store. Widget timeline provider reads; displays. Refresh timelines after sync.

**Doc change:** ARCHITECTURE; PLATFORM_SPECIFICS (widget data schema).

---

### 4.5 Context Selector (Medium)

**Gap:** GET /quests/current accepts ?context. No spec.

**Recommendation:** Picker or segmented control above quest card. Pass to API.

**Doc change:** ARCHITECTURE.

---

### 4.6 "Why This Quest?" (Medium)

**Gap:** API returns reasons. No display spec.

**Recommendation:** Disclosure group or info button; map codes to localized strings.

**Doc change:** ARCHITECTURE.

---

### 4.7 Companion Type at Register (Medium)

**Gap:** Backend accepts companion_type. Register needs selection.

**Recommendation:** After email/password/timezone: "Choose companion" — Plant or Creature. Default plant.

**Doc change:** ARCHITECTURE.

---

### 4.8 Delete Quest / Habit (Medium)

**Gap:** DELETE endpoints exist. No UI.

**Recommendation:** Swipe-to-delete or context menu → Delete; confirm alert; handle 409.

**Doc change:** ARCHITECTURE; EDGE_CASES.

---

### 4.9 Quest / Habit Create and Edit (Medium)

**Gap:** No form screen specs.

**Recommendation:** CreateQuestView: title, notes, priority, time_of_day, context_tag. CreateHabitView: title, currency, streak toggle. Edit prefilled; delete option. Navigation via Add button or empty state.

**Doc change:** ARCHITECTURE.

---

### 4.10 Idempotency-Key (Low)

**Gap:** Offline queue should send Idempotency-Key per mutation.

**Recommendation:** UUID per queued item; attach on sync replay.

**Doc change:** ARCHITECTURE, EDGE_CASES.

---

### 4.11 GET /quests/current 204 (Low)

**Gap:** 204 when no eligible quest. Client handling not explicit.

**Recommendation:** 204 → currentQuest = nil; show empty state.

**Doc change:** EDGE_CASES.

---

## 5. Coverage Summary

| Category | Covered | Gaps |
|----------|---------|------|
| Core quest HUD | 85% | Skip, reasons, context, block sheet |
| Block/unblock | 50% | Block UI, blocked list |
| Widget | 80% | Daily stats in App Group |
| Habits | 80% | Create/edit/delete |
| Sim | 90% | Companion change |
| Auth | 75% | Register + companion |
| Offline | 90% | Idempotency-Key |
| Forms | 30% | Full specs |

---

## 6. Action Items

**Before implementation:**

1. Add skip gesture to quest HUD.
2. Add BlockQuestSheet spec.
3. Add Blocked/Waiting view.
4. Add GET /stats/today to sync; write to App Group for widget.
5. Add context selector.
6. Add reasons display.
7. Add companion_type to register.
8. Add delete UI.
9. Add Create/Edit screens.
10. Add Idempotency-Key.
11. Document 204, 409 handling.

---

## 7. Audit Completion Status

Doc updates implemented:

| Doc | Changes |
|-----|---------|
| ARCHITECTURE.md | Skip gesture; BlockQuestSheet; Blocked list; context selector; reasons; daily stats; Idempotency-Key; queue mutations; Create/Edit views; Register + companion; API 204, context |
| PLATFORM_SPECIFICS.md | Skip haptic; widget data from GET /stats/today in App Group |
| EDGE_CASES.md | Idempotency-Key; 204 handling; 409 delete |
