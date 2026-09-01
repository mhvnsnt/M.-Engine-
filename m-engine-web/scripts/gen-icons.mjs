// Generates the PWA icon set from a single vector source.
// Run with: npm i -D sharp && npm run icons
// sharp is intentionally NOT a saved dependency: the icons it produces are
// committed, so keeping it out of package.json spares every CI deploy a large
// native install that the build itself never needs.
// Outputs are committed, so this is a one-off authoring tool, not a build step.
import sharp from 'sharp'
import { mkdirSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const outDir = join(root, 'public', 'icons')
mkdirSync(outDir, { recursive: true })

const BG = '#0b1020'
const ACCENT = '#3b82f6'

// `inset` is the fraction of the canvas left as padding. Maskable icons are
// cropped to a circle inscribed in the safe zone, so their glyph needs to sit
// well inside the edges or Android will clip the serifs off.
const mark = (size, inset) => {
  const s = size
  const pad = s * inset
  const box = s - pad * 2
  const stroke = box * 0.115
  const y0 = pad + box * 0.2
  const y1 = pad + box * 0.78
  const x0 = pad + box * 0.06
  const x1 = pad + box * 0.62
  const mid = (x0 + x1) / 2
  const dotR = box * 0.075
  return Buffer.from(`<svg xmlns="http://www.w3.org/2000/svg" width="${s}" height="${s}" viewBox="0 0 ${s} ${s}">
  <rect width="${s}" height="${s}" fill="${BG}"/>
  <g fill="none" stroke="${ACCENT}" stroke-width="${stroke}" stroke-linecap="round" stroke-linejoin="round">
    <path d="M ${x0} ${y1} L ${x0} ${y0} L ${mid} ${y0 + box * 0.34} L ${x1} ${y0} L ${x1} ${y1}"/>
  </g>
  <circle cx="${x1 + box * 0.2}" cy="${y1}" r="${dotR}" fill="${ACCENT}"/>
</svg>`)
}

const targets = [
  { file: 'icon-192.png', size: 192, inset: 0.14 },
  { file: 'icon-512.png', size: 512, inset: 0.14 },
  { file: 'icon-maskable-512.png', size: 512, inset: 0.26 },
  { file: 'apple-touch-icon.png', size: 180, inset: 0.14 },
]

for (const { file, size, inset } of targets) {
  await sharp(mark(size, inset)).png({ compressionLevel: 9 }).toFile(join(outDir, file))
  console.log('wrote', file, size + 'x' + size)
}

// Favicon as SVG: scales cleanly and needs no rasterisation.
writeFileSync(join(root, 'public', 'favicon.svg'), mark(64, 0.1).toString())
console.log('wrote favicon.svg')
