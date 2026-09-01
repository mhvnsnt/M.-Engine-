/**
 * Settings live in localStorage rather than IndexedDB: they are small, read
 * synchronously during the first render, and must survive a service-worker
 * update. Conversations go in IndexedDB (see db.js) because they grow.
 *
 * API keys entered here are held on this device only and are sent to the
 * provider they belong to and nowhere else. That is a deliberate compromise:
 * REALITY_CONTRACT.md asks for connector-first delegated auth, and no such flow
 * exists for a static browser app with no backend. OpenRouter's OAuth PKCE flow
 * is the connector-first upgrade path when a redirect URI can be registered.
 */

const KEY = 'mengine.settings.v1'

export const DEFAULTS = {
  controlPlaneUrl: '',
  provider: 'openrouter',
  model: '',
  keys: {
    openrouter: '',
    anthropic: '',
    gemini: '',
    ollama: '',
  },
  ollamaUrl: 'http://localhost:11434',
}

const listeners = new Set()
let cache = read()

function read() {
  try {
    const raw = localStorage.getItem(KEY)
    if (!raw) return { ...DEFAULTS }
    const parsed = JSON.parse(raw)
    return { ...DEFAULTS, ...parsed, keys: { ...DEFAULTS.keys, ...(parsed.keys ?? {}) } }
  } catch {
    // A corrupt or unreadable store must not stop the app from booting.
    return { ...DEFAULTS }
  }
}

export function getSettings() {
  return cache
}

export function setSettings(patch) {
  cache = { ...cache, ...patch, keys: { ...cache.keys, ...(patch.keys ?? {}) } }
  try {
    localStorage.setItem(KEY, JSON.stringify(cache))
  } catch {
    // Private-mode Safari throws on write. Keep the in-memory value so the
    // current session still works; it just will not persist.
  }
  listeners.forEach((l) => l())
  return cache
}

export function subscribe(listener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

export function activeKey(s = cache) {
  return s.provider === 'ollama' ? 'local' : s.keys[s.provider]
}
