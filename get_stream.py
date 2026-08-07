with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'r') as f:
    lines = f.readlines()

start = -1
for i, line in enumerate(lines):
    if "private suspend fun streamOpenRouterModel" in line:
        start = i
        break

if start != -1:
    end = start + 50
    print("".join(lines[start:end]))
