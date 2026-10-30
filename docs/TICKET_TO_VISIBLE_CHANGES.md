# Which Tickets Produce Visible Changes?

**Problem:** Tickets show "Done" in Plane.io, but you don't see changes in the app.  
**Reason:** "Done" in Plane ≠ code implemented + assets regenerated + app rebuilt.

---

## P0 Tickets That Produce Visible Changes

| Ticket | What You'd See When Done | Current Code Status | To See It |
|--------|--------------------------|---------------------|-----------|
| **P0-1** Citizen A* Pathfinding (LP-1,2,3) | Citizens walk toward buildings on roads instead of random jitter | ❌ **Not implemented** — `PedestrianManager.pickNextCell()` still uses `Math.random()` | Implement A* in `PedestrianManager.ts` |
| **P0-2** Park Asset (LP-4,5,6,7) | Park looks like illustrated 2D scene (trees, bench, path) instead of green cylinder | ✅ Pipeline has `park_2d` — but **assets not generated** | Run `cd web && npm run export-building-sprites` |
| **P0-3** Road Y-Bulge Fix (LP-8,9,10,11) | Roads lie flat, no vertical bulge | ❌ Still uses `Math.min()` uniform scale in `CityScene.buildRoadVisual()` | Fix scaling in `CityScene.ts` |
| **P0-4** Shirt Color Flashing (LP-12,13,14,15) | Pedestrians have stable shirt colors per character | ⚠️ **Partial** — `renderer.html` uses `colorIndex`, `run.mjs` outputs `person_v0_walk_0`… but **BootScene** loads `person_walk_0`–`person_walk_3` only | Update BootScene + PedestrianManager to use `person_v0_walk_0`…`person_v3_walk_3` |
| **P0-5** Visual Style Pass (LP-16–19) | Overall Cats Mansion look (pastels, no harsh shadows) | Partial in renderer; needs PM review | Aesthetic pass on assets + PM approval |

---

## Minimal Steps to See ANY P0 Change

1. **Generate assets** (required for P0-2, P0-4, buildings, roads):
   ```bash
   cd web && npm run export-building-sprites
   ```
   Without this, `web/public/assets/` has no PNGs and the app falls back to colored rectangles.

2. **Rebuild/refresh** the dev server after code or asset changes.

---

## Implementation Status Summary

| P0 Item | Code | Assets | Integration |
|---------|------|--------|-------------|
| P0-1 A* | ❌ | N/A | — |
| P0-2 Park | ✅ `park_2d` in renderer | ⚠️ Run export | ✅ |
| P0-3 Roads | ❌ | ✅ (from export) | — |
| P0-4 Shirts | ✅ colorIndex in pipeline | ✅ `person_v*` generated | ❌ BootScene/engine mismatch |
| P0-5 Style | Partial | — | PM review |

---

## What to Do Next

**To see changes soonest:**

1. Run `npm run export-building-sprites` — you'll at least see buildings, roads, and park_2d (if park_2d renders correctly).
2. Fix P0-4 integration: BootScene loads `person_v0_walk_0`…`person_v3_walk_3`; PedestrianManager assigns each pedestrian a variant.
3. Implement P0-1 (A*) — this is the most visible behavior change.
4. Fix P0-3 road scaling if the bulge is noticeable.

**If tickets are "Done" but nothing changed:** Re-open them or add comments that implementation/regeneration is still needed.
