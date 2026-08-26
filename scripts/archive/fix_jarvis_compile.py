import re

with open("app/src/main/java/com/example/ai/CodeJarvis.kt", "r") as f:
    content = f.read()

# Replace the jsonLine.startsWith("{") logic
old_block = """                    } else if (jsonLine.startsWith("{")) {
                        try {
                            val resp = adapter.fromJson(jsonLine)
                            resp?.choices?.firstOrNull()?.message?.content?.let { completeResponse += it }
                        } catch (e: Exception) {}
                    }"""
                    
new_block = """                    } else if (jsonLine.startsWith("{")) {
                        try {
                            val resp = adapter.fromJson(jsonLine)
                            resp?.choices?.firstOrNull()?.delta?.content?.let { completeResponse += it }
                        } catch (e: Exception) {}
                    }"""

content = content.replace(old_block, new_block)

with open("app/src/main/java/com/example/ai/CodeJarvis.kt", "w") as f:
    f.write(content)
