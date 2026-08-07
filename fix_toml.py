with open("gradle/libs.versions.toml", "r") as f:
    lines = f.readlines()

new_lines = []
versions_acra_seen = False
mail_acra_seen = False
toast_acra_seen = False

for line in lines:
    if line.startswith('acra ='):
        if not versions_acra_seen:
            new_lines.append(line)
            versions_acra_seen = True
    elif line.startswith('acra-mail ='):
        if not mail_acra_seen:
            new_lines.append(line)
            mail_acra_seen = True
    elif line.startswith('acra-toast ='):
        if not toast_acra_seen:
            new_lines.append(line)
            toast_acra_seen = True
    else:
        new_lines.append(line)

with open("gradle/libs.versions.toml", "w") as f:
    f.writelines(new_lines)
