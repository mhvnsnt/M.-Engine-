# M. Engine Supervisor

A REAL process supervisor daemon for the M. Engine operation. This is the
answer to the daemon-restart problem: when the VM restarts and kills
everything, the cron watchdog brings back the supervisor, and the supervisor
brings back every registered worker from its on-disk registry.

## The problem it solves

Subagents and long-running workers die when the VM/daemon restarts:
"subagent was active before daemon restart but has no live runtime handle."

The recovery chain:
```
cron (every 5 min) → watchdog.sh → supervisor (port 8771) → registered workers
```

- **cron** is the outermost loop. It survives VM restarts (runtime-managed).
- **watchdog.sh** checks if the supervisor is alive (PID + /health). If dead, restarts it.
- **supervisor** reads its on-disk registry on boot, then probes every registered
  worker every 15 seconds. Dead workers get restarted with their registered command.
- **Registry** (`registry.json`) survives supervisor restarts. Workers survive too.

## Files

| File | Purpose |
|------|---------|
| `server.js` | The supervisor daemon (Node stdlib only, zero deps) |
| `watchdog.sh` | Cron-run health check + restarter (executable) |
| `registry.json` | Worker registry (auto-created, persisted to disk) |
| `supervisor.pid` | PID file (auto-managed) |
| `supervisor.log` | Supervisor event log (JSON lines) |
| `worker-<name>.log` | Per-worker stdout/stderr |

## Quick start

```bash
# Start the supervisor (or let the watchdog do it)
cd ~/workspace/M.-Engine-/tools/supervisor
node server.js --port 8771 &

# Check health
curl http://127.0.0.1:8771/health

# Register a worker
curl -X POST http://127.0.0.1:8771/workers/register \
  -H 'Content-Type: application/json' \
  -d '{"name":"my-worker","command":"python3 /path/to/worker.py","cwd":"/path/to/dir"}'

# List workers with live status
curl http://127.0.0.1:8771/workers

# Force-restart a worker
curl -X POST http://127.0.0.1:8771/workers/restart \
  -H 'Content-Type: application/json' \
  -d '{"name":"my-worker"}'

# Deregister (stops supervision, does not kill the process)
curl -X POST http://127.0.0.1:8771/workers/deregister \
  -H 'Content-Type: application/json' \
  -d '{"name":"my-worker"}'

# Tail a worker's log
curl 'http://127.0.0.1:8771/workers/my-worker/log?lines=50'
```

## For agents: how to register long-running work

If you start something that must survive (a render, a download, a watcher),
register it with the supervisor:

```bash
curl -s -X POST http://127.0.0.1:8771/workers/register \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "gumball-segment-pull",
    "command": "python3 /home/hatch/workspace/cnleaks-pull-tmp/pull-gumball.py",
    "cwd": "/home/hatch/workspace/cnleaks-pull-tmp"
  }'
```

Rules:
- `name`: alphanumeric + dash/underscore, max 64 chars. Must be unique.
- `command`: the exact command to (re)start the worker. No pipes, no redirects
  (stdout/stderr go to `worker-<name>.log` automatically).
- `cwd`: must exist. The worker runs there.
- `env` (optional): extra environment variables as a JSON object.

The supervisor probes every 15 seconds. If your worker dies, it gets restarted
automatically. After 5 consecutive failed restarts, the worker is DISABLED
(to avoid hot-looping) — check `/workers` and fix the command, then hit
`/workers/restart` to re-enable.

When your work is done, deregister it:
```bash
curl -s -X POST http://127.0.0.1:8771/workers/deregister \
  -H 'Content-Type: application/json' \
  -d '{"name":"gumball-segment-pull"}'
```

## Cron watchdog

The watchdog runs every 5 minutes via the `m-engine-supervisor-watchdog` cron job.
It is silent when the supervisor is healthy. If the supervisor is dead, it
restarts it and logs the event.

To manually run the watchdog:
```bash
bash ~/workspace/M.-Engine-/tools/supervisor/watchdog.sh
```

## Design principles (M. Engine reality-first)

- **Health is probed, never assumed.** `kill -0` on the PID + HTTP `/health`.
- **Registry survives restarts.** JSON on disk, atomic write (tmp + rename).
- **No arbitrary command execution via API.** Workers are registered with a
  fixed command; there is no endpoint that takes a shell string at request time.
- **Failures are data.** Every start failure is logged with the error. After 5
  consecutive failures, the worker is disabled (not hot-looped forever).
- **Zero dependencies.** Node stdlib only. Drops onto any machine with Node.

## Testing performed

1. Started supervisor → `/health` returned `UP`.
2. Registered `test-sleeper` (`sleep 300`) → started, PID assigned, `alive: true`.
3. `kill -9` on the worker → within 20s the supervisor restarted it
   (new PID, `restarts: 1`, `alive: true`).
4. `kill -9` on the supervisor → ran `watchdog.sh` → supervisor back UP
   within seconds, registry intact, worker still tracked.
5. Deregistered test worker → registry empty, clean state.

All real execution, no mocks.
