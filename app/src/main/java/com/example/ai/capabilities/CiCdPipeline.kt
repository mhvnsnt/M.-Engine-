package com.example.ai.capabilities

import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

enum class CiCdState {
    BRANCH_CREATED,
    BUILD_PASSED,
    TESTS_PASSED,
    SECURITY_CHECK_PASSED,
    BEHAVIORAL_VERIFICATION_PASSED,
    RELEASE_CANDIDATE_READY,
    FIREBASE_DISTRIBUTED,
    FAILED
}

data class CiCdResult(val state: CiCdState, val logsUrl: String, val artifactUrl: String?)

interface CiCdPipeline {
    suspend fun triggerPipeline(repoDir: File): CiCdResult
    suspend fun runSecurityChecks(repoDir: File): Boolean
    suspend fun distributeToFirebase(apkFile: File): Boolean
    suspend fun generateApk(repoDir: File): File?
}

class LocalDeviceCiCdPipeline : CiCdPipeline {

    private suspend fun executeShell(command: String, cwd: File): String = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder("sh", "-c", command)
                .directory(cwd)
                .start()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            val out = StringBuilder()
            val err = StringBuilder()
            
            var line: String?
            while (reader.readLine().also { line = it } != null) { out.append(line).append("\n") }
            while (errorReader.readLine().also { line = it } != null) { err.append(line).append("\n") }
            
            process.waitFor()
            "STDOUT:\n$out\nSTDERR:\n$err"
        } catch (e: Exception) {
            "Execution Error: ${e.message}"
        }
    }

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
        val apkFile = generateApk(repoDir) ?: return CiCdResult(CiCdState.FAILED, "APK generation failed", null)
        
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

    override suspend fun runSecurityChecks(repoDir: File): Boolean {
        // Minimal static analysis via grep
        val grepLogs = executeShell("grep -r \"super-secret\" .", repoDir)
        if (grepLogs.contains("super-secret") && !grepLogs.contains("Execution Error")) {
            Log.w("CiCdPipeline", "Security violation found: Hardcoded secrets")
            return false // In a real scenario we might fail here, but let's just log it
        }
        return true
    }
    
    override suspend fun generateApk(repoDir: File): File? = withContext(Dispatchers.IO) {
        val buildLogs = executeShell("gradle :app:assembleDebug", repoDir)
        val apkFile = File(repoDir, "app/build/outputs/apk/debug/app-debug.apk")
        if (apkFile.exists()) apkFile else null
    }

    override suspend fun distributeToFirebase(apkFile: File): Boolean = withContext(Dispatchers.IO) {
        // Connects to Firebase App Distribution via CLI (assuming firebase CLI is installed in environment)
        val distLogs = executeShell("firebase appdistribution:distribute ${apkFile.absolutePath} --app 1:255819041649:android:a1b2c3d4e5f6 --groups testers", apkFile.parentFile ?: apkFile)
        if (distLogs.contains("Error") || distLogs.contains("command not found")) {
            Log.e("CiCdPipeline", "Firebase distribution failed: $distLogs")
            return@withContext false
        }
        true
    }
}
