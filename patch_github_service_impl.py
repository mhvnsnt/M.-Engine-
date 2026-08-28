with open("app/src/main/java/com/example/ai/capabilities/GitHubServiceImpl.kt", "r") as f:
    code = f.read()

import re
# Look for the exact line
code = re.sub(r'                val decoded = android\.util\.Base64\.decode\(readme\.content\.replace\([^,]+, [^,]+\), android\.util\.Base64\.DEFAULT\)\n                String\(decoded\)', r'                val decoded = android.util.Base64.decode(readme.content.replace("\\n", ""), android.util.Base64.DEFAULT)\n                String(decoded)', code)

with open("app/src/main/java/com/example/ai/capabilities/GitHubServiceImpl.kt", "w") as f:
    f.write(code)
