with open("app/build.gradle.kts", "r") as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    new_lines.append(line)
    if 'implementation("ch.acra:acra-toast:5.11.3")' in line:
        new_lines.append('  implementation("ch.acra:acra-core:5.11.3")\n')

with open("app/build.gradle.kts", "w") as f:
    f.writelines(new_lines)
