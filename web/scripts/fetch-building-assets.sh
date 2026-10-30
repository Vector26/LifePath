#!/bin/bash
# Fetches KayKit City Builder Bits OBJ models and texture for local use.
# Run from project root: ./web/scripts/fetch-building-assets.sh
# Assets are saved to web/public/assets/buildings/

set -e
BASE_URL="https://cdn.jsdelivr.net/gh/KayKit-Game-Assets/KayKit-City-Builder-Bits-1.0@main/addons/kaykit_city_builder_bits/Assets"
OUT_DIR="$(dirname "$0")/../public/assets/buildings"
mkdir -p "$OUT_DIR"

echo "Fetching KayKit City Builder Bits assets..."

for name in building_A building_B building_C building_D building_E building_F building_G building_H; do
  echo "  $name.obj"
  curl -sSL "$BASE_URL/obj/${name}.obj" -o "$OUT_DIR/${name}.obj"
  echo "  $name.mtl"
  curl -sSL "$BASE_URL/obj/${name}.mtl" -o "$OUT_DIR/${name}.mtl" 2>/dev/null || true
done

echo "  citybits_texture.png"
curl -sSL "$BASE_URL/texture/citybits_texture.png" -o "$OUT_DIR/citybits_texture.png"

echo "Done. Assets in $OUT_DIR"
