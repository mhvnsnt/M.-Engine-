/**
 * Minimal IndexedDB wrapper for conversation history.
 *
 * Deliberately dependency-free: the surface used here is three object stores
 * and a cursor, and a library would cost more bytes on a phone than it saves.
 */

const DB_NAME = 'mengine'
const DB_VERSION = 1
const STORE = 'messages'

let dbPromise = null

function open() {
  if (dbPromise) return dbPromise
  dbPromise = new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION)
    req.onupgradeneeded = () => {
      const db = req.result
      if (!db.objectStoreNames.contains(STORE)) {
        const store = db.createObjectStore(STORE, { keyPath: 'id', autoIncrement: true })
        store.createIndex('ts', 'ts')
      }
    }
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
  return dbPromise
}

function tx(mode, fn) {
  return open().then(
    (db) =>
      new Promise((resolve, reject) => {
        const t = db.transaction(STORE, mode)
        const store = t.objectStore(STORE)
        let out
        try {
          out = fn(store)
        } catch (e) {
          reject(e)
          return
        }
        t.oncomplete = () => resolve(out?.result ?? out)
        t.onerror = () => reject(t.error)
        t.onabort = () => reject(t.error)
      }),
  )
}

export async function appendMessage(message) {
  const record = { ...message, ts: message.ts ?? Date.now() }
  await tx('readwrite', (store) => store.add(record))
  return record
}

export async function loadMessages(limit = 200) {
  const all = await tx('readonly', (store) => store.getAll())
  const list = Array.isArray(all) ? all : []
  return list.sort((a, b) => a.ts - b.ts).slice(-limit)
}

export async function clearMessages() {
  await tx('readwrite', (store) => store.clear())
}

/** IndexedDB is unavailable in some private-browsing contexts. */
export function isAvailable() {
  return typeof indexedDB !== 'undefined'
}
