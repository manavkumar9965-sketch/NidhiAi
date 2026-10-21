package com.nidhi.ai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nidhi.ai.data.ai.AiConfig
import com.nidhi.ai.data.ai.AiErrorKind
import com.nidhi.ai.data.ai.AiErrorMessages
import com.nidhi.ai.data.ai.AiException
import com.nidhi.ai.data.ai.AiProvider
import com.nidhi.ai.data.settings.AppSettings
import com.nidhi.ai.data.settings.ResponseStyle
import com.nidhi.ai.data.settings.SettingsRepository
import com.nidhi.ai.data.settings.SettingsValidator
import com.nidhi.ai.data.settings.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Editable copy of the settings, committed only when the user taps "Save". */
data class SettingsDraft(
    val model: String = AppSettings.DEFAULT_MODEL,
    val temperature: Float = 0.8f,
    val maxTokens: Int = 1024,
    val style: ResponseStyle = ResponseStyle.BALANCED,
    val nickname: String = "",
    val customInstructions: String = ""
) {
    companion object {
        fun from(s: AppSettings) = SettingsDraft(
            model = s.model,
            temperature = s.temperature,
            maxTokens = s.maxOutputTokens,
            style = s.responseStyle,
            nickname = s.nickname,
            customInstructions = s.customInstructions
        )
    }
}

sealed interface TestState {
    data object Idle : TestState
    data object Running : TestState
    data object Success : TestState
    data class Failure(val message: String) : TestState
}

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val draft: SettingsDraft = SettingsDraft(),
    val keyInput: String = "",
    val testState: TestState = TestState.Idle,
    val message: String? = null
)

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val ai: AiProvider
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val first = repository.settings.first()
            _state.update { it.copy(settings = first, draft = SettingsDraft.from(first)) }
            repository.settings.collect { s -> _state.update { it.copy(settings = s) } }
        }
    }

    fun onKeyInputChange(text: String) {
        _state.update { it.copy(keyInput = text.take(200), message = null) }
    }

    fun onDraftChange(transform: (SettingsDraft) -> SettingsDraft) {
        _state.update { it.copy(draft = transform(it.draft), message = null) }
    }

    fun saveKey() {
        val cleaned = SettingsValidator.sanitizeApiKey(_state.value.keyInput)
        if (cleaned == null) {
            _state.update { it.copy(message = "Key ka format sahi nahi lag raha. Poori key paste karo.") }
            return
        }
        viewModelScope.launch {
            repository.saveApiKey(cleaned)
            _state.update { it.copy(keyInput = "", message = "API key save ho gayi ✓") }
        }
    }

    fun removeKey() {
        viewModelScope.launch {
            repository.clearApiKey()
            _state.update { it.copy(keyInput = "", message = "API key hata di gayi.") }
        }
    }

    fun saveDraft() {
        val d = _state.value.draft
        if (SettingsValidator.sanitizeModel(d.model) == null) {
            _state.update { it.copy(message = "Model ID galat hai (sirf letters, numbers, . - _ allowed).") }
            return
        }
        viewModelScope.launch {
            repository.saveChatSettings(
                d.model, d.temperature, d.maxTokens, d.style, d.nickname, d.customInstructions
            )
            _state.update { it.copy(message = "Settings save ho gayi ✓") }
        }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { repository.setTheme(mode) }
    }

    fun setAnimations(enabled: Boolean) {
        viewModelScope.launch { repository.setAnimationsEnabled(enabled) }
    }

    /** Tests the typed (unsaved) key if there is one, otherwise the saved key. */
    fun testConnection() {
        viewModelScope.launch {
            _state.update { it.copy(testState = TestState.Running, message = null) }
            val typed = _state.value.keyInput
            val key = if (typed.isNotBlank()) {
                SettingsValidator.sanitizeApiKey(typed)
                    ?: return@launch fail("Key ka format sahi nahi lag raha.")
            } else {
                repository.apiKey()
            }
            if (key.isNullOrBlank()) {
                return@launch fail(AiErrorMessages.forUser(AiErrorKind.MISSING_KEY, null))
            }

            val d = _state.value.draft
            val config = AiConfig(
                apiKey = key,
                model = SettingsValidator.sanitizeModel(d.model) ?: AppSettings.DEFAULT_MODEL,
                temperature = SettingsValidator.clampTemperature(d.temperature),
                maxOutputTokens = 64,
                systemInstruction = ""
            )
            ai.testConnection(config).fold(
                onSuccess = { _state.update { it.copy(testState = TestState.Success) } },
                onFailure = { e ->
                    val msg = if (e is AiException) {
                        AiErrorMessages.forUser(e.kind, e.detail)
                    } else {
                        AiErrorMessages.forUser(AiErrorKind.UNKNOWN, null)
                    }
                    fail(msg)
                }
            )
        }
    }

    private fun fail(message: String) {
        _state.update { it.copy(testState = TestState.Failure(message)) }
    }
}
