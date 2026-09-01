import React, { useEffect, useState, useSyncExternalStore } from 'react'
import {
  Download,
  Layers,
  MessageSquare,
  Settings as SettingsIcon,
  Terminal,
  WifiOff,
} from 'lucide-react'
import { getSettings, subscribe } from './lib/settings'
import { probe } from './lib/controlPlane'
import Chat from './screens/Chat'
import Engine from './screens/Engine'
import Capabilities from './screens/Capabilities'
import Settings from './screens/Settings'

const TABS = [
  { id: 'chat', label: 'Chat', icon: MessageSquare, Screen: Chat },
  { id: 'engine', label: 'Engine', icon: Terminal, Screen: Engine },
  { id: 'capabilities', label: 'Reality', icon: Layers, Screen: Capabilities },
  { id: 'settings', label: 'Settings', icon: SettingsIcon, Screen: Settings },
]

export default function App() {
  const settings = useSyncExternalStore(subscribe, getSettings)
  const [tab, setTab] = useState('chat')
  const [status, setStatus] = useState({ state: 'unconfigured' })
  const [online, setOnline] = useState(navigator.onLine)
  const [installEvent, setInstallEvent] = useState(null)

  useEffect(() => {
    const on = () => setOnline(true)
    const off = () => setOnline(false)
    addEventListener('online', on)
    addEventListener('offline', off)
    return () => {
      removeEventListener('online', on)
      removeEventListener('offline', off)
    }
  }, [])

  // Chrome fires this instead of showing its own prompt; holding the event lets
  // the app offer install at a moment that makes sense. iOS never fires it.
  useEffect(() => {
    const handler = (e) => {
      e.preventDefault()
      setInstallEvent(e)
    }
    addEventListener('beforeinstallprompt', handler)
    addEventListener('appinstalled', () => setInstallEvent(null))
    return () => removeEventListener('beforeinstallprompt', handler)
  }, [])

  // Poll the control plane for the header pill. 20s is slow enough to be free
  // on battery and fast enough that a restarted server shows up on its own.
  useEffect(() => {
    let cancelled = false
    const run = () =>
      probe(settings.controlPlaneUrl).then((s) => {
        if (!cancelled) setStatus(s)
      })
    run()
    const t = setInterval(run, 20000)
    return () => {
      cancelled = true
      clearInterval(t)
    }
  }, [settings.controlPlaneUrl])

  const active = TABS.find((t) => t.id === tab) ?? TABS[0]
  const Screen = active.Screen
  const openSettings = () => setTab('settings')

  return (
    <div className="flex h-[100dvh] flex-col bg-surface">
      <header className="flex shrink-0 items-center justify-between gap-3 border-b border-line bg-surface-raised/60 px-4 py-3">
        <div className="flex min-w-0 items-center gap-2">
          <Terminal size={20} className="shrink-0 text-accent" />
          <span className="truncate font-semibold tracking-wide text-slate-100">M. ENGINE</span>
        </div>

        <div className="flex shrink-0 items-center gap-2">
          {!online ? (
            <span className="flex items-center gap-1 text-xs text-amber-400">
              <WifiOff size={13} /> Offline
            </span>
          ) : null}

          {installEvent ? (
            <button
              onClick={async () => {
                installEvent.prompt()
                await installEvent.userChoice
                setInstallEvent(null)
              }}
              className="flex min-h-[36px] items-center gap-1.5 rounded-lg border border-accent bg-accent/15 px-2.5 text-xs font-medium text-blue-300"
            >
              <Download size={14} /> Install
            </button>
          ) : null}

          <StatusPill status={status} />
        </div>
      </header>

      <main className="min-h-0 flex-1 overflow-y-auto">
        <Screen settings={settings} onOpenSettings={openSettings} />
      </main>

      <nav className="grid shrink-0 grid-cols-4 border-t border-line bg-surface-raised/80 pb-[env(safe-area-inset-bottom)]">
        {TABS.map(({ id, label, icon: Icon }) => (
          <button
            key={id}
            onClick={() => setTab(id)}
            aria-current={tab === id ? 'page' : undefined}
            className={`flex min-h-[56px] flex-col items-center justify-center gap-1 text-[11px] font-medium transition-colors ${
              tab === id ? 'text-accent' : 'text-slate-500 active:text-slate-300'
            }`}
          >
            <Icon size={20} />
            {label}
          </button>
        ))}
      </nav>
    </div>
  )
}

function StatusPill({ status }) {
  const tone = {
    online: 'border-emerald-800 bg-emerald-950/50 text-emerald-300',
    offline: 'border-red-900 bg-red-950/50 text-red-300',
    unconfigured: 'border-line bg-surface text-slate-500',
  }[status.state]

  const label = {
    online: 'Connected',
    offline: 'No server',
    unconfigured: 'Standalone',
  }[status.state]

  return (
    <span
      title={status.message ?? label}
      className={`flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[11px] font-medium ${tone}`}
    >
      <span
        className={`h-1.5 w-1.5 rounded-full ${
          status.state === 'online'
            ? 'bg-emerald-400'
            : status.state === 'offline'
              ? 'bg-red-400'
              : 'bg-slate-600'
        }`}
      />
      {label}
    </span>
  )
}
