# Road Rendering Audit (LP-9, P0-3)

## Summary

All 6 road PNGs audited and re-rendered with 2:1 W:H diamond to match `TILE_W:TILE_H = 64:32`.

## Changes

### 1. Renderer (`renderer.html`)

- Added `aspect` query param. When `aspect=2`, orthographic camera uses 2:1 frustum (`frustumHalfH = frustumHalfW / 2`) so rendered content fits a diamond shape within 512×512.
- Roads now render with `?aspect=2`.

### 2. Export (`run.mjs`)

- Road items pass `{ aspect: 2 }` to `toRenderUrl()`.
- All 6 roads re-exported: `road_straight`, `road_corner`, `road_corner_curved`, `road_junction`, `road_tsplit`, `road_straight_crossing`.

### 3. CityScene (`CityScene.ts`)

- Road sprite scaling updated from uniform `setScale(s)` to non-uniform `setScale(scaleX, scaleY)`:
  - `scaleX = (TILE_W / frame.width) * 1.1`
  - `scaleY = (TILE_H / frame.height) * 1.1`
- Maps 512×512 texture to 64×32 tile diamond correctly.

## Verification

| Road                    | Canvas | Content Shape | Status |
|-------------------------|--------|---------------|--------|
| road_straight           | 512×512| 2:1 diamond   | OK     |
| road_corner             | 512×512| 2:1 diamond   | OK     |
| road_corner_curved      | 512×512| 2:1 diamond   | OK     |
| road_junction           | 512×512| 2:1 diamond   | OK     |
| road_tsplit             | 512×512| 2:1 diamond   | OK     |
| road_straight_crossing  | 512×512| 2:1 diamond   | OK     |

## Cross-Role Sign-off

- **Designer**: Sprite specs confirmed — 512×512 canvas, 2:1 content, transparent background.
- **Physics/Placement**: TILE_W=64, TILE_H=32; `gridToIso` and `drawTileHighlight` unchanged; road scaling maps sprite to tile diamond.
