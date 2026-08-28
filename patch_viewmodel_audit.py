with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    code = f.read()

import re

audit_func = """
    suspend fun runRecursiveAudit(repoNames: List<String>): List<com.example.ai.capabilities.CapabilityInventoryItem> {
        val githubService = com.example.ai.capabilities.GitHubServiceImpl(com.example.network.RetrofitInstance.gitHubApi, githubPat.value)
        val auditor = com.example.ai.capabilities.RecursiveRepoAuditorImpl(githubService)
        val refs = repoNames.map { com.example.ai.capabilities.RepositoryRef("mhvnsnt", it) }
        return auditor.auditWorkspace(refs)
    }
"""

if "runRecursiveAudit" not in code:
    code = code.replace("    fun cancelGithubDeviceFlow() {", audit_func + "\n    fun cancelGithubDeviceFlow() {")

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(code)
