with open('app/src/test/java/com/example/ai/capabilities/ecology/PhysicalFabricWorkerProbeTest.kt', 'r') as f:
    content = f.read()

content = content.replace('assertEquals("Python Physical Worker", worker.environmentName)', 'assertEquals("Python Physical Worker (Secure)", worker.environmentName)')
content = content.replace('src/main/python/native_worker.py', 'src/main/python/native_worker.py')

# Ensure process builder redirects error stream and reads
old_proc = 'val process = ProcessBuilder("python3", "src/main/python/native_worker.py", "--secret", "test_secret").start()'
new_proc = '''val pb = ProcessBuilder("python3", "src/main/python/native_worker.py", "--secret", "test_secret")
        pb.redirectErrorStream(true)
        val process = pb.start()
        
        // Print process output in a background coroutine
        launch(kotlinx.coroutines.Dispatchers.IO) {
            process.inputStream.bufferedReader().use { it.lines().forEach { line -> println("WORKER: $line") } }
        }'''

content = content.replace(old_proc, new_proc)

with open('app/src/test/java/com/example/ai/capabilities/ecology/PhysicalFabricWorkerProbeTest.kt', 'w') as f:
    f.write(content)

