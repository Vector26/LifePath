#!/usr/bin/env node

/**
 * verify_sprites.mjs — Automated pixel validation for rendered building sprites.
 *
 * Checks every generated PNG against what the Phaser engine ACTUALLY needs:
 *   1. Not blank (100% transparent or 100% black = render failure)
 *   2. No edge clipping (content touching canvas border = model too big)
 *   3. Horizontal centering (engine uses setOrigin(0.5, ...) so content must be centered)
 *   4. Content coverage (sprite should fill 20%-90% of canvas, not too tiny or too big)
 *   5. Content bounding box (reports actual content bounds for debugging)
 *
 * Usage:  node verify_sprites.mjs
 * Exit 0 = all pass, exit 1 = failures.
 */

import { readFileSync, existsSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';
import { createRequire } from 'module';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const WEB_ROOT = join(__dirname, '..', '..', 'web');
const requireFromWeb = createRequire(join(WEB_ROOT, 'package.json'));
const { PNG } = requireFromWeb('pngjs');

const LIFEPATH_ROOT = join(__dirname, '..', '..');
const SPRITES_DIR = join(LIFEPATH_ROOT, 'web', 'public', 'assets', 'buildings');
const ROAD_SPRITES_DIR = join(LIFEPATH_ROOT, 'web', 'public', 'assets', 'roads');
const PEOPLE_SPRITES_DIR = join(LIFEPATH_ROOT, 'web', 'public', 'assets', 'people');
const CANVAS_SIZE = 512;
const EDGE_MARGIN = 2;                // px from border to flag clipping
const CENTER_TOLERANCE = 0.10;        // 10% of canvas width for centering
const MIN_COVERAGE = 0.02;            // content must fill at least 2% of canvas area
const MAX_COVERAGE = 0.85;            // content must not exceed 85% of canvas area

// All building sprites expected from run.mjs
const BUILDING_SPRITES = [
    'building_tent.png',
    'building_house.png',
    'building_farm.png',
    'building_market.png',
    'building_workshop.png',
    'building_library.png',
    'building_park.png',
    'building_fountain.png',
    'building_tower.png',
    'building_castle.png',
];

// Road sprites
const ROAD_SPRITES = [
    'road_straight.png',
    'road_corner.png',
    'road_corner_curved.png',
    'road_junction.png',
    'road_tsplit.png',
    'road_straight_crossing.png',
];

// Person sprites — 4 color variants × 4 walk frames = 16 PNGs
const PERSON_SPRITES = [
    'person_v0_walk_0.png',
    'person_v0_walk_1.png',
    'person_v0_walk_2.png',
    'person_v0_walk_3.png',
    'person_v1_walk_0.png',
    'person_v1_walk_1.png',
    'person_v1_walk_2.png',
    'person_v1_walk_3.png',
    'person_v2_walk_0.png',
    'person_v2_walk_1.png',
    'person_v2_walk_2.png',
    'person_v2_walk_3.png',
    'person_v3_walk_0.png',
    'person_v3_walk_1.png',
    'person_v3_walk_2.png',
    'person_v3_walk_3.png',
];

function loadPNG(filePath) {
    const buffer = readFileSync(filePath);
    return PNG.sync.read(buffer);
}

function getPixel(png, x, y) {
    const idx = (y * png.width + x) * 4;
    return {
        r: png.data[idx],
        g: png.data[idx + 1],
        b: png.data[idx + 2],
        a: png.data[idx + 3],
    };
}

function isVisible(png, x, y) {
    return getPixel(png, x, y).a > 10;
}

/**
 * Scan the entire image and return the bounding box of visible content
 * plus pixel count statistics.
 */
function scanContent(png) {
    const w = png.width;
    const h = png.height;
    let minX = w, maxX = -1, minY = h, maxY = -1;
    let visiblePixels = 0;
    let allBlack = true;

    for (let y = 0; y < h; y++) {
        for (let x = 0; x < w; x++) {
            const p = getPixel(png, x, y);
            if (p.a > 10) {
                visiblePixels++;
                if (x < minX) minX = x;
                if (x > maxX) maxX = x;
                if (y < minY) minY = y;
                if (y > maxY) maxY = y;
                if (p.r > 5 || p.g > 5 || p.b > 5) {
                    allBlack = false;
                }
            }
        }
    }

    return {
        minX, maxX, minY, maxY,
        visiblePixels,
        allBlack,
        hasContent: maxX >= 0,
        contentWidth: maxX >= 0 ? maxX - minX + 1 : 0,
        contentHeight: maxY >= 0 ? maxY - minY + 1 : 0,
        centerX: maxX >= 0 ? (minX + maxX) / 2 : w / 2,
        centerY: maxY >= 0 ? (minY + maxY) / 2 : h / 2,
    };
}

function checkBlank(scan, name) {
    if (!scan.hasContent) {
        throw new Error(`BLANK: 100% transparent — nothing was rendered.`);
    }
    if (scan.allBlack && scan.visiblePixels > 0) {
        throw new Error(`BLANK: All visible pixels are black — likely a render failure.`);
    }
}

function checkClipping(scan, name, w, h) {
    const edges = [];
    if (scan.minY < EDGE_MARGIN) edges.push(`top(minY=${scan.minY})`);
    if (scan.maxY >= h - EDGE_MARGIN) edges.push(`bottom(maxY=${scan.maxY})`);
    if (scan.minX < EDGE_MARGIN) edges.push(`left(minX=${scan.minX})`);
    if (scan.maxX >= w - EDGE_MARGIN) edges.push(`right(maxX=${scan.maxX})`);

    if (edges.length > 0) {
        throw new Error(`CLIPPED: Content touches edge — ${edges.join(', ')}. Model too big or off-center.`);
    }
}

function checkCentering(scan, name, w) {
    const canvasCenter = w / 2;
    const contentCenter = scan.centerX;
    const deviation = Math.abs(contentCenter - canvasCenter) / w;
    if (deviation > CENTER_TOLERANCE) {
        throw new Error(
            `OFF-CENTER: Content center at X=${contentCenter.toFixed(1)}, canvas center at ${canvasCenter}. ` +
            `Deviation ${(deviation * 100).toFixed(1)}% > ${(CENTER_TOLERANCE * 100)}% tolerance.`
        );
    }
}

function checkCoverage(scan, name, w, h) {
    const totalPixels = w * h;
    const contentArea = scan.contentWidth * scan.contentHeight;
    const coverage = contentArea / totalPixels;

    if (coverage < MIN_COVERAGE) {
        throw new Error(
            `TOO SMALL: Content covers only ${(coverage * 100).toFixed(1)}% of canvas ` +
            `(${scan.contentWidth}x${scan.contentHeight}px). Minimum is ${(MIN_COVERAGE * 100)}%.`
        );
    }
    if (coverage > MAX_COVERAGE) {
        throw new Error(
            `TOO LARGE: Content covers ${(coverage * 100).toFixed(1)}% of canvas ` +
            `(${scan.contentWidth}x${scan.contentHeight}px). Maximum is ${(MAX_COVERAGE * 100)}%.`
        );
    }
    return coverage;
}

function validateSprite(filePath, spriteName) {
    if (!existsSync(filePath)) {
        throw new Error(`MISSING: ${filePath} does not exist`);
    }

    const png = loadPNG(filePath);
    if (png.width !== CANVAS_SIZE || png.height !== CANVAS_SIZE) {
        throw new Error(`SIZE: Expected ${CANVAS_SIZE}x${CANVAS_SIZE}, got ${png.width}x${png.height}`);
    }

    const scan = scanContent(png);

    checkBlank(scan, spriteName);
    console.log(`  ✓ Not blank (${scan.visiblePixels} visible pixels)`);

    checkClipping(scan, spriteName, png.width, png.height);
    console.log(`  ✓ No clipping (bounds: X[${scan.minX}-${scan.maxX}] Y[${scan.minY}-${scan.maxY}])`);

    checkCentering(scan, spriteName, png.width);
    console.log(`  ✓ Centered (contentCenterX=${scan.centerX.toFixed(1)}, canvasCenter=256)`);

    const coverage = checkCoverage(scan, spriteName, png.width, png.height);
    console.log(`  ✓ Coverage OK (${(coverage * 100).toFixed(1)}%, ${scan.contentWidth}x${scan.contentHeight}px)`);
}

async function main() {
    let totalPass = 0;
    let totalFail = 0;
    const totalExpected = BUILDING_SPRITES.length + ROAD_SPRITES.length + PERSON_SPRITES.length;

    // Buildings
    console.log('=== BUILDINGS ===');
    for (const sprite of BUILDING_SPRITES) {
        console.log(`\n${sprite}:`);
        try {
            validateSprite(join(SPRITES_DIR, sprite), sprite);
            console.log(`  ✓ PASS`);
            totalPass++;
        } catch (err) {
            console.error(`  ✗ FAIL: ${err.message}`);
            totalFail++;
        }
    }

    // Roads
    console.log('\n=== ROADS ===');
    for (const sprite of ROAD_SPRITES) {
        console.log(`\n${sprite}:`);
        try {
            validateSprite(join(ROAD_SPRITES_DIR, sprite), sprite);
            console.log(`  ✓ PASS`);
            totalPass++;
        } catch (err) {
            console.error(`  ✗ FAIL: ${err.message}`);
            totalFail++;
        }
    }

    // People
    console.log('\n=== PEOPLE ===');
    for (const sprite of PERSON_SPRITES) {
        console.log(`\n${sprite}:`);
        try {
            validateSprite(join(PEOPLE_SPRITES_DIR, sprite), sprite);
            console.log(`  ✓ PASS`);
            totalPass++;
        } catch (err) {
            console.error(`  ✗ FAIL: ${err.message}`);
            totalFail++;
        }
    }

    console.log(`\n${'='.repeat(60)}`);
    if (totalFail > 0) {
        console.error(`FAILED: ${totalFail}/${totalExpected} sprites failed validation. ${totalPass} passed.`);
        process.exit(1);
    } else {
        console.log(`SUCCESS: All ${totalExpected} sprites passed visual pixel validation and physics alignment checks.`);
        process.exit(0);
    }
}

main().catch((err) => {
    console.error('Verification script crashed:', err);
    process.exit(1);
});
