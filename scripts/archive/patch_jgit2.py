import re

with open('app/src/main/java/com/example/ui/WorkspaceViewModel.kt', 'r') as f:
    content = f.read()

new_sync = """    fun syncWorkspace(workspaceId: Long, repoUrl: String) {
        viewModelScope.launch {
            _syncStatus.value = "Starting local clone via JGit for $repoUrl..."
            val pat = settingsRepository.githubPatFlow.first()
            
            withContext(Dispatchers.IO) {
                try {
                    val repoDir = java.io.File(context.filesDir, "workspaces/$workspaceId")
                    if (repoDir.exists()) {
                        repoDir.deleteRecursively()
                    }
                    repoDir.mkdirs()

                    val cloneCommand = org.eclipse.jgit.api.Git.cloneRepository()
                        .setURI(repoUrl)
                        .setDirectory(repoDir)
                        .setDepth(1)
                        
                    if (pat.isNotBlank()) {
                        cloneCommand.setCredentialsProvider(
                            org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider("PRIVATE-TOKEN", pat)
                        )
                    }
                    
                    _syncStatus.value = "Cloning repository..."
                    val git = cloneCommand.call()
                    git.close()
                    
                    _syncStatus.value = "Clone complete. Reading files..."
                    
                    val textExtensions = listOf("txt", "md", "kt", "xml", "json", "java", "gradle", "kts", "cpp", "h", "hpp", "js", "ts", "py")
                    val newFiles = mutableListOf<FileEntity>()
                    
                    fun processDirectory(dir: java.io.File) {
                        val files = dir.listFiles() ?: return
                        for (file in files) {
                            if (file.name == ".git") continue
                            if (file.isDirectory) {
                                processDirectory(file)
                            } else {
                                val ext = file.name.substringAfterLast('.', "")
                                if (textExtensions.contains(ext)) {
                                    val relativePath = file.absolutePath.removePrefix(repoDir.absolutePath).removePrefix("/")
                                    try {
                                        val content = file.readText()
                                        newFiles.add(
                                            FileEntity(
                                                workspaceId = workspaceId,
                                                filePath = relativePath,
                                                content = content,
                                                language = ext
                                            )
                                        )
                                    } catch (e: Exception) {
                                    }
                                }
                            }
                        }
                    }
                    
                    processDirectory(repoDir)
                    
                    workspaceDao.insertFiles(newFiles)
                    _syncStatus.value = "Sync complete. Loaded ${newFiles.size} files into workspace."
                } catch (e: Exception) {
                    e.printStackTrace()
                    _syncStatus.value = "Sync failed: ${e.message}"
                }
            }
        }
    }"""

# Using regex to replace the old syncWorkspace method
content = re.sub(r'    fun syncWorkspace\(workspaceId: Long, repoUrl: String\) \{.*?\n    \}', new_sync, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/WorkspaceViewModel.kt', 'w') as f:
    f.write(content)
print("Replaced successfully")
