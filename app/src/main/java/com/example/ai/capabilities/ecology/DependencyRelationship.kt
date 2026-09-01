package com.example.ai.capabilities.ecology

enum class RelationshipCategory { STRUCTURAL, OPERATIONAL, SEMANTIC, NEGATIVE }

enum class ProjectRelationship(val category: RelationshipCategory) {
    IMPORTS(RelationshipCategory.STRUCTURAL), 
    DEPENDS_ON(RelationshipCategory.STRUCTURAL), 
    USES_PACKAGE(RelationshipCategory.STRUCTURAL), 
    USES_API(RelationshipCategory.STRUCTURAL), 
    REFERENCES_REPOSITORY(RelationshipCategory.STRUCTURAL), 
    SHARES_MODULE(RelationshipCategory.STRUCTURAL), 
    SHARES_WORKSPACE(RelationshipCategory.STRUCTURAL), 
    SUBMODULE_OF(RelationshipCategory.STRUCTURAL),
    
    CALLS_SERVICE(RelationshipCategory.OPERATIONAL), 
    SHARES_AUTHENTICATION(RelationshipCategory.OPERATIONAL), 
    EXCHANGES_DATA(RelationshipCategory.OPERATIONAL), 
    DEPLOYED_WITH(RelationshipCategory.OPERATIONAL), 
    TESTED_WITH(RelationshipCategory.OPERATIONAL), 
    BUILT_BY(RelationshipCategory.OPERATIONAL),
    
    SUPPORTS_GOAL(RelationshipCategory.SEMANTIC), 
    POTENTIAL_SYNERGY(RelationshipCategory.SEMANTIC), 
    MAY_BENEFIT_FROM(RelationshipCategory.SEMANTIC), 
    ARCHITECTURALLY_SIMILAR(RelationshipCategory.SEMANTIC),
    
    NO_EVIDENCE_OF_RELATIONSHIP(RelationshipCategory.NEGATIVE)
}

enum class EdgeStatus { DISCOVERED, ACTIVE, REVERIFIED, STALE, CONTESTED, SUPERSEDED, CURRENTLY_UNRELATED }
enum class EpistemicClassification { OBSERVATION, INFERENCE, HYPOTHESIS }
enum class ProbeType { MANIFEST_SEARCH, API_CHECK, SOURCE_GREP, DOCUMENTATION_REFERENCE }

data class FalsificationProbe(
    val type: ProbeType,
    val target: String,
    val expectedReference: String
)

data class DependencyRelationship(
    val sourceId: String,
    val targetId: String,
    val relationshipType: ProjectRelationship,
    val epistemicClassification: EpistemicClassification,
    val confidence: Double,
    val evidence: List<String>,
    val discoveredAt: Long = System.currentTimeMillis(),
    val lastVerifiedAt: Long = System.currentTimeMillis(),
    val verificationMethod: String,
    val falsificationCondition: FalsificationProbe?,
    var status: EdgeStatus,
    val downstreamImplications: List<String> = emptyList()
)
