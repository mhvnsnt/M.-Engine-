#!/bin/bash
cat app/src/main/java/com/example/ai/CodingTools.kt | grep -n "suspend fun commitAndPush" | cut -d: -f1
