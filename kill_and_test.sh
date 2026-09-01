#!/bin/bash
pkill -9 -f "native_worker.py"
pkill -9 -f "python3"
fuser -k 9092/tcp
gradle :app:testDebugUnitTest --tests "*PhysicalFabricWorkerProbeTest*" -i --no-build-cache
