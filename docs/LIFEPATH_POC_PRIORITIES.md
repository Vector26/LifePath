# LifePath POC — Priority & Assignment Guide

**Board:** http://localhost:8082/life-path/projects/8e0eabe0-d3a8-4429-ad73-f156c7b6ab1f/issues/  
**Generated:** 2026-02-27  
**Plane priorities:** Updated — P0 urgent/high, P1 medium, P2 low.

---

## 1. What a Working POC Requires

A **minimum viable demo** of LifePath City needs:

| Requirement | Why |
|-------------|-----|
| Citizens walk on roads using A* | Core "alive city" feel — no random jitter |
| Park has a real asset (not green cylinder) | Visual coherence — matches other buildings |
| Roads render flat (no Y bulge) | Isometric grid reads correctly |
| No shirt color flashing | Visual polish — no strobe effect |
| Visual style matches Cats Mansion | Product vision — cozy, illustrated 2.5D |

**POC = all P0 items done.** No feature work until these ship-blockers are fixed.

---

## 2. Priority Stack (PM §2.4)

### P0 — Ship-Blocking (Do First)

| ID | Priority | Task | Owner | Plane Ref |
|----|----------|------|-------|-----------|
| P0-1 | **urgent** | Citizen A* pathfinding on roads | Physics Engineer | LP-1 (Feature), LP-2 (SE), LP-3 (DevOps) |
| P0-2 | **urgent** | Park asset replacement (green cylinder → sprite) | Graphic Designer + SWE | LP-4, LP-5, LP-6, LP-7 |
| P0-3 | **urgent** | Road Y-axis bulge fix (flat rendering) | Physics Engineer | LP-8, LP-9, LP-10, LP-11 |
| P0-4 | **high** | Shirt color flashing fix | Senior SWE + Designer | LP-12, LP-13, LP-14, LP-15 |
| P0-5 | **high** | Visual style pass (Cats Mansion standard) | Graphic Designer + PM | LP-16, LP-17, LP-18, LP-19 |

### P1 — Core Experience (After P0)

| ID | Task | Owner |
|----|------|-------|
| P1-1 | Building animations (breathe, float, emitters) | GD → SE → DevOps |
| P1-2 | Economy loop (coins, XP, unlocks) | GD → SE → DevOps |
| P1-3 | Sound design | GD → SE → DevOps |
| P1-A | Depth & Z-sorting system | SE → DevOps |
| P1-B | Dynamic road graph | SE → DevOps |
| P1-C | Citizen rendering integration | SE → DevOps |
| P1-D | Character variant system | GD → SE → DevOps |
| P1-E | Grass & ground tiles | GD → SE → DevOps |
| P1-F | BridgeAdapter hardening | SE → DevOps |
| P1-G | State management audit | SE → DevOps |
| P1-H | Asset pipeline robustness | SE → DevOps |

### P2 — Polish (Later)

| ID | Task |
|----|------|
| P2-1 | Day/night cycle |

---

## 3. Role Assignment (Prime Order)

Per `LIFEPATH_AGENT_GUIDE.md` §10 — **respect order**: GD → SE → DevOps.

| Role | Label ID | Filter tickets by |
|------|----------|--------------------|
| Game Designer | `86717885-331a-44c9-a686-60e71669ad28` | `[GD]` prefix |
| Software Engineer | `b1dae36e-dac9-4f53-8158-9d9ce2df7f9a` | `[SE]` prefix |
| DevOps | `7134143f-296d-41ba-a572-f1bf73fc777b` | `[DevOps]` prefix |

---

## 4. POC Execution Order

1. **P0-1** (Physics) — A* in `PedestrianManager.ts`, road graph in `CityScene.ts`
2. **P0-2** (Design + SWE) — Replace `buildProceduralPark()` output or source OBJ
3. **P0-3** (Physics) — Fix `gridToIso`, road sprite scaling in `CityScene.ts`
4. **P0-4** (SWE + Design) — Pass `colorIndex` in `run.mjs` → `renderer.html`
5. **P0-5** (Design + PM) — Aesthetic review, pastel palette enforcement

**Dependencies:**
- P0-2 and P0-4 touch the asset pipeline (`run.mjs`, `renderer.html`)
- P0-1 and P0-3 touch the engine (`CityScene.ts`, `PedestrianManager.ts`)
- Can parallelize: (P0-1, P0-3) vs (P0-2, P0-4), then P0-5 last

---

## 5. Plane.io Priority Mapping

| Plane Value | Use For |
|-------------|---------|
| `urgent` | P0-1, P0-2, P0-3 |
| `high` | P0-4, P0-5 |
| `medium` | P1 items |
| `low` | P2 items |
| `none` | Unprioritized / backlog |

---

## 6. Acceptance Criteria (PM §5)

Before marking P0 done:

1. **Screenshot Test** — Looks like Cats Mansion? Y/N  
2. **Behavior Test** — Do citizens follow roads? Y/N  
3. **No Regressions** — `npm test` + `node verify_sprites.mjs` pass  
4. **Performance** — 60fps on mobile viewport
