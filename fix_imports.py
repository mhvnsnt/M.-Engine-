with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'r') as f:
    content = f.read()

if "import kotlinx.coroutines.flow.firstOrNull" not in content:
    content = content.replace("import kotlinx.coroutines.flow.first", "import kotlinx.coroutines.flow.first\nimport kotlinx.coroutines.flow.firstOrNull")

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'w') as f:
    f.write(content)
