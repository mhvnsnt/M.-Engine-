with open('app/src/main/java/com/example/ui/MainScreen.kt', 'r') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "ChatScreen" in line:
        print(line.strip())
