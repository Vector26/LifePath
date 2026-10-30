import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const packageJson = JSON.parse(
  readFileSync(resolve(process.cwd(), 'package.json'), 'utf-8')
) as { version?: string }
const appVersion = packageJson.version ?? '0.0.0'
const buildTime = new Date().toISOString()

function apkMiddleware(req: { url?: string }, res: { setHeader: (k: string, v: string) => void; end: (b: Buffer) => void }, next: () => void) {
  if (req.url === '/lifepath.apk' || req.url?.startsWith('/lifepath.apk?')) {
    const apkPath = resolve(process.cwd(), 'public/lifepath.apk')
    res.setHeader('Content-Type', 'application/vnd.android.package-archive')
    res.setHeader('Content-Disposition', 'attachment; filename="LifePath.apk"')
    res.setHeader('Cache-Control', 'no-cache')
    res.end(readFileSync(apkPath))
    return
  }
  next()
}

// https://vite.dev/config/
export default defineConfig({
  define: {
    __APP_VERSION__: JSON.stringify(appVersion),
    __BUILD_TIME__: JSON.stringify(buildTime),
  },
  plugins: [
    react(),
    {
      name: 'apk-download',
      configureServer(server) {
        server.middlewares.use(apkMiddleware)
      },
      configurePreviewServer(server) {
        server.middlewares.use(apkMiddleware)
      },
    },
  ],
  server: {
    host: true,
    port: 39173,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://localhost:39081',
        changeOrigin: true,
      },
    },
  },
})
