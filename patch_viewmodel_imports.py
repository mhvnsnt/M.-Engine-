import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

target = "import kotlinx.coroutines.flow.StateFlow"
new = """import kotlinx.coroutines.flow.StateFlow
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.example.ai.GithubMonitorWorker"""

if "import androidx.work.PeriodicWorkRequestBuilder" not in content:
    content = content.replace(target, new)

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
