# Economy HUD Icons Spec (LP-28, P1-2)

## Overview

Coin, XP, and level HUD icons for the economy loop. Warm palette per Cats Mansion aesthetic.

## Assets

| Icon       | File                | Size  | Usage                     |
|------------|---------------------|-------|---------------------------|
| Coin       | `icon_coin.png`     | 32×32 | Currency (coins) HUD, shop cost |
| XP         | `icon_xp.png`      | 32×32 | XP bar label              |
| Level      | `icon_level.png`    | 32×32 | Level badge               |

**Location:** `web/public/assets/hud/`

## Palette

- **Coin:** `#FFB74D` → `#FFA726` → `#FF9800` gradient, edge `#E65100`
- **XP:** `#FFB74D`, `#FF8F00`, edge `#E65100` (star/diamond)
- **Level:** Fill `#FFB74D`, chevron `#E65100`

## Export

```bash
cd web && npm run export-hud-icons
```

## Integration

- **CityView.tsx:** Level badge, XP row, currency items, shop card cost
- **CityView.module.css:** `.hudIconSmall`, `.currencyIconImg`, `.xpRow`, `.shopCardCoin`
