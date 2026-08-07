with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'r') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "fun ChatScreen(" in line:
        print("".join(lines[i:i+10]))
