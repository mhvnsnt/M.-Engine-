import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

content = content.replace("val endpointStatuses: StateFlow<Map<Int, String>> = _endpointStatuses.asStateFlow()", "val endpointStatuses: StateFlow<Map<Int, String>> = _endpointStatuses")

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
