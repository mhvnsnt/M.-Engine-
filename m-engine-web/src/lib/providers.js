/**
 * Direct-from-browser LLM providers, for standalone mode (no control plane).
 *
 * Each provider here is reachable from a browser: OpenRouter and Gemini send
 * permissive CORS headers, Anthropic requires an explicit opt-in header, and
 * Ollama runs on the user's own machine. Providers that refuse browser origins
 * outright are not listed rather than listed and broken.
 *
 * All four stream, because a non-streaming reply on a phone reads as a hang.
 */

export const PROVIDERS = {
  openrouter: {
    label: 'OpenRouter',
    keyLabel: 'API key',
    keyHint: 'openrouter.ai/keys',
    defaultModel: 'anthropic/claude-sonnet-5',
    models: [
      'anthropic/claude-opus-5',
      'anthropic/claude-sonnet-5',
      'openai/gpt-5',
      'google/gemini-2.5-pro',
    ],
  },
  anthropic: {
    label: 'Anthropic',
    keyLabel: 'API key',
    keyHint: 'console.anthropic.com',
    defaultModel: 'claude-opus-5',
    models: ['claude-opus-5', 'claude-sonnet-5', 'claude-haiku-4-5-20251001'],
  },
  gemini: {
    label: 'Google Gemini',
    keyLabel: 'API key',
    keyHint: 'aistudio.google.com/apikey',
    defaultModel: 'gemini-2.5-pro',
    models: ['gemini-2.5-pro', 'gemini-2.5-flash'],
  },
  ollama: {
    label: 'Ollama (local)',
    keyLabel: null,
    keyHint: 'Runs on your own machine — no key needed',
    defaultModel: 'llama3.2',
    models: ['llama3.2', 'qwen2.5-coder', 'mistral'],
  },
}

const SYSTEM_PROMPT =
  'You are M. Engine, a personal AI engineering assistant. Be concise and concrete. ' +
  'Distinguish observation from inference. Never claim a result you have not verified.'

/** Reads an SSE body and yields each `data:` payload as a parsed object. */
async function* sse(response) {
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })

    // Hold the final fragment back — it is very often a partial JSON object.
    const lines = buffer.split('\n')
    buffer = lines.pop() ?? ''

    for (const line of lines) {
      const trimmed = line.trim()
      if (!trimmed.startsWith('data:')) continue
      const payload = trimmed.slice(5).trim()
      if (!payload || payload === '[DONE]') continue
      try {
        yield JSON.parse(payload)
      } catch {
        // Ignore keep-alive comments and any non-JSON frame.
      }
    }
  }
}

/** Reads a newline-delimited JSON body (Ollama's streaming format). */
async function* ndjson(response) {
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() ?? ''
    for (const line of lines) {
      if (!line.trim()) continue
      try {
        yield JSON.parse(line)
      } catch {
        /* partial frame */
      }
    }
  }
}

async function assertOk(res, providerLabel) {
  if (res.ok) return
  let detail = ''
  try {
    detail = (await res.text()).slice(0, 300)
  } catch {
    /* body already consumed or unreadable */
  }
  if (res.status === 401 || res.status === 403) {
    throw new Error(`${providerLabel} rejected the API key (${res.status}). ${detail}`)
  }
  if (res.status === 429) {
    throw new Error(`${providerLabel} rate limit reached (429). ${detail}`)
  }
  throw new Error(`${providerLabel} returned ${res.status}. ${detail}`)
}

/**
 * Streams a completion. `messages` is [{role:'user'|'assistant', content}].
 * Calls onDelta(text) per chunk. Returns the full text.
 */
export async function streamChat({ settings, messages, onDelta, signal }) {
  const provider = settings.provider
  const spec = PROVIDERS[provider]
  if (!spec) throw new Error(`Unknown provider: ${provider}`)

  const model = settings.model || spec.defaultModel
  const key = settings.keys?.[provider]
  if (spec.keyLabel && !key) {
    throw new Error(`No ${spec.label} API key set. Add one in Settings.`)
  }

  let full = ''
  const push = (t) => {
    if (!t) return
    full += t
    onDelta?.(t)
  }

  if (provider === 'anthropic') {
    const res = await fetch('https://api.anthropic.com/v1/messages', {
      method: 'POST',
      signal,
      headers: {
        'Content-Type': 'application/json',
        'x-api-key': key,
        'anthropic-version': '2023-06-01',
        // Without this header the API refuses browser origins outright.
        'anthropic-dangerous-direct-browser-access': 'true',
      },
      body: JSON.stringify({
        model,
        max_tokens: 2048,
        system: SYSTEM_PROMPT,
        stream: true,
        messages,
      }),
    })
    await assertOk(res, spec.label)
    for await (const evt of sse(res)) {
      if (evt.type === 'content_block_delta' && evt.delta?.type === 'text_delta') {
        push(evt.delta.text)
      }
    }
    return full
  }

  if (provider === 'gemini') {
    const url =
      `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}` +
      `:streamGenerateContent?alt=sse&key=${encodeURIComponent(key)}`
    const res = await fetch(url, {
      method: 'POST',
      signal,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        systemInstruction: { parts: [{ text: SYSTEM_PROMPT }] },
        contents: messages.map((m) => ({
          role: m.role === 'assistant' ? 'model' : 'user',
          parts: [{ text: m.content }],
        })),
      }),
    })
    await assertOk(res, spec.label)
    for await (const evt of sse(res)) {
      const parts = evt.candidates?.[0]?.content?.parts ?? []
      for (const p of parts) push(p.text)
    }
    return full
  }

  if (provider === 'ollama') {
    const base = (settings.ollamaUrl || 'http://localhost:11434').replace(/\/+$/, '')
    const res = await fetch(`${base}/api/chat`, {
      method: 'POST',
      signal,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        model,
        stream: true,
        messages: [{ role: 'system', content: SYSTEM_PROMPT }, ...messages],
      }),
    })
    await assertOk(res, spec.label)
    for await (const evt of ndjson(res)) push(evt.message?.content)
    return full
  }

  // OpenRouter — OpenAI-compatible chat completions.
  const res = await fetch('https://openrouter.ai/api/v1/chat/completions', {
    method: 'POST',
    signal,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${key}`,
      'HTTP-Referer': location.origin,
      'X-Title': 'M. Engine',
    },
    body: JSON.stringify({
      model,
      stream: true,
      messages: [{ role: 'system', content: SYSTEM_PROMPT }, ...messages],
    }),
  })
  await assertOk(res, spec.label)
  for await (const evt of sse(res)) push(evt.choices?.[0]?.delta?.content)
  return full
}
