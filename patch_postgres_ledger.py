import re

file_path = '/app/applet/cloud_control_plane/src/main/kotlin/com/example/ai/cloud/PostgresLedgerRepository.kt'
with open(file_path, 'r') as f:
    content = f.read()

new_methods = """
    override fun enrollWorker(workerId: String, os: String, unrealVersion: String, repository: String, currentBranch: String, currentCommit: String): Map<String, Any> {
        return mapOf("status" to "ENROLLED", "workerId" to workerId)
    }

    override fun heartbeatWorker(workerId: String, state: String): Map<String, Any> {
        return mapOf("status" to "OK")
    }

    override fun leaseJob(workerId: String): Map<String, Any>? {
        return null
    }

    override fun createJob(operation: String, params: Map<String, Any>): Map<String, Any> {
        val jobId = "job-${System.currentTimeMillis()}"
        return mapOf("jobId" to jobId, "operation" to operation, "status" to "CREATED")
    }

    override fun completeJob(jobId: String, exitStatus: Int, evidenceLevel: String, stdout: String, stderr: String): Boolean {
        return true
    }

    override fun registerArtifact(jobId: String, workerId: String, sha256: String, size: Long, path: String, uri: String): Map<String, Any> {
        return mapOf("artifactId" to "art-${System.currentTimeMillis()}", "uri" to uri)
    }
}
"""

content = re.sub(r'}\s*$', new_methods, content)

with open(file_path, 'w') as f:
    f.write(content)
