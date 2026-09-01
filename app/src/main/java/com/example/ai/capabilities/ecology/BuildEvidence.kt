package com.example.ai.capabilities.ecology

data class BuildEvidence(
    val repository: String,
    val commitSha: String,
    val command: String,
    val environmentFingerprint: String,
    val exitCode: Int?,
    val stdoutDigest: String?,
    val stderrDigest: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val result: String
)
