'use strict'
/**
 * Self-tests for the Unreal worker. Run: node test.js
 *
 * This worker ships to the owner's own machine, so it must be checkable there
 * without a test framework or network access.
 *
 * The platform cases exist because a stray identifier once made the win32
 * branch throw ReferenceError at runtime while `node --check` reported the file
 * as valid. Parsing is not executing, and Windows is the platform most likely
 * to run Unreal — so every platform branch is exercised here, on every run.
 */
const assert = require('assert')
const fs = require('fs')
const os = require('os')
const path = require('path')

const MOD = path.join(__dirname, 'probe.js')
let passed = 0
const failures = []

async function check(name, fn) {
  try {
    await fn()
    passed++
    console.log(`PASS  ${name}`)
  } catch (e) {
    failures.push(`${name}: ${e.message}`)
    console.log(`FAIL  ${name}\n      ${e.message}`)
  }
}

function underPlatform(p, fn) {
  const orig = Object.getOwnPropertyDescriptor(process, 'platform')
  Object.defineProperty(process, 'platform', { value: p, configurable: true })
  delete require.cache[require.resolve(MOD)]
  try { return fn(require(MOD)) } finally {
    Object.defineProperty(process, 'platform', orig)
    delete require.cache[require.resolve(MOD)]
  }
}

;(async () => {
  // --- every platform branch must EXECUTE, not merely parse ----------------
  for (const plat of ['win32', 'darwin', 'linux']) {
    await check(`platform ${plat}: engine probe executes and returns a state`, async () => {
      const res = await underPlatform(plat, (m) => m.probeEngine(null))
      assert.ok(typeof res.state === 'string', 'no state returned')
      assert.ok('evidence' in res, 'no evidence returned')
    })
  }

  // --- absence must be reported as a gap, never as availability ------------
  await check('a host with no engine reports CAPABILITY_GAP, not AVAILABLE', async () => {
    const { probeEngine, STATES } = require(MOD)
    const res = await probeEngine(path.join(os.tmpdir(), 'definitely-not-unreal'))
    assert.strictEqual(res.state, STATES.CAPABILITY_GAP)
    assert.strictEqual(res.engineRoot, null)
  })

  // --- project discovery reads the filesystem, it does not guess -----------
  await check('project discovery finds a .uproject and reads EngineAssociation', async () => {
    const { probeProjects, STATES } = require(MOD)
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'uproj-'))
    fs.writeFileSync(path.join(dir, 'Demo.uproject'), JSON.stringify({ EngineAssociation: '5.3' }))
    const res = probeProjects([dir])
    assert.strictEqual(res.state, STATES.VERIFIED)
    assert.strictEqual(res.projects.length, 1)
    assert.strictEqual(res.projects[0].engineAssociation, '5.3')
    // No Content dir was created, so the project cannot render anything.
    assert.strictEqual(res.projects[0].hasContentDir, false)
    fs.rmSync(dir, { recursive: true, force: true })
  })

  await check('no .uproject anywhere is a gap, not an empty success', () => {
    const { probeProjects, STATES } = require(MOD)
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'empty-'))
    assert.strictEqual(probeProjects([dir]).state, STATES.CAPABILITY_GAP)
    fs.rmSync(dir, { recursive: true, force: true })
  })

  // --- the security boundary ----------------------------------------------
  await check('paths outside the configured roots are refused', () => {
    process.argv = ['node', 'server.js', '--projects', os.tmpdir()]
    delete require.cache[require.resolve(path.join(__dirname, 'server.js'))]
    const { withinRoots } = require(path.join(__dirname, 'server.js'))
    assert.ok(withinRoots(path.join(os.tmpdir(), 'ok', 'X.uproject')), 'inside root wrongly refused')
    assert.ok(!withinRoots('/etc/passwd'), 'outside root wrongly allowed')
    // Traversal must not escape by climbing out and back.
    assert.ok(!withinRoots(path.join(os.tmpdir(), '..', '..', 'etc', 'passwd')),
      'traversal escaped the configured root')
  })

  await check('there is no arbitrary-execution operation', () => {
    const { OPERATIONS } = require(path.join(__dirname, 'server.js'))
    const names = Object.keys(OPERATIONS)
    for (const forbidden of ['exec', 'shell', 'run', 'command', 'eval', 'spawn']) {
      assert.ok(!names.includes(forbidden), `operation "${forbidden}" must not exist`)
    }
    assert.deepStrictEqual(names.sort(), ['automationTest', 'build', 'inspectContent', 'probe'])
  })

  // --- content inspection: the Bannon-shaped case --------------------------
  await check('a project with no Content directory reports the render/animate gap', async () => {
    const { OPERATIONS } = require(path.join(__dirname, 'server.js'))
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'nocontent-'))
    const up = path.join(dir, 'Demo.uproject')
    fs.writeFileSync(up, '{}')
    process.argv = ['node', 'server.js', '--projects', dir]
    delete require.cache[require.resolve(path.join(__dirname, 'server.js'))]
    const ops = require(path.join(__dirname, 'server.js')).OPERATIONS
    const res = await ops.inspectContent({ uproject: up })
    assert.strictEqual(res.counts.uasset, 0)
    assert.strictEqual(res.counts.umap, 0)
    assert.ok(res.evidence.includes('regardless of C++ correctness'))
    fs.rmSync(dir, { recursive: true, force: true })
  })

  console.log(`\n${passed} passed, ${failures.length} failed`)
  if (failures.length) process.exit(1)
})()
