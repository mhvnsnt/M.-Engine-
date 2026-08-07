with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'r') as f:
    lines = f.readlines()

for i in range(320, 350):
    if i < len(lines):
        print(lines[i].rstrip())
