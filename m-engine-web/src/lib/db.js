/**
 * Minimal IndexedDB wrapper for conversation history.
 *
 * Deliberately dependency-free: the surface used here is three object stores
 * and a cursor, and a library would cost more bytes on a phone than it saves.
 */

import { getSettings } from './settings'
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
  const record = { 
    ...message, 
    id: message.id || crypto.randomUUID(),
    ts: message.ts ?? Date.now() 
  }
  await tx('readwrite', (store) => store.add(record))

  // The local write is the durable act and has already happened. The push is
  // deliberately not awaited: sending a message must not wait on the control
  // plane, and a failure here must not look like a failure to save.
  const cpUrl = getSettings().controlPlaneUrl
  if (cpUrl) {
    const payload = [{
      eventId: record.id,
      timestamp: record.ts,
      actor: record.role === 'user' ? 'OWNER' : 'M_ENGINE',
      content: record.content,
      source: 'PWA',
      conversationId: 'default',
    }]
    fetch(cpUrl + '/api/v1/ledger/sync', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    }).catch((e) => console.error('Failed to sync message to canonical ledger', e))
  }
  return record
}

/**
 * High-water mark of events that ARRIVED BY SYNC.
 *
 * Deliberately not the newest message overall: every message typed here is
 * newer than anything the control plane already holds, so using the overall
 * newest would push the cursor past events this device has never seen and make
 * them permanently unreachable.
 */
async function lastSyncedTimestamp() {
  const all = await tx('readonly', (store) => store.getAll())
  const list = Array.isArray(all) ? all : []
  return list.reduce((max, m) => (m.syncedFrom && m.ts > max ? m.ts : max), 0)
}

export async function syncFromCanonical() {
  const cpUrl = getSettings().controlPlaneUrl
  if (!cpUrl) return

  try {
    const since = await lastSyncedTimestamp()
    const res = await fetch(cpUrl + '/api/v1/ledger/events?since=' + since)
    if (!res.ok) {
      console.error('Canonical sync rejected', res.status)
      return
    }
    const events = await res.json()
    if (!events.length) return

    await tx('readwrite', (store) => {
      for (const ev of events) {
        store.put({
          id: ev.eventId,
          role: ev.actor === 'OWNER' ? 'user' : 'assistant',
          content: ev.content,
          ts: ev.timestamp,
          // Marks the row as control-plane-sourced, and is what the cursor
          // above reads back. put() keyed by eventId makes a re-pull idempotent.
          syncedFrom: ev.source || 'CANONICAL',
        })
      }
    })
  } catch (e) {
    console.error('Canonical sync failed', e)
  }
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
