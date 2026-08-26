import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

target = """    private val context: android.content.Context
) : ViewModel() {

    val memoryManager = com.example.github.MemoryManager(context)
) : ViewModel() {"""

replacement = """    private val context: android.content.Context
) : ViewModel() {

    val memoryManager = com.example.github.MemoryManager(context)"""

content = content.replace(target, replacement)
with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
