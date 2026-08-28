with open("app/src/main/java/com/example/ui/SettingsScreen.kt", "r") as f:
    code = f.read()

import re

# Remove githubPat related states and LaunchedEffect
code = re.sub(r'    val initialGithubPat.*?\n', '', code)
code = re.sub(r'    var githubPat by.*?\n', '', code)
code = re.sub(r'    LaunchedEffect\(initialGithubPat\).*?\n.*?\}\n', '', code, flags=re.DOTALL)

# Remove the PAT OutlinedTextField block
pat_textfield_regex = r'            OutlinedTextField\([\s\S]*?label = \{ Text\("GitHub Personal Access Token \(PAT\) / Auth Token"\) \}[\s\S]*?            \)'
code = re.sub(pat_textfield_regex, '', code)

with open("app/src/main/java/com/example/ui/SettingsScreen.kt", "w") as f:
    f.write(code)
