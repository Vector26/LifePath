# Backend Coverage Audit

Audit of backend documentation against the product plan and UI requirements. Ensures nothing is missing from a planning perspective and that a fully implemented UI would have complete backend support.

---

## 1. Product Vision → Backend Coverage

| Product Requirement | Backend Coverage | Status |
|---------------------|------------------|--------|
| Single-objective HUD (one quest) | GET /quests/current | Covered |
| Algorithmic funnel (system chooses) | Priority Algorithm (PRIORITY_ALGORITHM.md) | Covered |
| IO-multiplexing (block/unblock) | POST /quests/:id/block, /unblock; IO_MULTIPLEXING_LOGIC.md | Covered |
| Time-based unblock | Phase 5 job; BlockedQuest.unblock_at | Covered |
| Location-based unblock | Documented as future; unblock_location field exists | Partial (future) |
| Manual unblock | POST /quests/:id/unblock | Covered |
| Priority algorithm (P0–P4, modifiers) | PRIORITY_ALGORITHM.md; full spec | Covered |
| "Why this quest?" | GET /quests/current returns `reasons` | Covered |
| Sims-style rewards | GET /sim/state, /sim/milestones; GAMIFICATION_ENGINE | Covered |
| Habits → currency → Sim stages | POST /habits/:id/complete; SimState; stages | Covered |
| XP on quest complete | POST /quests/:id/complete returns xp_earned | Covered |
| XP on block (queue management reward) | POST /quests/:id/block returns xp_earned | Covered |
| Streaks (additive, no punishment) | GET /streaks; GAMIFICATION_ENGINE | Covered |
| Companion type choice at setup | SimState.companion_type | Gap: no API to set |
| No overdue penalty | Design choice in algorithm | Covered |
| No dashboard of guilt | No overdue/guilt endpoints | Covered |
| Offline-capable | Sync metadata in DATA_MODELS | Gap: no sync protocol |
| Local-first | Optional sync fields | Gap: no batch sync API |

---

## 2. UI Requirements → Backend Coverage

### Web

| UI Need | Backend | Status |
|---------|---------|--------|
| Current quest (QuestHUD) | GET /quests/current | Covered |
| Complete quest | POST /quests/:id/complete | Covered |
| Block quest | POST /quests/:id/block | Covered |
| Unblock quest | POST /quests/:id/unblock | Covered |
| Create quest | POST /quests | Covered |
| Habit list | GET /habits | Covered |
| Complete habit | POST /habits/:id/complete | Covered |
| Create habit | POST /habits | Covered |
| Sim view (currency, stage, milestone) | GET /sim/state | Covered |
| Milestones list | GET /sim/milestones | Covered |
| Streaks | GET /streaks | Covered |
| User profile | GET /users/me | Covered |
| Settings | GET /users/me/settings | Covered |
| Auth | /auth/register, login, refresh, logout | Covered |
| Offline queue mutations | completeQuest, blockQuest, etc. | Covered (per-endpoint) |
| Skip gesture | — | Gap: not defined |

### Android / iOS (Widget, Same APIs)

| UI Need | Backend | Status |
|---------|---------|--------|
| Widget: current quest title | GET /quests/current | Covered |
| Widget: streak count | GET /streaks | Covered |
| Widget: Sim stage | GET /sim/state | Covered |
| Widget: "X quests done today" | — | Gap: no daily stats endpoint |
| Lock screen: streak | GET /streaks | Covered |
| Lock screen: "X quests done today" | — | Gap: no daily stats endpoint |

---

## 3. Gaps and Recommendations

### 3.1 Companion Type at Setup (High)

**Gap:** User chooses companion type (plant, creature) at setup. SimState.companion_type exists, but there is no API to set it.

**Recommendation:**
- Add `companion_type` to POST /auth/register (optional; default "plant").
- On register, create SimState with companion_type.
- Or: Add POST /sim/setup or PATCH /sim/state with `companion_type` for first-time setup. Lazy-create SimState on first GET /sim/state if needed.
- Document: `companion_type` enum values: `plant`, `creature` (extensible).

**API addition:**
```
PATCH /sim/state
{ "companion_type": "plant" | "creature" }
```
Or include in register.

---

### 3.2 Daily Stats for Widget (Medium)

**Gap:** Widget displays "X quests done today." No endpoint returns this.

**Recommendation:**
- Add GET /stats/today or include in a dashboard response:
```json
{
  "quests_completed_today": 3,
  "habits_completed_today": 2
}
```
- Or: Add to GET /quests/current or a lightweight GET /dashboard/summary that returns current quest + today's counts. Keeps widget to one or two calls.

**Alternative:** Client derives from GET /quests?status=completed filtered by completed_at date in user TZ. Possible but less efficient than a dedicated counter or summary.

---

### 3.3 Skip Gesture (Medium)

**Gap:** Product says "complete, block, skip" — one gesture each. Block and complete exist; skip does not.

**Recommendation:**
- Define "skip" behavior:
  - Option A: Skip = block with `unblock_trigger: manual`, reason "Skipped for now." Same as block; user unblocks later.
  - Option B: New action "defer" — move quest to end of queue for today (complex; requires queue ordering).
  - Option C: Skip = do nothing; algorithm may surface a different quest next time if context changes (not reliable).
- **Recommended:** Treat skip as block (Option A). Document in API: "Skip can be implemented as block with reason 'Skipped'." No new endpoint.

---

### 3.4 Delete Quest / Habit (Medium)

**Gap:** No DELETE endpoint. Users may want to remove quests or habits.

**Recommendation:**
- Add DELETE /quests/:id (soft delete: set deleted_at; exclude from algorithm).
- Add DELETE /habits/:id (cascade or restrict: fail if habit_completions exist, or soft delete).
- Document in DATA_MODELS: soft delete with deleted_at.
- Edge case: Deleting a quest that is parent of others — reject or cascade; document in EDGE_CASES.

---

### 3.5 Offline Sync Protocol (Medium for Local-First)

**Gap:** Web and mobile docs assume offline queue and sync. No backend sync protocol is specified (batch, conflict resolution).

**Recommendation:**
- **Option A (simple):** Clients send queued mutations one-by-one on reconnect. Server processes each; last-write-wins. No batch endpoint.
- **Option B (batch):** Add POST /sync with array of mutations. Server applies in order; returns conflict results. More complex.
- Document chosen approach in a new SYNC_PROTOCOL.md or section in API_SPECIFICATION.
- Include: Idempotency-Key for retries; version/updated_at for conflict detection if needed.

---

### 3.6 Default Milestones Seed (Low)

**Gap:** IMPLEMENTATION_PLANS says "Seed sim_milestones with default milestones." No seed data is documented.

**Recommendation:**
- Add MILESTONES_SEED.md or section in GAMIFICATION_ENGINE with default rows:
  - starter: 0 "First step" / 50 "Getting started"
  - stable: 200 "First apartment" / 400 "Comfortable"
  - thriving: 500 "Thriving home" / 750 "Halfway there"
  - flourishing: 1000 "Flourishing"
- Or document in IMPLEMENTATION_PLANS Phase 4 as migration seed.

---

### 3.7 Weekly Summary (Low; Future)

**Gap:** Product vision mentions "Weekly summary" (quests done, streak maintained). No API.

**Recommendation:**
- Defer to post-MVP. Add GET /stats/weekly when needed:
```json
{
  "quests_completed": 12,
  "habits_completed": 7,
  "streak_maintained": true,
  "week_start": "2026-02-24"
}
```

---

### 3.8 GET /quests/current — 204 Response (Low)

**Gap:** When no eligible quest, response format should be explicit for UI.

**Recommendation:**
- Document: 204 No Content with empty body, or 200 with `{ "quest": null, "reasons": [] }`. Pick one; document in API_SPECIFICATION and EDGE_CASES.
- UI must handle both empty state and null quest.

---

### 3.9 Streak-Habit Link (Optional)

**Gap:** Priority algorithm has "streak alignment bonus" for quests linked to habits. Quest model has no streak_habit_id.

**Recommendation:**
- If implementing streak alignment: add optional `streak_habit_id` to Quest (FK to Habit). Default null; bonus = 0.
- If deferring: leave as-is; streak alignment = 0 until implemented.

---

### 3.10 PATCH /sim/state (Low)

**Gap:** User may want to change companion_type later. SimState has it; no PATCH.

**Recommendation:**
- Add PATCH /sim/state with optional `companion_type`. Used for setup and later preference changes. Read-only otherwise (currency, stage are system-managed).

---

## 4. Granular Additions for Implementation

### 4.1 API Specification Additions

| Addition | Purpose |
|----------|---------|
| companion_type in POST /auth/register (optional) | Set at signup; create SimState with it |
| PATCH /sim/state { companion_type } | Change companion type |
| GET /stats/today { quests_completed_today, habits_completed_today } | Widget daily stats |
| DELETE /quests/:id | Remove quest (soft delete) |
| DELETE /habits/:id | Remove habit |
| Explicit 204 vs 200 null for GET /quests/current | UI contract |

### 4.2 Data Model Additions

| Addition | Purpose |
|----------|---------|
| Quest.deleted_at (optional) | Soft delete |
| Habit.deleted_at (optional) | Soft delete |
| User or Stats cache: quests_completed_today, habits_completed_today | For widget; or compute on read |

### 4.3 New Documentation

| Document | Purpose |
|----------|---------|
| SYNC_PROTOCOL.md (optional) | Offline sync: mutation order, idempotency, conflicts |
| MILESTONES_SEED.md or section | Default sim_milestones rows for migration |

---

## 5. Coverage Summary

| Category | Covered | Gaps |
|----------|---------|------|
| Core quest flow | 100% | — |
| Block/unblock | 100% | Location future |
| Gamification (XP, streaks, Sim) | 95% | Companion setup, daily stats |
| Auth & user | 90% | — |
| Habits | 95% | Delete |
| Widget data | 80% | Daily stats |
| Offline/sync | 50% | Sync protocol |
| Lifecycle (create, update) | 90% | Delete |

---

## 6. Action Items for Full Coverage

**Before UI implementation:**

1. Add companion_type to register or PATCH /sim/state.
2. Add GET /stats/today (or equivalent) for widget.
3. Define skip = block; document.
4. Add DELETE /quests/:id, DELETE /habits/:id.
5. Document GET /quests/current empty-state response (204 vs 200 null).

**Can defer:**

- Sync protocol (if client does simple retry per mutation).
- Weekly summary.
- Location-based unblock.
- Streak-habit link (algorithm works without it).
- Default milestones seed (can be hardcoded in migration).

---

## 7. Audit Completion Status

This audit has been implemented in the following docs:

| Doc | Changes |
|-----|---------|
| API_SPECIFICATION.md | companion_type in register; PATCH /sim/state; GET /stats/today; DELETE /quests/:id, DELETE /habits/:id; 204 for empty current; skip-as-block note |
| DATA_MODELS.md | deleted_at on Quest and Habit |
| EDGE_CASES_AND_ERRORS.md | delete-quest-with-children; QUEST_NOT_BLOCKED, QUEST_HAS_CHILDREN codes |
