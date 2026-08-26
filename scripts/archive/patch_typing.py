with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'r') as f:
    content = f.read()

target = """                if (isGenerating) {
                    item {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(16.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                    }
                }"""
replacement = """                if (isGenerating) {
                    item {
                        TypingIndicator(modifier = Modifier.padding(16.dp))
                    }
                }"""
content = content.replace(target, replacement)

target2 = """import androidx.compose.ui.Modifier"""
replacement2 = """import androidx.compose.ui.Modifier
import androidx.compose.animation.core.*"""
content = content.replace(target2, replacement2)

target3 = """@Composable
fun MessageBubble"""
replacement3 = """@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(400), repeatMode = RepeatMode.Reverse),
        label = "dot1"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(400, delayMillis = 150), repeatMode = RepeatMode.Reverse),
        label = "dot2"
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(400, delayMillis = 300), repeatMode = RepeatMode.Reverse),
        label = "dot3"
    )

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = alpha1)))
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = alpha2)))
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = alpha3)))
    }
}

@Composable
fun MessageBubble"""
content = content.replace(target3, replacement3)

with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'w') as f:
    f.write(content)
