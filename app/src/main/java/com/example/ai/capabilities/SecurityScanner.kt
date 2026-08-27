package com.example.ai.capabilities

data class SecurityViolation(val file: String, val reason: String, val severity: String)

data class SecurityAuditResult(
    val passed: Boolean,
    val violations: List<SecurityViolation>
)

interface SecurityScanner {
    suspend fun scanRepository(repo: RepositoryRef, sandboxId: String): SecurityAuditResult
    suspend fun scanPatch(patch: String): SecurityAuditResult
}

class SecurityScannerImpl : SecurityScanner {
    private val bannedSignatures = listOf(
        Regex(".*\\.jks$"),
        Regex(".*\\.keystore$"),
        Regex(".*\\.env$"),
        Regex(".*\\.git_corrupted.*")
    )

    override suspend fun scanRepository(repo: RepositoryRef, sandboxId: String): SecurityAuditResult {
        // In reality, this would execute `find` or `grep` in the sandbox.
        // We mock it for the architectural implementation.
        return SecurityAuditResult(true, emptyList())
    }

    override suspend fun scanPatch(patch: String): SecurityAuditResult {
        val violations = mutableListOf<SecurityViolation>()
        
        bannedSignatures.forEach { regex ->
            if (regex.containsMatchIn(patch)) {
                violations.add(SecurityViolation("patch", "Contains banned signature: ${regex.pattern}", "CRITICAL"))
            }
        }
        
        return SecurityAuditResult(violations.isEmpty(), violations)
    }
}
