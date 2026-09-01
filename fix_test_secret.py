with open('app/src/test/java/com/example/ai/capabilities/ecology/PhysicalFabricWorkerProbeTest.kt', 'r') as f:
    content = f.read()

import re
old_process = 'val process = ProcessBuilder("python3", "src/main/python/native_worker.py").start()'
new_process = 'val process = ProcessBuilder("python3", "src/main/python/native_worker.py", "--secret", "test_secret").start()'

content = content.replace(old_process, new_process)
content = content.replace('RemoteFabricWorkerEnvironment("http://localhost:9092")', 'RemoteFabricWorkerEnvironment("http://localhost:9092", "test_secret")')

# Disable GovernorRegistryServer since we are outbound-only now
old_gov = '''        // Start Governor Registry Server
        val server = GovernorRegistryServer(9090)
        val serverJob = launch {
            server.startListening()
        }'''
content = content.replace(old_gov, "")

old_gov2 = '''        server.stopListening()
        serverJob.cancel()'''
content = content.replace(old_gov2, "")

old_wait = '''        // Give the Python daemon time to heartbeat and register
        var workerFound = false
        var workerId = ""
        for (i in 1..20) {
            val workers = GlobalWorkerRegistry.instance.getVerifiedWorkers()
            if (workers.isNotEmpty()) {
                workerFound = true
                workerId = workers.first().nodeId
                break
            }
            delay(500)
        }
        
        assertTrue("Python daemon did not register with Governor", workerFound)'''
        
new_wait = '''        // Give the Python daemon time to start
        delay(2000)'''
content = content.replace(old_wait, new_wait)

with open('app/src/test/java/com/example/ai/capabilities/ecology/PhysicalFabricWorkerProbeTest.kt', 'w') as f:
    f.write(content)
