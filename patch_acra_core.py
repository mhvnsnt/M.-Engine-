with open("gradle/libs.versions.toml", "r") as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    new_lines.append(line)
    if line.startswith('acra-toast ='):
        new_lines.append('acra-core = { group = "ch.acra", name = "acra-core", version.ref = "acra" }\n')

with open("gradle/libs.versions.toml", "w") as f:
    f.writelines(new_lines)
