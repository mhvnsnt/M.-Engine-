'use strict'
/**
 * M. Engine Unreal Remote Worker.
 *
 * Runs on a machine that ACTUALLY has Unreal Engine installed. M. Engine (phone,
 * PWA or control plane) governs and observes; this worker does the heavy work
 * and returns evidence.
 *
 * Deliberate constraints:
 *  - NO arbitrary shell execution. Operations are an allowlist of named
 *    functions; there is no endpoint that takes a command string. A worker that
 *    accepts arbitrary commands is a remote shell with extra steps.
 *  - Paths are confined to configured project roots. A traversal outside them
 *    is refused, not sanitised.
 *  - Every operation is bounded by a timeout and returns evidence, including on
 *    failure. Failure is data, not an exception to hide.
 *  - Capability states are PROBED, never configured. "Unreal is installed"
 *    according to a config file is not evidence.
 *
 * Zero dependencies: Node stdlib only, so it drops onto a Windows/macOS/Linux
 * dev machine with nothing to install.
 *
 * Usage:  node server.js --port 8770 --projects /path/to/projects [--engine /path/to/UE_5.3]
 */
const http = require('http')
const path = require('path')
const crypto = require('crypto')
const { probeAll, run, STATES } = require('./probe')

function parseArgs(argv) {
  const out = { port: 8770, projectRoots: [], engineRoot: null, token: process.env.MENGINE_WORKER_TOKEN || null }
  for (let i = 2; i < argv.length; i++) {
    const a = argv[i]
    if (a === '--port') out.port = Number(argv[++i])
    else if (a === '--projects') out.projectRoots.push(path.resolve(argv[++i]))
    else if (a === '--engine') out.engineRoot = path.resolve(argv[++i])
    else if (a === '--token') out.token = argv[++i]
  }
  if (!out.projectRoots.length) out.projectRoots = [process.cwd()]
  return out
}

const config = parseArgs(process.argv)

/** Rejects any path that escapes the configured project roots. */
function withinRoots(target) {
  const resolved = path.resolve(target)
  return config.projectRoots.some((root) => {
    const rel = path.relative(root, resolved)
    return rel === '' || (!rel.startsWith('..') && !path.isAbsolute(rel))
  })
}

/**
 * The operation allowlist. Each entry is a named function with declared,
 * validated arguments. Adding an operation is a deliberate act; there is no
 * generic escape hatch.
 */
const OPERATIONS = {
  /** Re-probe every capability. */
  async probe() {
    return probeAll(config)
  },

  /**
   * Compiles an Unreal project. The single most valuable operation: it is the
   * first point at which any claim about Bannon's C++ becomes verifiable.
   */
  async build({ uproject, target, platform, configuration }) {
    if (!uproject || !withinRoots(uproject)) {
      return { ok: false, error: 'REFUSED: uproject outside configured project roots' }
    }
    const probed = await probeAll(config)
    const engine = probed.capabilities.UNREAL_RUNTIME_DISCOVERED
    if (!engine.engineRoot) {
      return { ok: false, state: STATES.CAPABILITY_GAP, error: 'no Unreal Engine on this host', evidence: engine.evidence }
    }
    const script = probed.capabilities.UNREAL_BUILD_CAPABLE.path
    if (!script) {
      return { ok: false, state: STATES.CAPABILITY_GAP, error: 'no Unreal build script', evidence: probed.capabilities.UNREAL_BUILD_CAPABLE.evidence }
    }
    const args = [
      target || 'BannonCoreEditor',
      platform || (process.platform === 'win32' ? 'Win64' : process.platform === 'darwin' ? 'Mac' : 'Linux'),
      configuration || 'Development',
      `-Project=${uproject}`,
      '-WaitMutex',
    ]
    const started = Date.now()
    const res = await run(script, args, 45 * 60 * 1000)
    return {
      ok: res.ok,
      state: res.ok ? STATES.VERIFIED : STATES.BLOCKED,
      exitCode: res.code,
      durationMs: Date.now() - started,
      command: `${script} ${args.join(' ')}`,
      // The log IS the evidence, success or failure.
      stdout: res.stdout.slice(-40000),
      stderr: res.stderr.slice(-40000),
    }
  },

  /** Runs Unreal automation tests headlessly. */
  async automationTest({ uproject, tests }) {
    if (!uproject || !withinRoots(uproject)) {
      return { ok: false, error: 'REFUSED: uproject outside configured project roots' }
    }
    const probed = await probeAll(config)
    const editor = probed.capabilities.UNREAL_RUNTIME_DISCOVERED.editor
    if (!editor) {
      return { ok: false, state: STATES.CAPABILITY_GAP, error: 'no Unreal editor binary on this host' }
    }
    const filter = tests || 'Project'
    const args = [
      uproject, '-ExecCmds=Automation RunTests ' + filter,
      '-unattended', '-nopause', '-nullrhi', '-nosplash', '-testexit=Automation Test Queue Empty',
    ]
    const started = Date.now()
    const res = await run(editor, args, 30 * 60 * 1000)
    return {
      ok: res.ok, state: res.ok ? STATES.VERIFIED : STATES.BLOCKED,
      exitCode: res.code, durationMs: Date.now() - started,
      stdout: res.stdout.slice(-40000), stderr: res.stderr.slice(-40000),
    }
  },

  /**
   * Reports what content a project actually contains.
   *
   * Directly targets the Bannon finding: an Unreal project with no .uasset and
   * no .umap has no animation system to debug. This makes that measurable from
   * the machine that holds the project.
   */
  async inspectContent({ uproject }) {
    if (!uproject || !withinRoots(uproject)) {
      return { ok: false, error: 'REFUSED: uproject outside configured project roots' }
    }
    const fs = require('fs')
    const projectDir = path.dirname(uproject)
    const counts = { uasset: 0, umap: 0, uplugin: 0, fbx: 0 }
    const contentDir = path.join(projectDir, 'Content')
    const stack = fs.existsSync(contentDir) ? [contentDir] : []
    let scanned = 0
    while (stack.length && scanned < 200000) {
      const dir = stack.pop(); scanned++
      let entries = []
      try { entries = fs.readdirSync(dir, { withFileTypes: true }) } catch { continue }
      for (const e of entries) {
        if (e.isDirectory()) { stack.push(path.join(dir, e.name)); continue }
        if (e.name.endsWith('.uasset')) counts.uasset++
        else if (e.name.endsWith('.umap')) counts.umap++
        else if (e.name.endsWith('.uplugin')) counts.uplugin++
        else if (e.name.endsWith('.fbx')) counts.fbx++
      }
    }
    const hasContent = counts.uasset > 0 || counts.umap > 0
    return {
      ok: true,
      contentDir: fs.existsSync(contentDir) ? contentDir : null,
      counts,
      state: hasContent ? STATES.VERIFIED : STATES.CAPABILITY_GAP,
      evidence: hasContent
        ? `${counts.uasset} .uasset, ${counts.umap} .umap`
        : 'no Unreal content: project cannot render or animate regardless of C++ correctness',
    }
  },
}

function send(res, code, body) {
  const payload = JSON.stringify(body, null, 2)
  res.writeHead(code, { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(payload) })
  res.end(payload)
}

/** Constant-time compare so the token check cannot be timed. */
function tokenOk(header) {
  if (!config.token) return true // no token configured = local trusted use
  const given = (header || '').replace(/^Bearer\s+/i, '')
  const a = Buffer.from(given), b = Buffer.from(config.token)
  return a.length === b.length && crypto.timingSafeEqual(a, b)
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, 'http://localhost')

  // Health is deliberately unauthenticated: it is how the fabric discovers the
  // worker exists, and it reveals nothing beyond that.
  if (url.pathname === '/health') {
    return send(res, 200, { status: 'UP', worker: 'unreal', version: 1 })
  }

  if (!tokenOk(req.headers.authorization)) {
    return send(res, 401, { error: 'unauthorised' })
  }

  if (url.pathname === '/capabilities' && req.method === 'GET') {
    return send(res, 200, await probeAll(config))
  }

  if (url.pathname.startsWith('/op/') && req.method === 'POST') {
    const name = url.pathname.slice(4)
    const op = OPERATIONS[name]
    if (!op) {
      return send(res, 404, { error: `unknown operation: ${name}`, allowed: Object.keys(OPERATIONS) })
    }
    let body = ''
    req.on('data', (c) => { body += c; if (body.length > 1e6) req.destroy() })
    req.on('end', async () => {
      let args = {}
      try { args = body ? JSON.parse(body) : {} } catch { return send(res, 400, { error: 'invalid JSON body' }) }
      try {
        send(res, 200, await op(args))
      } catch (e) {
        // An operation that throws is still evidence, not a 500 with no detail.
        send(res, 200, { ok: false, error: e.message, stack: (e.stack || '').split('\n').slice(0, 5) })
      }
    })
    return
  }

  send(res, 404, { error: 'not found', endpoints: ['/health', '/capabilities', '/op/<name>'] })
})

if (require.main === module) {
  server.listen(config.port, () => {
    console.log(`M. Engine Unreal worker on :${config.port}`)
    console.log(`  project roots : ${config.projectRoots.join(', ')}`)
    console.log(`  engine override: ${config.engineRoot || '(auto-discover)'}`)
    console.log(`  auth          : ${config.token ? 'token required' : 'OPEN (local use only)'}`)
    console.log(`  operations    : ${Object.keys(OPERATIONS).join(', ')}`)
  })
}

module.exports = { server, OPERATIONS, config, withinRoots }
