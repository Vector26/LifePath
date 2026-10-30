# Character Variant System Spec (LP-42, P1-D)

## Overview

4 shirt color variants (dusty rose, sage, sky blue, warm orange) with walk and idle poses. All sprites share the same isometric camera angle.

## Color Variants

| Variant | Color     | Hex     |
|---------|-----------|---------|
| v0      | dusty rose| #E8967A |
| v1      | sage      | #9CAF88 |
| v2      | sky blue  | #81D4FA |
| v3      | warm orange| #FFB74D |

## Assets

### Walk (4 frames × 4 variants = 16)

- `person_v0_walk_0..3.png` — dusty rose
- `person_v1_walk_0..3.png` — sage
- `person_v2_walk_0..3.png` — sky blue
- `person_v3_walk_0..3.png` — warm orange

### Idle (2 poses × 4 variants = 8)

- `person_v0..3_idle_shop.png` — standing at shop
- `person_v0..3_idle_bench.png` — sitting on bench

### Fallback (single color)

- `person_walk_0..3.png` — v0 for backward compatibility

## BootScene Keys

All loaded under `/assets/people/`:
- `person_walk_0..3`, `person_v0..3_walk_0..3`, `person_v0..3_idle_shop`, `person_v0..3_idle_bench`

## PedestrianManager

- Each pedestrian gets `variant = random(0..3)` at spawn.
- Uses `person_v${variant}_walk_${frame}` for walk animation.
- Falls back to `person_walk_${frame}` if variant texture missing.

## Idle Integration (SE)

Idle poses (`person_vN_idle_shop`, `person_vN_idle_bench`) are loaded and available. Use when:
- Citizen reaches shop destination → show idle_shop
- Citizen reaches bench (e.g. park) → show idle_bench

## Export

```bash
cd web && npm run export-building-sprites
```
