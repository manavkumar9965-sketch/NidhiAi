package com.nidhi.ai.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "nidhi_settings")

class SettingsRepository(
    private val context: Context,
    private val cipher: SecretCipher
) {
    private object Keys {
        val MODEL = stringPreferencesKey("model")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val MAX_TOKENS = intPreferencesKey("max_tokens")
        val STYLE = stringPreferencesKey("response_style")
        val NICKNAME = stringPreferencesKey("nickname")
        val CUSTOM = stringPreferencesKey("custom_instructions")
        val THEME = stringPreferencesKey("theme")
        val ANIMATIONS = booleanPreferencesKey("animations")
        val API_KEY_ENC = stringPreferencesKey("api_key_enc")
    }

    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            val defaults = AppSettings()
            AppSettings(
                model = p[Keys.MODEL] ?: defaults.model,
                temperature = p[Keys.TEMPERATURE] ?: defaults.temperature,
                maxOutputTokens = p[Keys.MAX_TOKENS] ?: defaults.maxOutputTokens,
                responseStyle = ResponseStyle.fromName(p[Keys.STYLE]),
                nickname = p[Keys.NICKNAME] ?: "",
                customInstructions = p[Keys.CUSTOM] ?: "",
                theme = ThemeMode.fromName(p[Keys.THEME]),
                animationsEnabled = p[Keys.ANIMATIONS] ?: true,
                hasApiKey = !p[Keys.API_KEY_ENC].isNullOrEmpty()
            )
        }

    /** The decrypted API key, or null if none is saved (or it can no longer be decrypted). */
    suspend fun apiKey(): String? {
        val enc = context.dataStore.data.first()[Keys.API_KEY_ENC] ?: return null
        return withContext(Dispatchers.Default) { cipher.decrypt(enc) }
    }

    /** [key] must already be validated with [SettingsValidator.sanitizeApiKey]. */
    suspend fun saveApiKey(key: String) {
        val enc = withContext(Dispatchers.Default) { cipher.encrypt(key) }
        context.dataStore.edit { it[Keys.API_KEY_ENC] = enc }
    }

    suspend fun clearApiKey() {
        context.dataStore.edit { it.remove(Keys.API_KEY_ENC) }
    }

    suspend fun saveChatSettings(
        model: String,
        temperature: Float,
        maxTokens: Int,
        style: ResponseStyle,
        nickname: String,
        customInstructions: String
    ) {
        context.dataStore.edit {
            it[Keys.MODEL] = SettingsValidator.sanitizeModel(model) ?: AppSettings.DEFAULT_MODEL
            it[Keys.TEMPERATURE] = SettingsValidator.clampTemperature(temperature)
            it[Keys.MAX_TOKENS] = SettingsValidator.clampMaxTokens(maxTokens)
            it[Keys.STYLE] = style.name
            it[Keys.NICKNAME] = SettingsValidator.sanitizeNickname(nickname)
            it[Keys.CUSTOM] = SettingsValidator.sanitizeCustomInstructions(customInstructions)
        }
    }

    suspend fun setTheme(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setAnimationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ANIMATIONS] = enabled }
    }
}
