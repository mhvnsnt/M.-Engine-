import re

with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'r') as f:
    content = f.read()

# Add imports for image picker
imports = """import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.height
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
"""

if 'import coil.compose.AsyncImage' not in content:
    content = content.replace('import androidx.compose.ui.platform.LocalContext\n', 'import androidx.compose.ui.platform.LocalContext\n' + imports)

# Update state variables in ChatScreen
state_vars = """    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }
"""

content = content.replace('    val context = LocalContext.current', state_vars)

# Update attach button click listener
content = content.replace('onClick = { /* TODO: Open file picker */ }', 'onClick = { imagePickerLauncher.launch("image/*") }')

# Add UI for selected image preview above the input field
input_row = """            if (selectedImageUri != null) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected image",
                        modifier = Modifier
                            .height(100.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { selectedImageUri = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).background(Color.Black.copy(alpha=0.5f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Remove", tint = Color.White) // using placeholder icon to close
                    }
                }
            }
            Row("""

if 'if (selectedImageUri != null)' not in content:
    content = content.replace('            Row(\n                modifier = Modifier', input_row + '\n                modifier = Modifier')

# Update send button to pass imageUri
old_send = """                    onClick = {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    },"""

new_send = """                    onClick = {
                        viewModel.sendMessage(inputText, selectedImageUri?.toString())
                        inputText = ""
                        selectedImageUri = null
                    },"""
content = content.replace(old_send, new_send)

with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'w') as f:
    f.write(content)

