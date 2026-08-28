import re
import os

def fix_file(filepath):
    if not os.path.exists(filepath): return
    with open(filepath, "r") as f:
        content = f.read()
    
    # Add dummy getReadme mock where needed
    dummy_readme1 = "override suspend fun getReadme(auth: String?, owner: String, repo: String) = com.example.network.GitHubReadmeDto(\"dummy\", \"content\", \"utf-8\", \"html_url\")"
    if "override suspend fun checkRateLimit" in content and "getReadme" not in content:
        content = content.replace("override suspend fun checkRateLimit", dummy_readme1 + "\n    override suspend fun checkRateLimit")
        
    dummy_readme2 = "override suspend fun getReadme(repo: com.example.ai.capabilities.RepositoryRef): String = \"# Dummy\""
    if "override suspend fun searchRepositories" in content and "getReadme" not in content:
        content = content.replace("override suspend fun searchRepositories", dummy_readme2 + "\n    override suspend fun searchRepositories")

    with open(filepath, "w") as f:
        f.write(content)

fix_file("app/src/test/java/com/example/ai/capabilities/Phase8IntegrationTest.kt")
fix_file("app/src/test/java/com/example/ai/capabilities/Phase9IntegrationTest.kt")

