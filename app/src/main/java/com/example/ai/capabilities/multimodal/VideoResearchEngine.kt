package com.example.ai.capabilities.multimodal

import com.example.ai.capabilities.boundary.*

interface VideoResearchEngine {
    fun inspectVideo(uri: String, objectiveId: String, explicitOwnerAssertion: Boolean = false): ResearchArtifact
}

class VideoResearchEngineImpl(
    private val boundaryStateMachine: AgencyBoundaryStateMachine,
    private val authorizationEngine: GraduatedAuthorizationEngine = GraduatedAuthorizationEngineImpl()
) : VideoResearchEngine {
    
    override fun inspectVideo(uri: String, objectiveId: String, explicitOwnerAssertion: Boolean): ResearchArtifact {
        val evidence = mutableListOf<AuthorizationEvidence>()
        if (uri.contains("authorized-api") || uri.contains("youtube.com")) {
            evidence.add(AuthorizationEvidence("PUBLIC_ENDPOINT", "Normal public access method"))
        }
        if (explicitOwnerAssertion) {
            evidence.add(AuthorizationEvidence("OWNER_ASSERTION", "Owner asserted permission"))
        }
        
        val assessment = authorizationEngine.assess(uri, evidence, "Extract video knowledge")
        
        // Least-Restrictive Agency: Downgrade gracefully instead of immediate halt
        val compliance = when (assessment.decision) {
            AutonomyLevel.FULL_AUTONOMY, AutonomyLevel.BOUNDED_AUTOMATION -> PolicyCompliance.AUTHORIZED
            AutonomyLevel.SANDBOXED_EXPERIMENT, AutonomyLevel.OWNER_CONFIRMATION -> PolicyCompliance.RESTRICTED_METADATA_ONLY
            else -> PolicyCompliance.UNAUTHORIZED_WAITING_FOR_CAPABILITY
        }
        
        if (assessment.decision == AutonomyLevel.HALT || assessment.decision == AutonomyLevel.WAITING_FOR_CAPABILITY_OR_AUTHORIZATION) {
            boundaryStateMachine.transition(
                AgencyBoundaryEvent(
                    state = AgencyBoundaryState.WAITING_FOR_EXTERNAL_CAPABILITY,
                    description = assessment.reasoningSummary,
                    capabilityNeeded = "AUTHORIZED_API_ACCESS_OR_CONFIRMATION"
                )
            )
        } else {
            // Even if bounded or restricted, we log the boundary state we are operating under
            boundaryStateMachine.transition(
                AgencyBoundaryEvent(
                    state = AgencyBoundaryState.ACTING,
                    description = "Operating under ${assessment.decision}: ${assessment.reasoningSummary}"
                )
            )
        }

        // Simulate extraction, degraded based on authorization decision
        val mechanics = if (compliance == PolicyCompliance.AUTHORIZED) {
            listOf("Buffered animation transitions", "Root-motion driven grappling")
        } else if (compliance == PolicyCompliance.RESTRICTED_METADATA_ONLY) {
            listOf("Metadata: Wrestling game video")
        } else {
            emptyList()
        }
        
        val obsVsInf = if (compliance == PolicyCompliance.AUTHORIZED) {
            mapOf(
                "Buffered animation transitions" to "OBSERVED (Timestamp: 12:44)",
                "Root-motion driven grappling" to "INFERRED (Based on pivot foot tracking)"
            )
        } else if (compliance == PolicyCompliance.RESTRICTED_METADATA_ONLY) {
            mapOf("Metadata: Wrestling game video" to "OBSERVED")
        } else {
            emptyMap()
        }

        return ResearchArtifact(
            id = "artifact-${System.currentTimeMillis()}",
            sourceUri = uri,
            modality = SourceModality.VIDEO,
            objectiveId = objectiveId,
            extractedMechanics = mechanics,
            observationVsInference = obsVsInf,
            complianceStatus = compliance,
            acquisitionMethod = assessment.decision.name,
            authorizationStatusAtAcquisition = assessment.status.name
        )
    }
}
