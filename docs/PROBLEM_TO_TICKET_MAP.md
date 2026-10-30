# Problem → Ticket Path Mapping

**Purpose:** Map each reported problem to existing or new tickets, with sequential agent paths.

---

## 1. Roads Messed Up (Graphics, Angles, Bad Adjacency Logic)

**Problems:**
- Road graphics and angles in 3D space are wrong
- Road selection based on neighbors is dumb
- "Road on left shows moving right" — sprite orientation doesn't match adjacency

**Existing tickets:**
- **P0-3** Road Rendering Fix (LP-9 GD, LP-10 SE, LP-11 DevOps) — Y-axis bulge, scaling
- **P1-B** Dynamic Road Graph (LP-36 SE, LP-37 DevOps) — road data structure

**Gap:** No ticket for:
- Road sprite **rotation** based on neighbor config (straight n-s vs e-w needs 90° rotation)
- Smarter **pickRoadSprite** logic that maps grid direction to correct sprite + rotation
- Corner orientation (which way the curve opens)

**New ticket path (created in Plane):**

| Step | Role | Ticket | Description |
|------|------|--------|-------------|
| 1 | GD | **LP-78** [GD] Road Adjacency & Orientation Design | Design: map grid (n,s,e,w) to sprite type + rotation. Define which sprite for each of 16 neighbor configs. Corner opening direction. |
| 2 | SE | **LP-79** [SE] Road Adjacency & Orientation Implementation | Implement: pickRoadSprite returns { key, rotation }. Apply sprite.setRotation() in CityScene. Handle all junction/corner/straight cases. |
| 3 | DevOps | **LP-80** [DevOps] Testing: Road Adjacency | Verify roads join correctly in all directions. No "left shows right" mismatch. |

**Parent:** LP-77 [Feature] Road Adjacency & Orientation  
**Agent path:** LP-78 (GD) → LP-79 (SE) → LP-80 (DevOps)

---

## 2. People Oscillating Between 2 Destinations

**Problem:** Pedestrians move in predictable paths, oscillating between 2 spots.

**Root cause:** `PedestrianManager.pickNextCell()` uses **random** neighbor selection (no A*). Citizens pick a random adjacent road each step → can ping-pong between 2 cells. No destination concept.

**Existing ticket:**
- **P0-1** Citizen A* Pathfinding (LP-2 SE, LP-3 DevOps) — implements A* to buildings
  - When done: citizens path to a destination, then idle, then pick **new** destination
  - Reduces oscillation by having purposeful A→B→C flow

**If A* is done but still oscillating:** Add "destination variety" — ensure we don't always pick the same 2 buildings. Track recent destinations, exclude for next pick.

| Step | Role | Ticket | Description |
|------|------|--------|-------------|
| 1 | SE | **LP-2** [SE] Citizen A* Pathfinding (P0-1) | Implement A* pathfinding. Spawn at housing, path to commercial/utility, idle 5–15s, pick new destination. |
| 2 | DevOps | **LP-3** [DevOps] Testing: Citizen A* Pathfinding | Verify citizens follow roads, don't oscillate. |

**Agent path:** LP-2 (SE) → LP-3 (DevOps). No GD blocker.

**Enhancement if still oscillating after A*:** Add to LP-2 scope or create follow-up: "Destination variety — exclude recently visited buildings from next pick; visit 3+ distinct destinations per round."

---

## 3. Castle Wrong Look, Wrong Name, Diagonal Cut in PNG

**Problems:**
- Castle looks like 5-floor apartment, not a castle
- Wrong name (should reflect actual appearance)
- PNG has diagonal cut at lower end — bad clipping/design

**Existing tickets:** None. Building assets are from KayKit; castle uses `building_H.obj`.

**New ticket path (created in Plane):**

| Step | Role | Ticket | Description |
|------|------|--------|-------------|
| 1 | GD | **LP-82** [GD] Governor Building Asset & Naming | 1) Rename "Castle" to accurate name (e.g. "Grand Residence", "Town Manor") per actual KayKit model. 2) Fix/replace asset: no diagonal cut at base. 3) Ensure renderer doesn't clip. Canvas bounds, centering. |
| 2 | SE | **LP-83** [SE] Governor Building Integration | Update run.mjs, SPRITE_KEY_MAP, backend display name. Load new asset. Fix renderer clip bounds if needed. |
| 3 | DevOps | **LP-84** [DevOps] Testing: Governor Building | verify_sprites passes. No diagonal cut. Correct name in UI. |

**Parent:** LP-81 [Feature] Governor Building Asset Fix & Rename  
**Agent path:** LP-82 (GD) → LP-83 (SE) → LP-84 (DevOps)

---

## 4. Grass Not Showing, Barren Land Same as Background, Canvas Bounds Unclear

**Problems:**
- Grass not visible on land
- Barren land same color as background
- Need to see playable canvas bounds

**Existing ticket:**
- **P1-E** Grass & Ground Tiles (LP-46 GD, LP-47 SE, LP-48 DevOps)
  - Spec: `docs/GRASS_GROUND_TILES_SPEC.md`
  - GD designs grass tile; SE renders grass on empty grid cells; DevOps tests

**Gap:** Playable canvas bounds / barren land differentiation might need explicit scope.

| Step | Role | Ticket | Description |
|------|------|--------|-------------|
| 1 | GD | **LP-46** [GD] Grass & Ground Tiles (P1-E) | Design grass tile per spec. Ensure barren (no grass) areas use distinct color from background (#5a8a3c). |
| 2 | SE | **LP-47** [SE] Grass & Ground Tiles | Render grass on empty cells. Differentiate playable area from out-of-bounds. Consider subtle border or tint. |
| 3 | DevOps | **LP-48** [DevOps] Testing: Grass & Ground | Verify grass shows, bounds visible, no regressions. |

**Agent path:** LP-46 → LP-47 → LP-48

---

## 5. Overall Amateur Graphics

**Problem:** Whole graphics look amateur-built.

**Existing ticket:**
- **P0-5** Visual Style Pass (LP-16 Feature, LP-17 GD, LP-18 SE, LP-19 DevOps)
  - Cats Mansion standard, warm pastels, consistent aesthetic

| Step | Role | Ticket | Description |
|------|------|--------|-------------|
| 1 | GD | **LP-17** [GD] Visual Style Pass (P0-5) | Define and apply Cats Mansion aesthetic across all assets. |
| 2 | SE | **LP-18** [SE] Visual Style Pass | Implement renderer/render pipeline changes for consistent look. |
| 3 | DevOps | **LP-19** [DevOps] Testing: Visual Style Pass | PM acceptance: screenshot comparison, palette check. |

**Agent path:** LP-17 → LP-18 → LP-19

---

## Summary: Existing vs New Tickets

| Problem | Existing Path | New Path Needed |
|---------|---------------|-----------------|
| 1. Roads | P0-3 (LP-9,10,11) + P1-B (LP-36,37) | **Yes** — Road Adjacency & Orientation (GD→SE→DevOps) |
| 2. People oscillating | P0-1 (LP-2, LP-3) | No — LP-2, LP-3 cover it |
| 3. Castle | — | **Yes** — Governor Building fix (GD→SE→DevOps) |
| 4. Grass | P1-E (LP-46,47,48) | No |
| 5. Amateur look | P0-5 (LP-17,18,19) | No |

---

## Sequential Agent Paths (Copy-Paste)

### Path A: Roads (Existing P0-3 + New Road Adjacency)

1. **GD:** LP-9 [GD] Road Rendering Fix (P0-3)
2. **SE:** LP-10 [SE] Road Rendering Fix — *after LP-9 Done*
3. **DevOps:** LP-11 [DevOps] Testing: Road Rendering — *after LP-10 Done*
4. **New EPIC** (LP-77): Road Adjacency & Orientation
5. **GD:** LP-78 [GD] Road Adjacency Design
6. **SE:** LP-79 [SE] Road Adjacency Implementation — *after LP-78 Done*
7. **DevOps:** LP-80 [DevOps] Testing: Road Adjacency — *after LP-79 Done*

### Path B: People Oscillating

1. **SE:** LP-2 [SE] Citizen A* Pathfinding — *no blocker*
2. **DevOps:** LP-3 [DevOps] Testing: Citizen A* — *after LP-2 Done*

### Path C: Castle / Governor Building (New EPIC LP-81)

1. **GD:** LP-82 [GD] Governor Building Asset & Naming
2. **SE:** LP-83 [SE] Governor Building Integration — *after LP-82 Done*
3. **DevOps:** LP-84 [DevOps] Testing: Governor Building — *after LP-83 Done*

### Path D: Grass & Ground

1. **GD:** LP-46 [GD] Grass & Ground Tiles
2. **SE:** LP-47 [SE] Grass & Ground Tiles — *after LP-46 Done*
3. **DevOps:** LP-48 [DevOps] Testing: Grass & Ground — *after LP-47 Done*

### Path E: Overall Visual Polish

1. **GD:** LP-17 [GD] Visual Style Pass
2. **SE:** LP-18 [SE] Visual Style Pass — *after LP-17 Done*
3. **DevOps:** LP-19 [DevOps] Testing: Visual Style — *after LP-18 Done*

---

## Tickets Created (Plane)

**Road Adjacency & Orientation** — LP-77 (Feature), LP-78 (GD), LP-79 (SE), LP-80 (DevOps)  
**Governor Building Asset Fix** — LP-81 (Feature), LP-82 (GD), LP-83 (SE), LP-84 (DevOps)

Run `your project tooling script` to add Blocked by / Unlocks if needed.
