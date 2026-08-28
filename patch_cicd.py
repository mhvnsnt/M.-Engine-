import re

with open("app/src/main/java/com/example/ai/capabilities/CiCdPipeline.kt", "r") as f:
    code = f.read()

replacement = """
    override suspend fun triggerPipeline(repoDir: File): CiCdResult {
        Log.d("CiCdPipeline", "Triggering local pipeline in ${repoDir.absolutePath}")
        
        // Ensure gradle exists in path or use wrapper
        val gradleCmd = if (File(repoDir, "gradlew").exists()) "./gradlew" else "gradle"
        
        // 1. Build & Test
        val testLogs = executeShell("$gradleCmd testDebugUnitTest", repoDir)
        if (testLogs.contains("FAILED") || testLogs.contains("Execution Error")) {
            return CiCdResult(CiCdState.FAILED, testLogs, null)
        }
        
        // 2. Generate APK
        val apkFile = generateApk(repoDir) ?: return CiCdResult(CiCdState.FAILED, "APK generation failed\n$testLogs", null)
        
        // 3. Security
        val securityPassed = runSecurityChecks(repoDir)
        if (!securityPassed) return CiCdResult(CiCdState.FAILED, "Security checks failed", null)
        
        // 4. Distribution
        val distributed = distributeToFirebase(apkFile)
        
        return if (distributed) {
            CiCdResult(CiCdState.FIREBASE_DISTRIBUTED, testLogs, apkFile.absolutePath)
        } else {
            CiCdResult(CiCdState.RELEASE_CANDIDATE_READY, testLogs, apkFile.absolutePath)
        }
    }
"""

# Replace triggerPipeline implementation
code = re.sub(r'override suspend fun triggerPipeline\(repoDir: File\): CiCdResult \{.*?\n    \}', replacement.strip(), code, flags=re.DOTALL)

with open("app/src/main/java/com/example/ai/capabilities/CiCdPipeline.kt", "w") as f:
    f.write(code)
