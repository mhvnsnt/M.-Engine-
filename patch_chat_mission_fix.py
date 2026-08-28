import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    code = f.read()

code = code.replace("com.example.ai.capabilities.EvidenceType.BEHAVIORAL_EVIDENCE", "com.example.ai.capabilities.EvidenceType.VIDEO_OBSERVATION")

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(code)
