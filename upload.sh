#!/bin/bash
APK="app/build/outputs/apk/release/app-release.apk"
if [ -f "$APK" ]; then
  curl -s -F "file=@$APK" https://tmpfiles.org/api/v1/upload | grep -o '"url":"[^"]*"' | cut -d'"' -f4 | sed 's/tmpfiles.org\//tmpfiles.org\/dl\//'
else
  echo "APK not found!"
fi
