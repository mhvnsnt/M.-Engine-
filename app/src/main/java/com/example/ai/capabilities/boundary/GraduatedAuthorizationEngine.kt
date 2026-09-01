package com.example.ai.capabilities.boundary

interface GraduatedAuthorizationEngine {
    fun assess(target: String, evidence: List<AuthorizationEvidence>, intent: String): AuthorizationAssessment
}

class GraduatedAuthorizationEngineImpl : GraduatedAuthorizationEngine {
    override fun assess(target: String, evidence: List<AuthorizationEvidence>, intent: String): AuthorizationAssessment {
        val hasOwnerAssertion = evidence.any { it.type == "OWNER_ASSERTION" }
        val hasBypass = evidence.any { it.type == "SECURITY_BYPASS_REQUESTED" }
        val hasPublicEndpoint = evidence.any { it.type == "PUBLIC_ENDPOINT" }
        
        var status = AuthorizationStatus.UNCERTAIN
        var decision = AutonomyLevel.OWNER_CONFIRMATION
        val unknowns = mutableListOf<String>()
        var reasoning = "Evaluating authorization for $target."

        if (hasBypass) {
            status = AuthorizationStatus.PROHIBITED
            decision = AutonomyLevel.HALT
            reasoning = "Security bypass requested. Action prohibited."
        } else if (hasOwnerAssertion) {
            status = AuthorizationStatus.OWNER_ASSERTED_AUTHORIZATION
            decision = AutonomyLevel.BOUNDED_AUTOMATION
            unknowns.add("External permission independently unverified")
            reasoning = "Owner asserted permission. Proceeding with bounded automation."
        } else if (hasPublicEndpoint) {
            status = AuthorizationStatus.PUBLICLY_PERMITTED
            decision = AutonomyLevel.BOUNDED_AUTOMATION
            reasoning = "Public endpoint detected. Proceeding with bounded automation."
        } else {
            unknowns.add("Authorization status unknown")
            reasoning = "Uncertain authorization. Downgrading to sandboxed experiment or wait."
            decision = AutonomyLevel.SANDBOXED_EXPERIMENT
        }

        return AuthorizationAssessment(
            target = target,
            status = status,
            evidence = evidence,
            unknownFactors = unknowns,
            decision = decision,
            reasoningSummary = reasoning
        )
    }
}
