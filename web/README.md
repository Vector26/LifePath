# LifePath Web Application

React (Vite) web frontend for the LifePath gamified productivity app. Implements the UI specification from `docs/UI_IMPLEMENTATION_PLAN.md`.

## Tech Stack

- React 18 + TypeScript
- Vite
- React Router

## Design System

- **Base unit:** 8px (spacing: xs=4, sm=8, md=16, lg=24, xl=32)
- **Tap targets:** min 44×44px (iOS/Android accessible)
- **Themes:** Light/dark with system preference support

## Development

```bash
# Install dependencies
npm install

# Start dev server (default: http://localhost:39173)
npm run dev

# Build for production
npm run build
```

### API Proxy

The dev server proxies `/api` to the backend at `http://localhost:39081`. Ensure the LifePath backend is running:

```bash
cd ../backend && ./mvnw spring-boot:run
```

## Project Structure

```
src/
  components/     # Shared design system
    layout/       # AppShell, NavBar, AuthLayout
    ui/           # Button, Card, Input, Chip, Textarea
  screens/        # Page-level components
    auth/         # Login, Register
    home/         # QuestHUD, BlockQuestModal
    quests/       # QuestList
    habits/       # HabitList
    sim/          # SimView
    settings/     # Settings
  theme/          # Design tokens
```

## Routes

| Route | Description |
|-------|-------------|
| `/login` | Login form |
| `/register` | Registration (email, password, timezone, companion type) |
| `/quest` | Current quest HUD (primary view) |
| `/quests` | Quest list with filters |
| `/habits` | Habit list |
| `/sim` | Companion / gamification view |
| `/settings` | User settings |

## Implementation Status

- [x] Design tokens and global CSS
- [x] App Shell (mobile bottom nav, desktop sidebar)
- [x] Atomic components (Button, Card, Input, Chip)
- [x] Auth pages (Login, Register)
- [x] Home / Current Quest HUD with empty state
- [x] BlockQuestModal
- [x] Placeholder: Quest List, Habit List, Sim, Settings
- [ ] API integration (quests, habits, sim, stats)
- [ ] Create/Edit forms for quests and habits
- [ ] Offline queue and SyncIndicator
