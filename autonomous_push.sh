#!/bin/bash
# Autonomous script to add, commit, and push all changes.
git add .
git commit -m "Auto-commit: $(date)"
git push origin main || git push origin master
