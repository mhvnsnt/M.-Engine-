package com.example.ai.capabilities.ecology

enum class ProjectRelationship {
    DEPENDS_ON,
    SHARES_CODE_WITH,
    SUPPORTS_GOAL,
    EXPERIMENTAL_PREDECESSOR_OF,
    REPLACEMENT_CANDIDATE_FOR,
    COMPLEMENTS,
    DUPLICATES,
    CONTAINS_REUSABLE_COMPONENT,
    BLOCKS_PROGRESS_OF,
    HIGH_SYNERGY_WITH
}

data class EcologyRelationship(
    val targetProjectId: String,
    val relationshipType: ProjectRelationship,
    val reasoning: String
)

data class ProjectNode(
    val id: String,
    val name: String,
    val surfaces: MutableList<RealitySurface> = mutableListOf(),
    val relationships: MutableList<EcologyRelationship> = mutableListOf(),
    var profile: ProjectEcologyProfile? = null
)

interface ProjectEcologyEngine {
    fun registerProject(project: ProjectNode)
    fun addSurface(projectId: String, surface: RealitySurface)
    fun linkProjects(sourceId: String, targetId: String, relationship: ProjectRelationship, reasoning: String)
    fun updateProfile(projectId: String, profile: ProjectEcologyProfile)
    fun getEcologyConfidence(): Double
    fun getUnmappedSurfaces(): List<RealitySurface>
    fun getProject(projectId: String): ProjectNode?
    fun getAllProjects(): List<ProjectNode>
}

class ProjectEcologyEngineImpl : ProjectEcologyEngine {
    private val projects = mutableMapOf<String, ProjectNode>()

    override fun registerProject(project: ProjectNode) {
        projects[project.id] = project
    }

    override fun addSurface(projectId: String, surface: RealitySurface) {
        projects[projectId]?.surfaces?.add(surface)
    }

    override fun linkProjects(sourceId: String, targetId: String, relationship: ProjectRelationship, reasoning: String) {
        projects[sourceId]?.relationships?.add(EcologyRelationship(targetId, relationship, reasoning))
    }

    override fun updateProfile(projectId: String, profile: ProjectEcologyProfile) {
        projects[projectId]?.profile = profile
    }

    override fun getEcologyConfidence(): Double {
        if (projects.isEmpty()) return 0.0
        val allSurfaces = projects.values.flatMap { it.surfaces }
        if (allSurfaces.isEmpty()) return 0.0
        
        val mappedCount = allSurfaces.count { it.status == InspectionStatus.MAPPED }
        return mappedCount.toDouble() / allSurfaces.size.toDouble()
    }

    override fun getUnmappedSurfaces(): List<RealitySurface> {
        return projects.values.flatMap { it.surfaces }.filter { it.status != InspectionStatus.MAPPED }
    }
    
    override fun getProject(projectId: String): ProjectNode? = projects[projectId]
    
    override fun getAllProjects(): List<ProjectNode> = projects.values.toList()
}
