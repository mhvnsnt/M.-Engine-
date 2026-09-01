package com.example.ai.capabilities.federated

enum class CapabilityScope {
    DISCOVER_REPOSITORIES,
    READ_METADATA,
    READ_SOURCE,
    CREATE_SANDBOX_BRANCH,
    WRITE_BRANCH,
    CREATE_PULL_REQUEST,
    MERGE
}

data class CapabilityRequest(val scope: CapabilityScope, val parameters: Map<String, String>)
data class CapabilityAssessment(val authorized: Boolean, val risk: Double, val reasoning: String)
data class CapabilityAuthorization(val grantedScopes: List<CapabilityScope>, val maxCost: Double)
data class CapabilityResult(val success: Boolean, val data: Any?, val evidence: String)

interface AgencyCapability {
    val capabilityId: String
    
    suspend fun assess(request: CapabilityRequest): CapabilityAssessment
    suspend fun execute(authorization: CapabilityAuthorization, request: CapabilityRequest): CapabilityResult
}
