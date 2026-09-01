package com.example.ai.capabilities.boundary

enum class AuthorizationStatus {
    EXPLICITLY_AUTHORIZED,
    PUBLICLY_PERMITTED,
    OWNER_ASSERTED_AUTHORIZATION,
    UNCERTAIN,
    RESTRICTED,
    PROHIBITED
}

enum class AutonomyLevel {
    FULL_AUTONOMY,
    BOUNDED_AUTOMATION,
    SANDBOXED_EXPERIMENT,
    OWNER_CONFIRMATION,
    WAITING_FOR_CAPABILITY_OR_AUTHORIZATION,
    HALT
}

data class AuthorizationEvidence(
    val type: String,
    val description: String
)

data class AuthorizationAssessment(
    val target: String,
    val status: AuthorizationStatus,
    val evidence: List<AuthorizationEvidence>,
    val unknownFactors: List<String>,
    val decision: AutonomyLevel,
    val reasoningSummary: String
)
