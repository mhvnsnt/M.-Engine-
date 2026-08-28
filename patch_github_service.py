with open("app/src/main/java/com/example/ai/capabilities/GitHubService.kt", "r") as f:
    code = f.read()

import re
code = re.sub(r'    suspend fun inspectRepository\(repo: RepositoryRef\): RepoMetadata', r'''    suspend fun inspectRepository(repo: RepositoryRef): RepoMetadata
    suspend fun getReadme(repo: RepositoryRef): String''', code)

with open("app/src/main/java/com/example/ai/capabilities/GitHubService.kt", "w") as f:
    f.write(code)
