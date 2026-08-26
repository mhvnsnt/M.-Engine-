import re

with open("app/src/main/java/com/example/ui/ChatScreen.kt", "r") as f:
    content = f.read()

# Add imports
imports = """
import com.google.accompanist.permissions.shouldShowRationale
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocationOff
"""

if "import com.google.accompanist.permissions.shouldShowRationale" not in content:
    content = content.replace("import com.google.accompanist.permissions.rememberPermissionState", 
                              "import com.google.accompanist.permissions.rememberPermissionState\n" + imports)

# Find top of ChatScreen composable
# We'll use regex to insert variables right after `val context = LocalContext.current`
insert_vars = """
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showMicDeniedDialog by remember { mutableStateOf(false) }
    var micRequestedBefore by rememberSaveable { mutableStateOf(false) }
    
    var showLocationDeniedDialog by remember { mutableStateOf(false) }
    var locationRequestedBefore by rememberSaveable { mutableStateOf(false) }
"""
if "val snackbarHostState" not in content:
    content = re.sub(r'(val context = LocalContext\.current)', r'\1\n' + insert_vars, content)

with open("app/src/main/java/com/example/ui/ChatScreen.kt", "w") as f:
    f.write(content)

