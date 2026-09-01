import React, { useCallback, useEffect, useState } from 'react'
import { Layers, RefreshCw, ScanSearch } from 'lucide-react'
import { createClient } from '../lib/controlPlane'
import {
  Button,
  Card,
  Empty,
  ErrorNote,
  NotConfigured,
  SectionTitle,
  Spinner,
} from '../components/ui'

// REALITY_CONTRACT.md defines these states. Colouring them by how real they are
// is the whole point of the screen: a green MOCK would defeat the contract.
const REALITY_TONE = {
  REAL_AND_CONNECTED: 'border-emerald-800 bg-emerald-950/50 text-emerald-300',
  REAL_BUT_UNCONFIGURED: 'border-sky-800 bg-sky-950/50 text-sky-300',
  REAL_BUT_UNVERIFIED: 'border-sky-800 bg-sky-950/50 text-sky-300',
  PARTIAL_REAL_IMPLEMENTATION: 'border-amber-800 bg-amber-950/50 text-amber-300',
  BLOCKED_BY_EXTERNAL_DEPENDENCY: 'border-amber-800 bg-amber-950/50 text-amber-300',
  SIMULATION: 'border-red-800 bg-red-950/50 text-red-300',
  MOCK: 'border-red-800 bg-red-950/50 text-red-300',
  STUB: 'border-red-800 bg-red-950/50 text-red-300',
}

export default function Capabilities({ settings, onOpenSettings }) {
  const url = settings.controlPlaneUrl
  const [items, setItems] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(null)

  const load = useCallback(async () => {
    if (!url) {
      setLoading(false)
      return
    }
    setLoading(true)
    setError(null)
    try {
      setItems(await createClient(url).capabilities())
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }, [url])

  useEffect(() => {
    load()
  }, [load])

  async function act(fn, id) {
    setBusy(id)
    try {
      await fn()
      await load()
    } catch (e) {
      setError(e.message)
    } finally {
      setBusy(null)
    }
  }

  if (!url) {
    return (
      <div className="p-4">
        <NotConfigured
          onOpenSettings={onOpenSettings}
          reason="The capability reality matrix is held by the control plane ledger. Without it there is nothing to read."
        />
      </div>
    )
  }

  if (loading) return <div className="p-4"><Spinner label="Reading capabilities…" /></div>

  const client = createClient(url)

  return (
    <div className="space-y-4 p-4">
      {error ? <ErrorNote>{error}</ErrorNote> : null}

      <div className="flex gap-2">
        <Button onClick={load} className="flex-1">
          <RefreshCw size={16} /> Refresh
        </Button>
        <Button
          variant="primary"
          className="flex-1"
          disabled={busy === '__sweep'}
          onClick={() => act(() => client.realitySweep(), '__sweep')}
        >
          <ScanSearch size={16} /> Reality sweep
        </Button>
      </div>

      <Card>
        <SectionTitle icon={Layers}>Capability reality</SectionTitle>
        {Array.isArray(items) && items.length ? (
          <ul className="space-y-2">
            {items.map((cap, i) => {
              const id = cap.id ?? cap.name ?? String(i)
              const reality = cap.reality ?? cap.realityState ?? cap.state
              return (
                <li
                  key={id}
                  className="rounded-lg border border-line bg-surface-sunken p-3"
                >
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <span className="min-w-0 break-words font-medium text-slate-200">
                      {cap.name ?? id}
                    </span>
                    {reality ? (
                      <span
                        className={`shrink-0 rounded-full border px-2.5 py-0.5 text-[11px] font-medium ${
                          REALITY_TONE[reality] ?? 'border-line bg-surface text-slate-400'
                        }`}
                      >
                        {reality}
                      </span>
                    ) : null}
                  </div>

                  {cap.evidence ? (
                    <p className="mt-1.5 break-words text-xs text-slate-500">{cap.evidence}</p>
                  ) : null}

                  <div className="mt-2.5 flex gap-2">
                    <Button
                      className="!min-h-0 !px-2.5 !py-1.5 text-xs"
                      disabled={busy === id}
                      onClick={() => act(() => client.verifyCapability(id), id)}
                    >
                      Verify
                    </Button>
                    <Button
                      className="!min-h-0 !px-2.5 !py-1.5 text-xs"
                      disabled={busy === id}
                      onClick={() => act(() => client.toggleCapability(id, !cap.enabled), id)}
                    >
                      {cap.enabled ? 'Disable' : 'Enable'}
                    </Button>
                  </div>
                </li>
              )
            })}
          </ul>
        ) : (
          <Empty>The ledger holds no capabilities yet.</Empty>
        )}
      </Card>
    </div>
  )
}
