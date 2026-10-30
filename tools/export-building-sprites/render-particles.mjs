#!/usr/bin/env node
/**
 * Renders particle emitter textures (smoke, sparkle) using a minimal HTML page.
 * Avoids Three.js/Puppeteer CDN for faster, more reliable export.
 */

import { mkdirSync, readFileSync, writeFileSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';
import { createServer } from 'http';
import { createRequire } from 'module';

const __dirname = dirname(fileURLToPath(import.meta.url));
const WEB_ROOT = join(__dirname, '..', '..', 'web');
const requireFromWeb = createRequire(join(WEB_ROOT, 'package.json'));
const puppeteer = requireFromWeb('puppeteer');
const OUT = join(__dirname, '..', '..', 'web', 'public', 'assets', 'particles');

const PARTICLE_HTML = `<!DOCTYPE html><html><head><meta charset="utf-8"></head><body>
<canvas id="c"></canvas>
<script>
(function() {
  const params = new URLSearchParams(location.search);
  const gen = params.get('gen');
  if (!gen) { window.__SPRITE_ERROR__ = 'Missing gen'; return; }
  const size = gen === 'particle_smoke' ? 32 : 24;
  const c = document.createElement('canvas');
  c.width = c.height = size;
  const ctx = c.getContext('2d');
  const cx = size / 2;
  const r = cx - 1;
  const grad = ctx.createRadialGradient(cx, cx, 0, cx, cx, r);
  if (gen === 'particle_smoke') {
    grad.addColorStop(0, 'rgba(239,235,233,0.35)');
    grad.addColorStop(0.5, 'rgba(239,235,233,0.15)');
    grad.addColorStop(1, 'rgba(239,235,233,0)');
  } else {
    grad.addColorStop(0, 'rgba(255,183,77,0.9)');
    grad.addColorStop(0.4, 'rgba(255,183,77,0.5)');
    grad.addColorStop(1, 'rgba(255,183,77,0)');
  }
  ctx.fillStyle = grad;
  ctx.beginPath();
  ctx.arc(cx, cx, r, 0, Math.PI * 2);
  ctx.fill();
  window.__SPRITE_DATA__ = c.toDataURL('image/png');
})();
</script></body></html>`;

async function main() {
  mkdirSync(OUT, { recursive: true });
  const server = createServer((req, res) => {
    if (req.url?.startsWith('/')) {
      res.writeHead(200, { 'Content-Type': 'text/html' });
      res.end(PARTICLE_HTML);
    } else {
      res.writeHead(404);
      res.end();
    }
  });
  await new Promise((r) => server.listen(0, '127.0.0.1', r));
  const port = server.address().port;
  const browser = await puppeteer.launch({ headless: true, args: ['--no-sandbox'] });
  try {
    for (const name of ['particle_smoke', 'particle_sparkle']) {
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
    console.log('Particle textures written.');
  } finally {
    await browser.close();
    server.close();
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
