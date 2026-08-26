import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

target = "import kotlinx.coroutines.flow.update"
new = """import kotlinx.coroutines.flow.update
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.example.ai.GithubMonitorWorker"""
content = content.replace(target, new)

target2 = "lindyEngine.startProactiveLoop { getPrimaryEndpointSync() }"
new2 = """lindyEngine.startProactiveLoop { getPrimaryEndpointSync() }
        
        // Start proactive GitHub Action monitoring (Lindy background trigger)
        val workRequest = PeriodicWorkRequestBuilder<GithubMonitorWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "github_monitor",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )"""
content = content.replace(target2, new2)

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
