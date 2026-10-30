# Sound Design Spec (LP-32, P1-3)

## Overview

Warm, cozy audio for LifePath City — ambient BGM loop and placement SFX. Aligns with Cats Mansion / Pocket Build aesthetic.

---

## 1. Background Music (BGM)

### Requirements

| Attribute | Spec |
|-----------|------|
| **Mood** | Warm, calm, cozy — like a small town at golden hour |
| **Style** | Acoustic or soft synth, no aggressive beats |
| **Loop** | Seamless loop, 60–120s duration |
| **Format** | OGG (primary), MP3 fallback |
| **Volume** | Default 0.4–0.5, user-adjustable |

### Reference Games

- **Cats Mansion** — soft, loopable town music
- **Pocket Build** — gentle, non-intrusive
- **Tsuki's Odyssey** — acoustic, folky undertones

### Sourcing

- **Kenney Music Jingles** (CC0) — short jingles; may need extended/looped variant
- **OpenGameArt** — search "ambient", "chill", "village", "town" (check license)
- **Freesound** — search "ambient loop", "cozy", "village" (CC0/CC-BY)
- **Custom** — if nothing fits, commission a 60s loop

### File Location

```
web/public/assets/audio/
  bgm_city.ogg
  bgm_city.mp3   # fallback
```

---

## 2. Placement SFX

### Building Place

| Attribute | Spec |
|-----------|------|
| **Feel** | Satisfying “pop” or soft thud — object settling into place |
| **Duration** | 0.2–0.4s |
| **Pitch** | Mid-range, warm (avoid tinny) |
| **Format** | OGG preferred |

### Road Place

| Attribute | Spec |
|-----------|------|
| **Feel** | Lighter than building — brush, scrape, or click |
| **Duration** | 0.15–0.3s |
| **Pitch** | Slightly higher than building, still warm |
| **Format** | OGG preferred |

### Sourcing

- **Kenney Impact Sounds** (CC0) — has soft impacts
- **Kenney Interface Sounds** (CC0) — buttons, clicks
- **Kenney RPG Audio** (CC0) — placement-like SFX
- **Kenney Game Icons** — some packs include UI clicks; check audio bundles
- **OpenGameArt** — "place", "build", "click" (check license)

### File Locations

```
web/public/assets/audio/
  sfx_building_place.ogg
  sfx_road_place.ogg
```

---

## 3. Integration Points (for SE)

### Phaser BootScene

Add to `preload()`:

```js
// BGM
this.load.audio('bgm_city', [
  '/assets/audio/bgm_city.ogg',
  '/assets/audio/bgm_city.mp3'
]);

// SFX
this.load.audio('sfx_building_place', '/assets/audio/sfx_building_place.ogg');
this.load.audio('sfx_road_place', '/assets/audio/sfx_road_place.ogg');
```

### Playback Triggers

| Event | Action |
|-------|--------|
| City scene starts | Start BGM (loop, volume 0.4) |
| Building placed (PLACEMENT_CONFIRMED / BUILDING_PLACED) | Play `sfx_building_place` |
| Road placed (ROAD_PLACED) | Play `sfx_road_place` |

### User Preferences

- Mute toggle (master or BGM/SFX separate)
- Volume sliders if time permits

---

## 4. Deliverables Summary

| Asset | Status | Source |
|-------|--------|--------|
| BGM loop | Spec complete; SE to source/download | Kenney, OGA, Freesound |
| Building place SFX | Spec complete; SE to source/download | Kenney Impact/Interface |
| Road place SFX | Spec complete; SE to source/download | Kenney Impact/Interface |

GD handoff: This spec defines requirements and integration. SE implements loading, playback, and optionally sources the actual files per the recommendations above.
