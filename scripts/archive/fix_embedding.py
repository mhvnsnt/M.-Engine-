import re

with open('app/src/main/java/com/example/ai/EmbeddingEngine.kt', 'r') as f:
    content = f.read()

old_init = """    init {
        val modelBytes = context.assets.open("all-MiniLM-L6-v2.onnx").readBytes()
        session = env.createSession(modelBytes, OrtSession.SessionOptions())
        loadVocab()
    }"""
    
new_init = """    private var isInitialized = false

    init {
        try {
            val modelBytes = context.assets.open("all-MiniLM-L6-v2.onnx").readBytes()
            session = env.createSession(modelBytes, OrtSession.SessionOptions())
            loadVocab()
            isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
            // Create a dummy session or just handle the flag
            session = env.createSession(ByteArray(0)) // This will throw, so we catch it outside
        }
    }"""

# Actually a better approach is to make session nullable
content = content.replace("private val session: OrtSession", "private var session: OrtSession? = null")

old_init_2 = """    init {
        val modelBytes = context.assets.open("all-MiniLM-L6-v2.onnx").readBytes()
        session = env.createSession(modelBytes, OrtSession.SessionOptions())
        loadVocab()
    }"""

new_init_2 = """    init {
        try {
            val modelBytes = context.assets.open("all-MiniLM-L6-v2.onnx").readBytes()
            session = env.createSession(modelBytes, OrtSession.SessionOptions())
            loadVocab()
        } catch (e: Exception) {
            android.util.Log.e("EmbeddingEngine", "Failed to load ONNX model: ${e.message}")
        }
    }"""
content = content.replace(old_init_2, new_init_2)

# Fix run()
old_run = "val result = session.run(inputs)"
new_run = """val currentSession = session ?: return@withContext FloatArray(384)
        val result = currentSession.run(inputs)"""
content = content.replace(old_run, new_run)

with open('app/src/main/java/com/example/ai/EmbeddingEngine.kt', 'w') as f:
    f.write(content)
