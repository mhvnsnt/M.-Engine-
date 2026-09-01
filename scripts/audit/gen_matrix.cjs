/**
 * Generates the subsystem section of M_ENGINE_COMPLETENESS_MATRIX.md from the
 * reachability measurement, so the matrix is reproducible rather than asserted.
 *
 * Run: node scripts/audit/reachability.cjs && node scripts/audit/gen_matrix.cjs
 */
const fs = require('fs')
const path = require('path')

const ROOT = '/home/user/m.-engine-'
const { rows } = require(path.join(ROOT, 'scripts/audit/reachability.json'))

// Group by the package directory that identifies a subsystem.
const group = (f) => {
  const m = f.match(/com\/example\/(.*)\/[^/]+\.kt$/)
  if (!m) return f.startsWith('cloud_control_plane') ? 'cloud_control_plane' : 'other'
  return m[1]
}

const buckets = new Map()
for (const r of rows) {
  const g = group(r.file)
  if (!buckets.has(g)) buckets.set(g, { files: 0, loc: 0, reached: 0, io: 0, marks: 0 })
  const b = buckets.get(g)
  b.files++
  b.loc += r.lines
  if (r.reached) b.reached++
  if (r.realIO) b.io++
  b.marks += Object.values(r.marks).reduce((a, n) => a + n, 0)
}

/**
 * Classification rule, applied uniformly so no subsystem gets a flattering
 * reading. Reachability is necessary but not sufficient: a subsystem carrying
 * simulation markers cannot rank above PARTIAL regardless of how well it is
 * wired.
 */
function classify(b) {
  const frac = b.reached / b.files
  if (frac === 0) return 'DISCONNECTED'
  if (b.marks > 0 && frac < 1) return 'PARTIAL / MARKERS PRESENT'
  if (b.marks > 0) return 'IMPLEMENTED_UNVERIFIED (markers present)'
  if (frac < 1) return 'PARTIALLY CONNECTED'
  return b.io ? 'CONNECTED (performs real I/O)' : 'CONNECTED (in-process only)'
}

const sorted = [...buckets.entries()].sort((a, b) => b[1].loc - a[1].loc)

let out = '| Subsystem (package) | Files | LOC | Reachable | Real I/O | Sim markers | Measured state |\n'
out += '| --- | ---: | ---: | ---: | ---: | ---: | --- |\n'
for (const [g, b] of sorted) {
  out += `| \`${g}\` | ${b.files} | ${b.loc} | ${b.reached}/${b.files} | ${b.io} | ${b.marks} | ${classify(b)} |\n`
}

const tot = [...buckets.values()].reduce(
  (a, b) => ({ files: a.files + b.files, loc: a.loc + b.loc, reached: a.reached + b.reached, marks: a.marks + b.marks }),
  { files: 0, loc: 0, reached: 0, marks: 0 },
)
out += `| **TOTAL** | **${tot.files}** | **${tot.loc}** | **${tot.reached}/${tot.files}** | | **${tot.marks}** | |\n`

fs.writeFileSync(path.join(ROOT, 'scripts/audit/subsystem_table.md'), out)
console.log(out)
console.log(`\ndisconnected: ${tot.files - tot.reached} files, ${(100 * (tot.files - tot.reached) / tot.files).toFixed(1)}%`)
