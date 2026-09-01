import React, { useState } from 'react'
import { CheckCircle2, KeyRound, Server, XCircle } from 'lucide-react'
import { PROVIDERS } from '../lib/providers'
import { probe } from '../lib/controlPlane'
import { setSettings } from '../lib/settings'
import { Button, Card, Field, SectionTitle, inputClass } from '../components/ui'

export default function Settings({ settings }) {
  const [testing, setTesting] = useState(false)
  const [result, setResult] = useState(null)

  const spec = PROVIDERS[settings.provider]

  async function test() {
    setTesting(true)
    setResult(null)
    setResult(await probe(settings.controlPlaneUrl))
    setTesting(false)
  }

  return (
    <div className="space-y-4 p-4">
      <Card>
        <SectionTitle icon={Server}>Control plane</SectionTitle>
        <Field
          label="Server URL"
          hint="The cloud_control_plane Ktor server. Leave blank to run standalone — chat still works."
        >
          <input
            className={inputClass}
            type="url"
            inputMode="url"
            autoCapitalize="none"
            autoCorrect="off"
            spellCheck={false}
            placeholder="http://192.168.1.20:8080"
            value={settings.controlPlaneUrl}
            onChange={(e) => setSettings({ controlPlaneUrl: e.target.value.trim() })}
          />
        </Field>

        <div className="mt-3 flex items-center gap-3">
          <Button onClick={test} disabled={testing || !settings.controlPlaneUrl}>
            {testing ? 'Testing…' : 'Test connection'}
          </Button>
          {result ? (
            <span
              className={`flex min-w-0 items-center gap-1.5 text-sm ${
                result.state === 'online' ? 'text-emerald-400' : 'text-red-400'
              }`}
            >
              {result.state === 'online' ? (
                <>
                  <CheckCircle2 size={16} /> Online
                </>
              ) : (
                <>
                  <XCircle size={16} className="shrink-0" />
                  <span className="truncate">{result.message ?? 'Unreachable'}</span>
                </>
              )}
            </span>
          ) : null}
        </div>

        <p className="mt-3 text-xs text-slate-500">
          Served over HTTPS, this page cannot call an <code>http://</code> server — browsers
          block mixed content. Use HTTPS for the control plane, or open this app over http on
          your local network.
        </p>
      </Card>

      <Card>
        <SectionTitle icon={KeyRound}>Chat provider</SectionTitle>

        <Field label="Provider">
          <select
            className={inputClass}
            value={settings.provider}
            onChange={(e) => setSettings({ provider: e.target.value, model: '' })}
          >
            {Object.entries(PROVIDERS).map(([id, p]) => (
              <option key={id} value={id}>
                {p.label}
              </option>
            ))}
          </select>
        </Field>

        <div className="mt-3">
          <Field label="Model" hint={`Default: ${spec.defaultModel}`}>
            <input
              className={inputClass}
              list="model-suggestions"
              placeholder={spec.defaultModel}
              autoCapitalize="none"
              autoCorrect="off"
              spellCheck={false}
              value={settings.model}
              onChange={(e) => setSettings({ model: e.target.value.trim() })}
            />
            <datalist id="model-suggestions">
              {spec.models.map((m) => (
                <option key={m} value={m} />
              ))}
            </datalist>
          </Field>
        </div>

        {spec.keyLabel ? (
          <div className="mt-3">
            <Field label={`${spec.label} ${spec.keyLabel}`} hint={spec.keyHint}>
              <input
                className={inputClass}
                type="password"
                autoCapitalize="none"
                autoCorrect="off"
                spellCheck={false}
                placeholder="Paste key"
                value={settings.keys[settings.provider] ?? ''}
                onChange={(e) => setSettings({ keys: { [settings.provider]: e.target.value.trim() } })}
              />
            </Field>
          </div>
        ) : (
          <div className="mt-3">
            <Field
              label="Ollama URL"
              hint="Ollama must allow this origin: set OLLAMA_ORIGINS before starting it."
            >
              <input
                className={inputClass}
                type="url"
                inputMode="url"
                autoCapitalize="none"
                autoCorrect="off"
                spellCheck={false}
                value={settings.ollamaUrl}
                onChange={(e) => setSettings({ ollamaUrl: e.target.value.trim() })}
              />
            </Field>
          </div>
        )}

        <p className="mt-3 text-xs text-slate-500">
          Keys are stored on this device only and are sent to the provider they belong to and
          nowhere else. They are not synced and never reach the control plane. Anyone with
          access to this device can read them.
        </p>
      </Card>

      <Card>
        <SectionTitle>About</SectionTitle>
        <p className="text-sm text-slate-400">
          M. Engine web control plane. Installable — use “Add to Home Screen” to run it
          fullscreen. The app shell works offline; live control plane and chat data need a
          connection.
        </p>
      </Card>
    </div>
  )
}
