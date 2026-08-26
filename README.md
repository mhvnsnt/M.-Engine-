# M. Engine Applet

This repository contains the M. Engine Android Applet. It is configured with autonomous workflows and GitHub Actions to minimize manual intervention.

## Features & Automations
- **GitHub Actions CI/CD:** Automatically builds both `Debug` and `Release` APKs on every push to the `main` branch. APKs are uploaded as artifacts to the workflow run.
- **Dependabot:** Configured to check for Gradle and GitHub Actions updates weekly, keeping dependencies secure and up-to-date automatically.
- **Agent Instructions:** The repository contains an `AGENTS.md` file instructing AI assistants (like Claude/M. Engine) to operate completely autonomously, auto-fix errors, and auto-commit/push changes.

## Downloading APKs
To get the latest compiled APK without building locally:
1. Go to the **Actions** tab in this GitHub repository.
2. Click on the latest workflow run.
3. Scroll down to the **Artifacts** section and download `app-debug` or `app-release`.

## Setup & Development
The project is built with Jetpack Compose and Kotlin.
To build manually:
```bash
./gradlew assembleDebug
```
