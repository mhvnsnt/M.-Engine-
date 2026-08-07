with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'r') as f:
    lines = f.readlines()

for i in range(250, 320):
    if "fun " in lines[i]:
        print(f"Line {i}: {lines[i].strip()}")
