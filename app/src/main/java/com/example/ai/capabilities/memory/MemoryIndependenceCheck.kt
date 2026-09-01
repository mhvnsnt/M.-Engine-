package com.example.ai.capabilities.memory

interface MemoryIndependenceCheck {
    fun verifyIndependence(newArtifact: PersistentResearchArtifact, existingArtifacts: List<PersistentResearchArtifact>): IndependenceResult
}

data class IndependenceResult(
    val isIndependent: Boolean,
    val dependentOnInternalInference: Boolean,
    val uniqueOriginsCount: Int,
    val explanation: String
)

class MemoryIndependenceCheckImpl : MemoryIndependenceCheck {
    override fun verifyIndependence(newArtifact: PersistentResearchArtifact, existingArtifacts: List<PersistentResearchArtifact>): IndependenceResult {
        // Prevent artificial confidence inflation by checking if the "new" evidence 
        // is actually just citing M. Engine's own past inference or an identical source.
        
        val uniqueSourceUris = existingArtifacts.map { it.sourceUri }.toSet()
        val isDuplicateSource = uniqueSourceUris.contains(newArtifact.sourceUri)
        
        // Check if the new artifact relies on previous assumptions/inferences rather than primary external data
        val citesInternalInference = newArtifact.lineage.parentIds.any { parentId ->
            val parent = existingArtifacts.find { it.artifactId == parentId }
            parent != null && parent.inferences.isNotEmpty()
        }

        if (isDuplicateSource || citesInternalInference) {
            return IndependenceResult(
                isIndependent = false,
                dependentOnInternalInference = citesInternalInference,
                uniqueOriginsCount = uniqueSourceUris.size,
                explanation = "ECHO_CHAMBER_WARNING: This artifact cites an existing source or relies on internal inference. It does not constitute independent evidence."
            )
        }

        return IndependenceResult(
            isIndependent = true,
            dependentOnInternalInference = false,
            uniqueOriginsCount = uniqueSourceUris.size + 1,
            explanation = "Independent external evidence verified."
        )
    }
}
