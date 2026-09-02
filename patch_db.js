import fs from 'fs'

const path = '/app/applet/m-engine-web/src/lib/db.js'
let content = fs.readFileSync(path, 'utf-8')

// Inject sync dependencies
content = content.replace("const DB_NAME = 'mengine'", "import { getSettings } from './settings'\nconst DB_NAME = 'mengine'")

// Update appendMessage to send to server
const appendMessage = `
export async function appendMessage(message) {
  const record = { 
    ...message, 
    id: message.id || crypto.randomUUID(),
    ts: message.ts ?? Date.now() 
  }
  await tx('readwrite', (store) => store.add(record))
  
  // Sync to control plane
  const cpUrl = getSettings().controlPlaneUrl
  if (cpUrl) {
    try {
      const payload = [{
        eventId: record.id,
        timestamp: record.ts,
        actor: record.role === 'user' ? 'OWNER' : 'M_ENGINE',
        content: record.content,
        source: 'PWA',
        conversationId: 'default'
      }]
      await fetch(cpUrl + '/api/v1/ledger/sync', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      })
    } catch (e) {
      console.error('Failed to sync message to canonical ledger', e)
    }
  }
  return record
}

export async function syncFromCanonical() {
  const cpUrl = getSettings().controlPlaneUrl
  if (!cpUrl) return
  
  try {
    // Get latest timestamp we have
    let lastTs = 0
    const local = await loadMessages(1)
    if (local && local.length > 0) lastTs = local[0].ts

    const res = await fetch(cpUrl + '/api/v1/ledger/events?since=' + lastTs)
    if (res.ok) {
      const events = await res.json()
      if (events.length > 0) {
        await tx('readwrite', (store) => {
          for (const ev of events) {
            // Only add if source isn't PWA, to avoid duplicate rendering (or just let UI handle it)
            store.put({
              id: ev.eventId,
              role: ev.actor === 'OWNER' ? 'user' : 'assistant',
              content: ev.content,
              ts: ev.timestamp
            })
          }
        })
      }
    }
  } catch(e) {
    console.error('Canonical sync failed', e)
  }
}
`
// replace appendMessage implementation
content = content.replace(/export async function appendMessage\(message\) \{[\s\S]*?return record\n\}/, appendMessage)

fs.writeFileSync(path, content)
