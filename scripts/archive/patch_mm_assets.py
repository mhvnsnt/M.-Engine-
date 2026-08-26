import re

with open("app/src/main/java/com/example/github/MemoryManager.kt", "r") as f:
    content = f.read()

target = """            val memoryDir = File(context.filesDir, "memory")
            val file = File(memoryDir, "system-prompt.md")
            if (file.exists()) file.readText() else null"""

replacement = """            val memoryDir = File(context.filesDir, "memory")
            val file = File(memoryDir, "system-prompt.md")
            if (file.exists()) {
                file.readText()
            } else {
                context.assets.open("memory/system-prompt.md").bufferedReader().use { it.readText() }
            }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/github/MemoryManager.kt", "w") as f:
    f.write(content)
