/**
 * M. Engine reality audit — static reachability + reality-marker analysis.
 *
 * REALITY_CONTRACT.md forbids claiming a capability exists because an interface
 * exists. The decisive question for every declaration is therefore not "was it
 * written?" but "is it reachable from something that actually runs?"
 *
 * Method:
 *  1. Index every top-level declaration — types AND functions — in the
 *     production source sets. Indexing only types is a trap: every Compose
 *     screen here is a top-level `fun`, and omitting them reported all of them
 *     disconnected while AppShell.kt calls them directly.
 *  2. Build a reference graph: file A depends on declaration D if D's name
 *     appears as a whole identifier in A and A is not D's declaring file.
 *  3. Breadth-first search from the real process entry points.
 *  4. Anything unreached is DISCONNECTED — written, compiled, never invoked.
 *
 * This OVER-approximates reachability (a mention in a comment counts), which
 * makes DISCONNECTED findings conservative: if this reports something
 * unreachable, nothing in the tree names it at all.
 */
const fs = require('fs')
const path = require('path')

const ROOT = '/home/user/m.-engine-'
const SOURCE_SETS = ['app/src/main/java', 'cloud_control_plane/src/main/kotlin']
const ENTRY_FILES = [
  'app/src/main/java/com/example/MainActivity.kt',
  'cloud_control_plane/src/main/kotlin/com/example/ai/cloud/Main.kt',
]

function walk(dir, out = []) {
  if (!fs.existsSync(dir)) return out
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name)
    if (e.isDirectory()) walk(p, out)
    else if (e.name.endsWith('.kt')) out.push(p)
  }
  return out
}

const files = SOURCE_SETS.flatMap((s) => walk(path.join(ROOT, s)))
const src = new Map(files.map((f) => [f, fs.readFileSync(f, 'utf8')]))

// --- 1. index declarations (types AND top-level functions) -----------------
const DECL_TYPE = /^\s*(?:@\w+\s+)*(?:public\s+|internal\s+|private\s+|abstract\s+|open\s+|sealed\s+|data\s+|value\s+)*(?:class|object|interface|enum class)\s+([A-Z]\w*)/gm
const DECL_FUN = /^(?:@\w+(?:\([^)]*\))?\s*)*(?:public\s+|internal\s+|private\s+|suspend\s+|inline\s+)*fun\s+(?:<[^>]+>\s+)?([A-Za-z]\w*)\s*\(/gm

const declOf = new Map()
const declsIn = new Map()
for (const [f, text] of src) {
  const list = []
  for (const re of [DECL_TYPE, DECL_FUN]) {
    for (const m of text.matchAll(re)) {
      const name = m[1]
      if (!declOf.has(name)) declOf.set(name, f)
      list.push(name)
    }
  }
  declsIn.set(f, list)
}

// --- 2. reference graph ----------------------------------------------------
const deps = new Map()
for (const [f, text] of src) {
  const set = new Set()
  const code = text.replace(/"(?:[^"\\]|\\.)*"/g, '""') // strip string literals
  for (const id of new Set(code.match(/\b[A-Za-z]\w*/g) ?? [])) {
    const owner = declOf.get(id)
    if (owner && owner !== f) set.add(owner)
  }
  deps.set(f, set)
}

// --- 3. BFS from entry points ---------------------------------------------
const entries = ENTRY_FILES.map((e) => path.join(ROOT, e)).filter((e) => src.has(e))
const reached = new Set(entries)
const queue = [...entries]
while (queue.length) {
  for (const d of deps.get(queue.shift()) ?? []) {
    if (!reached.has(d)) { reached.add(d); queue.push(d) }
  }
}

// --- 4. reality markers ----------------------------------------------------
const MARKERS = {
  TODO: /\bTODO\b/g, FIXME: /\bFIXME\b/g,
  mock: /\bmock(?:ed|s)?\b/gi, stub: /\bstub(?:bed|s)?\b/gi,
  simulate: /\bsimulat(?:e|ed|es|ion)\b/gi, placeholder: /\bplaceholder\b/gi,
  notImplemented: /TODO\(\)|NotImplementedError|UnsupportedOperationException/g,
}
// Evidence a file actually reaches outside the process.
const REAL_IO = /\b(?:HttpURLConnection|OkHttpClient|Retrofit|ProcessBuilder|Socket|embeddedServer|DriverManager|FileInputStream|openConnection|newCall|execute)\b/

const rows = []
for (const [f, text] of src) {
  const marks = {}
  for (const [k, re] of Object.entries(MARKERS)) {
    const n = (text.match(re) ?? []).length
    if (n) marks[k] = n
  }
  rows.push({
    file: path.relative(ROOT, f),
    lines: text.split('\n').length,
    decls: declsIn.get(f) ?? [],
    reached: reached.has(f),
    realIO: REAL_IO.test(text),
    marks,
  })
}

const disconnected = rows.filter((r) => !r.reached && r.decls.length)
const connected = rows.filter((r) => r.reached)
const sum = (rs) => rs.reduce((a, r) => a + r.lines, 0)

console.log('=== SCOPE ===')
console.log(`production .kt files : ${files.length}`)
console.log(`top-level declarations: ${declOf.size}`)
console.log()
console.log('=== REACHABILITY FROM ENTRY POINTS ===')
console.log(`reachable files      : ${connected.length}  (${sum(connected)} LOC)`)
console.log(`DISCONNECTED files   : ${disconnected.length}  (${sum(disconnected)} LOC)`)
console.log(`disconnected share   : ${(100 * disconnected.length / rows.length).toFixed(1)}% of files`)
console.log()
console.log('=== LARGEST DISCONNECTED SUBSYSTEMS ===')
for (const r of disconnected.sort((a, b) => b.lines - a.lines).slice(0, 25)) {
  console.log(`${String(r.lines).padStart(5)}  ${r.realIO ? 'io ' : '   '} ${r.file}`)
}
const marked = rows.filter((r) => Object.keys(r.marks).length)
const totals = {}
for (const r of marked) for (const [k, n] of Object.entries(r.marks)) totals[k] = (totals[k] ?? 0) + n
console.log()
console.log('=== REALITY MARKERS ===')
console.log(`files carrying markers: ${marked.length}   totals: ${JSON.stringify(totals)}`)
for (const r of marked.sort((a,b)=>Object.values(b.marks).reduce((x,y)=>x+y,0)-Object.values(a.marks).reduce((x,y)=>x+y,0)).slice(0, 15)) {
  console.log(`  ${r.file} ${JSON.stringify(r.marks)}${r.reached ? '' : '  [DISCONNECTED]'}`)
}
fs.writeFileSync(path.join(ROOT, 'scripts/audit/reachability.json'), JSON.stringify({ rows }, null, 2))
console.log('\nwrote scripts/audit/reachability.json')
