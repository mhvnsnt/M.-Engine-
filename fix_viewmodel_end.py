import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

# I need to remove getPrimaryEndpointSync from the end and put it inside ChatViewModel.
# First, let's remove it from the end.
content = re.sub(r'\s+private suspend fun getPrimaryEndpointSync\(\)[\s\S]*$', "\n}", content)

# Now inject it into ChatViewModel. Let's find the end of ChatViewModel class.
# It ends right before `class ChatViewModelFactory`.
target = "class ChatViewModelFactory"
new_target = """    suspend fun getPrimaryEndpointSync(): com.example.data.EndpointEntity? {
        val active = repository.getActiveEndpoints()
        return active.find { it.isPrimary } ?: active.firstOrNull()
    }
}

class ChatViewModelFactory"""
content = content.replace("}\n\nclass ChatViewModelFactory", new_target)

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
