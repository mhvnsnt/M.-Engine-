import re

with open('app/src/main/java/com/example/ui/WorkspaceViewModel.kt', 'r') as f:
    content = f.read()

old_sync = """    fun syncWorkspace(workspaceId: Long, repoUrl: String) {
        viewModelScope.launch {
            _syncStatus.value = "Starting sync for $repoUrl..."
            val pat = settingsRepository.githubPatFlow.first()
            val authHeader = if (pat.isNotBlank()) "Bearer $pat" else null
            // Parse repoUrl, assuming format https://github.com/owner/repo
            val parts = repoUrl.removeSuffix("/").split("/")
            if (parts.size < 2) {
                _syncStatus.value = "Invalid GitHub URL"
                return@launch
            }
            val repo = parts.last()
            val owner = parts[parts.size - 2]
            
            try {
                val treeResponse = RetrofitClient.githubService.getRepoTree(
                    auth = authHeader,
                    owner = owner,
                    repo = repo,
                    branch = "main" // Default to main for now
                )
                
                _syncStatus.value = "Fetched tree, sha: ${treeResponse.sha}. Found ${treeResponse.tree.size} items."
                // Download text files (naive approach, skip images/binaries based on extension)
                val textExtensions = listOf("txt", "md", "kt", "xml", "json", "java", "gradle", "kts", "cpp", "h", "hpp", "js", "ts", "py")
                val blobsToDownload = treeResponse.tree.filter { item ->
                    item.type == "blob" && textExtensions.any { ext -> item.path.endsWith(".$ext") }
                }
                _syncStatus.value = "Downloading ${blobsToDownload.size} source files..."
                
                val newFiles = mutableListOf<FileEntity>()
                withContext(Dispatchers.IO) {
                    for ((index, blob) in blobsToDownload.withIndex()) {
                        try {
                            val rawUrl = "https://raw.githubusercontent.com/$owner/$repo/main/${blob.path}"
                            val response = RetrofitClient.githubService.downloadFile(rawUrl, authHeader)
                            val content = response.string()
                            newFiles.add(
                                FileEntity(
                                    workspaceId = workspaceId,
                                    filePath = blob.path,
                                    content = content,
                                    language = blob.path.substringAfterLast('.', "")
                                )
                            )
                        } catch (e: Exception) {
                            // Skip on error
                        }
                    }
                    workspaceDao.insertFiles(newFiles)
                }
                _syncStatus.value = "Sync complete. Downloaded ${newFiles.size} files."
            } catch (e: Exception) {
                _syncStatus.value = "Sync failed: ${e.message}"
            }
        }
    }"""

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

if old_sync in content:
    content = content.replace(old_sync, new_sync)
    with open('app/src/main/java/com/example/ui/WorkspaceViewModel.kt', 'w') as f:
        f.write(content)
    print("Replaced successfully")
else:
    print("Old sync not found")
