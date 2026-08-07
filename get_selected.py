with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'r') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "selectedFile" in line or "workspaceViewModel" in line:
        print(f"{i+1}: {line.strip()}")
