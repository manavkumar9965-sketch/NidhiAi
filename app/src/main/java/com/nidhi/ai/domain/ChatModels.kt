package com.nidhi.ai.domain

import com.nidhi.ai.data.ai.AiErrorKind

data class ChatMessage(
    val id: Long,
    val isUser: Boolean,
    val text: String
)

enum class AssistantStatus(val label: String) {
    IDLE("Taiyaar hoon"),
    LISTENING("Sun rahi hoon…"),
    THINKING("Soch rahi hoon…"),
    SPEAKING("Bol rahi hoon…")
}

data class ChatError(val kind: AiErrorKind, val message: String)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val status: AssistantStatus = AssistantStatus.IDLE,
    val error: ChatError? = null
)
