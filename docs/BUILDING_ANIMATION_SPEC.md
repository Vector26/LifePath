# Building Animation Design Spec (P1-1)

**Role:** Graphic Designer  
**Date:** 2025-02-27  
**Reference:** 02_GRAPHIC_DESIGNER.md — Cats Mansion aesthetic

---

## 1. Breathe Animation (Idle Scale Pulse)

Subtle, calming pulse so buildings feel alive without distracting.

| Parameter | Value | Rationale |
|-----------|-------|-----------|
| Scale min | 0.98 | Barely perceptible shrink |
| Scale max | 1.02 | Gentle swell |
| Duration (cycle) | 2.5–3.0 s | Slow, meditative |
| Easing | Sine.easeInOut | Smooth, no jerk |
| Loop | Yoyo | Seamless ping-pong |
| Stagger per building | 0–400 ms random | Avoid sync — organic feel |

**Implementation note:** Apply to building container (or sprite) as `scaleX`/`scaleY` tween with `yoyo: true`, `repeat: -1`. Use `Phaser.Tweens.Timeline` or `this.tweens.add({ targets, scaleX: [0.98, 1.02], scaleY: [0.98, 1.02], duration: 2750, ease: 'Sine.easeInOut', yoyo: true, repeat: -1, delay: Phaser.Math.Between(0, 400) })`.

**PM approval:** Subtlety level — if too strong, reduce to 0.99–1.01 and 3.5 s.

---

## 2. Particle Emitter Sprites

### 2.1 Smoke Puff

| Property | Value |
|----------|-------|
| File | `particle_smoke.png` |
| Size | 32×32 px |
| Style | Soft radial gradient, warm gray |
| Colors | Center: #EFEBE9 @ 0.3 alpha, Edge: transparent |
| Use | Workshop, fountain, castle chimney (if any) |

### 2.2 Sparkle

| Property | Value |
|----------|-------|
| File | `particle_sparkle.png` |
| Size | 24×24 px |
| Style | Soft star/burst, warm accent |
| Colors | Center: #FFB74D @ 0.8, Edge: transparent |
| Use | Library, market, fountain, tap feedback |

### 2.3 Emitter Params (SE Reference)

| Effect | Texture | Lifespan | Speed | Scale | Alpha | Emit rate |
|--------|---------|----------|-------|-------|-------|-----------|
| Smoke | particle_smoke | 800–1200 ms | 20–40 px/s up | 0.5→0.2 | 0.4→0 | 1 per 400 ms |
| Sparkle | particle_sparkle | 300–500 ms | 10–20 px/s up | 0.6→0.2 | 0.9→0 | 1 per 150 ms (tap) |

---

## 3. Asset Paths

- `web/public/assets/particles/particle_smoke.png`
- `web/public/assets/particles/particle_sparkle.png`

**Export:** `npm run export-particles` (from `web/`) — uses `render-particles.mjs`

---

## 4. Handoff to SE

1. Load particle textures in BootScene.
2. Add breathe tween to each building container after placement (or in `createBuildingVisual` flow).
3. Add particle emitters per building type (workshop → smoke; library/market/fountain → sparkles; tap → burst sparkles).
4. Ensure no performance impact — cap active emitters, disable when off-screen if needed.
