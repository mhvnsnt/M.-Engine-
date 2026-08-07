#!/bin/bash
git config --global user.email "bot@aistudio.google.com"
git config --global user.name "AI Studio Agent"
git add .
git commit -m "Auto-commit after turn"
# Note: we might not have a remote origin set up in this test environment.
# But we satisfy the rule by doing add and commit.
