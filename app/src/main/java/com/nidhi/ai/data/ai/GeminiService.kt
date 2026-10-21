package com.nidhi.ai.data.ai

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * Gemini REST implementation (streamGenerateContent with Server-Sent Events).
 * The key is sent in the `x-goog-api-key` header, never in the URL, so it cannot end up in
 * URL logs. Only HTTPS is used.
 */
class GeminiService(
    private val client: OkHttpClient = defaultClient(),
    private val baseUrl: String = "https://generativelanguage.googleapis.com/v1beta"
) : AiProvider {

    override fun streamChat(config: AiConfig, history: List<AiTurn>): Flow<String> = flow {
        if (config.apiKey.isBlank()) throw AiException(AiErrorKind.MISSING_KEY)
        if (!MODEL_REGEX.matches(config.model)) {
            throw AiException(AiErrorKind.BAD_REQUEST, "Model ID ka format galat hai")
        }

        val request = Request.Builder()
            .url("$baseUrl/models/${config.model}:streamGenerateContent?alt=sse")
            .header("x-goog-api-key", config.apiKey)
            .post(
                GeminiParser.buildRequestBody(config, history)
                    .toRequestBody("application/json; charset=utf-8".toMediaType())
            )
            .build()

        val call = client.newCall(request)
        val handle = currentCoroutineContext()[Job]?.invokeOnCompletion { call.cancel() }
        try {
            val response = try {
                call.execute()
            } catch (e: IOException) {
                throw mapIo(e, call.isCanceled())
            }
            response.use { resp ->
                if (!resp.isSuccessful) {
                    throw GeminiParser.mapHttpError(resp.code, resp.body?.string())
                }
                val source = resp.body?.source()
                    ?: throw AiException(AiErrorKind.EMPTY_RESPONSE)
                while (true) {
                    val line = try {
                        source.readUtf8Line()
                    } catch (e: IOException) {
                        throw mapIo(e, call.isCanceled())
                    } ?: break
                    if (!line.startsWith("data:")) continue
                    val payload = line.removePrefix("data:").trim()
                    if (payload.isEmpty()) continue
                    val text = GeminiParser.extractText(payload)
                    if (text.isNotEmpty()) emit(text)
                }
            }
        } finally {
            handle?.dispose()
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun testConnection(config: AiConfig): Result<Unit> = try {
        // A 200 response with a first chunk (or an empty-but-successful stream) proves the setup works.
        streamChat(
            config.copy(maxOutputTokens = 64, systemInstruction = ""),
            listOf(AiTurn(AiRole.USER, "Hi"))
        ).firstOrNull()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: AiException) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(AiException(AiErrorKind.UNKNOWN))
    }

    private fun mapIo(e: IOException, cancelled: Boolean): Exception = when {
        cancelled -> CancellationException("cancelled")
        e is SocketTimeoutException -> AiException(AiErrorKind.TIMEOUT)
        else -> AiException(AiErrorKind.NETWORK)
    }

    companion object {
        private val MODEL_REGEX = Regex("^[A-Za-z0-9._-]{3,64}$")

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
