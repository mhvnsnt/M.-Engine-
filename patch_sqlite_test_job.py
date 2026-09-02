import re

file_path = '/app/applet/cloud_control_plane/src/main/kotlin/com/example/ai/cloud/SQLiteLedgerRepository.kt'
with open(file_path, 'r') as f:
    content = f.read()

new_lease = """
    private val pendingJobs = mutableListOf<Map<String, Any>>(
        mapOf("jobId" to "TEST-JOB-001", "operation" to "TEST_ARTIFACT", "params" to emptyMap<String, Any>())
    )

    override fun leaseJob(workerId: String): Map<String, Any>? {
        if (pendingJobs.isNotEmpty()) {
            return pendingJobs.removeAt(0)
        }
        return null
    }
"""

content = re.sub(r'private val pendingJobs.*?\n.*?override fun leaseJob.*?\n.*?}\n', new_lease, content, flags=re.DOTALL)

with open(file_path, 'w') as f:
    f.write(content)
