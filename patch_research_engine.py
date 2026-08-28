import re

with open("app/src/main/java/com/example/ai/capabilities/ResearchEngine.kt", "r") as f:
    code = f.read()

# Update ResearchCandidate
new_candidate = """data class ResearchCandidate(
    val id: String,
    val name: String,
    val sourceType: String, // GITHUB, PAPER, HUGGINGFACE, DOCS
    val url: String,
    val description: String,
    val versionOrCommit: String,
    val createdAtYear: Int = 2026,
    val lastUpdatedYear: Int = 2026,
    val stars: Int = 0,
    val forkCount: Int = 0,
    val issuesResolved: Int = 0,
    val isNativeMengine: Boolean = false
)"""
code = re.sub(r'data class ResearchCandidate\([\s\S]*?\)', new_candidate, code, count=1)

# Update CandidateEvaluation
new_eval = """data class CandidateEvaluation(
    val effectivenessScore: Int,
    val efficiencyScore: Int,
    val maturityScore: Int,
    val recencyScore: Int,
    val adoptionScore: Int,
    val maintenanceScore: Int,
    val integrationComplexity: Int,
    val evidenceConfidence: String,
    val licenseCompatibility: Boolean,
    val androidCompatible: Boolean,
    val securityRisks: List<String>,
    val dependencyHealth: String,
    val recommendedIntegrationMode: IntegrationMode,
    val provenance: ProvenanceRecord? = null
)"""
code = re.sub(r'data class CandidateEvaluation\([\s\S]*?\)', new_eval, code, count=1)

with open("app/src/main/java/com/example/ai/capabilities/ResearchEngine.kt", "w") as f:
    f.write(code)
