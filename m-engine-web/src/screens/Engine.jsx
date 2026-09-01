import React, { useCallback, useEffect, useState } from 'react'
import { Activity, Gauge, Pause, Play, RefreshCw, ShieldAlert, Waves } from 'lucide-react'
import { createClient } from '../lib/controlPlane'
import {
  Button,
  Card,
  Empty,
  ErrorNote,
  KeyValues,
  NotConfigured,
  SectionTitle,
  Spinner,
} from '../components/ui'

export default function Engine({ settings, onOpenSettings }) {
  const url = settings.controlPlaneUrl
  const [state, setState] = useState({ loading: true })
  const [busy, setBusy] = useState(null)

  const load = useCallback(async () => {
    if (!url) {
      setState({ loading: false })
      return
    }
    setState((s) => ({ ...s, loading: true, error: null }))
    const client = createClient(url)

    // Each panel is fetched independently: one dead endpoint should not blank the
    // whole screen, so failures are captured per-call rather than thrown.
    const settle = (p) => p.then((value) => ({ value }), (error) => ({ error }))
    const [status, mindstream, opportunities, telemetry, cycle] = await Promise.all([
      settle(client.controlPlane()),
      settle(client.mindstream()),
      settle(client.opportunities()),
      settle(client.telemetry()),
      settle(client.activeCycle()),
    ])

    setState({
      loading: false,
      // A total failure of the first call is a connection problem worth naming.
      error: status.error ? status.error.message : null,
      status: status.value,
      mindstream: mindstream.value,
      opportunities: opportunities.value,
      telemetry: telemetry.value,
      // /cycles/active answers 404 when idle — that is "no cycle", not an error.
      cycle: cycle.value,
    })
  }, [url])

  useEffect(() => {
    load()
  }, [load])

  async function govern(action) {
    setBusy(action)
    try {
      await createClient(url)[action]()
      await load()
    } catch (e) {
      setState((s) => ({ ...s, error: e.message }))
    } finally {
      setBusy(null)
    }
  }

  if (!url) {
    return (
      <div className="space-y-4 p-4">
        <NotConfigured onOpenSettings={onOpenSettings} />
      </div>
    )
  }

  if (state.loading) return <div className="p-4"><Spinner label="Reading control plane…" /></div>

  return (
    <div className="space-y-4 p-4">
      {state.error ? <ErrorNote>{state.error}</ErrorNote> : null}

      <Card>
        <SectionTitle
          icon={Activity}
          action={
            <Button onClick={load} className="!min-h-0 !px-2.5 !py-1.5 text-xs">
              <RefreshCw size={14} /> Refresh
            </Button>
          }
        >
          Governance
        </SectionTitle>

        {state.status ? (
          <div className="mb-4 flex flex-wrap gap-2">
            <Pill
              on={state.status.autonomyEnabled}
              onLabel="Autonomy enabled"
              offLabel="Autonomy paused"
            />
            <Pill
              on={!state.status.emergencyStop}
              onLabel="No emergency stop"
              offLabel="EMERGENCY STOP ACTIVE"
              danger
            />
          </div>
        ) : (
          <Empty>Could not read governance state.</Empty>
        )}

        <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
          <Button onClick={() => govern('pause')} disabled={busy}>
            <Pause size={16} /> Pause
          </Button>
          <Button variant="primary" onClick={() => govern('resume')} disabled={busy}>
            <Play size={16} /> Resume
          </Button>
          <Button variant="danger" onClick={() => govern('emergencyStop')} disabled={busy}>
            <ShieldAlert size={16} /> Emergency stop
          </Button>
        </div>
      </Card>

      <Card>
        <SectionTitle icon={Waves}>Mindstream</SectionTitle>
        {Array.isArray(state.mindstream) && state.mindstream.length ? (
          <ol className="max-h-72 space-y-1.5 overflow-y-auto rounded-lg bg-black/40 p-3 font-mono text-xs text-emerald-300">
            {state.mindstream.map((line, i) => (
              <li key={i} className="break-words">{line}</li>
            ))}
          </ol>
        ) : (
          <Empty>Ledger is empty — no cycles have emitted yet.</Empty>
        )}
      </Card>

      <Card>
        <SectionTitle icon={Activity}>Opportunities</SectionTitle>
        {Array.isArray(state.opportunities) && state.opportunities.length ? (
          <ul className="space-y-2">
            {state.opportunities.map((o, i) => (
              <li
                key={i}
                className="rounded-lg border border-line bg-surface-sunken px-3 py-2.5 text-sm text-slate-300"
              >
                {o}
              </li>
            ))}
          </ul>
        ) : (
          <Empty>No pending opportunities.</Empty>
        )}
      </Card>

      <Card>
        <SectionTitle icon={Activity}>Active cycle</SectionTitle>
        {state.cycle ? <KeyValues data={state.cycle} /> : <Empty>No cycle running.</Empty>}
      </Card>

      <Card>
        <SectionTitle icon={Gauge}>Telemetry</SectionTitle>
        {state.telemetry ? (
          <KeyValues data={state.telemetry} />
        ) : (
          <Empty>No telemetry returned.</Empty>
        )}
      </Card>
    </div>
  )
}

function Pill({ on, onLabel, offLabel, danger }) {
  const good = 'border-emerald-800 bg-emerald-950/50 text-emerald-300'
  const bad = danger
    ? 'border-red-800 bg-red-950/50 text-red-300'
    : 'border-amber-800 bg-amber-950/50 text-amber-300'
  return (
    <span className={`rounded-full border px-3 py-1 text-xs font-medium ${on ? good : bad}`}>
      {on ? onLabel : offLabel}
    </span>
  )
}
