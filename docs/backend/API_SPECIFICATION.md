# LifePath API Specification

RESTful API for the LifePath gamified productivity application. Tech-agnostic; implement with any backend framework.

---

## Base URL and Versioning

- **Base URL**: `https://api.lifepath.app` (replace with your host)
- **Version prefix**: `/api/v1`
- **Example**: `https://api.lifepath.app/api/v1/quests`

Version is included in the path. Future breaking changes introduce `/api/v2`.

---

## Conventions

- **Content-Type**: `application/json` for request and response bodies
- **Accept**: `application/json`
- **Auth**: Bearer token in `Authorization` header: `Authorization: Bearer <access_token>`
- **Idempotency**: Use `Idempotency-Key` header for mutations when retries are possible

---

## Authentication Endpoints

### POST /auth/register

Create a new user account.

**Request:**

```json
{
  "email": "user@example.com",
  "password": "securePassword123",
  "display_name": "Optional Name",
  "timezone": "America/New_York"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| email | string | Yes | Valid email, unique |
| password | string | Yes | Min 8 chars, complexity rules |
| display_name | string | No | Max 100 chars |
| timezone | string | Yes | IANA timezone |
| companion_type | string | No | `plant` or `creature`; default `plant`. Creates SimState on register. |

**Response:** 201 Created

```json
{
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "display_name": "Optional Name",
    "timezone": "America/New_York",
    "created_at": "2026-02-26T12:00:00Z"
  },
  "access_token": "jwt_or_opaque",
  "refresh_token": "refresh_token",
  "expires_in": 900
}
```

---

### POST /auth/login

Authenticate and receive tokens.

**Request:**

```json
{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

**Response:** 200 OK

```json
{
  "access_token": "jwt_or_opaque",
  "refresh_token": "refresh_token",
  "expires_in": 900
}
```

---

### POST /auth/refresh

Exchange refresh token for new access token.

**Request:**

```json
{
  "refresh_token": "refresh_token"
}
```

**Response:** 200 OK

```json
{
  "access_token": "new_access_token",
  "expires_in": 900
}
```

---

### POST /auth/logout

Revoke refresh token. Requires auth.

**Request:** (optional body for refresh token to revoke)

```json
{
  "refresh_token": "optional_specific_token"
}
```

**Response:** 204 No Content

---

## User Endpoints

### GET /users/me

Get current user profile. Requires auth.

**Response:** 200 OK

```json
{
  "id": "uuid",
  "email": "user@example.com",
  "display_name": "Optional Name",
  "timezone": "America/New_York",
  "total_xp": 150,
  "level": 1,
  "created_at": "2026-02-26T12:00:00Z",
  "updated_at": "2026-02-26T12:00:00Z"
}
```

---

### PATCH /users/me

Update current user profile. Requires auth.

**Request:**

```json
{
  "display_name": "New Name",
  "timezone": "Europe/London"
}
```

All fields optional; only provided fields are updated.

**Response:** 200 OK (same shape as GET /users/me)

---

### GET /users/me/gamification

Get gamification state (XP, level). Requires auth. Optional endpoint; can be merged into GET /users/me.

**Response:** 200 OK

```json
{
  "total_xp": 150,
  "level": 1
}
```

---

### GET /users/me/settings

Get user settings (notifications, themes, etc.). Requires auth.

**Response:** 200 OK

```json
{
  "notifications_enabled": true,
  "haptic_feedback": true,
  "theme": "system"
}
```

---

## Quest Endpoints

### GET /quests

List quests for current user. Requires auth.

**Query params:**

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| status | string | all | Filter: `pending`, `blocked`, `completed` |
| limit | int | 50 | Max 100 |
| cursor | string | - | Pagination cursor |

**Response:** 200 OK

```json
{
  "quests": [
    {
      "id": "uuid",
      "title": "Complete report",
      "notes": "...",
      "priority_level": "P1",
      "parent_id": null,
      "status": "pending",
      "time_of_day": "morning",
      "context_tag": "focus",
      "created_at": "2026-02-26T12:00:00Z",
      "updated_at": "2026-02-26T12:00:00Z"
    }
  ],
  "next_cursor": "base64_cursor_or_null"
}
```

---

### GET /quests/current

Get the single current objective (highest-priority eligible quest). Requires auth.

**Query params:**

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| context | string | null | User context for algorithm: `focus`, `quick`, `low_energy` |

**Response:** 200 OK when quest exists; **204 No Content** when no eligible quest (empty body).

When 200 OK:

```json
{
  "quest": {
    "id": "uuid",
    "title": "Complete report",
    "notes": "...",
    "priority_level": "P1",
    "parent_id": null,
    "status": "pending",
    "time_of_day": "morning",
    "context_tag": "focus",
    "created_at": "2026-02-26T12:00:00Z",
    "updated_at": "2026-02-26T12:00:00Z"
  },
  "reasons": ["high_priority", "matches_morning"]
}
```

`reasons` is optional; explains "Why this quest?" (see [PRIORITY_ALGORITHM.md](PRIORITY_ALGORITHM.md)).

---

### POST /quests

Create a new quest. Requires auth.

**Request:**

```json
{
  "title": "Complete report",
  "notes": "Optional notes",
  "priority_level": "P1",
  "parent_id": null,
  "time_of_day": "morning",
  "context_tag": "focus"
}
```

| Field | Type | Required | Default |
|-------|------|----------|---------|
| title | string | Yes | - |
| notes | string | No | "" |
| priority_level | enum | Yes | - |
| parent_id | uuid | No | null |
| time_of_day | enum | No | any |
| context_tag | enum | No | null |

**Response:** 201 Created

```json
{
  "id": "uuid",
  "title": "Complete report",
  "notes": "...",
  "priority_level": "P1",
  "parent_id": null,
  "status": "pending",
  "time_of_day": "morning",
  "context_tag": "focus",
  "created_at": "2026-02-26T12:00:00Z",
  "updated_at": "2026-02-26T12:00:00Z"
}
```

---

### PATCH /quests/:id

Update a quest. Requires auth. 404 if not found or not owned.

**Request:** Same fields as POST; all optional.

**Response:** 200 OK (full quest object)

---

### POST /quests/:id/complete

Mark quest as completed. Requires auth.

**Response:** 200 OK

```json
{
  "quest": { /* updated quest with status=completed, completed_at set */ },
  "xp_earned": 10
}
```

---

### POST /quests/:id/block

Mark quest as blocked (IO-Wait). Requires auth.

**Skip gesture:** The product's "complete, block, skip" gestures can be implemented as: complete = POST /quests/:id/complete, block = this endpoint with `unblock_trigger: manual`, skip = this endpoint with `unblock_trigger: manual` and `reason: "Skipped for now"`. No separate skip endpoint.

**Request:**

```json
{
  "reason": "Waiting on vendor quote",
  "unblock_trigger": "manual",
  "unblock_at": null,
  "unblock_location": null
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| reason | string | No | User note |
| unblock_trigger | enum | Yes | `manual`, `time`, `location` |
| unblock_at | ISO8601 | If time | For time trigger |
| unblock_location | string | If location | For location trigger |

**Response:** 200 OK

```json
{
  "quest": { /* quest with status=blocked */ },
  "next_quest": { /* next current objective, or null */ },
  "xp_earned": 2
}
```

---

### POST /quests/:id/unblock

Unblock a blocked quest. Requires auth.

**Response:** 200 OK (quest with status=pending; optional `current_quest` in body)

---

### DELETE /quests/:id

Soft-delete a quest. Requires auth. Sets deleted_at; quest excluded from algorithm and lists.

**Response:** 204 No Content

**Note:** Reject with 409 if quest has children (parent_id references it). Document cascade policy (reject vs orphan children).

---

## Habit Endpoints

### GET /habits

List habits for current user. Requires auth.

**Response:** 200 OK

```json
{
  "habits": [
    {
      "id": "uuid",
      "title": "Morning run",
      "currency_per_completion": 10,
      "is_streak_tracked": true,
      "created_at": "2026-02-26T12:00:00Z",
      "updated_at": "2026-02-26T12:00:00Z"
    }
  ]
}
```

---

### POST /habits

Create a new habit. Requires auth.

**Request:**

```json
{
  "title": "Morning run",
  "currency_per_completion": 10,
  "is_streak_tracked": true
}
```

| Field | Type | Required | Default |
|-------|------|----------|---------|
| title | string | Yes | - |
| currency_per_completion | int | No | 10 |
| is_streak_tracked | boolean | No | true |

**Response:** 201 Created (habit object)

---

### PATCH /habits/:id

Update habit. Requires auth.

**Request:** Partial object; all fields optional.

**Response:** 200 OK (habit object)

---

### DELETE /habits/:id

Soft-delete a habit. Requires auth. Sets deleted_at; habit excluded from lists. Existing HabitCompletions and streak history retained for audit.

**Response:** 204 No Content

---

### POST /habits/:id/complete

Record habit completion. Requires auth. Awards currency, updates streak.

**Response:** 200 OK

```json
{
  "habit": { /* updated habit */ },
  "currency_earned": 10,
  "streak": {
    "current_streak_days": 5,
    "longest_streak_days": 12
  }
}
```

---

## Sim / Rewards Endpoints

### GET /sim/state

Get current Sim state. Requires auth.

**Response:** 200 OK (404 if SimState not yet created; create on first habit completion or register with companion_type)

---

### PATCH /sim/state

Update Sim preferences. Requires auth. Only `companion_type` is editable; currency and stage are system-managed.

**Request:**

```json
{
  "companion_type": "plant"
}
```

| Field | Type | Required | Values |
|-------|------|----------|--------|
| companion_type | string | No | `plant`, `creature` |

**Response:** 200 OK (full sim state)

```json
{
  "total_currency": 340,
  "current_stage": "stable",
  "companion_type": "plant",
  "next_milestone": {
    "stage": "thriving",
    "currency_threshold": 500,
    "currency_to_go": 160
  }
}
```

---

### GET /sim/milestones

List all milestones (or user's unlocked). Requires auth.

**Response:** 200 OK

```json
{
  "milestones": [
    {
      "id": "uuid",
      "stage": "stable",
      "currency_threshold": 200,
      "title": "First apartment",
      "description": "Your Sim moves into a cozy apartment.",
      "unlocked": true
    }
  ]
}
```

---

## Streak Endpoints

### GET /streaks

List streaks for all streak-tracked habits. Requires auth.

**Response:** 200 OK

```json
{
  "streaks": [
    {
      "habit_id": "uuid",
      "habit_title": "Morning run",
      "current_streak_days": 5,
      "longest_streak_days": 12,
      "last_completed_date": "2026-02-25"
    }
  ]
}
```

---

### GET /streaks/:habitId

Get streak for a specific habit. Requires auth.

**Response:** 200 OK

---

## Stats Endpoints

### GET /stats/today

Get today's completion counts for widget and summary. Requires auth.

**Response:** 200 OK

```json
{
  "quests_completed_today": 3,
  "habits_completed_today": 2,
  "date": "2026-02-26"
}
```

**Note:** `date` is the calendar date in user's timezone. Counts use completed_at within that day.

```json
{
  "habit_id": "uuid",
  "habit_title": "Morning run",
  "current_streak_days": 5,
  "longest_streak_days": 12,
  "last_completed_date": "2026-02-25"
}
```

---

## Error Format

All errors use a consistent shape:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Human-readable message",
    "details": [
      { "field": "title", "reason": "must be between 1 and 500 characters" }
    ]
  }
}
```

### HTTP Status Codes

| Code | Usage |
|------|-------|
| 200 | Success (GET, PATCH, POST for idempotent) |
| 201 | Created (POST for create) |
| 204 | No Content (DELETE, logout) |
| 400 | Bad Request (validation) |
| 401 | Unauthorized (missing/invalid token) |
| 403 | Forbidden (no access to resource) |
| 404 | Not Found |
| 409 | Conflict (e.g., circular dependency, duplicate) |
| 422 | Unprocessable Entity (business rule violation) |
| 429 | Too Many Requests (rate limit) |
| 500 | Internal Server Error |

---

## Rate Limiting

- **Authenticated**: 100 req/min per user
- **Auth endpoints**: 10 req/min per IP
- Headers: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`

---

## Pagination

Cursor-based pagination for list endpoints:

- Request: `?limit=50&cursor=base64_cursor`
- Response: `next_cursor` (null if no more)
- Cursor is opaque; do not parse
