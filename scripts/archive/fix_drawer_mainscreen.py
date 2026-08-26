import re

with open('app/src/main/java/com/example/ui/MainScreen.kt', 'r') as f:
    content = f.read()

# Make sure imports are present
imports = """import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.rememberDrawerState
"""

if 'import androidx.compose.material3.DrawerValue' not in content:
    content = content.replace('import androidx.compose.material3.*', 'import androidx.compose.material3.*\n' + imports)

with open('app/src/main/java/com/example/ui/MainScreen.kt', 'w') as f:
    f.write(content)

