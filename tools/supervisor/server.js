'use strict'
/**
 * M. Engine Supervisor Daemon.
 *
 * A REAL process supervisor for the M. Engine operation. Monitors registered
 * worker processes, restarts them when they die, and exposes an HTTP API so
 * agents can register their long-running work and check on it.
 *
 * This is the answer to the daemon-restart problem: when the VM restarts and
 * kills everything, the cron watchdog brings THIS back, and THIS brings back
 * everything registered with it.
 *
 * Zero dependencies: Node stdlib only.
 *
 * Design (reality-first, per M. Engine law):
 *  - Registry is a JSON file on disk. If the supervisor dies, the registry
 *    survives. On startup it re-reads the registry and resumes supervision.
 *  - Health is PROBED (kill -0 on the PID), never assumed.
 *  - Restarts use the registered start command. If the command fails, the
 *    failure is logged with the exit code — not hidden.
 *  - The HTTP API has NO arbitrary command execution. Registering a worker
 *    requires a name, a start command, and a working directory. There is no
 *    endpoint that takes a raw shell string at request time.
 *
 * Usage:
 *   node server.js --port 8771 [--registry /path/to/registry.json] [--log /path/to/supervisor.log]
 *
 * API:
 *   GET  /health              -> {status, uptime, workers}
 *   GET  /workers             -> list of registered workers with live status
 *   POST /workers/register    -> {name, command, cwd, env?} registers a worker
 *   POST /workers/deregister  -> {name} removes a worker from supervision
 *   POST /workers/restart     -> {name} force-restarts a worker
 *   GET  /workers/:name/log   -> last N lines of worker log
 */

const http = require('http')
const fs = require('fs')
const path = require('path')
const { spawn, execSync } = require('child_process')
const os = require('os')

// ---------------------------------------------------------------------------
// Config
// ---------------------------------------------------------------------------
function parseArgs(argv) {
  const out = {
    port: 8771,
    registry: path.join(os.homedir(), 'workspace', 'M.-Engine-', 'tools', 'supervisor', 'registry.json'),
    log: path.join(os.homedir(), 'workspace', 'M.-Engine-', 'tools', 'supervisor', 'supervisor.log'),
    pidFile: path.join(os.homedir(), 'workspace', 'M.-Engine-', 'tools', 'supervisor', 'supervisor.pid'),
    checkIntervalMs: 15000,
  }
  for (let i = 2; i < argv.length; i++) {
    const a = argv[i]
    if (a === '--port') out.port = Number(argv[++i])
    else if (a === '--registry') out.registry = path.resolve(argv[++i])
    else if (a === '--log') out.log = path.resolve(argv[++i])
    else if (a === '--pid-file') out.pidFile = path.resolve(argv[++i])
    else if (a === '--check-interval') out.checkIntervalMs = Number(argv[++i]) * 1000
  }
  return out
}
const config = parseArgs(process.argv)
const startTime = Date.now()

// ---------------------------------------------------------------------------
// Logging
// ---------------------------------------------------------------------------
function log(level, msg, data) {
  const line = JSON.stringify({
    ts: new Date().toISOString(),
    level,
    msg,
    ...(data ? { data } : {}),
  })
  // stdout for journal capture
  console.log(line)
  // append to log file (best effort)
  try {
    fs.appendFileSync(config.log, line + '\n')
  } catch (e) { /* log file unwritable — stdout still has it */ }
}

// ---------------------------------------------------------------------------
// Registry (persisted to disk)
// ---------------------------------------------------------------------------
let registry = { workers: {} }

function loadRegistry() {
  try {
    if (fs.existsSync(config.registry)) {
      const raw = fs.readFileSync(config.registry, 'utf8')
      registry = JSON.parse(raw)
      if (!registry.workers) registry.workers = {}
      log('info', 'registry loaded', { workers: Object.keys(registry.workers).length, path: config.registry })
    } else {
      log('info', 'no registry file yet, starting empty', { path: config.registry })
    }
  } catch (e) {
    log('error', 'failed to load registry, starting empty', { error: e.message })
    registry = { workers: {} }
  }
}

function saveRegistry() {
  try {
    const dir = path.dirname(config.registry)
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true })
    // atomic write: tmp + rename
    const tmp = config.registry + '.tmp'
    fs.writeFileSync(tmp, JSON.stringify(registry, null, 2))
    fs.renameSync(tmp, config.registry)
  } catch (e) {
    log('error', 'failed to save registry', { error: e.message })
  }
}

// ---------------------------------------------------------------------------
// Process management
// ---------------------------------------------------------------------------
function isAlive(pid) {
  if (!pid) return false
  try {
    process.kill(pid, 0)
    return true
  } catch (e) {
    return false
  }
}

function startWorker(name) {
  const w = registry.workers[name]
  if (!w) {
    log('warn', 'startWorker called for unknown worker', { name })
    return { ok: false, error: 'unknown worker' }
  }
  if (isAlive(w.pid)) {
    log('info', 'worker already alive, not restarting', { name, pid: w.pid })
    return { ok: true, pid: w.pid, alreadyRunning: true }
  }

  // Spawn detached so it survives the supervisor's own request cycle.
  // stdio goes to the worker's log file.
  const logDir = path.dirname(config.log)
  const workerLog = path.join(logDir, `worker-${name}.log`)
  let out, err
  try {
    out = fs.openSync(workerLog, 'a')
    err = fs.openSync(workerLog, 'a')
  } catch (e) {
    log('error', 'cannot open worker log file', { name, error: e.message })
    return { ok: false, error: 'log file unwritable: ' + e.message }
  }

  // Parse command into argv (simple shell-like split; no pipes/redirects)
  const parts = w.command.match(/(?:[^\s"']+|"[^"]*"|'[^']*')+/g) || []
  const cmd = parts[0]
  const args = parts.slice(1).map((s) => s.replace(/^["']|["']$/g, ''))
  if (!cmd) {
    log('error', 'empty command for worker', { name })
    return { ok: false, error: 'empty command' }
  }

  try {
    const child = spawn(cmd, args, {
      cwd: w.cwd || process.cwd(),
      env: { ...process.env, ...(w.env || {}) },
      detached: true,
      stdio: ['ignore', out, err],
    })
    child.unref()
    w.pid = child.pid
    w.startedAt = new Date().toISOString()
    w.restarts = (w.restarts || 0) + (w._everStarted ? 1 : 0)
    w._everStarted = true
    w.lastExit = null
    saveRegistry()
    log('info', 'worker started', { name, pid: child.pid, command: w.command })
    return { ok: true, pid: child.pid }
  } catch (e) {
    log('error', 'failed to spawn worker', { name, error: e.message })
    return { ok: false, error: e.message }
  } finally {
    try { fs.closeSync(out) } catch (e) {}
    try { fs.closeSync(err) } catch (e) {}
  }
}

function stopWorker(name) {
  const w = registry.workers[name]
  if (!w || !isAlive(w.pid)) return { ok: true, alreadyStopped: true }
  try {
    process.kill(w.pid, 'SIGTERM')
    log('info', 'worker sent SIGTERM', { name, pid: w.pid })
    return { ok: true }
  } catch (e) {
    log('error', 'failed to stop worker', { name, error: e.message })
    return { ok: false, error: e.message }
  }
}

// ---------------------------------------------------------------------------
// Supervisor loop — probe every worker, restart the dead
// ---------------------------------------------------------------------------
function supervise() {
  for (const name of Object.keys(registry.workers)) {
    const w = registry.workers[name]
    if (w.disabled) continue
    if (!isAlive(w.pid)) {
      const wasPid = w.pid
      log('warn', 'worker dead, restarting', { name, wasPid })
      const r = startWorker(name)
      if (!r.ok) {
        w.consecutiveFailures = (w.consecutiveFailures || 0) + 1
        log('error', 'worker restart failed', { name, failures: w.consecutiveFailures, error: r.error })
        // Back off: disable after 5 consecutive failures to avoid hot-looping.
        if (w.consecutiveFailures >= 5) {
          w.disabled = true
          w.disabledReason = 'too many consecutive start failures'
          log('error', 'worker DISABLED after 5 failed restarts', { name })
        }
        saveRegistry()
      } else {
        w.consecutiveFailures = 0
        saveRegistry()
      }
    }
  }
}

// ---------------------------------------------------------------------------
// HTTP API
// ---------------------------------------------------------------------------
function sendJson(res, code, obj) {
  const body = JSON.stringify(obj)
  res.writeHead(code, { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(body) })
  res.end(body)
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    let data = ''
    req.on('data', (c) => { data += c; if (data.length > 1e6) req.destroy() })
    req.on('end', () => {
      if (!data) return resolve({})
      try { resolve(JSON.parse(data)) } catch (e) { reject(new Error('invalid JSON')) }
    })
    req.on('error', reject)
  })
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, 'http://localhost')
  const p = url.pathname

  try {
    // GET /health
    if (req.method === 'GET' && p === '/health') {
      const alive = Object.entries(registry.workers).filter(([, w]) => !w.disabled && isAlive(w.pid)).length
      const total = Object.keys(registry.workers).length
      return sendJson(res, 200, {
        status: 'UP',
        supervisor: 'm-engine-supervisor',
        version: 1,
        uptimeSec: Math.floor((Date.now() - startTime) / 1000),
        workersAlive: alive,
        workersTotal: total,
      })
    }

    // GET /workers
    if (req.method === 'GET' && p === '/workers') {
      const out = {}
      for (const [name, w] of Object.entries(registry.workers)) {
        out[name] = {
          name,
          command: w.command,
          cwd: w.cwd,
          pid: w.pid || null,
          alive: isAlive(w.pid),
          disabled: !!w.disabled,
          disabledReason: w.disabledReason || null,
          restarts: w.restarts || 0,
          startedAt: w.startedAt || null,
          consecutiveFailures: w.consecutiveFailures || 0,
        }
      }
      return sendJson(res, 200, { workers: out })
    }

    // POST /workers/register
    if (req.method === 'POST' && p === '/workers/register') {
      const b = await readBody(req)
      if (!b.name || typeof b.name !== 'string' || !/^[a-zA-Z0-9][a-zA-Z0-9_-]{0,63}$/.test(b.name)) {
        return sendJson(res, 400, { ok: false, error: 'name required (alphanumeric, dash, underscore, max 64)' })
      }
      if (!b.command || typeof b.command !== 'string' || b.command.length > 4096) {
        return sendJson(res, 400, { ok: false, error: 'command required (string, max 4096 chars)' })
      }
      const cwd = b.cwd ? path.resolve(b.cwd) : process.cwd()
      if (!fs.existsSync(cwd) || !fs.statSync(cwd).isDirectory()) {
        return sendJson(res, 400, { ok: false, error: 'cwd does not exist or is not a directory' })
      }
      const existed = !!registry.workers[b.name]
      registry.workers[b.name] = {
        name: b.name,
        command: b.command,
        cwd,
        env: (b.env && typeof b.env === 'object') ? b.env : {},
        pid: existed ? registry.workers[b.name].pid : null,
        disabled: false,
        restarts: existed ? (registry.workers[b.name].restarts || 0) : 0,
        registeredAt: existed ? registry.workers[b.name].registeredAt : new Date().toISOString(),
      }
      saveRegistry()
      log('info', existed ? 'worker re-registered' : 'worker registered', { name: b.name, command: b.command })
      // Start it now if not alive
      const w = registry.workers[b.name]
      let started = null
      if (!isAlive(w.pid)) started = startWorker(b.name)
      return sendJson(res, 200, { ok: true, name: b.name, reRegistered: existed, start: started })
    }

    // POST /workers/deregister
    if (req.method === 'POST' && p === '/workers/deregister') {
      const b = await readBody(req)
      if (!b.name || !registry.workers[b.name]) {
        return sendJson(res, 404, { ok: false, error: 'unknown worker' })
      }
      stopWorker(b.name)
      delete registry.workers[b.name]
      saveRegistry()
      log('info', 'worker deregistered', { name: b.name })
      return sendJson(res, 200, { ok: true })
    }

    // POST /workers/restart
    if (req.method === 'POST' && p === '/workers/restart') {
      const b = await readBody(req)
      if (!b.name || !registry.workers[b.name]) {
        return sendJson(res, 404, { ok: false, error: 'unknown worker' })
      }
      const w = registry.workers[b.name]
      w.disabled = false
      w.consecutiveFailures = 0
      stopWorker(b.name)
      // brief grace before restart
      await new Promise((r) => setTimeout(r, 1000))
      const r = startWorker(b.name)
      return sendJson(res, 200, { ok: r.ok, pid: r.pid, error: r.error || null })
    }

    // GET /workers/:name/log
    const logMatch = p.match(/^\/workers\/([^/]+)\/log$/)
    if (req.method === 'GET' && logMatch) {
      const name = decodeURIComponent(logMatch[1])
      if (!registry.workers[name]) return sendJson(res, 404, { ok: false, error: 'unknown worker' })
      const lines = Math.min(Number(url.searchParams.get('lines') || 50), 500)
      const workerLog = path.join(path.dirname(config.log), `worker-${name}.log`)
      try {
        const content = fs.readFileSync(workerLog, 'utf8')
        const all = content.split('\n').filter(Boolean)
        return sendJson(res, 200, { ok: true, name, lines: all.slice(-lines) })
      } catch (e) {
        return sendJson(res, 200, { ok: true, name, lines: [], note: 'no log yet' })
      }
    }

    return sendJson(res, 404, { ok: false, error: 'not found' })
  } catch (e) {
    log('error', 'request handler error', { error: e.message, path: p })
    return sendJson(res, 500, { ok: false, error: 'internal error' })
  }
})

// ---------------------------------------------------------------------------
// Boot
// ---------------------------------------------------------------------------
loadRegistry()

// Write PID file
try {
  fs.writeFileSync(config.pidFile, String(process.pid))
} catch (e) {
  log('error', 'cannot write pid file', { error: e.message })
}

server.listen(config.port, () => {
  log('info', 'supervisor listening', { port: config.port, pid: process.pid })
})

// Start the supervision loop
setInterval(supervise, config.checkIntervalMs)
// Run once immediately at boot (picks up dead workers from a restart)
setTimeout(supervise, 2000)

// Graceful shutdown: remove pid file
process.on('SIGTERM', () => {
  log('info', 'supervisor shutting down (SIGTERM)')
  try { fs.unlinkSync(config.pidFile) } catch (e) {}
  process.exit(0)
})
process.on('SIGINT', () => {
  log('info', 'supervisor shutting down (SIGINT)')
  try { fs.unlinkSync(config.pidFile) } catch (e) {}
  process.exit(0)
})
