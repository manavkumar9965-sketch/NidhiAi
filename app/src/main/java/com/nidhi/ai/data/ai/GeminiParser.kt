package com.nidhi.ai.data.ai

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.math.roundToInt

/** Pure JSON helpers for the Gemini REST API (kept separate so they are unit-testable). */
object GeminiParser {

    fun buildRequestBody(config: AiConfig, history: List<AiTurn>): String {
        val root = JSONObject()

        if (config.systemInstruction.isNotBlank()) {
            root.put(
                "systemInstruction",
                JSONObject().put(
                    "parts",
                    JSONArray().put(JSONObject().put("text", config.systemInstruction))
                )
            )
        }

        val contents = JSONArray()
        history.forEach { turn ->
            contents.put(
                JSONObject()
                    .put("role", if (turn.role == AiRole.USER) "user" else "model")
                    .put("parts", JSONArray().put(JSONObject().put("text", turn.text)))
            )
        }
        root.put("contents", contents)

        root.put(
            "generationConfig",
            JSONObject()
                .put("temperature", (config.temperature * 100).roundToInt() / 100.0)
                .put("maxOutputTokens", config.maxOutputTokens)
        )
        return root.toString()
    }

    /** Extracts the text of one streamed SSE JSON payload. Returns "" when there is none. */
    fun extractText(payload: String): String {
        try {
            val obj = JSONObject(payload)
            val candidates = obj.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                val reason = obj.optJSONObject("promptFeedback")?.optString("blockReason").orEmpty()
                if (reason.isNotEmpty()) throw AiException(AiErrorKind.BLOCKED, "blockReason=$reason")
                return ""
            }
            val parts = candidates.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts") ?: return ""
            val sb = StringBuilder()
            for (i in 0 until parts.length()) {
                val part = parts.optJSONObject(i) ?: continue
                if (part.optBoolean("thought", false)) continue
                sb.append(part.optString("text", ""))
            }
            return sb.toString()
        } catch (e: JSONException) {
            throw AiException(AiErrorKind.UNKNOWN, "Response padh nahi paye")
        }
    }

    fun errorMessageFrom(body: String?): String? {
        if (body.isNullOrBlank()) return null
        return try {
            JSONObject(body).optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
        } catch (e: JSONException) {
            null
        }
    }

    fun mapHttpError(code: Int, body: String?): AiException {
        val msg = errorMessageFrom(body)?.take(200)
        return when {
            code == 400 && msg?.contains("API key", ignoreCase = true) == true ->
                AiException(AiErrorKind.INVALID_KEY, null, code)
            code == 400 -> AiException(AiErrorKind.BAD_REQUEST, msg, code)
            code == 401 || code == 403 -> AiException(AiErrorKind.INVALID_KEY, null, code)
            code == 404 -> AiException(AiErrorKind.MODEL_NOT_FOUND, null, code)
            code == 429 -> AiException(AiErrorKind.RATE_LIMIT, null, code)
            code in 500..599 -> AiException(AiErrorKind.SERVER, null, code)
            else -> AiException(AiErrorKind.UNKNOWN, msg ?: "HTTP $code", code)
        }
    }
}
