with open(".github/workflows/alpha-release.yml", "r") as f:
    content = f.read()

import re

# Replace Firebase action with WIF auth + Firebase action without static creds
new_steps = """    - name: Authenticate to Google Cloud
      id: auth
      uses: google-github-actions/auth@v2
      with:
        workload_identity_provider: ${{ secrets.WIF_PROVIDER }}
        service_account: ${{ secrets.WIF_SERVICE_ACCOUNT }}

    - name: Upload artifact to Firebase App Distribution
      uses: wzieba/Firebase-Distribution-Github-Action@v1
      with:
        appId: ${{ secrets.FIREBASE_APP_ID }}
        groups: testers
        file: app/build/outputs/apk/release/app-release.apk"""

content = re.sub(
    r"    - name: Upload artifact to Firebase App Distribution[\s\S]*file: app/build/outputs/apk/release/app-release\.apk",
    new_steps,
    content
)

# Add permissions required for OIDC
permissions = """
permissions:
  contents: read
  id-token: write

jobs:"""
content = content.replace("jobs:", permissions)

with open(".github/workflows/alpha-release.yml", "w") as f:
    f.write(content)
