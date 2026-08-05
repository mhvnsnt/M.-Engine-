package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.FileEntity
import com.example.data.SettingsRepository
import com.example.data.WorkspaceDao
import com.example.data.WorkspaceEntity
import com.example.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorkspaceViewModel(
    private val workspaceDao: WorkspaceDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val workspaces: StateFlow<List<WorkspaceEntity>> = workspaceDao.getAllWorkspaces().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _syncStatus = MutableStateFlow<String>("")
    val syncStatus: StateFlow<String> = _syncStatus

    fun getFilesForWorkspace(workspaceId: Long): StateFlow<List<FileEntity>> {
        return workspaceDao.getFilesForWorkspace(workspaceId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun addWorkspace(name: String, repoUrl: String) {
        viewModelScope.launch {
            val workspace = WorkspaceEntity(name = name, githubRepoUrl = repoUrl)
            val id = workspaceDao.insertWorkspace(workspace)
            syncWorkspace(id, repoUrl)
        }
    }

    fun syncWorkspace(workspaceId: Long, repoUrl: String) {
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
    }

    fun updateFileContent(fileId: Long, newContent: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = workspaceDao.getFileById(fileId)
            if (file != null) {
                workspaceDao.insertFile(file.copy(content = newContent))
            }
        }
    }
}

class WorkspaceViewModelFactory(
    private val workspaceDao: WorkspaceDao,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkspaceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkspaceViewModel(workspaceDao, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
