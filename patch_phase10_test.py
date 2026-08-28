import re

with open("app/src/test/java/com/example/ai/capabilities/Phase10IntegrationTest.kt", "r") as f:
    code = f.read()

# Replace MockCiCd methods to match CiCdPipeline interface
code = code.replace(
    "override suspend fun triggerPipeline(repo: RepositoryRef, commitSha: String) = CiCdResult(CiCdState.BUILD_PASSED, \"\", \"\")",
    "override suspend fun triggerPipeline(repoDir: java.io.File): CiCdResult = CiCdResult(CiCdState.BUILD_PASSED, \"\", \"\")"
)
code = code.replace(
    "override suspend fun runSecurityChecks(repo: RepositoryRef, commitSha: String) = true",
    "override suspend fun runSecurityChecks(repoDir: java.io.File): Boolean = true"
)
code = code.replace(
    "override suspend fun distributeToFirebase(artifactUrl: String) = true",
    "override suspend fun distributeToFirebase(apkFile: java.io.File): Boolean = true"
)
code = code.replace(
    "override suspend fun generateApk(repo: RepositoryRef, commitSha: String) = \"apk\"",
    "override suspend fun generateApk(repoDir: java.io.File): java.io.File? = java.io.File(\"apk\")"
)

with open("app/src/test/java/com/example/ai/capabilities/Phase10IntegrationTest.kt", "w") as f:
    f.write(code)

