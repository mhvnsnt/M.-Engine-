#!/bin/bash
# M. Engine Supervisor watchdog.
#
# Run from cron every 5 minutes. Checks if the supervisor daemon is alive
# (PID file + /health endpoint). If dead, restarts it.
#
# This is the outer recovery loop: cron -> watchdog -> supervisor -> workers.
# When the VM restarts and kills everything, cron brings back the watchdog,
# the watchdog brings back the supervisor, and the supervisor brings back
# every registered worker from its on-disk registry.

SUP_DIR="$HOME/workspace/M.-Engine-/tools/supervisor"
PID_FILE="$SUP_DIR/supervisor.pid"
PORT="${SUPERVISOR_PORT:-8771}"
LOG="$SUP_DIR/supervisor.log"

ts() { date -u +"%Y-%m-%dT%H:%M:%SZ"; }

# 1. Check PID file
ALIVE=false
if [ -f "$PID_FILE" ]; then
  PID=$(cat "$PID_FILE" 2>/dev/null)
  if [ -n "$PID" ] && kill -0 "$PID" 2>/dev/null; then
    # 2. Check HTTP health (process might be a zombie or stuck)
    if curl -sf --max-time 10 "http://127.0.0.1:$PORT/health" >/dev/null 2>&1; then
      ALIVE=true
    else
      echo "$(ts) supervisor PID $PID alive but /health unreachable — restarting" >> "$LOG"
      kill "$PID" 2>/dev/null
      sleep 2
      kill -9 "$PID" 2>/dev/null
    fi
  else
    echo "$(ts) supervisor PID file stale (PID $PID not alive)" >> "$LOG"
  fi
fi

if [ "$ALIVE" = true ]; then
  # Quiet when healthy — cron stays silent
  exit 0
fi

# 3. Restart the supervisor
echo "$(ts) starting M. Engine supervisor on port $PORT" >> "$LOG"
cd "$SUP_DIR" || exit 1
# Detached, survives the cron run
nohup node server.js --port "$PORT" >> "$LOG" 2>&1 &
disown 2>/dev/null

sleep 3
if curl -sf --max-time 10 "http://127.0.0.1:$PORT/health" >/dev/null 2>&1; then
  echo "$(ts) supervisor restarted successfully" >> "$LOG"
  exit 0
else
  echo "$(ts) ERROR: supervisor failed to start" >> "$LOG"
  exit 1
fi
