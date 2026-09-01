import React, { useCallback, useEffect, useRef, useState } from 'react'
import { Send, Square, Trash2 } from 'lucide-react'
import { PROVIDERS, streamChat } from '../lib/providers'
import { appendMessage, clearMessages, isAvailable, loadMessages } from '../lib/db'
import { Button, ErrorNote, inputClass } from '../components/ui'

export default function Chat({ settings, onOpenSettings }) {
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [streaming, setStreaming] = useState(false)
  const [error, setError] = useState(null)
  const [ready, setReady] = useState(false)
  const abortRef = useRef(null)
  const endRef = useRef(null)

  useEffect(() => {
    if (!isAvailable()) {
      setReady(true)
      return
    }
    loadMessages()
      .then(setMessages)
      .catch(() => {
        /* history is a convenience; a failure here must not block chat */
      })
      .finally(() => setReady(true))
  }, [])

  // Only glue to the bottom while a reply is arriving, so scrolling back through
  // history is not yanked away from the reader.
  useEffect(() => {
    if (streaming) endRef.current?.scrollIntoView({ block: 'end' })
  }, [messages, streaming])

  const stop = useCallback(() => {
    abortRef.current?.abort()
    abortRef.current = null
    setStreaming(false)
  }, [])

  async function send(e) {
    e?.preventDefault()
    const text = input.trim()
    if (!text || streaming) return

    setError(null)
    setInput('')

    const userMsg = { role: 'user', content: text, ts: Date.now() }
    const history = [...messages, userMsg]
    setMessages(history)
    appendMessage(userMsg).catch(() => {})

    setStreaming(true)
    const controller = new AbortController()
    abortRef.current = controller

    // The assistant turn is appended empty and mutated in place as deltas land.
    setMessages((m) => [...m, { role: 'assistant', content: '', ts: Date.now(), pending: true }])

    try {
      const full = await streamChat({
        settings,
        messages: history.map(({ role, content }) => ({ role, content })),
        signal: controller.signal,
        onDelta: (chunk) =>
          setMessages((m) => {
            const next = [...m]
            const last = next[next.length - 1]
            if (last?.role === 'assistant') {
              next[next.length - 1] = { ...last, content: last.content + chunk }
            }
            return next
          }),
      })
      setMessages((m) => {
        const next = [...m]
        next[next.length - 1] = { ...next[next.length - 1], pending: false }
        return next
      })
      appendMessage({ role: 'assistant', content: full, ts: Date.now() }).catch(() => {})
    } catch (err) {
      if (err.name !== 'AbortError') setError(err.message)
      // Drop the empty assistant turn so a failure does not leave a blank bubble.
      setMessages((m) => {
        const next = [...m]
        const last = next[next.length - 1]
        if (last?.role === 'assistant' && !last.content) next.pop()
        else if (last?.role === 'assistant') next[next.length - 1] = { ...last, pending: false }
        return next
      })
    } finally {
      setStreaming(false)
      abortRef.current = null
    }
  }

  async function reset() {
    stop()
    setMessages([])
    setError(null)
    await clearMessages().catch(() => {})
  }

  const spec = PROVIDERS[settings.provider]
  const needsKey = spec?.keyLabel && !settings.keys?.[settings.provider]

  return (
    <div className="flex h-full flex-col">
      <div className="min-h-0 flex-1 space-y-3 overflow-y-auto px-4 pb-4 pt-4">
        {needsKey ? (
          <div className="rounded-xl border border-dashed border-line bg-surface-raised/50 p-4">
            <h3 className="font-medium text-slate-200">Add an API key to start</h3>
            <p className="mt-1 text-sm text-slate-400">
              Chat runs directly from this device to {spec.label}. Your key is stored on this
              device only and is sent to {spec.label} and nowhere else.
            </p>
            <Button className="mt-3" variant="primary" onClick={onOpenSettings}>
              Open settings
            </Button>
          </div>
        ) : null}

        {ready && !messages.length && !needsKey ? (
          <p className="py-10 text-center text-sm text-slate-500">
            Ask M. Engine anything. History is kept on this device.
          </p>
        ) : null}

        {messages.map((m, i) => (
          <div
            key={i}
            className={`flex ${m.role === 'user' ? 'justify-end' : 'justify-start'}`}
          >
            <div
              className={`max-w-[85%] whitespace-pre-wrap break-words rounded-2xl px-3.5 py-2.5 text-sm leading-relaxed ${
                m.role === 'user'
                  ? 'rounded-br-sm bg-accent text-white'
                  : 'rounded-bl-sm border border-line bg-surface-raised text-slate-200'
              }`}
            >
              {m.content}
              {m.pending && !m.content ? (
                <span className="inline-block animate-pulse text-slate-500">▍</span>
              ) : null}
            </div>
          </div>
        ))}

        {error ? <ErrorNote>{error}</ErrorNote> : null}
        <div ref={endRef} />
      </div>

      <form
        onSubmit={send}
        className="flex items-end gap-2 border-t border-line bg-surface-raised/60 p-3"
      >
        {messages.length ? (
          <Button
            type="button"
            onClick={reset}
            aria-label="Clear conversation"
            className="!px-3 shrink-0"
          >
            <Trash2 size={16} />
          </Button>
        ) : null}
        <textarea
          rows={1}
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => {
            // Enter sends; Shift+Enter is a newline. On a touch keyboard Enter is
            // usually a newline key, so the send button stays the primary route.
            if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) send(e)
          }}
          placeholder="Command M. Engine…"
          className={`${inputClass} max-h-32 min-h-[44px] flex-1 resize-none py-3`}
        />
        {streaming ? (
          <Button type="button" variant="danger" onClick={stop} className="!px-3 shrink-0">
            <Square size={16} />
          </Button>
        ) : (
          <Button
            type="submit"
            variant="primary"
            disabled={!input.trim()}
            className="!px-3 shrink-0"
            aria-label="Send"
          >
            <Send size={16} />
          </Button>
        )}
      </form>
    </div>
  )
}
