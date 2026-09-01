package com.example.ai.capabilities.ecology

data class ProjectNode(
    val id: String,
    val name: String,
    val surfaces: MutableList<RealitySurface> = mutableListOf(),
    val relationships: MutableList<DependencyRelationship> = mutableListOf(),
    var profile: ProjectEcologyProfile? = null
)

interface ProjectEcologyEngine {
    fun registerProject(project: ProjectNode)
    fun addSurface(projectId: String, surface: RealitySurface)
    fun linkProjects(relationship: DependencyRelationship)
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

    override fun linkProjects(relationship: DependencyRelationship) {
        projects[relationship.sourceId]?.relationships?.add(relationship)
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
