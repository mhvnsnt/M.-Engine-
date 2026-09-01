package com.example.ai.capabilities.ecology

enum class ClaimType {
    OBSERVATION,
    INFERENCE,
    HYPOTHESIS
}

data class KnowledgeClaim(
    val type: ClaimType,
    val statement: String,
    val confidence: Double,
    val evidence: String,
    val requiresAction: String? = null
)
