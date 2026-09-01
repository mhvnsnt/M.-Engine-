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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import com.example.data.ALL_MIGRATIONS
import com.example.data.AppDatabase
import com.example.data.ChatRepository
import com.example.data.CanonicalMemory
import com.example.data.RoomConversationLedger
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

import com.example.ui.AppShell

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var repository: ChatRepository
    private lateinit var conversationLedger: RoomConversationLedger
    private lateinit var canonicalMemory: CanonicalMemory
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
        // Declared migrations run FIRST. Before this, the destructive fallback
        // alone meant every schema bump deleted the whole encrypted database —
        // and with it all conversation history. A Level 0 record cannot be
        // immutable on a store configured to be discarded.
        .addMigrations(*ALL_MIGRATIONS)
        .fallbackToDestructiveMigration(true)
        .build()
        
        memoryDao = database.memoryFragmentDao()
        embeddingEngine = EmbeddingEngine(applicationContext)
        ttsEngine = TTSEngine(applicationContext)
        
        conversationLedger = RoomConversationLedger(database.conversationEventDao())
        canonicalMemory = CanonicalMemory(conversationLedger, database.ownerContextDao())

        repository = ChatRepository(
            database.messageDao(),
            database.styleDao(),
            database.endpointDao(),
            database.sessionDao(),
            conversationLedger,
        )

        // Backfill any pre-ledger messages into Level 0 exactly once. The
        // operation is idempotent — event ids derive from message row ids and
        // inserts use IGNORE — so running it on every launch is safe and
        // self-healing rather than duplicating history.
        lifecycleScope.launch {
            val added = runCatching { repository.backfillLedgerFromMessages() }.getOrDefault(0)
            if (added > 0) {
                android.util.Log.i("MEngine", "Ledger backfill: $added legacy messages migrated to Level 0")
            }
            // Hydrate Level 1 owner context from persistent storage. Seeding
            // runs once and only when the table is empty, so an owner edit is
            // never overwritten on a later launch.
            runCatching {
                canonicalMemory.seedDefaultsIfEmpty()
                val prefs = canonicalMemory.hydrate()
                android.util.Log.i("MEngine", "Owner context hydrated: $prefs terminology preference(s)")
            }
        }
        settingsRepository = SettingsRepository(applicationContext)
        val locationRepository = com.example.data.LocationRepository(applicationContext, database.locationDao())
        val astroRepository = com.example.data.AstroNumerologyRepository(database.astroDao())
        val localIntelligenceRepository = com.example.data.LocalIntelligenceRepository(applicationContext)
        viewModel = ViewModelProvider(this, ChatViewModelFactory(locationRepository, astroRepository, localIntelligenceRepository, repository, settingsRepository, memoryDao, database.graphNodeDao(), database.jobDao(), embeddingEngine, ttsEngine, applicationContext, database.missionDao()))[ChatViewModel::class.java]
        workspaceViewModel = ViewModelProvider(this, WorkspaceViewModelFactory(applicationContext, database.workspaceDao(), settingsRepository))[WorkspaceViewModel::class.java]

        com.example.ui.RecoveryShell.initiateSafeUpdate(this)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppShell(viewModel = viewModel, workspaceViewModel = workspaceViewModel)
                }
            }
        }
    }
}
