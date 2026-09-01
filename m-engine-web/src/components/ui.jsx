import React from 'react'
import { AlertTriangle, Loader2, PlugZap } from 'lucide-react'

export function Card({ children, className = '' }) {
  return (
    <div
      className={`rounded-xl border border-line bg-surface-raised/70 p-4 shadow-lg shadow-black/20 ${className}`}
    >
      {children}
    </div>
  )
}

export function SectionTitle({ icon: Icon, children, action }) {
  return (
    <div className="mb-3 flex items-center justify-between gap-3">
      <h2 className="flex items-center gap-2 text-base font-semibold text-slate-100">
        {Icon ? <Icon size={18} className="text-accent" /> : null}
        {children}
      </h2>
      {action}
    </div>
  )
}

export function Button({ children, variant = 'default', className = '', ...props }) {
  const variants = {
    default: 'bg-surface-raised border-line hover:bg-line active:bg-line text-slate-200',
    primary: 'bg-accent border-accent hover:bg-blue-500 active:bg-blue-700 text-white',
    danger: 'bg-red-600/90 border-red-600 hover:bg-red-600 active:bg-red-700 text-white',
  }
  return (
    <button
      // min-height 44px: below that a target is unreliable under a thumb.
      className={`inline-flex min-h-[44px] items-center justify-center gap-2 rounded-lg border px-4 text-sm font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-40 ${variants[variant]} ${className}`}
      {...props}
    >
      {children}
    </button>
  )
}

export function Field({ label, hint, children }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-sm font-medium text-slate-300">{label}</span>
      {children}
      {hint ? <span className="mt-1.5 block text-xs text-slate-500">{hint}</span> : null}
    </label>
  )
}

export const inputClass =
  'w-full rounded-lg border border-line bg-surface-sunken px-3 py-2.5 text-slate-100 ' +
  'placeholder:text-slate-600 focus:border-accent focus:outline-none focus:ring-1 focus:ring-accent'

export function Spinner({ label }) {
  return (
    <div className="flex items-center gap-2 py-6 text-sm text-slate-400">
      <Loader2 size={16} className="animate-spin" />
      {label ?? 'Loading…'}
    </div>
  )
}

/**
 * Shown wherever live server data would go when no control plane is reachable.
 * REALITY_CONTRACT.md forbids substituting placeholder data for a missing
 * dependency, so this names the missing dependency instead of filling the gap.
 */
export function NotConfigured({ onOpenSettings, reason }) {
  return (
    <Card className="border-dashed">
      <div className="flex items-start gap-3">
        <PlugZap size={20} className="mt-0.5 shrink-0 text-slate-500" />
        <div className="min-w-0">
          <h3 className="font-medium text-slate-200">No control plane connected</h3>
          <p className="mt-1 text-sm text-slate-400">
            {reason ??
              'This view shows live state from the M. Engine control plane. Nothing is displayed because no server is reachable — not because there is nothing to show.'}
          </p>
          <p className="mt-2 text-xs text-slate-500">
            Run <code className="text-slate-400">cloud_control_plane</code>, then set its URL in
            Settings.
          </p>
          {onOpenSettings ? (
            <Button className="mt-3" onClick={onOpenSettings}>
              Open settings
            </Button>
          ) : null}
        </div>
      </div>
    </Card>
  )
}

export function ErrorNote({ children }) {
  return (
    <Card className="border-red-900/60 bg-red-950/30">
      <div className="flex items-start gap-3">
        <AlertTriangle size={18} className="mt-0.5 shrink-0 text-red-400" />
        <p className="min-w-0 break-words text-sm text-red-200">{children}</p>
      </div>
    </Card>
  )
}

export function Empty({ children }) {
  return <p className="py-6 text-center text-sm text-slate-500">{children}</p>
}

/** Renders arbitrary server JSON as labelled rows without inventing structure. */
export function KeyValues({ data }) {
  const entries = Object.entries(data ?? {})
  if (!entries.length) return <Empty>Server returned no fields.</Empty>
  return (
    <dl className="divide-y divide-line">
      {entries.map(([k, v]) => (
        <div key={k} className="flex items-start justify-between gap-4 py-2.5">
          <dt className="text-sm text-slate-400">{k}</dt>
          <dd className="min-w-0 break-words text-right font-mono text-sm text-slate-200">
            {typeof v === 'object' && v !== null ? JSON.stringify(v) : String(v)}
          </dd>
        </div>
      ))}
    </dl>
  )
}
