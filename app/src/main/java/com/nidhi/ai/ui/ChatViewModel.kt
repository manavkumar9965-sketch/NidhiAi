package com.nidhi.ai.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nidhi.ai.data.ai.AiConfig
import com.nidhi.ai.data.ai.AiErrorKind
import com.nidhi.ai.data.ai.AiErrorMessages
import com.nidhi.ai.data.ai.AiException
import com.nidhi.ai.data.ai.AiProvider
import com.nidhi.ai.data.ai.AiRole
import com.nidhi.ai.data.ai.AiTurn
import com.nidhi.ai.data.settings.AppSettings
import com.nidhi.ai.data.settings.SettingsRepository
import com.nidhi.ai.data.settings.SettingsValidator
import com.nidhi.ai.domain.AssistantStatus
import com.nidhi.ai.domain.ChatError
import com.nidhi.ai.domain.ChatMessage
import com.nidhi.ai.domain.ChatUiState
import com.nidhi.ai.domain.PromptBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val settingsRepository: SettingsRepository,
    private val ai: AiProvider
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var job: Job? = null
    private var nextId = 0L

    /** True when the current question was asked by voice, so the reply is spoken aloud. */
    private var speakReply = false
    private val _speak = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val speakEvents: SharedFlow<String> = _speak.asSharedFlow()

    fun onInputChange(text: String) {
        _state.update { it.copy(input = text.take(MAX_INPUT_CHARS)) }
    }

    fun send() = submit(fromVoice = false)

    /** Called with the recognised speech: sends it and speaks the reply. */
    fun sendVoice(text: String) {
        _state.update { it.copy(input = text.take(MAX_INPUT_CHARS)) }
        submit(fromVoice = true)
    }

    /** A voice command handled on the phone (e.g. "YouTube kholo"): show it and speak the reply. */
    fun addLocalExchange(userText: String, reply: String) {
        _state.update {
            it.copy(
                input = "",
                error = null,
                messages = it.messages +
                    ChatMessage(nextId++, isUser = true, text = userText) +
                    ChatMessage(nextId++, isUser = false, text = reply)
            )
        }
        _speak.tryEmit(reply)
    }

    /** Lets the UI show LISTENING / SPEAKING. Ignored while Gemini is thinking. */
    fun setVoiceStatus(status: AssistantStatus) {
        _state.update {
            if (it.status == AssistantStatus.THINKING && status != AssistantStatus.THINKING) it
            else it.copy(status = status)
        }
    }

    private fun submit(fromVoice: Boolean) {
        val current = _state.value
        if (current.status == AssistantStatus.THINKING) return
        val text = current.input.trim()
        if (text.isEmpty()) return
        speakReply = fromVoice
        _state.update {
            it.copy(
                input = "",
                error = null,
                messages = it.messages + ChatMessage(nextId++, isUser = true, text = text)
            )
        }
        generate()
    }

    /** Re-asks the last user message after a failure. */
    fun retry() {
        if (_state.value.status == AssistantStatus.THINKING) return
        val trimmed = _state.value.messages.dropLastWhile { !it.isUser }
        if (trimmed.isEmpty()) return
        _state.update { it.copy(messages = trimmed, error = null) }
        generate()
    }

    fun stop() {
        speakReply = false
        job?.cancel()
    }

    fun clearChat() {
        job?.cancel()
        _state.update { ChatUiState(input = it.input) }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    private fun generate() {
        job?.cancel()
        job = viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val key = settingsRepository.apiKey()
            if (key.isNullOrBlank()) {
                fail(AiErrorKind.MISSING_KEY, null)
                return@launch
            }

            val history = PromptBuilder.trimHistory(
                _state.value.messages
                    .filter { it.text.isNotBlank() }
                    .map { AiTurn(if (it.isUser) AiRole.USER else AiRole.MODEL, it.text) }
            )
            val config = AiConfig(
                apiKey = key,
                model = SettingsValidator.sanitizeModel(settings.model) ?: AppSettings.DEFAULT_MODEL,
                temperature = settings.temperature,
                maxOutputTokens = settings.maxOutputTokens,
                systemInstruction = PromptBuilder.systemInstruction(
                    settings.nickname, settings.customInstructions, settings.responseStyle
                )
            )

            val placeholderId = nextId++
            _state.update {
                it.copy(
                    messages = it.messages + ChatMessage(placeholderId, isUser = false, text = ""),
                    status = AssistantStatus.THINKING,
                    error = null
                )
            }

            try {
                ai.streamChat(config, history).collect { chunk -> append(placeholderId, chunk) }
                if (messageText(placeholderId).isBlank()) {
                    removeMessage(placeholderId)
                    fail(AiErrorKind.EMPTY_RESPONSE, null)
                } else {
                    _state.update { it.copy(status = AssistantStatus.IDLE) }
                    if (speakReply) _speak.tryEmit(messageText(placeholderId))
                }
                speakReply = false
            } catch (e: CancellationException) {
                if (messageText(placeholderId).isBlank()) removeMessage(placeholderId)
                _state.update { it.copy(status = AssistantStatus.IDLE) }
                throw e
            } catch (e: AiException) {
                // Log only the error kind: never prompts, replies or keys.
                Log.w(TAG, "AI request failed: ${e.kind} http=${e.httpCode}")
                if (messageText(placeholderId).isBlank()) removeMessage(placeholderId)
                fail(e.kind, e.detail)
            } catch (e: Exception) {
                Log.w(TAG, "AI request failed: ${e.javaClass.simpleName}")
                if (messageText(placeholderId).isBlank()) removeMessage(placeholderId)
                fail(AiErrorKind.UNKNOWN, null)
            }
        }
    }

    private fun append(id: Long, chunk: String) {
        _state.update { s ->
            s.copy(messages = s.messages.map { if (it.id == id) it.copy(text = it.text + chunk) else it })
        }
    }

    private fun messageText(id: Long): String =
        _state.value.messages.firstOrNull { it.id == id }?.text.orEmpty()

    private fun removeMessage(id: Long) {
        _state.update { s -> s.copy(messages = s.messages.filterNot { it.id == id }) }
    }

    private fun fail(kind: AiErrorKind, detail: String?) {
        _state.update {
            it.copy(
                status = AssistantStatus.IDLE,
                error = ChatError(kind, AiErrorMessages.forUser(kind, detail))
            )
        }
    }

    private companion object {
        const val TAG = "NidhiChat"
        const val MAX_INPUT_CHARS = 4000
    }
}
