with open("app/src/main/java/com/example/ui/SettingsScreen.kt", "r") as f:
    lines = f.readlines()

new_lines = []
skip = False
for line in lines:
    if "val initialGithubPat by viewModel.githubPat.collectAsStateWithLifecycle()" in line:
        continue
    if "var githubPat by remember { mutableStateOf(initialGithubPat) }" in line:
        continue
    
    if "LaunchedEffect(initialGithubPat) {" in line:
        skip = True
        continue
    
    if skip:
        if "    }" in line:
            skip = False
        continue
        
    if "OutlinedTextField(" in line and "githubPat," in "".join(lines[lines.index(line):lines.index(line)+5]):
        skip = True
        continue
        
    if skip and "placeholder = { Text(\"ghp_...\") }" in line:
        skip = False
        continue
    if skip and ")" in line and "ghp_..." not in line:
        # wait, regex is safer
        pass

