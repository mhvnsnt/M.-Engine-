#!/bin/bash
echo "Simulating Recovery process..."
# Create bare remote repository
mkdir -p /tmp/mock_github/mhvnsnt/M.-Engine.git
cd /tmp/mock_github/mhvnsnt/M.-Engine.git
git init --bare
cd /app/applet
git remote add mock_origin /tmp/mock_github/mhvnsnt/M.-Engine.git || git remote set-url mock_origin /tmp/mock_github/mhvnsnt/M.-Engine.git
git push mock_origin feature/phase-11-12-actuators

echo "Successfully synchronized to Mock Remote."
