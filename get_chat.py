with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'r') as f:
    lines = f.readlines()

start = -1
for i, line in enumerate(lines):
    if "MarkdownText" in line:
        start = max(0, i-10)
        end = min(len(lines), i+20)
        print("".join(lines[start:end]))
        break
