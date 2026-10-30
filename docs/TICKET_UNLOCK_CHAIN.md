# Ticket Unlock Chain — GD → SE → DevOps

**Purpose:** When a GD ticket is Done, the SE agent should pick the corresponding SE ticket. When that SE ticket is Done, the DevOps agent should pick the DevOps ticket.

**Location:** `docs/TICKET_UNLOCK_CHAIN.md`  
**API:** `internal agent guide (not in this repo)`

---

## Unlock Rules

| Your Role | When You Can Start | Check This |
|-----------|--------------------|------------|
| **GD** | Immediately (no blocker) | — |
| **SE** | GD sibling (same parent) is **Done**, OR no GD sibling exists | `Blocked by` ticket state |
| **DevOps** | SE sibling (same parent) is **Done** | `Blocked by` ticket state |

---

## EPIC Chain Table (Feature → GD → SE → DevOps)

Each row is one EPIC. SE picks the SE ticket when GD in that row is Done. DevOps picks the DevOps ticket when SE in that row is Done.

| Feature / EPIC | GD (Blocked by: none) | SE (Blocked by: GD) | DevOps (Blocked by: SE) |
|----------------|----------------------|---------------------|-------------------------|
| **P0-1** Citizen A* Pathfinding | — *no GD* | LP-2 ✓ *unlocked* | LP-3 |
| **P0-2** Park Asset Replacement | LP-5 | LP-6 | LP-7 |
| **P0-3** Road Rendering Fix | LP-9 | LP-10 | LP-11 |
| **P0-4** Shirt Color Flashing Fix | LP-13 | LP-14 | LP-15 |
| **P0-5** Visual Style Pass | LP-17 | LP-18 | LP-19 |
| **P1-A** Depth & Z-Sorting | — *no GD* | LP-21 ✓ *unlocked* | LP-22 |
| **P1-1** Building Animations | LP-24 | LP-25 | LP-26 |
| **P1-2** Economy Loop | LP-28 | LP-29 | LP-30 |
| **P1-3** Sound Design | LP-32 | LP-33 | LP-34 |
| **P1-B** Dynamic Road Graph | — *no GD* | LP-36 ✓ *unlocked* | LP-37 |
| **P1-C** Citizen Rendering Integration | — *no GD* | LP-39 ✓ *unlocked* | LP-40 |
| **P1-D** Character Variant System | LP-42 | LP-43 | LP-44 |
| **P1-E** Grass & Ground Tiles | LP-46 | LP-47 | LP-48 |
| **P1-F** BridgeAdapter Hardening | — *no GD* | LP-50 ✓ *unlocked* | LP-51 |
| **P1-G** State Management Audit | — *no GD* | LP-53 ✓ *unlocked* | LP-54 |
| **P1-H** Asset Pipeline Robustness | — *no GD* | LP-56 ✓ *unlocked* | LP-57 |
| **P2-1** Day/Night Cycle | LP-59 | LP-60 | — *no DevOps* |
| **LP-77** Road Adjacency & Orientation | LP-78 | LP-79 | LP-80 |
| **LP-81** Governor Building Asset Fix | LP-82 | LP-83 | LP-84 |

---

## Copy-Paste Handoff Prompts

**When GD ticket is Done → tell SE agent:**

| GD Done | Prompt to give SE agent |
|---------|-------------------------|
| LP-5 | GD ticket LP-5 (Park Asset Replacement) is Done. Pick the corresponding SE ticket and start work. |
| LP-9 | GD ticket LP-9 (Road Rendering Fix) is Done. Pick LP-10 and start work. |
| LP-13 | GD ticket LP-13 (Shirt Color Flashing Fix) is Done. Pick LP-14 and start work. |
| LP-17 | GD ticket LP-17 (Visual Style Pass) is Done. Pick LP-18 and start work. |
| LP-24 | GD ticket LP-24 (Building Animations) is Done. Pick LP-25 and start work. |
| LP-28 | GD ticket LP-28 (Economy Loop) is Done. Pick LP-29 and start work. |
| LP-32 | GD ticket LP-32 (Sound Design) is Done. Pick LP-33 and start work. |
| LP-42 | GD ticket LP-42 (Character Variant System) is Done. Pick LP-43 and start work. |
| LP-46 | GD ticket LP-46 (Grass & Ground Tiles) is Done. Pick LP-47 and start work. |
| LP-59 | GD ticket LP-59 (Day/Night Cycle) is Done. Pick LP-60 and start work. |
| LP-78 | GD ticket LP-78 (Road Adjacency Design) is Done. Pick LP-79 and start work. |
| LP-82 | GD ticket LP-82 (Governor Building Asset & Naming) is Done. Pick LP-83 and start work. |

**When SE ticket is Done → tell DevOps agent:**

| SE Done | Prompt to give DevOps agent |
|---------|-----------------------------|
| LP-2 | SE ticket LP-2 (Citizen A* Pathfinding) is Done. Pick LP-3 and run tests. |
| LP-6 | SE ticket LP-6 (Park Asset Replacement) is Done. Pick LP-7 and run tests. |
| LP-10 | SE ticket LP-10 (Road Rendering Fix) is Done. Pick LP-11 and run tests. |
| LP-14 | SE ticket LP-14 (Shirt Color Flashing Fix) is Done. Pick LP-15 and run tests. |
| LP-18 | SE ticket LP-18 (Visual Style Pass) is Done. Pick LP-19 and run tests. |
| LP-21 | SE ticket LP-21 (Depth & Z-Sorting) is Done. Pick LP-22 and run tests. |
| LP-25 | SE ticket LP-25 (Building Animations) is Done. Pick LP-26 and run tests. |
| LP-29 | SE ticket LP-29 (Economy Loop) is Done. Pick LP-30 and run tests. |
| LP-33 | SE ticket LP-33 (Sound Design) is Done. Pick LP-34 and run tests. |
| LP-36 | SE ticket LP-36 (Dynamic Road Graph) is Done. Pick LP-37 and run tests. |
| LP-39 | SE ticket LP-39 (Citizen Rendering) is Done. Pick LP-40 and run tests. |
| LP-43 | SE ticket LP-43 (Character Variant) is Done. Pick LP-44 and run tests. |
| LP-47 | SE ticket LP-47 (Grass & Ground) is Done. Pick LP-48 and run tests. |
| LP-50 | SE ticket LP-50 (BridgeAdapter) is Done. Pick LP-51 and run tests. |
| LP-53 | SE ticket LP-53 (State Management) is Done. Pick LP-54 and run tests. |
| LP-56 | SE ticket LP-56 (Asset Pipeline) is Done. Pick LP-57 and run tests. |
| LP-79 | SE ticket LP-79 (Road Adjacency Implementation) is Done. Pick LP-80 and run tests. |
| LP-83 | SE ticket LP-83 (Governor Building Integration) is Done. Pick LP-84 and run tests. |

**Generic prompt (agent looks up in chain table):**
> "[Role] ticket LP-X is Done. Pick the corresponding [next role] ticket from TICKET_UNLOCK_CHAIN.md and start work."

---

## API Query: Find Unlocked Tickets for Your Role

```bash
# 1. Get all work items with parent (children)
# 2. For each child, get siblings via same parent
# 3. If SE: only claim if (no GD sibling) OR (GD sibling state == completed)
# 4. If DevOps: only claim if (no SE sibling) OR (SE sibling state == completed)
```

**States:** `6a83ef37-e45d-4900-b311-156d6855ded5` = Done (completed)

**Example — list SE tickets whose GD sibling is Done:**
- GET work-items with label SE
- For each: GET parent's children (or filter work-items by parent=X)
- If any sibling has label GD and state ≠ Done → skip
- Else → ticket is unlocked
