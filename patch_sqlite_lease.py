import re

file_path = '/app/applet/cloud_control_plane/src/main/kotlin/com/example/ai/cloud/SQLiteLedgerRepository.kt'
with open(file_path, 'r') as f:
    content = f.read()

new_lease = """
    private val pendingJobs = mutableListOf<Map<String, Any>>()

    override fun leaseJob(workerId: String): Map<String, Any>? {
        if (pendingJobs.isNotEmpty()) {
            return pendingJobs.removeAt(0)
        }
        return null
    }

    override fun createJob(operation: String, params: Map<String, Any>): Map<String, Any> {
        val jobId = "job-${System.currentTimeMillis()}"
        val job = mapOf("jobId" to jobId, "operation" to operation, "params" to params, "status" to "CREATED")
        pendingJobs.add(job)
        return job
    }
"""

content = re.sub(r'override fun leaseJob.*?\n\s*}\n\n\s*override fun createJob.*?\n\s*}\n', new_lease, content, flags=re.DOTALL)

with open(file_path, 'w') as f:
    f.write(content)
