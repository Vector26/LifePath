# Quest Specificity Engine — Priority Algorithm

Full implementation specification for the deterministic priority algorithm that selects the single "current objective" quest. Non-AI; rule-based; composable.

---

## Overview

The algorithm receives all quests for a user and returns the single quest that should be displayed as the current objective. Only **eligible** quests are scored; the highest-scoring eligible quest wins. Tie-breaking is deterministic.

---

## Design Rationale: Why These Formulas Exist

This section explains the reasoning behind each component—what psychological or design principles led to the formulas, and how the numbers were chosen.

### Why a Numeric Score at All?

We need to pick exactly **one** quest. That requires:
1. **Eligibility** — which quests are allowed to be chosen
2. **Ordering** — among eligible quests, which one wins

A numeric score provides clean ordering: highest wins. We can combine multiple factors into one comparable number.

### Base Scores (P0 = 1000, P1 = 400, P2 = 200, …)

**Why these numbers?**
- **P0 dominates.** 1000 is large enough that a P0 almost always wins, even with zero bonuses and full penalty.
- **Gaps between tiers** (600, 200, 100, 50) are larger than typical modifiers. So a P1 cannot accidentally beat a P0 due to recency or time-of-day.
- **Decreasing increments** — urgent tiers matter more, so we space them more.
- **Range of 50–1000** — leaves room for bonuses/penalties without overflow.

**Design intent:** "If something is critical (P0), it should almost always surface. Modifiers fine-tune choice *within* a tier, not override the tier."

### Recency Bonus: `max(0, 50 - days × 5)`

**Why it exists:** Recently added quests are top-of-mind; older ones may be stale or forgotten. We want "I just added this" to matter, but not override priority.

**Why this shape:** Linear decay is simple. `50` max keeps it smaller than base-score gaps. Factor `5` ≈ "loses 5 points per day" → ~10 days to zero.

**Design intent:** "A quest added yesterday vs 5 days ago should feel different. A 30-day-old quest shouldn't get a bonus."

### Staleness Penalty: `min(20, days × 2)`

**Why it exists:** A quest untouched for weeks might be forgotten, less urgent, or subconsciously avoided. We nudge away from it—without "punishing."

**Why this shape:** Cap at 20 keeps penalty mild. Factor 2 is gentler than recency decay—being old matters less than being new.

**Design intent:** "We deliberately avoid overdue penalty. Staleness is a soft hint, not punishment."

### Time-of-Day Bonus: +20 if Match

**Why it exists:** Humans have energy rhythms (morning deep work, afternoon meetings, evening low-energy). Aligning quests to current window increases likelihood of execution.

**Why +20:** Noticeable within a tier; not enough to override priority. Fixed because "matches time" is binary.

### Context Match Bonus: +25

**Why it exists:** User explicitly signals state ("I'm in focus mode" or "low energy"). Matching task difficulty to state reduces friction.

**Why +25:** Slightly more than time-of-day since it's an explicit user signal.

### Blocked-Return Bonus: +200

**Why it exists:** The quest was blocked *because* it mattered. User was waiting. We want to surface it immediately so it doesn't fall through the cracks.

**Why +200:** Strong enough that an unblocked P1 can beat other P1s. Not so large that it beats a fresh P0.

**24-hour window:** After a day, we treat it as a normal pending quest again.

### Why No Overdue Penalty

**Explicit design choice.** We never penalize "past due" or "missed deadline."

**Reasoning:** Guilt-inducing dashboards cause app abandonment. Users avoid the app when they need it most. Graceful degradation: the system stays supportive.

### Relative Magnitude of Factors

| Factor | Typical Range | Role |
|--------|---------------|------|
| Base (priority) | 50–1000 | Dominates; primary driver |
| Blocked-return | 0 or +200 | Strong; can shift ordering across tiers |
| Recency | 0–50 | Medium; breaks ties within tier |
| Context | 0 or +25 | Medium; breaks ties |
| Time-of-day | 0 or +20 | Medium; breaks ties |
| Staleness | 0 to −20 | Small; mild nudge away |

### Design Principles Summary

| Principle | Implementation |
|-----------|----------------|
| Priority dominates | Large base gaps (50–1000) |
| Freshness matters | Recency bonus (max 50, decays) |
| Fit to time/state matters | Time + context bonuses |
| Resurface blocked quests | Blocked-return bonus (+200) |
| Nudge away from neglect | Staleness penalty (small, capped) |
| No guilt | No overdue penalty |
| Explainable | Each component has a clear rationale |

---

## Table Legend and Column Guide

When reading scoring tables in examples, use this legend.

### Quest Attributes (Input Data)

| Column | Meaning |
|--------|---------|
| **Quest** | Name/description of the task |
| **Priority** | P0 (critical today) through P4 (backlog). Determines base score. |
| **Time-of-day** | When it's meant to be done: `morning` (6–11), `afternoon` (12–17), `evening` (18–22), `any` (any time) |
| **Context** | Energy/attention tag: `focus` (deep work), `quick` (short tasks), `low_energy` (easy tasks) |
| **Parent** | Dependency. Children are **ineligible** until parent is completed. |
| **Created** | When added. Drives recency bonus. |
| **Updated** | Last modified. Drives staleness penalty. |
| **Status** | `pending` (ready), `blocked` (waiting), `completed` (done) |

### Scoring Columns (How the Score Is Built)

| Column | Meaning | Formula / Rule |
|--------|---------|----------------|
| **Base** | Raw score from priority only | P0=1000, P1=400, P2=200, P3=100, P4=50 |
| **Recency** | Bonus for newer quests | `max(0, 50 - days_since_creation × 5)`; max +50 |
| **Blocked-return** | Bonus for just-unblocked | +200 if unblocked in last 24h |
| **Time** | Bonus for matching current time | +20 if current hour fits quest's time-of-day |
| **Context** | Bonus for matching user context | +25 if quest context matches user context |
| **Staleness** | Penalty for old untouched quests | `min(20, days_since_updated × 2)`; max −20 |
| **Total** | Final score | Base + all bonuses − penalties |

### Time-of-Day Windows

| Tag | Local Hour Range |
|-----|------------------|
| morning | 6:00–11:59 |
| afternoon | 12:00–17:59 |
| evening | 18:00–21:59 |
| any | Always +20 |

### Tie-Breaking Order

1. Higher base priority (P0 beats P1)
2. Longer blocked time (if either was recently unblocked)
3. Older created_at (FIFO)
4. UUID lexicographic (deterministic final tie-break)

---

## Human Examples: Three Flows

### Flow 1: Alex — Straightforward Day (No Blocking)

**Setup:** Alex, America/New_York, Monday 8:00 AM.

**Queue:**

| Quest | Priority | Time | Context | Created | Updated |
|-------|----------|------|---------|---------|---------|
| Submit tax form (today) | P0 | morning | focus | 2 days ago | 2 days ago |
| Review Q1 report | P1 | morning | focus | 5 days ago | 5 days ago |
| Call dentist | P1 | afternoon | any | 3 days ago | 3 days ago |
| Research vacation spots | P3 | evening | low_energy | 1 day ago | 1 day ago |
| Reorganize bookshelf | P4 | any | any | 7 days ago | 7 days ago |

All pending, no parents, no blocked → all eligible.

**Scoring at 8:00 AM (morning):**

| Quest | Base | Recency | Time | Staleness | Total |
|-------|------|---------|------|-----------|-------|
| Submit tax form | 1000 | 40 | 20 | 4 | **1056** |
| Review Q1 report | 400 | 25 | 20 | 10 | **435** |
| Call dentist | 400 | 35 | 0 | 6 | **429** |
| Research vacation | 100 | 45 | 0 | 2 | **143** |
| Reorganize bookshelf | 50 | 15 | 20 | 14 | **71** |

**Current quest:** Submit tax form (1056). **Reasons:** high_priority, recently_added, matches_morning.

**Alex's flow:**
- 8:15 AM — Completes tax form. Next: Review Q1 report (435).
- 10:30 AM — Completes Q1 report. Next: Call dentist (429).
- 1:00 PM — Completes Call dentist. Next: Research vacation (143).
- 6:00 PM — Completes Research vacation. Next: Reorganize bookshelf (71).
- 8:00 PM — Completes bookshelf. No quest ready.

---

### Flow 2: Sam — Day with Blocking (IO-Wait)

**Setup:** Sam, Europe/London, Tuesday 9:00 AM.

**Queue:**

| Quest | Priority | Time | Status |
|-------|----------|------|--------|
| Send project proposal | P0 | morning | pending |
| Approve budget | P1 | any | **blocked** |
| Update documentation | P2 | afternoon | pending |
| Order team lunch | P2 | morning | pending |

**Scoring at 9:00 AM:**

| Quest | Base | Recency | Time | Staleness | Total |
|-------|------|---------|------|-----------|-------|
| Send proposal | 1000 | 50 | 20 | 0 | **1070** |
| Order team lunch | 200 | 45 | 20 | 2 | **263** |
| Update documentation | 200 | 40 | 0 | 4 | **236** |

**Current quest:** Send project proposal.

**10:00 AM** — Sam blocks "Send proposal" (waiting on vendor quote). Algorithm re-runs without it.

**Next current:** Order team lunch (263) beats Update documentation (236).

**2:00 PM** — Sam unblocks "Send proposal." Blocked-return bonus (+200) applies.

| Quest | Base | Recency | Blocked-return | Time | Staleness | Total |
|-------|------|---------|----------------|------|-----------|-------|
| Send proposal | 1000 | 50 | **200** | 0 | 0 | **1250** |
| Update documentation | 200 | 40 | 0 | 20 | 4 | 256 |

**Current quest:** Send proposal (1250). **Reasons:** high_priority, just_unblocked.

---

### Flow 3: Jordan — Day with Dependencies (Parent–Child)

**Setup:** Jordan has a chain A → B → C. Only **leaf** quests (all parents completed) are eligible.

**Queue:**

| Quest | Priority | Parent |
|-------|----------|--------|
| A: Plan project phase 1 | P1 | — |
| B: Write phase 1 spec | P1 | A |
| C: Implement phase 1 | P1 | B |
| D: Buy groceries | P2 | — |
| E: Water plants | P3 | — |

**8:00 AM:** Only A, D, E are eligible (B and C have incomplete parents).

**Current:** A (Plan phase 1) wins.

**9:00 AM** — Jordan completes A. B becomes a leaf.

**Current:** B (Write spec) wins.

**11:00 AM** — Jordan completes B. C becomes a leaf.

**Current:** C (Implement phase 1) wins.

**2:00 PM** — Jordan completes C. Next: D, then E.

---

## Eligibility Rules

A quest is **eligible** only if all of the following hold:

| Rule | Check |
|------|-------|
| Status | `status = 'pending'` |
| Not blocked | No active BlockedQuest record (unblocked_at IS NULL) for this quest |
| Dependencies satisfied | All ancestors in the parent chain have `status = 'completed'` |

**Ineligible:** `completed`, `blocked`, or any parent is `pending` or `blocked`.

### Leaf Node Definition

A quest is a **leaf** (dependency-satisfied) if:

- `parent_id IS NULL` (root), OR
- Its parent has `status = 'completed'`, and recursively all ancestors are completed.

---

## Scoring Formula

```
FINAL_SCORE = BASE_SCORE + SUM(modifiers) - SUM(penalties)
```

All values are integers. Result is used for ordering only; absolute value is not exposed.

---

## Base Scores by Priority Level

| Priority | Base Score | Semantic |
|----------|------------|----------|
| P0 | 1000 | Critical / deadline today |
| P1 | 400 | High / this week |
| P2 | 200 | Normal / no deadline |
| P3 | 100 | Low / whenever |
| P4 | 50 | Backlog / someday |

---

## Modifiers

### 1. Recency Bonus

- **Purpose:** Favor recently added quests (salience).
- **Formula:** `recency_bonus = max(0, 50 - (days_since_creation * 5))`
- **Range:** 0 to 50
- **days_since_creation:** Floor of (now - quest.created_at) in days

### 2. Blocked-Return Bonus

- **Purpose:** Prioritize quests that were just unblocked (returning from IO-wait).
- **Condition:** Quest was blocked and unblocked within last 24 hours (unblocked_at within 24h of now).
- **Value:** 200
- **Otherwise:** 0

### 3. Streak Alignment Bonus

- **Purpose:** Slight boost for quests that support an active streak (e.g., daily habit).
- **Condition:** Quest is linked to a streak-tracked habit that would extend or maintain current streak if completed. (Simplified: quest has `streak_habit_id` and that habit has active streak.)
- **Value:** 30
- **Otherwise:** 0
- **Note:** Requires habit-quest linkage; if not implemented, always 0.

### 4. Time-of-Day Affinity

- **Purpose:** Match quest to current time window.
- **Input:** User timezone, current UTC time → local hour. Quest `time_of_day`.
- **Windows:** morning = 6–11, afternoon = 12–17, evening = 18–22, any = always.
- **Condition:** Current local hour falls in quest's time_of_day window.
- **Value:** 20
- **Otherwise:** 0

### 5. Context Match

- **Purpose:** Match quest to user's current context (focus, quick, low-energy).
- **Input:** User context (from request or profile): `focus`, `quick`, `low_energy`, or null.
- **Condition:** Quest `context_tag` equals user context.
- **Value:** 25
- **Otherwise:** 0

---

## Penalties

### Staleness Penalty

- **Purpose:** Mild nudge away from very old, untouched quests.
- **Formula:** `staleness_penalty = min(20, floor(days_since_updated) * 2)`
- **Range:** 0 to 20
- **days_since_updated:** Floor of (now - quest.updated_at) in days

### No Overdue Penalty

- **Explicit:** There is NO penalty for overdue or "past due" quests. This supports graceful degradation and avoids guilt mechanics.

---

## Tie-Breaking

When two or more quests have the same FINAL_SCORE, apply in order:

1. **Higher base priority:** P0 > P1 > P2 > P3 > P4
2. **Longer blocked time:** Quest that was blocked longer (higher `blocked_duration` before unblock) wins. If neither was blocked, skip.
3. **Older created_at:** Earlier created wins (FIFO within same tier)
4. **UUID lexicographic:** Compare quest.id as strings; deterministic final tie-break

---

## Context Input

The algorithm receives:

| Input | Source | Description |
|-------|--------|-------------|
| user_id | Request | For querying quests |
| now | System | Current UTC timestamp |
| user_timezone | User | IANA timezone for time-of-day |
| user_context | Request (optional) | `focus`, `quick`, `low_energy`, or null |
| quests | DB | All quests for user (pending + blocked + completed for dependency check) |
| blocked_quests | DB | Active BlockedQuest records |
| habit_streaks | DB (optional) | For streak alignment |

---

## Output

- **Primary:** Single Quest or null (no eligible quests)
- **Optional:** `reasons` array for "Why this quest?" UI:

| Reason | Condition |
|--------|-----------|
| high_priority | priority_level is P0 or P1 |
| just_unblocked | Blocked-return bonus was applied |
| matches_morning / matches_afternoon / matches_evening | Time-of-day affinity applied |
| matches_context | Context match applied |
| supports_streak | Streak alignment applied |
| recently_added | Recency bonus > 0 |

---

## Pseudocode

```
function getCurrentQuest(userId, now, userTimezone, userContext):
    quests = loadQuestsForUser(userId)
    blockedQuests = loadActiveBlockedQuests(userId)
    
    eligible = []
    for q in quests:
        if q.status != 'pending': continue
        if isBlocked(q.id, blockedQuests): continue
        if not allAncestorsCompleted(q, quests): continue
        eligible.append(q)
    
    if eligible.isEmpty(): return null, []
    
    scores = []
    for q in eligible:
        base = BASE_SCORES[q.priority_level]
        mod_recency = computeRecencyBonus(q.created_at, now)
        mod_blocked_return = computeBlockedReturnBonus(q, blockedQuests, now)
        mod_streak = computeStreakBonus(q, userId)  // 0 if not implemented
        mod_time = computeTimeOfDayBonus(q.time_of_day, now, userTimezone)
        mod_context = computeContextBonus(q.context_tag, userContext)
        penalty_stale = computeStalenessPenalty(q.updated_at, now)
        
        score = base + mod_recency + mod_blocked_return + mod_streak + mod_time + mod_context - penalty_stale
        reasons = collectReasons(q, mod_recency, mod_blocked_return, mod_streak, mod_time, mod_context)
        scores.append((q, score, reasons))
    
    sort scores by (score DESC, base_priority DESC, blocked_duration DESC, created_at ASC, id ASC)
    winner = scores[0]
    return winner.quest, winner.reasons
```

### Dependency Check Pseudocode

```
function allAncestorsCompleted(quest, quests):
    current = quest
    while current.parent_id != null:
        parent = findQuest(current.parent_id, quests)
        if parent == null: return false
        if parent.status != 'completed': return false
        current = parent
    return true
```

### Cycle Detection (on Create/Update)

```
function wouldCreateCycle(questId, newParentId, quests):
    if newParentId == null: return false
    visited = {}
    node = newParentId
    while node != null:
        if node == questId: return true  // Cycle
        if node in visited: return true
        visited.add(node)
        q = findQuest(node, quests)
        node = q.parent_id
    return false
```

---

## Edge Cases

| Case | Behavior |
|------|----------|
| Empty eligible set | Return null, [] |
| All quests blocked | Return null, [] |
| All quests completed | Return null, [] |
| Circular dependency | Reject at create/update (409); never reach algorithm |
| User timezone invalid | Fall back to UTC for time-of-day |
| user_context null | Context match bonus = 0 |

---

## Constants Reference

| Constant | Value |
|----------|-------|
| BASE_P0 | 1000 |
| BASE_P1 | 400 |
| BASE_P2 | 200 |
| BASE_P3 | 100 |
| BASE_P4 | 50 |
| RECENCY_BONUS_MAX | 50 |
| RECENCY_DAYS_FACTOR | 5 |
| BLOCKED_RETURN_BONUS | 200 |
| BLOCKED_RETURN_WINDOW_HOURS | 24 |
| STREAK_BONUS | 30 |
| TIME_AFFINITY_BONUS | 20 |
| CONTEXT_MATCH_BONUS | 25 |
| STALENESS_PENALTY_MAX | 20 |
| STALENESS_DAYS_FACTOR | 2 |

---

## Extensibility

New rules can be added as modifiers or penalties:

- Add a new modifier with a fixed value or formula.
- Ensure it is bounded to avoid overwhelming base score.
- Add corresponding reason string for "Why this quest?".
- No retraining or ML pipeline required.
