import re

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'r') as f:
    content = f.read()

# Make sure Moshi handles Any or we just use OpenRouterContentPart for all for simplicity.
