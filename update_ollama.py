import re
with open('app/src/main/java/com/example/network/OllamaModels.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'val content: String\n)',
    'val content: String,\n    @Transient val imageUri: String? = null\n)'
)
content = content.replace('import com.squareup.moshi.JsonClass', 'import com.squareup.moshi.JsonClass\nimport kotlin.jvm.Transient')

with open('app/src/main/java/com/example/network/OllamaModels.kt', 'w') as f:
    f.write(content)
