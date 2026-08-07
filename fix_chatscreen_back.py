import re

with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'r') as f:
    content = f.read()

# Also let's fix the alignment for drawer top padding
# Not strictly necessary but the TopAppBar is handling it. 
