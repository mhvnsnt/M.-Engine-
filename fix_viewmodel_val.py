import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

target = "    private val _deviceFlowState = MutableStateFlow<DeviceFlowState?>(null)"
new_target = "    private val _deviceFlowState = MutableStateFlow<DeviceFlowState?>(null)\n    private val _endpointStatuses = MutableStateFlow<Map<Int, String>>(emptyMap())\n    val endpointStatuses: StateFlow<Map<Int, String>> = _endpointStatuses.asStateFlow()"
content = content.replace(target, new_target)

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
