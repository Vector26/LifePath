# Grass & Ground Tiles Spec (P1-E)

**Role:** Graphic Designer  
**Date:** 2025-02-27  
**Reference:** 02_GRAPHIC_DESIGNER.md, 03_PHYSICS_GAME_ENGINEER.md §2.1

---

## Asset Summary

| Asset | Palette | Size | Content |
|-------|---------|------|---------|
| tile_grass.png | #7CB342 (base), #8BC34A (highlight), #689F38 (shadow) | 512×512 | Isometric diamond, 2:1 aspect |
| tile_water.png | #81D4FA (base), #90CAF9 (highlight), #4FC3F7 (shadow) | 512×512 | Isometric diamond, 2:1 aspect |

---

## Spec Details

### Grass Tile
- Warm green gradient (top lighter, bottom darker) for depth
- Subtle dark edge stroke (#5D8A2F)
- Diamond matches isometric grid tile shape
- Transparent outside diamond

### Water/Pond Tile
- Light blue gradient (#90CAF9 → #81D4FA → #4FC3F7)
- Edge stroke (#29B6F6)
- Same diamond geometry as grass
- For ponds, water features, future water tiles

---

## Export

```bash
cd web && node ../tools/export-building-sprites/render-tiles.mjs
```

Output: `web/public/assets/tiles/tile_grass.png`, `tile_water.png`

---

## Handoff to SE

1. Load `tile_grass` and `tile_water` in BootScene (add to preload).
2. CityScene: Render grass tile for each empty grid cell (not road, not building).
3. Water: Reserve for future pond/water features or special cells.
4. Use same placement as roads: `gridToIso(gx + 0.5, gy + 0.5)`, scale to fit TILE_W×TILE_H diamond, `setOrigin(0.5, 0.5)`, depth `gy + gx - 0.5`.
