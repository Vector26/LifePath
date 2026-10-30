# LifePath Web Responsive Design

Responsive design guidelines for the LifePath web application. Aligns with the "phone as progress tracker" and minimal UI psychology.

---

## Breakpoints

| Breakpoint | Width | Target |
|------------|-------|--------|
| Mobile | < 480px | Phones, narrow viewports |
| Tablet | 480px – 1024px | Tablets, small laptops |
| Desktop | > 1024px | Desktops, large screens |

Use `min-width` media queries. Mobile-first: base styles for mobile; enhance for tablet and desktop.

---

## Single-Quest HUD Layout

### Mobile (< 480px)

- Full-width card; minimal padding (16px).
- Quest title: 18–20px; 1.2 line-height.
- Three actions: Complete (primary, full-width), Block (secondary), Skip (tertiary or swipe). One-tap each.
- Stats row: streak, XP, "X quests · Y habits today" (from GET /stats/today); small, muted.
- Optional context selector: focus | quick | low_energy — subtle chips or segmented control above card.
- "Why this quest?" — expandable section or info icon; display reasons from API.
- White space: At least 40% of viewport; avoid clutter.

### Tablet (480px – 1024px)

- Card max-width 400px; centered.
- Same hierarchy; slightly larger touch targets.
- Optional: Side panel for nav or Sim preview.

### Desktop (> 1024px)

- Card max-width 420px; centered or offset left.
- Right sidebar: Sim state, streak summary, daily stats (optional).
- Keyboard: Tab through actions; Enter to complete; optional shortcuts for Block, Skip.

### Block Quest Modal/Drawer

- Mobile: Bottom sheet or full-screen modal.
- Fields: unblock_trigger (manual | time | location), unblock_at (datetime picker if time), unblock_location (text if location), reason (optional).
- Submit and Cancel. Touch targets 44×44.

---

## Sim View

- **Layout**: Card or panel; companion visual (simple illustration or placeholder).
- **Progress**: Bar from current to next milestone; numbers (e.g., "340 / 500 coins").
- **Sizing**: Card scales with viewport; max 400px width on desktop.
- **Typography**: One prominent number (currency); one label. Muted secondary text.

---

## Touch Targets

| Element | Minimum Size |
|---------|--------------|
| Buttons | 44 x 44px |
| Links | 44 x 44px tap area |
| Icons | 24 x 24px; 44px tap area |

Spacing between tappable elements: min 8px.

---

## Typography Scale

| Use | Size (mobile) | Size (desktop) |
|-----|---------------|----------------|
| Quest title | 18px | 20px |
| Body | 16px | 16px |
| Secondary | 14px | 14px |
| Caption | 12px | 12px |
| Large number (currency, streak) | 24px | 28px |

Line height: 1.4–1.5 for body; 1.2 for titles.

---

## Color Palette

Reference from UI psychology doc. Use CSS variables.

**Light mode:**

| Token | Hex | Use |
|-------|-----|-----|
| background | #F8F6F2 | Page background |
| surface | #FFFFFF | Cards, panels |
| text-primary | #2C2A26 | Primary text |
| text-secondary | #6B6560 | Secondary text |
| accent | #C4785A | Primary actions |
| accent-secondary | #7A9B76 | Success, growth |
| success | #6B8E6B | Completed state |
| blocked | #8B9AA8 | Blocked/waiting state |

**Dark mode:**

| Token | Hex | Use |
|-------|-----|-----|
| background | #1E1C19 | Page background |
| surface | #2C2A26 | Cards |
| text-primary | #F0EDE8 | Primary text |
| text-secondary | #9A948C | Secondary text |
| accent | #D4937A | Primary actions |
| accent-secondary | #8BAB87 | Success |
| success | #7A9E7A | Completed |
| blocked | #9AA8B8 | Blocked |

Avoid pure black (#000), pure white (#FFF), and bright red for errors (use muted amber if needed).

---

## Viewport and Meta

```html
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
```

- `viewport-fit=cover`: Enables safe-area insets for notches.
- Use `env(safe-area-inset-*)` for padding in full-bleed layouts.

---

## Reduced Motion

```css
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
```

Respect user preference; avoid parallax and long animations.

---

## Accessibility

- **Focus**: Visible focus ring (2px, contrast color).
- **Contrast**: Min 4.5:1 for body text; 3:1 for large text.
- **Labels**: All interactive elements have accessible names.
- **Skip link**: "Skip to main content" for keyboard users.

---

## Form Layouts (Create/Edit Quest, Habit)

- **CreateQuestForm**: title (required, prominent), notes (collapsible or secondary), priority_level (P0–P4 chips or select), time_of_day (optional), context_tag (optional). Full-width on mobile; max 400px centered on desktop.
- **CreateHabitForm**: title (required), currency_per_completion (number input, default 10), is_streak_tracked (toggle). Same width constraints.
- **Edit forms**: Same layout; prefilled; include Delete with confirmation.

---

## Grid and Spacing

- **Base unit**: 4px.
- **Spacing scale**: 4, 8, 12, 16, 24, 32, 48.
- **Card padding**: 16px mobile; 24px desktop.
- **Section gaps**: 24px vertical.
