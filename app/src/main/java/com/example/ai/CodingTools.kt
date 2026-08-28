package com.example.ai

import android.content.Context
import android.util.Log
import com.example.network.RetrofitClient
import com.example.network.TelegramMessageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

import java.util.concurrent.ConcurrentLinkedQueue

class CodingTools(private val context: Context) {
    private val activeProcesses = ConcurrentLinkedQueue<Process>()

    fun cancelActiveProcesses() {
        activeProcesses.forEach { 
            try {
                it.destroyForcibly()
            } catch (e: Exception) {
                Log.e("CodingTools", "Failed to destroy process", e)
            }
        }
        activeProcesses.clear()
    }

    private fun getRepoDir(owner: String, repo: String): File {
        return File(context.filesDir, "repos/$owner/$repo")
    }

    suspend fun cloneOrPull(pat: String, owner: String, repo: String): String = withContext(Dispatchers.IO) {
        val repoDir = getRepoDir(owner, repo)
        val url = "https://github.com/$owner/$repo.git"
        val credentials = UsernamePasswordCredentialsProvider(pat, "")
        
        try {
            if (repoDir.exists() && File(repoDir, ".git").exists()) {
                val git = Git.open(repoDir)
                val pullResult = git.pull().setCredentialsProvider(credentials).call()
                "Successfully pulled latest changes: ${pullResult.isSuccessful}"
            } else {
                repoDir.mkdirs()
                Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(repoDir)
                    .setCredentialsProvider(credentials)
                    .call()
                "Successfully cloned repository."
            }
        } catch (e: Exception) {
            Log.e("CodingTools", "Git error", e)
            "Git Error: ${e.message}"
        }
    }

    suspend fun listFiles(pat: String, owner: String, repo: String, branch: String = "main"): List<String> = withContext(Dispatchers.IO) {
        val repoDir = getRepoDir(owner, repo)
        if (!repoDir.exists()) {
            cloneOrPull(pat, owner, repo)
        }
        
        val files = mutableListOf<String>()
        repoDir.walkTopDown().forEach { file ->
            if (file.isFile && !file.absolutePath.contains("/.git/")) {
                files.add(file.absolutePath.removePrefix(repoDir.absolutePath + "/"))
            }
        }
        files
    }

    suspend fun readFile(pat: String, owner: String, repo: String, branch: String = "main", path: String): String? = withContext(Dispatchers.IO) {
        val repoDir = getRepoDir(owner, repo)
        if (!repoDir.exists()) {
            cloneOrPull(pat, owner, repo)
        }
        val file = File(repoDir, path)
        if (file.exists()) file.readText() else null
    }

    suspend fun writeFile(pat: String, owner: String, repo: String, path: String, content: String): Boolean = withContext(Dispatchers.IO) {
        val repoDir = getRepoDir(owner, repo)
        val file = File(repoDir, path)
        try {
            file.parentFile?.mkdirs()
            file.writeText(content)
            true
        } catch(e: Exception) {
            false
        }
    }


    suspend fun checkoutBranch(pat: String, owner: String, repo: String, branch: String): Boolean = withContext(Dispatchers.IO) {
        val repoDir = getRepoDir(owner, repo)
        if (!repoDir.exists()) cloneOrPull(pat, owner, repo)
        try {
            val git = Git.open(repoDir)
            val createBranch = git.branchList().call().none { it.name.endsWith(branch) }
            if (createBranch) {
                git.checkout().setCreateBranch(true).setName(branch).call()
            } else {
                git.checkout().setName(branch).call()
            }
            true
        } catch (e: Exception) {
            Log.e("CodingTools", "Checkout error", e)
            false
        }
    }

    suspend fun fetchAndPull(pat: String, owner: String, repo: String): Boolean = withContext(Dispatchers.IO) {
        val repoDir = getRepoDir(owner, repo)
        if (!repoDir.exists()) {
            cloneOrPull(pat, owner, repo)
            return@withContext true
        }
        val credentials = UsernamePasswordCredentialsProvider(pat, "")
        try {
            val git = Git.open(repoDir)
            git.fetch().setCredentialsProvider(credentials).call()
            git.pull().setCredentialsProvider(credentials).call()
            true
        } catch (e: Exception) {
            Log.e("CodingTools", "Fetch error", e)
            false
        }
    }
    
    suspend fun getCommitSha(owner: String, repo: String): String = withContext(Dispatchers.IO) {
        val repoDir = getRepoDir(owner, repo)
        try {
            val git = Git.open(repoDir)
            val head = git.repository.resolve("HEAD")
            head.name
        } catch (e: Exception) {
            "unknown"
        }
    }

    suspend fun commitAndPush(
        pat: String, 
        owner: String, 
        repo: String, 
        branch: String = "main", 
        message: String, 
        files: Map<String, String>? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val repoDir = getRepoDir(owner, repo)
        val credentials = UsernamePasswordCredentialsProvider(pat, "")
        
        try {
            if (files != null) {
                files.forEach { (path, content) ->
                    writeFile(pat, owner, repo, path, content)
                }
            }
            
            val git = Git.open(repoDir)
            git.add().addFilepattern(".").call()
            git.commit().setMessage(message).setAuthor("CodeJarvis", "jarvis@mengine.ai").call()
            git.push().setCredentialsProvider(credentials).call()
            true
        } catch (e: Exception) {
            Log.e("CodingTools", "Git commit/push error", e)
            false
        }
    }

    suspend fun executeShell(command: String, cwd: File? = null): String = withContext(Dispatchers.IO) {
        try {
            val builder = ProcessBuilder("sh", "-c", command)
            if (cwd != null && cwd.exists()) {
                builder.directory(cwd)
            }
            val process = builder.start()
            activeProcesses.add(process)
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            val out = StringBuilder()
            val err = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) { out.append(line).append("\n") }
            while (errorReader.readLine().also { line = it } != null) { err.append(line).append("\n") }
            val completed = process.waitFor(60, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                return@withContext "Execution timed out (60s)."
            }
            "STDOUT:\n$out\nSTDERR:\n$err"
        } catch (e: Exception) {
            "Shell Execution Error: ${e.message}"
        } finally {
            // Cleanup from active list, though we don't strictly need to track finished ones
            // but it avoids memory leak of Process objects
            val it = activeProcesses.iterator()
            while (it.hasNext()) {
                if (!it.next().isAlive) {
                    it.remove()
                }
            }
        }
    }

    suspend fun executePython(pythonCode: String): String = withContext(Dispatchers.IO) {
        try {
            val scriptFile = File(context.cacheDir, "temp_script_${System.currentTimeMillis()}.py")
            scriptFile.writeText(pythonCode)
            
            val builder = ProcessBuilder("python3", scriptFile.absolutePath)
            val process = builder.start()
            activeProcesses.add(process)
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            val out = StringBuilder()
            val err = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) { out.append(line).append("\n") }
            while (errorReader.readLine().also { line = it } != null) { err.append(line).append("\n") }
            
            val completed = process.waitFor(30, TimeUnit.SECONDS)
            scriptFile.delete()
            
            if (!completed) {
                process.destroyForcibly()
                return@withContext "Python Execution Timed Out (30s)."
            }
            "PYTHON STDOUT:\n$out\nPYTHON STDERR:\n$err"
        } catch (e: Exception) {
            "Python Execution Error: ${e.message}"
        } finally {
            val it = activeProcesses.iterator()
            while (it.hasNext()) {
                if (!it.next().isAlive) {
                    it.remove()
                }
            }
        }
    }

    suspend fun buildApk(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val result = executeShell("gradle :app:assembleDebug")
            val apkFile = File("app/build/outputs/apk/debug/app-debug.apk")
            if (apkFile.exists()) {
                val sizeMb = "%.2f".format(apkFile.length() / (1024.0 * 1024.0))
                Pair(true, "APK Build Successful! Output file: ${apkFile.absolutePath} ($sizeMb MB)\n\n$result")
            } else {
                Pair(false, "APK Build Completed but app-debug.apk was not found.\n\n$result")
            }
        } catch (e: Exception) {
            Pair(false, "APK Build Error: ${e.message}")
        }
    }

    suspend fun deployApkToTelegram(token: String, chatId: Long, caption: String = "M Engine Build Artifact"): String = withContext(Dispatchers.IO) {
        if (token.isEmpty()) return@withContext "Error: Telegram Bot Token is not configured."
        
        var apkFile = File("app/build/outputs/apk/debug/app-debug.apk")
        if (!apkFile.exists()) {
            val (built, buildLog) = buildApk()
            if (!built) {
                return@withContext "APK Build Failed during deployment:\n$buildLog"
            }
            apkFile = File("app/build/outputs/apk/debug/app-debug.apk")
        }

        try {
            val chatIdBody = chatId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val captionBody = caption.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileReq = apkFile.asRequestBody("application/vnd.android.package-archive".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("document", apkFile.name, fileReq)

            val response = RetrofitClient.telegramService.sendDocument(token, chatIdBody, part, captionBody)
            if (response.ok) {
                "Successfully uploaded and deployed APK (${apkFile.length() / 1024} KB) directly to Telegram Chat ID $chatId!"
            } else {
                "Telegram Document Upload returned non-ok status."
            }
        } catch (e: Exception) {
            Log.e("CodingTools", "Failed Telegram document upload, attempting shell fallback", e)
            val uploadRes = executeShell("curl -s -F chat_id=$chatId -F document=@app/build/outputs/apk/debug/app-debug.apk https://api.telegram.org/bot$token/sendDocument")
            if (uploadRes.contains("\"ok\":true")) {
                "Successfully deployed APK to Telegram via direct multipart request!"
            } else {
                "Error deploying APK to Telegram: ${e.message}\n$uploadRes"
            }
        }
    }
}
