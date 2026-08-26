import re

with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'r') as f:
    lines = f.readlines()

unique_lines = []
for line in lines:
    if line.startswith('import '):
        if line not in unique_lines:
            unique_lines.append(line)
    else:
        unique_lines.append(line)

with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'w') as f:
    f.writelines(unique_lines)
