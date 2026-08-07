with open('app/src/main/java/com/example/ui/MarkdownText.kt', 'r') as f:
    content = f.read()

content = content.replace(
    '.usePlugin(TablePlugin.create(context))',
    '.usePlugin(TablePlugin.create(context))\n            .usePlugin(SyntaxHighlightPlugin())'
)

with open('app/src/main/java/com/example/ui/MarkdownText.kt', 'w') as f:
    f.write(content)
