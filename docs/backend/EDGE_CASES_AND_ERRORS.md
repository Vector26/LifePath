# LifePath Backend Edge Cases and Errors

Edge cases, failure modes, and error handling for the LifePath backend.

---

## Quest Lifecycle

### Complete already-completed quest

- **Scenario**: Client calls `POST /quests/:id/complete` for quest with `status=completed`.
- **Response**: 422 Unprocessable Entity
- **Payload**: `{ "error": { "code": "QUEST_ALREADY_COMPLETED", "message": "Quest has already been completed" } }`
- **Idempotency**: Consider treating as success (200) if completion timestamp matches; otherwise 422.

### Block quest with no next eligible

- **Scenario**: User blocks the only eligible quest; no other quest is ready (all blocked or have incomplete dependencies).
- **Response**: 200 OK. Quest is blocked; `GET /quests/current` returns 204 or empty.
- **Behavior**: Not an error. System correctly surfaces "no current objective." Client shows empty state.

### Delete quest with children

- **Scenario**: User calls `DELETE /quests/:id` for a quest that has children (other quests with `parent_id` referencing it).
- **Response**: 409 Conflict
- **Payload**: `{ "error": { "code": "QUEST_HAS_CHILDREN", "message": "Cannot delete a quest that has dependent quests; delete or reparent children first" } }`
- **Policy**: Reject delete until children are deleted or reparented (parent_id set to null or another quest).

### Circular dependencies

- **Scenario**: User sets `parent_id` such that A depends on B and B depends on A (or longer cycle).
- **Response**: 409 Conflict on `POST /quests` or `PATCH /quests/:id`
- **Payload**: `{ "error": { "code": "CIRCULAR_DEPENDENCY", "message": "This change would create a circular dependency" } }`
- **Prevention**: Validate dependency graph on create/update; reject if cycle detected.

### Block a completed quest

- **Response**: 422 Unprocessable Entity. Only `pending` quests can be blocked.

### Unblock a non-blocked quest

- **Response**: 422 Unprocessable Entity. Only `blocked` quests can be unblocked.

---

## Concurrency

### Two devices complete same quest

- **Scenario**: User marks quest complete on phone; tablet still shows it and completes seconds later.
- **Options**:
  1. **Last-write-wins**: Second complete is no-op; return 200 with existing completed state.
  2. **Optimistic locking**: Reject second complete with 409 if `version` or `updated_at` changed.
- **Recommendation**: Last-write-wins for simplicity; completion is idempotent.

### Two devices block same quest

- **Scenario**: Both devices issue block for the same quest.
- **Behavior**: Both succeed; idempotent. Single BlockedQuest record.

### Concurrent habit completion (same habit, same day)

- **Scenario**: User completes habit on two devices for same calendar day.
- **Behavior**: Only first completion counts for currency and streak. Second returns 200 but no additional currency; streak unchanged. Deduplicate by (habit_id, completed_date).

---

## Validation

### Empty title

- **Field**: `Quest.title`, `Habit.title`
- **Rule**: Min 1 character, max 500 (quest) or 200 (habit).
- **Response**: 400 Bad Request
- **Payload**: `{ "error": { "code": "VALIDATION_ERROR", "details": [{ "field": "title", "reason": "must be between 1 and 500 characters" }] } }`

### Invalid priority

- **Field**: `Quest.priority_level`
- **Rule**: Must be one of P0, P1, P2, P3, P4.
- **Response**: 400 Bad Request

### Invalid dependency (parent_id)

- **Scenario**: `parent_id` references non-existent quest or quest owned by another user.
- **Response**: 404 Not Found or 400 Bad Request with clear message.

### Invalid UUID in path

- **Response**: 400 Bad Request. Use consistent message for malformed IDs.

---

## Auth

### Expired token mid-request

- **Response**: 401 Unauthorized
- **Header**: `WWW-Authenticate: Bearer error="invalid_token", error_description="Token expired"`
- **Client**: Retry with refresh flow.

### Invalid refresh token

- **Scenario**: Expired, revoked, or tampered refresh token.
- **Response**: 401 Unauthorized
- **Client**: Redirect to login; clear stored tokens.

### Session hijack mitigation

- **Recommendation**: Bind refresh token to device fingerprint or IP (optional). Rotate refresh tokens on use. Revoke all sessions on password change.

### Token in wrong format

- **Scenario**: `Authorization: Bearer invalid` or missing `Bearer` prefix.
- **Response**: 401 Unauthorized

---

## Sim / Rewards

### Currency overflow

- **Field**: `SimState.total_currency`
- **Rule**: Use bounded integer (e.g., 64-bit); cap at max safe value if needed.
- **Edge case**: Large batch of completions. Process atomically; never decrease.

### Milestone calculation during partial sync

- **Scenario**: Offline completions synced out of order; milestone computed incorrectly.
- **Mitigation**: Recompute `current_stage` and milestone progress from `total_currency` on each update. Currency is source of truth.

### Negative currency

- **Prevention**: Never decrement. Only add on habit completion. Reject any negative delta.

---

## Streaks

### Timezone handling for "day"

- **Rule**: Use user's `timezone` (from User) for date boundary.
- **Edge case**: User changes timezone. Recompute streaks for affected completions or define policy (e.g., day is fixed at completion timezone).

### Midnight boundary

- **Scenario**: User completes habit at 11:59 PM and 12:01 AM.
- **Behavior**: Two different calendar days. Both count. No double-credit for same day.

### Streak freeze

- **If supported**: Document in product spec. Backend stores `streak_freeze_used_at` or similar; apply business rules consistently.

### First-time streak (no prior completions)

- **Behavior**: `current_streak_days = 1`, `longest_streak_days = 1`.

---

## Idempotency

### Duplicate completion (quest)

- **Scenario**: Client retries `POST /quests/:id/complete` due to network.
- **Options**:
  - Idempotent: Return 200 with current state if already completed.
  - Non-idempotent: Return 422 if already completed.
- **Recommendation**: Idempotent (200) for simpler retry logic.

### Duplicate completion (habit)

- **Scenario**: Same habit completed twice for same day (e.g., double-tap or retry).
- **Behavior**: First wins; second returns 200 with no additional currency. Idempotent per (habit_id, date).

### Idempotency-Key header

- **Usage**: Client sends `Idempotency-Key: <uuid>` for mutation requests.
- **Server**: Cache response for that key; return cached response for duplicate within TTL (e.g., 24h).
- **Scope**: Per user, per key.

---

## Retry Semantics

### Transient failures (5xx, network)

- **Client**: Retry with exponential backoff. Do not retry 4xx (except 429 with Retry-After).
- **Server**: Ensure mutations are idempotent where possible.

### Rate limit (429)

- **Response**: `Retry-After` header (seconds)
- **Client**: Wait and retry.

---

## Priority Algorithm

### Empty eligible set

- **Scenario:** No quests are pending, or all have incomplete dependencies, or all are blocked.
- **Behavior:** GET /quests/current returns 204 or empty. Not an error.
- **Tie-breaking consistency:** Same input must always produce same output. Unit test with fixed timestamps and IDs.

### All quests blocked

- **Behavior:** Same as empty set; 204.

### Circular dependency

- **Behavior:** Reject at create/update with 409. Never reach algorithm with cyclic graph.

---

## Gamification

### XP overflow

- **Field:** User.total_xp
- **Rule:** Use 64-bit integer; cap at safe max if needed. Level is derived; no separate overflow.
- **Mitigation:** Unlikely in practice; document upper bound.

### Streak timezone change

- **Scenario:** User changes timezone. Completions were recorded with old TZ.
- **Policy options:** (a) Recompute all streaks using new TZ; (b) Keep dates as stored (completion TZ); (c) Define "day" as fixed at completion time.
- **Recommendation:** Recompute on timezone change; dates are "local date at completion." Document in GAMIFICATION_ENGINE.

### Currency overflow (Sim)

- Same as Sim/Rewards section; total_currency monotonically increasing.

---

## IO-Multiplexing

### Block then immediate unblock

- **Scenario:** User blocks, then unblocks within same second.
- **Behavior:** Two sequential requests. Both succeed. Quest ends pending. Idempotent.

### Time trigger race (two workers)

- **Scenario:** Two job workers pick same BlockedQuest for time-based unblock.
- **Mitigation:** `SELECT FOR UPDATE` or optimistic lock. First commit wins; second sees unblocked_at set, skips.
- **Alternative:** Distributed lock per quest_id.

### Unblock when parent still incomplete

- **Scenario:** Unblock a quest whose parent is pending or blocked.
- **Behavior:** Quest becomes pending. Algorithm excludes it (not a leaf). Another quest or null is current. Correct.

---

## Error Response Format

All errors:

```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable description",
    "details": []
  }
}
```

### Error Codes

| Code | HTTP | Description |
|------|------|-------------|
| VALIDATION_ERROR | 400 | Invalid request body or params |
| UNAUTHORIZED | 401 | Missing or invalid token |
| FORBIDDEN | 403 | Valid token but no access to resource |
| NOT_FOUND | 404 | Resource not found |
| CONFLICT | 409 | Conflict (e.g., circular dependency) |
| QUEST_ALREADY_COMPLETED | 422 | Business rule violation |
| QUEST_ALREADY_BLOCKED | 422 | Quest is blocked |
| QUEST_NOT_BLOCKED | 422 | Unblock called on non-blocked quest |
| QUEST_HAS_CHILDREN | 409 | Delete quest with dependent quests |
| RATE_LIMIT_EXCEEDED | 429 | Too many requests |
| INTERNAL_ERROR | 500 | Server error |
