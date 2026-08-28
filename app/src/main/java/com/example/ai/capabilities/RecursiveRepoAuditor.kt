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
            val readme = githubService.getReadme(repo).lowercase()
            
            // Heuristic detection based on languages and description/readme
            val textToSearch = metadata.description.lowercase() + " " + readme

            if (textToSearch.contains("database") || textToSearch.contains("room") || textToSearch.contains("persistence")) {
                inventory.add(CapabilityInventoryItem(
                    id = "${repo.name}-persistence",
                    name = "Local Data Persistence",
                    description = "Durable on-device storage",
                    state = InventoryState.ALREADY_EXISTS,
                    implementationRef = "github.com/${repo.owner}/${repo.name}"
                ))
            }

            if (textToSearch.contains("agent") || textToSearch.contains("autonomous") || textToSearch.contains("worker")) {
                inventory.add(CapabilityInventoryItem(
                    id = "${repo.name}-agent",
                    name = "Autonomous Worker",
                    description = "Agentic looping and task execution",
                    state = InventoryState.EXPERIMENTAL,
                    implementationRef = "github.com/${repo.owner}/${repo.name}"
                ))
            }
            
            if (textToSearch.contains("sandbox") || textToSearch.contains("docker") || textToSearch.contains("execution")) {
                inventory.add(CapabilityInventoryItem(
                    id = "${repo.name}-sandbox",
                    name = "Sandbox Execution",
                    description = "Isolated environment for running untrusted code",
                    state = InventoryState.ALREADY_EXISTS,
                    implementationRef = "github.com/${repo.owner}/${repo.name}"
                ))
            }
            
            if (textToSearch.contains("benchmark") || textToSearch.contains("eval")) {
                inventory.add(CapabilityInventoryItem(
                    id = "${repo.name}-benchmark",
                    name = "Capability Benchmarking",
                    description = "Quantitative capability evaluation",
                    state = InventoryState.PARTIALLY_EXISTS,
                    implementationRef = "github.com/${repo.owner}/${repo.name}"
                ))
            }
            
            if (textToSearch.contains("mcp") || textToSearch.contains("protocol") || textToSearch.contains("connector")) {
                inventory.add(CapabilityInventoryItem(
                    id = "${repo.name}-mcp",
                    name = "Model Context Protocol",
                    description = "Universal tool capability connector",
                    state = InventoryState.MISSING,
                    implementationRef = "github.com/${repo.owner}/${repo.name}"
                ))
            }

            if (textToSearch.contains("research") || textToSearch.contains("deep-research") || textToSearch.contains("search")) {
                inventory.add(CapabilityInventoryItem(
                    id = "${repo.name}-research",
                    name = "Deep Research Engine",
                    description = "Automated exploration and synthesis",
                    state = InventoryState.EXPERIMENTAL,
                    implementationRef = "github.com/${repo.owner}/${repo.name}"
                ))
            }
            
            if (textToSearch.contains("ui") || textToSearch.contains("compose") || textToSearch.contains("react")) {
                inventory.add(CapabilityInventoryItem(
                    id = "${repo.name}-ui",
                    name = "Reactive UI",
                    description = "Declarative user interface patterns",
                    state = InventoryState.ALREADY_EXISTS,
                    implementationRef = "github.com/${repo.owner}/${repo.name}"
                ))
            }
        }
        
        // Remove duplicates favoring higher maturity states
        val uniqueInventory = inventory.groupBy { it.name }.map { (_, items) ->
            items.minByOrNull { it.state.ordinal } ?: items.first()
        }
        
        return uniqueInventory.sortedBy { it.state.ordinal }
    }

    override suspend fun diagnoseCapabilityState(repo: RepositoryRef, capabilityName: String): InventoryState {
        return InventoryState.MISSING
    }
}
