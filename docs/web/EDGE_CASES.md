# LifePath Web Edge Cases

Edge cases and failure handling for the LifePath web application.

---

## Offline

### Queue actions while offline

- **Behavior**: Store mutations in IndexedDB or localStorage queue.
- **UI**: Show subtle "Offline" indicator; actions succeed locally.
- **Conflict**: Mark items as "pending sync"; show on reconnect.

### Sync on reconnect

- **Trigger**: `online` event; optional Visibility API when tab becomes visible.
- **Process**: Drain queue in FIFO order; call API for each. Attach `Idempotency-Key: <uuid>` per mutation (generated when queued) to prevent duplicate processing on retry.
- **Success**: Remove from queue; update local state.
- **Failure**: Keep in queue; retry with backoff; after N failures, show "Sync failed" with manual retry.

### Conflict UI

- **Scenario**: Server returns 409 or 422 (e.g., quest already completed elsewhere).
- **Behavior**: Remove from queue; refresh local state from server.
- **Message**: Brief toast: "Synced – some actions were already applied." No guilt.

### Delete quest with children (409)

- **Scenario**: User deletes quest; backend returns 409 QUEST_HAS_CHILDREN (quest has dependent quests).
- **Behavior**: Show message: "Delete or reparent dependent quests first." Link to quest list filtered by parent or children.
- **Tone**: Supportive; no guilt.

---

## Slow Network

### Loading states

- **Skeleton**: Use skeleton for quest card, habit list.
- **Spinner**: Only for initial app load or full-page transitions.
- **Timeout**: After 10s, show "Taking longer than usual" with retry option.

### Optimistic updates

- **Complete quest**: Immediately show as done in UI; revert on API error.
- **Complete habit**: Show +N currency immediately; revert if API fails.
- **Risk**: Avoid optimistic update for critical state (e.g., currency) if revert would confuse user. Prefer loading state for habit completion if uncertain.

### Timeout handling

- **API timeout**: 30s default; show error after timeout.
- **Retry**: Offer "Try again" button; do not auto-retry indefinitely.

---

## Empty States

### No quests

- **Message**: "No quests yet. Add your first quest to get started."
- **Action**: CTA to add quest. Do not show backlog or guilt.

### No habits

- **Message**: "No habits yet. Create a habit to start earning and growing."
- **Action**: CTA to create habit.

### No current quest (all blocked or none)

- **Trigger**: GET /quests/current returns 204 No Content (empty body). API client sets currentQuest = null.
- **Message**: "No quest ready right now. Unblock a quest or add a new one."
- **Tone**: Neutral, supportive. No red or alarming visuals.

### First-time onboarding

- **Flow**: Brief intro (1–2 screens); prompt to add first quest or habit.
- **Skip**: Allow skip; land on empty state with clear CTAs.

---

## Accessibility and Preferences

### Large text / zoom

- **Support**: Use rem/em; avoid fixed px for critical text.
- **Zoom**: Test at 200% zoom; layout should not break.
- **Max width**: Constrain content width so long lines don't span full viewport at high zoom.

### Reduced motion

- **Detection**: `prefers-reduced-motion: reduce`.
- **Behavior**: Disable or shorten animations; no parallax.

### Color contrast

- **Check**: All text meets WCAG AA (4.5:1 normal, 3:1 large).
- **Dark mode**: Support `prefers-color-scheme: dark` or manual toggle.

---

## Browser Support

### Minimum matrix

| Browser | Min Version |
|---------|-------------|
| Chrome | Last 2 versions |
| Firefox | Last 2 versions |
| Safari | Last 2 versions |
| Edge | Last 2 versions |
| Mobile Safari | iOS 14+ |
| Chrome Android | Last 2 versions |

### Unsupported

- Show message: "LifePath works best in modern browsers. Please update."
- No polyfills for core functionality (fetch, Promise, async/await).

### Service Worker

- Graceful degradation if not supported: App works without offline caching.
- Do not block first load on SW registration.

---

## Auth Edge Cases

### Token expired mid-session

- **Detection**: 401 on API call.
- **Action**: Attempt refresh; retry request. If refresh fails, redirect to login.
- **UX**: Show brief "Session expired" if redirect; avoid jarring redirect without message.

### Logout on another tab

- **Scenario**: User logs out in tab A; tab B still has token.
- **Behavior**: Tab B receives 401 on next request; refresh fails; redirect to login.
- **Optional**: Use BroadcastChannel to notify other tabs to clear state and redirect.

---

## Form Validation

### Inline validation

- **On blur**: Validate required fields, length limits.
- **On submit**: Full validation; show all errors.
- **Errors**: Inline below field; no toast for validation (toast for success/network).

### Rate limiting (429)

- **Display**: "Too many requests. Please wait a moment."
- **Retry-After**: Use header if present; disable submit for that duration.
