package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import com.example.data.AppDatabase
import com.example.data.ChatRepository
import com.example.ui.ChatScreen
import com.example.ui.ChatViewModel
import com.example.ui.ChatViewModelFactory
import com.example.ui.WorkspaceViewModel
import com.example.ui.WorkspaceViewModelFactory
import com.example.ui.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

import com.example.data.SettingsRepository
import com.example.ai.EmbeddingEngine
import com.example.ai.TTSEngine

import com.example.ui.MainScreen

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var repository: ChatRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: ChatViewModel
    private lateinit var workspaceViewModel: WorkspaceViewModel
    private lateinit var embeddingEngine: EmbeddingEngine
    private lateinit var ttsEngine: TTSEngine
    private lateinit var memoryDao: com.example.data.MemoryFragmentDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        SQLiteDatabase.loadLibs(this)
        val factory = SupportFactory("super-secret-mengine-key".toByteArray())
        
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "mengine-encrypted-db"
        )
        .openHelperFactory(factory)
        .fallbackToDestructiveMigration(true)
        .build()
        
        memoryDao = database.memoryFragmentDao()
        embeddingEngine = EmbeddingEngine(applicationContext)
        ttsEngine = TTSEngine(applicationContext)
        
        repository = ChatRepository(database.messageDao(), database.styleDao(), database.endpointDao(), database.sessionDao())
        settingsRepository = SettingsRepository(applicationContext)
        val locationRepository = com.example.data.LocationRepository(applicationContext, database.locationDao())
        val astroRepository = com.example.data.AstroNumerologyRepository(database.astroDao())
        val localIntelligenceRepository = com.example.data.LocalIntelligenceRepository(applicationContext)
        viewModel = ViewModelProvider(this, ChatViewModelFactory(locationRepository, astroRepository, localIntelligenceRepository, repository, settingsRepository, memoryDao, database.graphNodeDao(), embeddingEngine, ttsEngine, applicationContext))[ChatViewModel::class.java]
        workspaceViewModel = ViewModelProvider(this, WorkspaceViewModelFactory(applicationContext, database.workspaceDao(), settingsRepository))[WorkspaceViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel = viewModel, workspaceViewModel = workspaceViewModel)
                }
            }
        }
    }
}
