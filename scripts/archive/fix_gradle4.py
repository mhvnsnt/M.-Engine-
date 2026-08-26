with open("app/build.gradle.kts", "r") as f:
    content = f.read()

# Remove ignoreList
content = content.replace('ignoreList.add("Mengine_Github_PAT")', '')

# Remove manual buildConfigField
lines = content.split('\n')
new_lines = []
for line in lines:
    if 'buildConfigField("String", "Mengine_Github_PAT"' in line:
        continue
    new_lines.append(line)

content = '\n'.join(new_lines)
with open("app/build.gradle.kts", "w") as f:
    f.write(content)
