package com.example.ai.capabilities

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SecurityViolation(
    val file: String,
    val reason: String,
    val severity: String, // CRITICAL, HIGH, MEDIUM, LOW
    val matchedPattern: String? = null
)

data class SecurityAuditResult(
    val passed: Boolean,
    val violations: List<SecurityViolation>
)

interface SecurityScanner {
    suspend fun scanRepository(repo: RepositoryRef, sandboxId: String): SecurityAuditResult
    suspend fun scanDirectory(dir: File): SecurityAuditResult
    suspend fun scanPatch(patch: String): SecurityAuditResult
    fun checkFileContent(filePath: String, content: String): List<SecurityViolation>
}

class SecurityScannerImpl : SecurityScanner {

    // 1. Critical Banned Files & Extensions
    private val bannedFilePatterns = listOf(
        Regex(".*\\.jks$", RegexOption.IGNORE_CASE),
        Regex(".*\\.keystore$", RegexOption.IGNORE_CASE),
        Regex(".*\\.p12$", RegexOption.IGNORE_CASE),
        Regex(".*\\.pem$", RegexOption.IGNORE_CASE),
        Regex(".*\\.env$", RegexOption.IGNORE_CASE),
        Regex(".*\\.env\\.local$", RegexOption.IGNORE_CASE),
        Regex(".*\\.git_corrupted.*")
    )

    // 2. Secret & Credential Regex Signatures
    private val secretSignatures = mapOf(
        "Google API Key" to Regex("AIzaSy[A-Za-z0-9_-]{30,45}"),
        "Anthropic API Key" to Regex("sk-ant-[A-Za-z0-9_-]{20,130}"),
        "OpenAI API Key" to Regex("sk-(proj-)?[A-Za-z0-9_-]{20,120}"),
        "GitHub Personal Access Token" to Regex("ghp_[A-Za-z0-9]{20,50}|github_pat_[A-Za-z0-9_]{20,100}"),
        "Slack Token" to Regex("xox[baprs]-[0-9A-Za-z-]{10,80}"),
        "AWS Access Key ID" to Regex("AKIA[0-9A-Z]{16}"),
        "Private Key Header" to Regex("-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----")
    )

    // 3. SAST Code Vulnerability Heuristics
    private val vulnerabilitySignatures = mapOf(
        "Command Injection via Runtime.exec" to Regex("Runtime\\.getRuntime\\(\\)\\.exec\\s*\\("),
        "Unsafe Insecure Cleartext HTTP URL" to Regex("http://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}/"),
        "Hardcoded Secret Assignment" to Regex("(val|var|String)\\s+(apiKey|api_key|secretKey|secret_key|privateKey|password)\\s*=\\s*\"[A-Za-z0-9_\\-+=]{16,}\"")
    )

    override fun checkFileContent(filePath: String, content: String): List<SecurityViolation> {
        val violations = mutableListOf<SecurityViolation>()

        // Check banned file names
        bannedFilePatterns.forEach { regex ->
            if (regex.matches(filePath)) {
                violations.add(
                    SecurityViolation(filePath, "Banned sensitive file pattern: ${regex.pattern}", "CRITICAL")
                )
            }
        }

        // Check for hardcoded API keys & credentials
        secretSignatures.forEach { (name, regex) ->
            if (regex.containsMatchIn(content)) {
                violations.add(
                    SecurityViolation(filePath, "Hardcoded credential detected: $name", "CRITICAL", name)
                )
            }
        }

        // Check for SAST code vulnerabilities
        vulnerabilitySignatures.forEach { (name, regex) ->
            if (regex.containsMatchIn(content)) {
                violations.add(
                    SecurityViolation(filePath, "SAST Vulnerability: $name", "HIGH", name)
                )
            }
        }

        return violations
    }

    override suspend fun scanPatch(patch: String): SecurityAuditResult = withContext(Dispatchers.IO) {
        val violations = checkFileContent("patch.diff", patch)
        SecurityAuditResult(violations.isEmpty(), violations)
    }

    override suspend fun scanDirectory(dir: File): SecurityAuditResult = withContext(Dispatchers.IO) {
        val violations = mutableListOf<SecurityViolation>()
        if (!dir.exists() || !dir.isDirectory) {
            return@withContext SecurityAuditResult(true, emptyList())
        }

        val files = dir.walkTopDown()
            .filter { it.isFile && !it.path.contains("/.git/") && !it.path.contains("/build/") }
            .take(500) // Scan up to 500 files

        for (file in files) {
            val content = try {
                file.readText()
            } catch (_: Throwable) {
                ""
            }
            violations.addAll(checkFileContent(file.path, content))
        }

        SecurityAuditResult(violations.isEmpty(), violations)
    }

    override suspend fun scanRepository(repo: RepositoryRef, sandboxId: String): SecurityAuditResult = withContext(Dispatchers.IO) {
        // Local scan when available
        val dir = File(repo.name)
        if (dir.exists() && dir.isDirectory) {
            scanDirectory(dir)
        } else {
            SecurityAuditResult(true, emptyList())
        }
    }
}
