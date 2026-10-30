# IO-Multiplexing Logic — Quest Blocking and Unblocking

State machine and flow for quest blocking/unblocking. Implements the "IO-Wait" model: when a quest is blocked (waiting on external dependency), it is suspended and the next ready quest surfaces immediately.

---

## State Machine

```mermaid
stateDiagram-v2
    [*] --> pending: Create quest
    pending --> completed: Complete
    pending --> blocked: Block (IO-Wait)
    blocked --> pending: Unblock
    completed --> [*]
    
    note right of blocked: State preserved
    note right of blocked: Unblock: manual, time, location
```

### Valid Transitions

| From | To | Trigger |
|------|-----|---------|
| pending | completed | POST /quests/:id/complete |
| pending | blocked | POST /quests/:id/block |
| blocked | pending | POST /quests/:id/unblock or time/location trigger |

### Invalid Transitions

- `completed` → any: Reject with 422
- `blocked` → blocked: Idempotent no-op if already blocked
- `blocked` → completed: Not allowed; must unblock first (or define business rule)

---

## Block Action Flow

### Request

```
POST /quests/:id/block
Authorization: Bearer <token>
Content-Type: application/json

{
  "reason": "Waiting on vendor quote",
  "unblock_trigger": "manual",
  "unblock_at": null,
  "unblock_location": null
}
```

### Steps (Sequential)

1. **Validate quest exists and is owned by user.**  
   If not: 404.

2. **Validate quest status is `pending`.**  
   If completed or already blocked: 422.

3. **Validate unblock_trigger and params:**
   - `manual`: No unblock_at or unblock_location required.
   - `time`: `unblock_at` required, must be future (or allow past for "already due").
   - `location`: `unblock_location` required (e.g., place ID or coordinates). Future implementation.

4. **Create BlockedQuest record:**
   - quest_id = quest.id
   - reason = request.reason
   - unblock_trigger = request.unblock_trigger
   - unblock_at = request.unblock_at (if time)
   - unblock_location = request.unblock_location (if location)
   - created_at = now
   - unblocked_at = NULL

5. **Update Quest:**
   - status = 'blocked'
   - updated_at = now

6. **Compute next current quest:**  
   Call Priority Algorithm (see [PRIORITY_ALGORITHM.md](PRIORITY_ALGORITHM.md)) for this user. Excludes the just-blocked quest (it is now ineligible).

7. **Return response:**
   - 200 OK
   - Body: Updated quest (status=blocked)
   - Optional: `next_quest` in body (or separate field) for immediate UI update

### Response Shape

```json
{
  "quest": {
    "id": "uuid",
    "title": "...",
    "status": "blocked",
    ...
  },
  "next_quest": {
    "id": "uuid",
    "title": "...",
    "status": "pending",
    ...
  }
}
```

If no next eligible quest, `next_quest` is null.

---

## Unblock Action Flow (Manual)

### Request

```
POST /quests/:id/unblock
Authorization: Bearer <token>
```

### Steps

1. **Validate quest exists and is owned by user.**  
   If not: 404.

2. **Validate quest status is `blocked`.**  
   If pending or completed: 422.

3. **Find active BlockedQuest** for this quest (unblocked_at IS NULL).  
   If not found: 422 (inconsistent state).

4. **Update BlockedQuest:**
   - unblocked_at = now

5. **Update Quest:**
   - status = 'pending'
   - updated_at = now

6. **Compute next current quest:**  
   Priority Algorithm now includes this quest (it is eligible). The algorithm may or may not select it depending on scores.

7. **Return response:**
   - 200 OK
   - Body: Updated quest (status=pending)
   - Optional: `current_quest` (result of GET /quests/current) for consistency

---

## Unblock Triggers

### Manual

- User taps "Unblock" in UI.
- Calls POST /quests/:id/unblock.
- Synchronous.

### Time

- Scheduled job (cron or queue) runs periodically (e.g., every minute).
- Query: `BlockedQuest` WHERE `unblock_trigger = 'time'` AND `unblock_at <= now()` AND `unblocked_at IS NULL`.
- For each match:
  - Set BlockedQuest.unblocked_at = now
  - Set Quest.status = 'pending'
  - (Optional) Notify user via push or in-app
- Batch processing; use transaction per quest.

### Location (Future)

- **Interface:** Webhook or push from location service when user arrives at `unblock_location`.
- **Flow:** Same as manual unblock (steps 4–7).
- **Precondition:** Integrate with geofencing or place API; document interface when implemented.

---

## Time-Based Unblock Job

### Schedule

- **Frequency:** Every 1–5 minutes (trade-off: latency vs. load).
- **Query example (SQL):**

```sql
SELECT bq.id, bq.quest_id
FROM blocked_quests bq
JOIN quests q ON q.id = bq.quest_id
WHERE bq.unblock_trigger = 'time'
  AND bq.unblock_at <= NOW()
  AND bq.unblocked_at IS NULL
  AND q.status = 'blocked'
```

### Processing

```
for each row:
  BEGIN TRANSACTION
    UPDATE blocked_quests SET unblocked_at = NOW() WHERE id = ?
    UPDATE quests SET status = 'pending', updated_at = NOW() WHERE id = ?
  COMMIT
  (Optional) enqueue push notification
```

### Idempotency

- If job runs twice for same quest, second run: no rows (unblocked_at already set). Safe.

---

## Next-Quest Surfacing

After **block** or **unblock**, the "current" quest may change:

| Event | Effect |
|-------|--------|
| Block quest A | A removed from eligible set. Algorithm runs on remaining eligible quests. Next highest becomes current. |
| Unblock quest A | A added to eligible set. Algorithm runs on all eligible quests. May or may not select A. |

**Empty state:** If no eligible quests after block, return null. Client shows "No quest ready" with CTAs to unblock or add quest.

---

## Edge Cases

### Block when no other eligible quest

- **Behavior:** Block succeeds. `next_quest` = null. GET /quests/current returns 204.
- **Client:** Show empty state. Not an error.

### Unblock when parent dependency still incomplete

- **Behavior:** Unblock succeeds. Quest becomes pending. If parent is still pending/blocked, this quest is NOT a leaf. Algorithm will not select it until parent completes.
- **Correct:** Quest is eligible for *scoring* but may be excluded by dependency rule (all ancestors completed). Re-check dependency logic: a quest with incomplete parent is **ineligible**. So when we unblock a quest whose parent is incomplete, it remains ineligible. Algorithm correctly returns another quest or null.

### Block then immediate unblock (race)

- **Scenario:** User blocks, then unblocks within same second.
- **Behavior:** Two sequential requests. First: block. Second: unblock. Both succeed. Quest ends pending. Idempotent.

### Time trigger race (two workers)

- **Scenario:** Two job workers pick same BlockedQuest.
- **Mitigation:** Use `SELECT ... FOR UPDATE` or optimistic lock. First commit wins; second sees unblocked_at set, skips.
- **Alternative:** Distributed lock per quest_id before processing.

### Block already-blocked quest

- **Behavior:** 422. "Quest is already blocked."

### Unblock already-pending quest

- **Behavior:** 422. "Quest is not blocked."

---

## Data Integrity

- **BlockedQuest.quest_id:** Unique active record per quest (one blocking at a time). Use unique partial index: `UNIQUE(quest_id) WHERE unblocked_at IS NULL`.
- **Quest.status:** Must match presence of active BlockedQuest: if BlockedQuest exists with unblocked_at NULL, Quest.status must be 'blocked'.

---

## API Summary

| Endpoint | Purpose |
|----------|---------|
| POST /quests/:id/block | Suspend quest; surface next |
| POST /quests/:id/unblock | Resume quest; recompute current |
| GET /quests/current | Get current objective (uses Priority Algorithm) |

Time-based unblock is backend-internal; no client endpoint.
