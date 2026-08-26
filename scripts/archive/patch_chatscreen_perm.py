import re

with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'r') as f:
    content = f.read()

target = """    val micPermissionState = rememberPermissionState(
        android.Manifest.permission.RECORD_AUDIO
    )"""

replacement = """    val micPermissionState = rememberPermissionState(
        android.Manifest.permission.RECORD_AUDIO
    )
    val locationPermissionsState = com.google.accompanist.permissions.rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    
    LaunchedEffect(Unit) {
        if (!locationPermissionsState.allPermissionsGranted) {
            locationPermissionsState.launchMultiplePermissionRequest()
        }
    }"""

if "locationPermissionsState" not in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'w') as f:
        f.write(content)
