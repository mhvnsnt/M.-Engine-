import re

# Fix MainScreen.kt
with open('app/src/main/java/com/example/ui/MainScreen.kt', 'r') as f:
    ms_content = f.read()

ms_content = ms_content.replace('import kotlinx.coroutines.launch\n\n@Composable', '@Composable')
if 'import kotlinx.coroutines.launch' not in ms_content:
    ms_content = ms_content.replace('import androidx.compose.ui.unit.dp', 'import androidx.compose.ui.unit.dp\nimport kotlinx.coroutines.launch')

ms_content = ms_content.replace('PrivacyScreen(onNavigateBack = { navController.popBackStack() })', 'PrivacyScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() })')

with open('app/src/main/java/com/example/ui/MainScreen.kt', 'w') as f:
    f.write(ms_content)

# Fix ChatScreen.kt
with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'r') as f:
    cs_content = f.read()

if 'import androidx.compose.foundation.shape.CircleShape' not in cs_content:
    cs_content = cs_content.replace('import androidx.compose.foundation.shape.RoundedCornerShape', 'import androidx.compose.foundation.shape.RoundedCornerShape\nimport androidx.compose.foundation.shape.CircleShape')
    if 'import androidx.compose.foundation.shape.CircleShape' not in cs_content:
        cs_content = cs_content.replace('import androidx.compose.ui.Modifier', 'import androidx.compose.ui.Modifier\nimport androidx.compose.foundation.shape.CircleShape')

with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'w') as f:
    f.write(cs_content)

