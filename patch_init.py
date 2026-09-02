import re

file_path = '/app/applet/app/src/main/java/com/example/data/RoomConversationLedger.kt'
with open(file_path, 'r') as f:
    content = f.read()

init_block = """
    init {
        scope.launch {
            syncFromCanonical()
        }
    }
"""

content = content.replace("class RoomConversationLedger(", "class RoomConversationLedger(\n" + init_block)
# Actually it needs to be inside the class body, not before the constructor...
