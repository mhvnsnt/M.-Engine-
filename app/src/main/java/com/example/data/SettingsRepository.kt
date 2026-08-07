package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mengine_settings")

class SettingsRepository(private val context: Context) {
    
    companion object {
        val SYSTEM_INSTRUCTION = stringPreferencesKey("system_instruction")
        val OLLAMA_URL = stringPreferencesKey("ollama_url_encrypted")
        val OPENROUTER_KEY = stringPreferencesKey("openrouter_key_encrypted")
        val GITHUB_PAT = stringPreferencesKey("github_pat_encrypted")
        val GITHUB_CLIENT_ID = stringPreferencesKey("github_client_id")
        val USE_OPENROUTER = booleanPreferencesKey("use_openrouter")
        val COUNCIL_MODE = booleanPreferencesKey("council_mode")
        val USE_WHISPER_MODEL = booleanPreferencesKey("use_whisper_model")
        val VOICE_ADAPTATION = booleanPreferencesKey("voice_adaptation")
        val TRANSCRIPTION_LANGUAGE = stringPreferencesKey("transcription_language")
        val IS_ONBOARDING_COMPLETE = booleanPreferencesKey("is_onboarding_complete")
        
        const val DEFAULT_SYSTEM_INSTRUCTION = """You are M. Engine, a highly advanced personal AI assistant engineered to operate as a true intellectual partner.
Your core operating principles are inspired by advanced system prompts like Claude Code and Fable 5.

CRITICAL DIRECTIVES:
1. INFORMATIVE & TRANSPARENT: Never be vague or overly brief. You must explicitly detail what you have accomplished, how you did it, and the reasoning behind your decisions.
2. CONTINUOUS PROGRESSION: Always conclude your response by outlining clear, actionable next steps or asking the user for their preference on how to proceed.
3. ADAPTIVE LEARNING: You are programmed to be a personal AI "clone" of the user. You grow, learn, research, and adapt to the user's language and way of thinking, while expanding upon it with your vast knowledge base.
4. METICULOUS EXECUTION: When discussing code, workspaces, or open-source integrations, provide precise guidance, consider edge cases, and think through problems step-by-step before arriving at a conclusion.

Your goal is not just to answer questions, but to drive projects forward with proactive insights and comprehensive summaries."""
        const val DEFAULT_OLLAMA_URL = "http://10.0.2.2:11434/api/chat"
    }
    
    val systemInstructionFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SYSTEM_INSTRUCTION] ?: DEFAULT_SYSTEM_INSTRUCTION
    }
    
    val ollamaUrlFlow: Flow<String> = context.dataStore.data.map { preferences ->
        val encryptedUrl = preferences[OLLAMA_URL]
        if (encryptedUrl != null) {
            try {
                String(CryptoManager.decrypt(encryptedUrl))
            } catch (e: Exception) {
                DEFAULT_OLLAMA_URL
            }
        } else {
            DEFAULT_OLLAMA_URL
        }
    }

    val openRouterKeyFlow: Flow<String> = context.dataStore.data.map { preferences ->
        val encryptedKey = preferences[OPENROUTER_KEY]
        if (encryptedKey != null) {
            try {
                String(CryptoManager.decrypt(encryptedKey))
            } catch (e: Exception) {
                ""
            }
        } else {
            ""
        }
    }

    
    val githubClientIdFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[GITHUB_CLIENT_ID] ?: ""
    }

    val githubPatFlow: Flow<String> = context.dataStore.data.map { preferences ->
        val encryptedKey = preferences[GITHUB_PAT]
        if (encryptedKey != null) {
            try {
                String(CryptoManager.decrypt(encryptedKey))
            } catch (e: Exception) {
                ""
            }
        } else {
            ""
        }
    }
    
    val useOpenRouterFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[USE_OPENROUTER] ?: false
    }

    val councilModeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[COUNCIL_MODE] ?: false
    }

    val useWhisperModelFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[USE_WHISPER_MODEL] ?: true
    }

    val voiceAdaptationFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[VOICE_ADAPTATION] ?: true
    }

    val transcriptionLanguageFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[TRANSCRIPTION_LANGUAGE] ?: "en"
    }
    
    val isOnboardingCompleteFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_ONBOARDING_COMPLETE] ?: false
    }

    suspend fun updateSystemInstruction(instruction: String) {
        context.dataStore.edit { preferences ->
            preferences[SYSTEM_INSTRUCTION] = instruction
        }
    }

    suspend fun updateOllamaUrl(url: String) {
        val encryptedUrl = CryptoManager.encrypt(url.toByteArray())
        context.dataStore.edit { preferences ->
            preferences[OLLAMA_URL] = encryptedUrl
        }
    }

    suspend fun updateOpenRouterKey(key: String) {
        val encryptedKey = CryptoManager.encrypt(key.toByteArray())
        context.dataStore.edit { preferences ->
            preferences[OPENROUTER_KEY] = encryptedKey
        }
    }

    
    suspend fun updateGithubClientId(clientId: String) {
        context.dataStore.edit { preferences ->
            preferences[GITHUB_CLIENT_ID] = clientId
        }
    }

    suspend fun updateGithubPat(key: String) {
        val encryptedKey = CryptoManager.encrypt(key.toByteArray())
        context.dataStore.edit { preferences ->
            preferences[GITHUB_PAT] = encryptedKey
        }
    }

    suspend fun updateUseOpenRouter(use: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_OPENROUTER] = use
        }
    }

    suspend fun updateCouncilMode(mode: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[COUNCIL_MODE] = mode
        }
    }

    suspend fun updateUseWhisperModel(use: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_WHISPER_MODEL] = use
        }
    }

    suspend fun updateVoiceAdaptation(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VOICE_ADAPTATION] = enabled
        }
    }

    suspend fun updateTranscriptionLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[TRANSCRIPTION_LANGUAGE] = lang
        }
    }
    
    suspend fun completeOnboarding() {
        context.dataStore.edit { preferences ->
            preferences[IS_ONBOARDING_COMPLETE] = true
        }
    }
}
