package com.example.ai.capabilities

interface RecursiveRepoAuditor {
    suspend fun auditWorkspace(repos: List<RepositoryRef>): List<CapabilityInventoryItem>
    suspend fun diagnoseCapabilityState(repo: RepositoryRef, capabilityName: String): InventoryState
}

class RecursiveRepoAuditorImpl(
    private val githubService: GitHubService
) : RecursiveRepoAuditor {

    override suspend fun auditWorkspace(repos: List<RepositoryRef>): List<CapabilityInventoryItem> {
        val inventory = mutableListOf<CapabilityInventoryItem>()
        for (repo in repos) {
            val metadata = githubService.inspectRepository(repo)
            
            // Heuristic detection based on languages and description
            val hasPersistence = metadata.description.contains("database") || metadata.description.contains("persistence")
            val hasNetworking = metadata.description.contains("api") || metadata.description.contains("network")
            
            if (hasPersistence) {
                inventory.add(CapabilityInventoryItem(
                    id = "${repo.name}-persistence",
                    name = "Local Data Persistence",
                    description = "Stores data locally",
                    state = InventoryState.ALREADY_EXISTS,
                    implementationRef = "github.com/${repo.owner}/${repo.name}"
                ))
            }
            
            if (hasNetworking) {
                 inventory.add(CapabilityInventoryItem(
                    id = "${repo.name}-networking",
                    name = "REST Networking",
                    description = "Makes remote HTTP calls",
                    state = InventoryState.ALREADY_EXISTS,
                    implementationRef = "github.com/${repo.owner}/${repo.name}"
                ))
            }

            // A mocked capability that doesn't exist
            inventory.add(CapabilityInventoryItem(
                id = "${repo.name}-vision",
                name = "Computer Vision",
                description = "Processes images or streams for object detection",
                state = InventoryState.MISSING,
                implementationRef = null
            ))
        }
        return inventory
    }

    override suspend fun diagnoseCapabilityState(repo: RepositoryRef, capabilityName: String): InventoryState {
        return when (capabilityName) {
            "Agent Memory" -> InventoryState.PARTIALLY_EXISTS
            "Computer Vision" -> InventoryState.MISSING
            "Sandbox Execution" -> InventoryState.ALREADY_EXISTS
            "Video Stream Actuation" -> InventoryState.EXPERIMENTAL
            else -> InventoryState.MISSING
        }
    }
}
