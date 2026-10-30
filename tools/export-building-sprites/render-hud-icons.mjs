#!/usr/bin/env node
/**
 * Renders economy HUD icons (coin, XP, level) for LifePath City.
 * 32×32px, warm palette per Cats Mansion aesthetic.
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
const OUT = join(__dirname, '..', '..', 'web', 'public', 'assets', 'hud');

// Warm palette from Cats Mansion spec
const HUD_HTML = `<!DOCTYPE html><html><head><meta charset="utf-8"></head><body>
<canvas id="c"></canvas>
<script>
(function() {
  const params = new URLSearchParams(location.search);
  const gen = params.get('gen');
  if (!gen) { window.__SPRITE_ERROR__ = 'Missing gen'; return; }
  const size = 32;
  const c = document.createElement('canvas');
  c.width = c.height = size;
  const ctx = c.getContext('2d');
  const cx = size / 2;
  const cy = size / 2;

  if (gen === 'icon_coin') {
    const r = 12;
    const g = ctx.createRadialGradient(cx - 3, cy - 2, 0, cx, cy, r);
    g.addColorStop(0, '#FFB74D');
    g.addColorStop(0.5, '#FFA726');
    g.addColorStop(1, '#FF9800');
    ctx.fillStyle = g;
    ctx.beginPath();
    ctx.ellipse(cx, cy, r, r * 0.92, 0, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = '#E65100';
    ctx.lineWidth = 1.5;
    ctx.stroke();
    ctx.strokeStyle = '#E65100';
    ctx.lineWidth = 2;
    ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.ellipse(cx, cy, 6, 5, 0.2, Math.PI * 0.6, Math.PI * 2.4);
    ctx.stroke();
  } else if (gen === 'icon_xp') {
    const r = 12;
    const g = ctx.createRadialGradient(cx, cy, 0, cx, cy, r);
    g.addColorStop(0, '#FFB74D');
    g.addColorStop(0.6, '#FF8F00');
    g.addColorStop(1, 'rgba(255, 143, 0, 0)');
    ctx.fillStyle = g;
    ctx.beginPath();
    ctx.moveTo(cx, cy - r);
    for (let i = 1; i <= 4; i++) {
      const angle = (i * Math.PI * 2) / 4 - Math.PI / 2;
      ctx.lineTo(cx + Math.cos(angle) * r, cy + Math.sin(angle) * r);
    }
    ctx.closePath();
    ctx.fill();
    ctx.strokeStyle = '#E65100';
    ctx.lineWidth = 1;
    ctx.stroke();
    ctx.fillStyle = '#FFB74D';
    ctx.beginPath();
    ctx.moveTo(cx, cy - 6);
    ctx.lineTo(cx + 5, cy);
    ctx.lineTo(cx, cy + 6);
    ctx.lineTo(cx - 5, cy);
    ctx.closePath();
    ctx.fill();
    ctx.stroke();
  } else if (gen === 'icon_level') {
    const w = 20;
    const h = 16;
    const x = cx - w / 2;
    const y = cy - h / 2;
    const rad = 4;
    ctx.fillStyle = '#FFB74D';
    ctx.beginPath();
    ctx.roundRect(x, y, w, h, rad);
    ctx.fill();
    ctx.strokeStyle = '#E65100';
    ctx.lineWidth = 1;
    ctx.stroke();
    ctx.fillStyle = '#E65100';
    ctx.beginPath();
    ctx.moveTo(cx, cy - 4);
    ctx.lineTo(cx + 5, cy + 3);
    ctx.lineTo(cx + 2, cy + 3);
    ctx.lineTo(cx + 2, cy + 6);
    ctx.lineTo(cx - 2, cy + 6);
    ctx.lineTo(cx - 2, cy + 3);
    ctx.lineTo(cx - 5, cy + 3);
    ctx.closePath();
    ctx.fill();
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
    res.end(HUD_HTML);
  });
  await new Promise((r) => server.listen(0, '127.0.0.1', r));
  const port = server.address().port;
  const browser = await puppeteer.launch({ headless: true, args: ['--no-sandbox'] });
  try {
    for (const name of ['icon_coin', 'icon_xp', 'icon_level']) {
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
    console.log('HUD icons (coin, XP, level) written.');
  } finally {
    await browser.close();
    server.close();
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
