with open("app/build.gradle.kts", "r") as f:
    lines = f.readlines()

# find dependencies {
dep_idx = -1
for i, line in enumerate(lines):
    if line.strip() == 'dependencies {':
        dep_idx = i
        break

new_lines = []
for line in lines:
    if 'implementation("ch.acra:acra-mail:5.11.3")' in line or 'implementation("ch.acra:acra-toast:5.11.3")' in line:
        pass
    else:
        new_lines.append(line)

new_lines.insert(dep_idx + 1, '  implementation("ch.acra:acra-mail:5.11.3")\n')
new_lines.insert(dep_idx + 2, '  implementation("ch.acra:acra-toast:5.11.3")\n')

with open("app/build.gradle.kts", "w") as f:
    f.writelines(new_lines)
