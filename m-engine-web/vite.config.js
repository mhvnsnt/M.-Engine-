import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { VitePWA } from 'vite-plugin-pwa'

// GitHub Pages serves this project from a repository subpath, so every asset URL
// has to be prefixed with it. Overridable for other hosts (Vercel, Netlify, a
// custom domain) where the app sits at the root.
const base = process.env.PUBLIC_BASE_PATH ?? '/M.-Engine-/'

export default defineConfig({
  base,
  plugins: [
    react(),
    tailwindcss(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.svg', 'icons/apple-touch-icon.png'],
      manifest: {
        name: 'M. Engine',
        short_name: 'M. Engine',
        description:
          'Control plane for M. Engine — missions, capability reality, evidence and telemetry.',
        start_url: base,
        scope: base,
        display: 'standalone',
        orientation: 'portrait-primary',
        background_color: '#0b1020',
        theme_color: '#0b1020',
        categories: ['developer', 'productivity'],
        icons: [
          { src: 'icons/icon-192.png', sizes: '192x192', type: 'image/png' },
          { src: 'icons/icon-512.png', sizes: '512x512', type: 'image/png' },
          {
            src: 'icons/icon-maskable-512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'maskable',
          },
        ],
      },
      workbox: {
        globPatterns: ['**/*.{js,css,html,svg,png,woff2}'],
        // The control plane and the LLM providers are live state. Caching their
        // responses would show stale telemetry as though it were current, which
        // the reality contract forbids — so they are never served from cache.
        navigateFallbackDenylist: [/^\/api\//],
        runtimeCaching: [],
        cleanupOutdatedCaches: true,
      },
      devOptions: { enabled: false },
    }),
  ],
})
