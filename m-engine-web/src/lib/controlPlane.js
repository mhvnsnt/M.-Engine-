/**
 * Client for the cloud_control_plane Ktor API.
 *
 * Routes and response shapes are taken from
 * cloud_control_plane/src/main/kotlin/com/example/ai/cloud/ControlPlaneServer.kt
 * and AgencyLedgerRepository.kt — not inferred. Where the server returns a bare
 * List<String> (mindstream, opportunities) this client returns exactly that.
 *
 * The app is fully usable with no control plane configured. Every call throws
 * NotConfiguredError in that case and the UI renders a disconnected state rather
 * than inventing data.
 */

export class NotConfiguredError extends Error {
  constructor() {
    super('No control plane URL configured')
    this.name = 'NotConfiguredError'
  }
}

export class ControlPlaneError extends Error {
  constructor(message, status) {
    super(message)
    this.name = 'ControlPlaneError'
    this.status = status
  }
}

function normalise(baseUrl) {
  if (!baseUrl) throw new NotConfiguredError()
  return baseUrl.replace(/\/+$/, '')
}

async function request(baseUrl, path, { method = 'GET', body, timeoutMs = 8000 } = {}) {
  const url = normalise(baseUrl) + path

  // A control plane on a home network can hang rather than refuse, so every
  // request carries its own deadline instead of waiting on the browser default.
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), timeoutMs)

  let res
  try {
    res = await fetch(url, {
      method,
      signal: controller.signal,
      headers: body ? { 'Content-Type': 'application/json' } : undefined,
      body: body ? JSON.stringify(body) : undefined,
    })
  } catch (e) {
    // fetch rejects identically for DNS failure, connection refused, a CORS
    // rejection and an abort, so the message has to stay honest about that.
    if (e.name === 'AbortError') {
      throw new ControlPlaneError(`Timed out after ${timeoutMs}ms: ${url}`)
    }
    throw new ControlPlaneError(
      `Could not reach ${url}. The server may be down, or it may not allow this origin (CORS).`,
    )
  } finally {
    clearTimeout(timer)
  }

  if (!res.ok) {
    throw new ControlPlaneError(`${res.status} ${res.statusText} from ${path}`, res.status)
  }

  const text = await res.text()
  if (!text) return null
  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

export function createClient(baseUrl) {
  const call = (path, opts) => request(baseUrl, path, opts)
  return {
    baseUrl,
    health: () => call('/health', { timeoutMs: 4000 }),
    ready: () => call('/ready', { timeoutMs: 4000 }),

    mindstream: () => call('/api/v1/mindstream'),
    opportunities: () => call('/api/v1/opportunities'),
    controlPlane: () => call('/api/v1/control_plane'),

    capabilities: () => call('/api/v1/capabilities'),
    capabilityTransitions: () => call('/api/v1/capabilities/transitions'),
    verifyCapability: (id) =>
      call(`/api/v1/capabilities/${encodeURIComponent(id)}/verify`, { method: 'POST' }),
    toggleCapability: (id, enabled) =>
      call(`/api/v1/capabilities/${encodeURIComponent(id)}/toggle`, {
        method: 'POST',
        body: { enabled },
      }),
    realitySweep: () =>
      call('/api/v1/capabilities/reality_sweep', { method: 'POST', timeoutMs: 30000 }),

    activeCycle: () => call('/api/v1/cycles/active'),
    cancelCycle: (id) =>
      call(`/api/v1/cycles/${encodeURIComponent(id)}/cancel`, { method: 'POST' }),
    cancelWorker: (id) =>
      call(`/api/v1/workers/${encodeURIComponent(id)}/cancel`, { method: 'POST' }),

    telemetry: () => call('/api/v1/telemetry'),
    tandem: () => call('/api/v1/tandem'),
    developmentSignal: (type, project, intent) =>
      call('/api/v1/development_signals', {
        method: 'POST',
        body: { type, project, intent },
      }),

    pause: () => call('/api/v1/control_plane/pause', { method: 'POST' }),
    resume: () => call('/api/v1/control_plane/resume', { method: 'POST' }),
    emergencyStop: () => call('/api/v1/control_plane/emergency_stop', { method: 'POST' }),
  }
}

/** Probes /health. Returns a status rather than throwing, for the status pill. */
export async function probe(baseUrl) {
  if (!baseUrl) return { state: 'unconfigured' }
  try {
    await createClient(baseUrl).health()
    return { state: 'online' }
  } catch (e) {
    if (e instanceof NotConfiguredError) return { state: 'unconfigured' }
    return { state: 'offline', message: e.message }
  }
}
