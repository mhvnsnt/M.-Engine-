import re

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'r') as f:
    content = f.read()

# Fix OpenRouter mismatch again, making sure we caught the exact line.
# Let's just do a brute force replacement of the OpenRouterRequest call.
old_str = "val request = OpenRouterRequest(model = endpoint.modelName, messages = history, stream = true)"
new_str = "val request = OpenRouterRequest(model = endpoint.modelName, messages = history.map { com.example.network.OpenRouterMessage(it.role, it.content) }, stream = true)"
content = content.replace(old_str, new_str)

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'w') as f:
    f.write(content)

