#!/bin/bash
gradle :app:assembleRelease
git add .
git commit -m "Fix KSP source sets not being compiled, resolving Room and Moshi generated class not found errors"
