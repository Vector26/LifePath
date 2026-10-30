#!/usr/bin/env node
/**
 * Renders grass and water ground tiles for LifePath City.
 * Isometric diamond (2:1 aspect), 512×512 canvas, warm palette.
 */

import { mkdirSync, writeFileSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';
import { createServer } from 'http';
import { createRequire } from 'module';

const __dirname = dirname(fileURLToPath(import.meta.url));
const WEB_ROOT = join(__dirname, '..', '..', 'web');
const requireFromWeb = createRequire(join(WEB_ROOT, 'package.json'));
const puppeteer = requireFromWeb('puppeteer');
const OUT = join(__dirname, '..', '..', 'web', 'public', 'assets', 'tiles');

const TILE_HTML = `<!DOCTYPE html><html><head><meta charset="utf-8"></head><body>
<canvas id="c"></canvas>
<script>
(function() {
  const params = new URLSearchParams(location.search);
  const gen = params.get('gen');
  if (!gen) { window.__SPRITE_ERROR__ = 'Missing gen'; return; }
  const size = 512;
  const c = document.createElement('canvas');
  c.width = c.height = size;
  const ctx = c.getContext('2d');
  const cx = size / 2;
  const cy = size / 2;
  // Isometric diamond: 2:1 aspect (w=512, h=256 effective), centered
  const halfW = 256;
  const halfH = 128;
  const top = [cx, cy - halfH];
  const right = [cx + halfW, cy];
  const bottom = [cx, cy + halfH];
  const left = [cx - halfW, cy];
  const drawDiamond = function() {
    ctx.beginPath();
    ctx.moveTo(top[0], top[1]);
    ctx.lineTo(right[0], right[1]);
    ctx.lineTo(bottom[0], bottom[1]);
    ctx.lineTo(left[0], left[1]);
    ctx.closePath();
  };
  drawDiamond();
  if (gen === 'tile_grass') {
    const g = ctx.createLinearGradient(cx, cy - halfH, cx, cy + halfH);
    g.addColorStop(0, '#8BC34A');
    g.addColorStop(0.5, '#7CB342');
    g.addColorStop(1, '#689F38');
    ctx.fillStyle = g;
    ctx.fill();
    ctx.strokeStyle = '#5D8A2F';
    ctx.lineWidth = 2;
    drawDiamond();
    ctx.stroke();
  } else if (gen === 'tile_water') {
    const g = ctx.createLinearGradient(cx, cy - halfH, cx, cy + halfH);
    g.addColorStop(0, '#90CAF9');
    g.addColorStop(0.5, '#81D4FA');
    g.addColorStop(1, '#4FC3F7');
    ctx.fillStyle = g;
    ctx.fill();
    ctx.strokeStyle = '#29B6F6';
    ctx.lineWidth = 2;
    drawDiamond();
    ctx.stroke();
  } else {
    window.__SPRITE_ERROR__ = 'Unknown gen: ' + gen;
    return;
  }
  window.__SPRITE_DATA__ = c.toDataURL('image/png');
})();
</script></body></html>`;

async function main() {
  mkdirSync(OUT, { recursive: true });
  const server = createServer((req, res) => {
    res.writeHead(200, { 'Content-Type': 'text/html' });
    res.end(TILE_HTML);
  });
  await new Promise((r) => server.listen(0, '127.0.0.1', r));
  const port = server.address().port;
  const browser = await puppeteer.launch({ headless: true, args: ['--no-sandbox'] });
  try {
    for (const name of ['tile_grass', 'tile_water']) {
      const page = await browser.newPage();
      await page.goto(`http://127.0.0.1:${port}/?gen=${name}`, { waitUntil: 'domcontentloaded' });
      await page.waitForFunction('window.__SPRITE_DATA__ || window.__SPRITE_ERROR__', { timeout: 3000 });
      const err = await page.evaluate(() => window.__SPRITE_ERROR__);
      if (err) throw new Error(`[${name}] ${err}`);
      const data = await page.evaluate(() => window.__SPRITE_DATA__);
      const b64 = data.slice(data.indexOf(',') + 1);
      writeFileSync(join(OUT, `${name}.png`), Buffer.from(b64, 'base64'));
      console.log(`  -> ${join(OUT, `${name}.png`)}`);
    }
    console.log('Grass & water tiles written.');
  } finally {
    await browser.close();
    server.close();
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
