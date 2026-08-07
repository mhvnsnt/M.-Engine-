#!/bin/bash
while true; do
  STATUS=$(gradle :app:assembleDebug --dry-run 2>&1 || true)
  if echo "$STATUS" | grep -q "BUILD SUCCESSFUL"; then
    echo "Build succeeded."
    break
  elif echo "$STATUS" | grep -q "FAILED"; then
    echo "Build failed."
    break
  fi
  sleep 2
done
