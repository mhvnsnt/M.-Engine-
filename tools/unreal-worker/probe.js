'use strict'
/**
 * Unreal capability probing.
 *
 * Every capability here is DISCOVERED BY LOOKING, never asserted from
 * configuration. "The owner said Unreal is installed" is not evidence; finding
 * and successfully executing `UnrealEditor-Cmd -Version` is.
 *
 * Each probe returns { state, evidence, detail } where `state` is one of the
 * reality-contract states. A probe that cannot establish something returns
 * CAPABILITY_GAP with the reason — never a hopeful AVAILABLE.
 */
const { execFile } = require('child_process')
const fs = require('fs')
const path = require('path')
const os = require('os')

const STATES = {
  VERIFIED: 'VERIFIED',
  PARTIALLY_VERIFIED: 'PARTIALLY_VERIFIED',
  IMPLEMENTED_UNVERIFIED: 'IMPLEMENTED_UNVERIFIED',
  CAPABILITY_GAP: 'CAPABILITY_GAP',
  BLOCKED: 'BLOCKED_BY_EXTERNAL_DEPENDENCY',
}

/** Runs a command with a hard timeout. Never throws; failure is data. */
function run(cmd, args, timeoutMs = 20000) {
  return new Promise((resolve) => {
    execFile(cmd, args, { timeout: timeoutMs, windowsHide: true }, (err, stdout, stderr) => {
      resolve({
        ok: !err,
        code: err && typeof err.code === 'number' ? err.code : err ? -1 : 0,
        stdout: (stdout || '').toString(),
        stderr: (stderr || '').toString(),
        error: err ? err.message : null,
      })
    })
  })
}

/**
 * Candidate engine locations per platform. Deliberately explicit rather than a
 * filesystem-wide search: scanning an entire disk for an engine is slow and is
 * the kind of thing a worker should never do unprompted.
 */
function engineCandidates(engineRootOverride) {
  if (engineRootOverride) return [engineRootOverride]
  const home = os.homedir()
  switch (process.platform) {
    case 'win32':
      return [
        'C:\\Program Files\\Epic Games\\UE_5.5',
        'C:\\Program Files\\Epic Games\\UE_5.4',
        'C:\\Program Files\\Epic Games\\UE_5.3',
      ]
    case 'darwin':
      return [
        '/Users/Shared/Epic Games/UE_5.5',
        '/Users/Shared/Epic Games/UE_5.4',
        '/Users/Shared/Epic Games/UE_5.3',
        path.join(home, 'UnrealEngine'),
      ]
    default:
      return [
        path.join(home, 'UnrealEngine'),
        '/opt/UnrealEngine',
        '/usr/local/UnrealEngine',
      ]
  }
}

function editorBinary(engineRoot) {
  const base = path.join(engineRoot, 'Engine', 'Binaries')
  if (process.platform === 'win32') return path.join(base, 'Win64', 'UnrealEditor-Cmd.exe')
  if (process.platform === 'darwin') return path.join(base, 'Mac', 'UnrealEditor-Cmd')
  return path.join(base, 'Linux', 'UnrealEditor-Cmd')
}

function buildToolScript(engineRoot) {
  const build = path.join(engineRoot, 'Engine', 'Build', 'BatchFiles')
  if (process.platform === 'win32') return path.join(build, 'Build.bat')
  if (process.platform === 'darwin') return path.join(build, 'Mac', 'Build.sh')
  return path.join(build, 'Linux', 'Build.sh')
}

/** Finds an engine by looking for its actual binaries on disk. */
async function probeEngine(engineRootOverride) {
  const tried = []
  for (const root of engineCandidates(engineRootOverride)) {
    const editor = editorBinary(root)
    tried.push(root)
    if (!fs.existsSync(editor)) continue

    // Existence is not capability — execute it and read the version back.
    const res = await run(editor, ['-Version'], 30000)
    const out = `${res.stdout}\n${res.stderr}`
    const m = out.match(/(\d+\.\d+\.\d+)|Version:\s*([\d.]+)/)
    return {
      state: res.ok ? STATES.VERIFIED : STATES.PARTIALLY_VERIFIED,
      engineRoot: root,
      editor,
      version: m ? (m[1] || m[2]) : null,
      evidence: res.ok
        ? `executed ${path.basename(editor)} -Version, exit 0`
        : `binary present at ${editor} but -Version exited ${res.code}`,
      detail: out.trim().slice(0, 400) || null,
    }
  }
  return {
    state: STATES.CAPABILITY_GAP,
    engineRoot: null,
    editor: null,
    version: null,
    evidence: 'no UnrealEditor-Cmd binary found',
    detail: `searched: ${tried.join(', ')}`,
  }
}

async function probeBuildTool(engineRoot) {
  if (!engineRoot) {
    return { state: STATES.CAPABILITY_GAP, evidence: 'no engine root; build tool cannot exist' }
  }
  const script = buildToolScript(engineRoot)
  return fs.existsSync(script)
    ? { state: STATES.PARTIALLY_VERIFIED, path: script, evidence: `build script present at ${script}` }
    : { state: STATES.CAPABILITY_GAP, evidence: `no build script at ${script}` }
}

/** Android packaging needs the SDK, the NDK and a JDK — all three or none. */
async function probeAndroid() {
  const sdk = process.env.ANDROID_HOME || process.env.ANDROID_SDK_ROOT
  const ndk = process.env.ANDROID_NDK_ROOT || process.env.NDK_ROOT
  const java = await run(process.platform === 'win32' ? 'java.exe' : 'java', ['-version'], 15000)
  const missing = []
  if (!sdk || !fs.existsSync(sdk)) missing.push('Android SDK (ANDROID_HOME)')
  if (!ndk || !fs.existsSync(ndk)) missing.push('Android NDK (ANDROID_NDK_ROOT)')
  if (!java.ok) missing.push('JDK (java not executable)')

  return missing.length === 0
    ? { state: STATES.PARTIALLY_VERIFIED, sdk, ndk, evidence: 'SDK, NDK and JDK all present' }
    : { state: STATES.CAPABILITY_GAP, sdk: sdk || null, ndk: ndk || null, evidence: `missing: ${missing.join(', ')}` }
}

/** A connected device is proof; adb existing is not. */
async function probeDevice() {
  const adb = await run('adb', ['devices'], 15000)
  if (!adb.ok) return { state: STATES.CAPABILITY_GAP, devices: [], evidence: 'adb not available' }
  const devices = adb.stdout
    .split('\n').slice(1)
    .map((l) => l.trim()).filter((l) => l && !l.startsWith('*'))
    .filter((l) => l.endsWith('device'))
    .map((l) => l.split(/\s+/)[0])
  return devices.length
    ? { state: STATES.VERIFIED, devices, evidence: `${devices.length} device(s) reported by adb` }
    : { state: STATES.CAPABILITY_GAP, devices: [], evidence: 'adb present but no authorised device attached' }
}

/** Locates .uproject files under the allowed project roots. */
function probeProjects(projectRoots) {
  const found = []
  for (const root of projectRoots) {
    if (!fs.existsSync(root)) continue
    const stack = [root]
    let scanned = 0
    // Bounded walk: a worker must not crawl an entire filesystem.
    while (stack.length && scanned < 4000) {
      const dir = stack.pop()
      scanned++
      let entries = []
      try { entries = fs.readdirSync(dir, { withFileTypes: true }) } catch { continue }
      for (const e of entries) {
        const full = path.join(dir, e.name)
        if (e.isDirectory()) {
          // Unreal's generated dirs are large and contain no .uproject.
          if (['Intermediate', 'Saved', 'DerivedDataCache', 'Binaries', '.git', 'node_modules'].includes(e.name)) continue
          stack.push(full)
        } else if (e.name.endsWith('.uproject')) {
          let engineAssociation = null
          try {
            engineAssociation = JSON.parse(fs.readFileSync(full, 'utf8')).EngineAssociation ?? null
          } catch { /* unreadable uproject is still a finding */ }
          // A project with no Content directory cannot render anything.
          const contentDir = path.join(path.dirname(full), 'Content')
          found.push({
            uproject: full,
            engineAssociation,
            hasContentDir: fs.existsSync(contentDir),
          })
        }
      }
    }
  }
  return found.length
    ? { state: STATES.VERIFIED, projects: found, evidence: `${found.length} .uproject file(s) found` }
    : { state: STATES.CAPABILITY_GAP, projects: [], evidence: `no .uproject under: ${projectRoots.join(', ')}` }
}

async function probeAll(config) {
  const engine = await probeEngine(config.engineRoot)
  const [buildTool, android, device] = await Promise.all([
    probeBuildTool(engine.engineRoot),
    probeAndroid(),
    probeDevice(),
  ])
  const projects = probeProjects(config.projectRoots)

  return {
    probedAt: new Date().toISOString(),
    host: { platform: process.platform, arch: process.arch, hostname: os.hostname(), cpus: os.cpus().length,
            totalMemGb: Math.round(os.totalmem() / 1e9) },
    capabilities: {
      UNREAL_RUNTIME_DISCOVERED: engine,
      UNREAL_BUILD_CAPABLE: buildTool,
      UNREAL_PROJECT_AVAILABLE: projects,
      ANDROID_TOOLCHAIN_AVAILABLE: android,
      PHYSICAL_DEVICE_AVAILABLE: device,
    },
  }
}

module.exports = { probeAll, probeEngine, probeProjects, STATES, run }
