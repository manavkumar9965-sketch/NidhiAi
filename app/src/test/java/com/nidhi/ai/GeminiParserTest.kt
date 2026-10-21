package com.nidhi.ai

import com.nidhi.ai.data.ai.AiConfig
import com.nidhi.ai.data.ai.AiErrorKind
import com.nidhi.ai.data.ai.AiException
import com.nidhi.ai.data.ai.AiRole
import com.nidhi.ai.data.ai.AiTurn
import com.nidhi.ai.data.ai.GeminiParser
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GeminiParserTest {

    private val config = AiConfig("KEY", "gemini-2.5-flash", 0.8f, 512, "be nice")

    @Test
    fun requestBody_hasRolesSystemInstructionAndConfig() {
        val body = JSONObject(
            GeminiParser.buildRequestBody(
                config,
                listOf(AiTurn(AiRole.USER, "hi"), AiTurn(AiRole.MODEL, "hello"))
            )
        )
        val contents = body.getJSONArray("contents")
        assertEquals("user", contents.getJSONObject(0).getString("role"))
        assertEquals("model", contents.getJSONObject(1).getString("role"))
        assertEquals(
            "be nice",
            body.getJSONObject("systemInstruction").getJSONArray("parts").getJSONObject(0).getString("text")
        )
        assertEquals(512, body.getJSONObject("generationConfig").getInt("maxOutputTokens"))
        assertEquals(0.8, body.getJSONObject("generationConfig").getDouble("temperature"), 0.001)
    }

    @Test
    fun requestBody_omitsBlankSystemInstruction() {
        val body = JSONObject(
            GeminiParser.buildRequestBody(config.copy(systemInstruction = " "), listOf(AiTurn(AiRole.USER, "x")))
        )
        assertFalse(body.has("systemInstruction"))
    }

    @Test
    fun requestBody_neverContainsApiKey() {
        val body = GeminiParser.buildRequestBody(config, listOf(AiTurn(AiRole.USER, "x")))
        assertFalse(body.contains("KEY"))
    }

    @Test
    fun configToString_hidesKey() {
        assertFalse(AiConfig("SECRET123", "m", 1f, 1, "").toString().contains("SECRET123"))
    }

    @Test
    fun extractText_joinsPartsAndSkipsThoughts() {
        val json = """{"candidates":[{"content":{"parts":[
            {"text":"Namaste "},{"text":"hidden","thought":true},{"text":"dost"}]}}]}"""
        assertEquals("Namaste dost", GeminiParser.extractText(json))
    }

    @Test
    fun extractText_emptyWhenNoCandidates() {
        assertEquals("", GeminiParser.extractText("""{"usageMetadata":{}}"""))
    }

    @Test
    fun extractText_throwsWhenPromptBlocked() {
        try {
            GeminiParser.extractText("""{"promptFeedback":{"blockReason":"SAFETY"}}""")
            fail("expected AiException")
        } catch (e: AiException) {
            assertEquals(AiErrorKind.BLOCKED, e.kind)
        }
    }

    @Test
    fun extractText_malformedJsonThrowsUnknown() {
        try {
            GeminiParser.extractText("not json")
            fail("expected AiException")
        } catch (e: AiException) {
            assertEquals(AiErrorKind.UNKNOWN, e.kind)
        }
    }

    @Test
    fun mapHttpError_coversMainCases() {
        val badKey = """{"error":{"code":400,"message":"API key not valid. Please pass a valid API key."}}"""
        assertEquals(AiErrorKind.INVALID_KEY, GeminiParser.mapHttpError(400, badKey).kind)
        assertEquals(AiErrorKind.BAD_REQUEST, GeminiParser.mapHttpError(400, """{"error":{"message":"bad field"}}""").kind)
        assertEquals(AiErrorKind.INVALID_KEY, GeminiParser.mapHttpError(403, null).kind)
        assertEquals(AiErrorKind.MODEL_NOT_FOUND, GeminiParser.mapHttpError(404, null).kind)
        assertEquals(AiErrorKind.RATE_LIMIT, GeminiParser.mapHttpError(429, null).kind)
        assertEquals(AiErrorKind.SERVER, GeminiParser.mapHttpError(503, null).kind)
        assertEquals(AiErrorKind.UNKNOWN, GeminiParser.mapHttpError(418, null).kind)
    }

    @Test
    fun errorMessageFrom_handlesGarbage() {
        assertEquals(null, GeminiParser.errorMessageFrom("<html>"))
        assertTrue(GeminiParser.errorMessageFrom("""{"error":{"message":"x"}}""") == "x")
    }
}
