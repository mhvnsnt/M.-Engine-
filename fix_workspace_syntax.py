import re

with open('app/src/main/java/com/example/ui/WorkspaceScreen.kt', 'r') as f:
    content = f.read()

# Extract the imports
imports = [
    "import androidx.compose.ui.text.AnnotatedString",
    "import androidx.compose.ui.text.SpanStyle",
    "import androidx.compose.ui.text.buildAnnotatedString",
    "import androidx.compose.ui.text.input.OffsetMapping",
    "import androidx.compose.ui.text.input.TransformedText",
    "import androidx.compose.ui.text.input.VisualTransformation"
]

for imp in imports:
    content = content.replace(imp + "\n", "")

# Add imports at the beginning
first_import_idx = content.find("import ")
content = content[:first_import_idx] + "\n".join(imports) + "\n" + content[first_import_idx:]

# Extract SyntaxHighlighter
highlighter_start = content.find("class SyntaxHighlighter")
highlighter_end = content.find("fun CodeEditor(")
if highlighter_start != -1 and highlighter_end != -1:
    highlighter_code = content[highlighter_start:highlighter_end]
    content = content[:highlighter_start] + content[highlighter_end:]
    # Append to end of file
    content = content + "\n\n" + highlighter_code

# The @Composable annotation might be separated from fun CodeEditor if there was an empty line
# Actually, now that SyntaxHighlighter is moved, @Composable is right above fun CodeEditor again.

with open('app/src/main/java/com/example/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(content)

