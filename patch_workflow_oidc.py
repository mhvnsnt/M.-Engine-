with open(".github/workflows/alpha-release.yml", "r") as f:
    code = f.read()

new_steps = """
      - name: Authenticate to Google Cloud
        uses: google-github-actions/auth@v2
        with:
          workload_identity_provider: ${{ vars.WORKLOAD_IDENTITY_PROVIDER }}
          service_account: ${{ vars.SERVICE_ACCOUNT_EMAIL }}

      - name: Decode google-services.json
        env:
          GOOGLE_SERVICES_JSON: ${{ secrets.GOOGLE_SERVICES_JSON }}
        run: |
          if [ -n "$GOOGLE_SERVICES_JSON" ]; then
            echo "$GOOGLE_SERVICES_JSON" | base64 --decode > app/google-services.json
          else
            echo "GOOGLE_SERVICES_JSON secret not found. Build will fail."
            exit 1
          fi
          
      - name: Run Unit Tests (Evidence Gate)
        run: ./gradlew testDebugUnitTest --no-daemon

      - name: Run Linter & Static Analysis (Security Gate)
        run: ./gradlew lintDebug --no-daemon

      - name: Build Debug APK
        run: ./gradlew assembleDebug --no-daemon
        
      - name: Generate Evidence Release Notes
        run: |
          echo "Commit: ${{ github.sha }}" > release-notes.txt
          echo "Run ID: ${{ github.run_id }}" >> release-notes.txt
          echo "Evidence Gates: PASSED" >> release-notes.txt
          echo "Security Gates: PASSED" >> release-notes.txt

      - name: Distribute to Firebase App Distribution
        uses: wzieba/Firebase-Distribution-Github-Action@v1
        with:
          appId: ${{ vars.FIREBASE_APP_ID }}
          groups: testers
          releaseNotesFile: release-notes.txt
          file: app/build/outputs/apk/debug/app-debug.apk
"""

import re
code = re.sub(r'      - name: Decode google-services\.json.*?file: app/build/outputs/apk/debug/app-debug\.apk', new_steps.strip(), code, flags=re.DOTALL)

with open(".github/workflows/alpha-release.yml", "w") as f:
    f.write(code)
