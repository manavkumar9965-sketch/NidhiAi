package com.nidhi.ai.data.ai

import kotlinx.coroutines.flow.Flow

enum class AiRole { USER, MODEL }

data class AiTurn(val role: AiRole, val text: String)

data class AiConfig(
    val apiKey: String,
    val model: String,
    val temperature: Float,
    val maxOutputTokens: Int,
    val systemInstruction: String
) {
    // Never let the key leak through toString() into logs.
    override fun toString(): String =
        "AiConfig(model=$model, temperature=$temperature, maxOutputTokens=$maxOutputTokens, apiKey=***)"
}

enum class AiErrorKind {
    MISSING_KEY,
    INVALID_KEY,
    MODEL_NOT_FOUND,
    RATE_LIMIT,
    SERVER,
    NETWORK,
    TIMEOUT,
    BLOCKED,
    BAD_REQUEST,
    EMPTY_RESPONSE,
    UNKNOWN
}

class AiException(
    val kind: AiErrorKind,
    val detail: String? = null,
    val httpCode: Int? = null
) : Exception(detail ?: kind.name)

/**
 * Provider-agnostic AI interface. Only [com.nidhi.ai.data.ai.GeminiService] knows about Gemini,
 * so another provider can be dropped in later without touching the UI or view models.
 */
interface AiProvider {
    /** Emits text chunks as they arrive. Fails with [AiException]. */
    fun streamChat(config: AiConfig, history: List<AiTurn>): Flow<String>

    /** Cheap request to verify key + model + network. */
    suspend fun testConnection(config: AiConfig): Result<Unit>
}
