import re

with open('app/src/main/java/com/example/ai/capabilities/RemoteSandbox.kt', 'r') as f:
    content = f.read()

content = re.sub(r'data class RepositoryRef\([^)]+\)\n', '', content)

with open('app/src/main/java/com/example/ai/capabilities/RemoteSandbox.kt', 'w') as f:
    f.write(content)


with open('app/src/main/java/com/example/ai/capabilities/WorkerPool.kt', 'r') as f:
    content = f.read()

content = re.sub(r'data class CostProfile\(val level: String\)\n', '', content)
content = re.sub(r'data class CandidateEvaluation\([^)]+\)\n', '', content)

def implement_abstracts(match):
    name = match.group(1)
    return f'''class {name}Runtime(sandboxManager: RemoteSandboxManager, sandboxId: String) : CodingWorkerRuntime(sandboxManager, sandboxId) {{
    override val name = "{name}"
    override val type = CapabilityType.SYSTEM_NATIVE
    override val isLocal = false
    override val status = CapabilityStatus.ACTIVE
    override val permissionLevel = PermissionLevel.RESTRICTED
    override val supportedOperations = listOf("inspect", "plan", "modify", "build", "test", "review")
    override val networkRequired = true
    override val costProfile = CostProfile("MEDIUM")'''

content = re.sub(r'class ([A-Za-z0-9_]+)Runtime\(sandboxManager: RemoteSandboxManager, sandboxId: String\) : CodingWorkerRuntime\(sandboxManager, sandboxId\) \{', implement_abstracts, content)

with open('app/src/main/java/com/example/ai/capabilities/WorkerPool.kt', 'w') as f:
    f.write(content)

