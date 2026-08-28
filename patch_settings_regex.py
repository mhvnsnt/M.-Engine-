import re
with open("app/src/main/java/com/example/ui/SettingsScreen.kt", "r") as f:
    code = f.read()

code = re.sub(r'    val initialGithubPat by viewModel\.githubPat\.collectAsStateWithLifecycle\(\)\n', '', code)
code = re.sub(r'    var githubPat by remember \{ mutableStateOf\(initialGithubPat\) \}\n', '', code)

launched_effect_regex = r'    LaunchedEffect\(initialGithubPat\) \{\n        if \(githubPat != initialGithubPat\) \{\n            githubPat = initialGithubPat\n        \}\n    \}\n'
code = re.sub(launched_effect_regex, '', code)

textfield_regex = r'            OutlinedTextField\(\n                value = githubPat,\n                onValueChange = \{\n                    githubPat = it\n                    viewModel\.updateGithubPat\(it\)\n                \},\n                modifier = Modifier\.fillMaxWidth\(\),\n                label = \{ Text\("GitHub Personal Access Token \(PAT\) / Auth Token"\) \},\n                placeholder = \{ Text\("ghp_..."\) \}\n            \)\n'
code = re.sub(textfield_regex, '', code)

with open("app/src/main/java/com/example/ui/SettingsScreen.kt", "w") as f:
    f.write(code)
