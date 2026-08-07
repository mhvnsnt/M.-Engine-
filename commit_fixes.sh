#!/bin/bash
git add app/
git commit -m "Fix Room Database schema migration crash and ONNX fallback"
git push origin master || true
