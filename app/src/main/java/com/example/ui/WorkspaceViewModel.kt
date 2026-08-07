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

class WorkspaceViewModel(private val context: android.content.Context, 
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
    private val _selectedFile = MutableStateFlow<FileEntity?>(null)
    val selectedFile: StateFlow<FileEntity?> = _selectedFile

    fun selectFile(file: FileEntity?) {
        _selectedFile.value = file
    }

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

class WorkspaceViewModelFactory(private val context: android.content.Context, 
    private val workspaceDao: WorkspaceDao,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkspaceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkspaceViewModel(context, workspaceDao, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
