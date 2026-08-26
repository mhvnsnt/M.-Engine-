#!/bin/bash
set -e
git add app/src/main/assets/all-MiniLM-L6-v2.onnx
git commit -m "Fix truncated ONNX model file"
gradle :app:assembleRelease
APK="app/build/outputs/apk/release/app-release.apk"
if [ -f "$APK" ]; then
  URL=$(curl -s -F "file=@$APK" https://tmpfiles.org/api/v1/upload | grep -o '"url":"[^"]*"' | cut -d'"' -f4 | sed 's/tmpfiles.org\//tmpfiles.org\/dl\//')
  echo "UPLOADED_URL: $URL"
else
  echo "APK not found!"
  exit 1
fi
