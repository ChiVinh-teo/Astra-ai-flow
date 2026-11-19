package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import com.example.model.AiSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.astraDataStore: DataStore<Preferences> by preferencesDataStore(name = "astraflow_prefs")

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val API_KEY = stringPreferencesKey("custom_gemini_api_key")
        val FAST_MODEL = stringPreferencesKey("fast_model_id")
        val DEEP_MODEL = stringPreferencesKey("deep_model_id")
        val WHITE_PAPER_EXAM = booleanPreferencesKey("is_white_paper_exam")
        val TTS_SPEED = floatPreferencesKey("tts_speed")
        val STREAK_DAYS = intPreferencesKey("streak_days")
        val LAST_STUDY_DATE = stringPreferencesKey("last_study_date")
        val DISCOVERED_MODELS = stringPreferencesKey("discovered_models_csv")
    }

    val settingsFlow: Flow<AiSettings> = context.astraDataStore.data.map { prefs ->
        val savedKey = prefs[Keys.API_KEY].orEmpty()
        val fallbackKey = BuildConfig.GEMINI_API_KEY
            .takeIf { it.isNotBlank() && it != "MY_GEMINI_API_KEY" }
            .orEmpty()
        val discoveredCsv = prefs[Keys.DISCOVERED_MODELS].orEmpty()
        val discovered = discoveredCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
            .ifEmpty { listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.5-flash-lite") }

        AiSettings(
            apiKey = savedKey.ifBlank { fallbackKey },
            fastModelId = prefs[Keys.FAST_MODEL]?.ifBlank { "gemini-2.5-flash" } ?: "gemini-2.5-flash",
            deepModelId = prefs[Keys.DEEP_MODEL]?.ifBlank { "gemini-2.5-pro" } ?: "gemini-2.5-pro",
            isWhitePaperExam = prefs[Keys.WHITE_PAPER_EXAM] ?: false,
            ttsSpeed = prefs[Keys.TTS_SPEED] ?: 1.0f,
            streakDays = prefs[Keys.STREAK_DAYS] ?: 3,
            discoveredModels = discovered
        )
    }

    suspend fun saveAiConfig(apiKey: String, fastModel: String, deepModel: String) {
        context.astraDataStore.edit { prefs ->
            prefs[Keys.API_KEY] = apiKey.trim()
            prefs[Keys.FAST_MODEL] = fastModel.trim().ifBlank { "gemini-2.5-flash" }
            prefs[Keys.DEEP_MODEL] = deepModel.trim().ifBlank { "gemini-2.5-pro" }
        }
    }

    suspend fun saveDiscoveredModels(models: List<String>) {
        if (models.isEmpty()) return
        context.astraDataStore.edit { prefs ->
            prefs[Keys.DISCOVERED_MODELS] = models.distinct().joinToString(",")
        }
    }

    suspend fun setWhitePaperExam(enabled: Boolean) {
        context.astraDataStore.edit { it[Keys.WHITE_PAPER_EXAM] = enabled }
    }

    suspend fun setTtsSpeed(speed: Float) {
        context.astraDataStore.edit { it[Keys.TTS_SPEED] = speed.coerceIn(0.6f, 1.8f) }
    }

    suspend fun recordDailyStreak(todayIso: String) {
        context.astraDataStore.edit { prefs ->
            val last = prefs[Keys.LAST_STUDY_DATE]
            if (last != todayIso) {
                val current = prefs[Keys.STREAK_DAYS] ?: 3
                prefs[Keys.STREAK_DAYS] = current + 1
                prefs[Keys.LAST_STUDY_DATE] = todayIso
            }
        }
    }
}
