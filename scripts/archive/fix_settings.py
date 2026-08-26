import re

with open('app/src/main/java/com/example/data/SettingsRepository.kt', 'r') as f:
    content = f.read()

claude_prompt = 'You are an advanced AI assistant powered by Claude Code architecture.\\nYour behavior is governed by the following core instructions:\\n- Use structured thinking inside <thinking></thinking> tags before responding.\\n- Formulate step-by-step plans before writing code.\\n- Act autonomously and confidently.\\n- Always include the user\\'s workspace context and system constraints in your evaluation.\\n- Speak with the precision, depth, and clarity characteristic of Anthropic\\'s Opus and Fable models.'

# We want to replace DEFAULT_SYSTEM_INSTRUCTION
content = re.sub(r'const val DEFAULT_SYSTEM_INSTRUCTION = ".*?"', 'const val DEFAULT_SYSTEM_INSTRUCTION = "' + claude_prompt.replace("'", "\\'") + '"', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/data/SettingsRepository.kt', 'w') as f:
    f.write(content)

