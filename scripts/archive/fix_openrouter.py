import re

with open('app/src/main/java/com/example/network/OpenRouterModels.kt', 'r') as f:
    content = f.read()

new_classes = """@JsonClass(generateAdapter = true)
data class OpenRouterMessage(
    val role: String,
    val content: Any // Can be String or List<OpenRouterContentPart>
)

@JsonClass(generateAdapter = true)
data class OpenRouterContentPart(
    val type: String,
    val text: String? = null,
    val image_url: OpenRouterImageUrl? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterImageUrl(
    val url: String
)"""

content = content.replace('val messages: List<OllamaMessage>', 'val messages: List<OpenRouterMessage>')
content = content + "\n\n" + new_classes

with open('app/src/main/java/com/example/network/OpenRouterModels.kt', 'w') as f:
    f.write(content)

