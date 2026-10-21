package com.nidhi.ai

import com.nidhi.ai.data.ai.AiRole
import com.nidhi.ai.data.ai.AiTurn
import com.nidhi.ai.data.settings.ResponseStyle
import com.nidhi.ai.data.settings.SettingsValidator
import com.nidhi.ai.domain.PromptBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptAndValidatorTest {

    @Test
    fun systemInstruction_includesNicknameStyleAndCustom() {
        val s = PromptBuilder.systemInstruction("Jaan", "Hamesha emoji use karo", ResponseStyle.SHORT)
        assertTrue(s.contains("Jaan"))
        assertTrue(s.contains(ResponseStyle.SHORT.promptHint))
        assertTrue(s.contains("Hamesha emoji use karo"))
        assertTrue(s.contains("AI"))
    }

    @Test
    fun systemInstruction_withoutNicknameOrCustom_hasNoExtras() {
        val s = PromptBuilder.systemInstruction("  ", "", ResponseStyle.BALANCED)
        assertFalse(s.contains("bulao"))
        assertFalse(s.contains("extra instructions"))
    }

    @Test
    fun systemInstruction_capsCustomLength() {
        val s = PromptBuilder.systemInstruction("", "a".repeat(5000), ResponseStyle.BALANCED)
        assertTrue(s.contains("a".repeat(1000)))
        assertFalse(s.contains("a".repeat(1001)))
    }

    @Test
    fun trimHistory_keepsLastTurnsAndStartsWithUser() {
        val turns = (1..30).map { AiTurn(if (it % 2 == 1) AiRole.USER else AiRole.MODEL, "m$it") }
        val trimmed = PromptBuilder.trimHistory(turns, maxTurns = 5)
        // Last 5 are m26..m30; m26 is a model turn and is dropped so the list starts with a user turn.
        assertEquals(AiRole.USER, trimmed.first().role)
        assertEquals("m27", trimmed.first().text)
        assertEquals("m30", trimmed.last().text)
        assertEquals(4, trimmed.size)
    }

    @Test
    fun trimHistory_emptyStaysEmpty() {
        assertTrue(PromptBuilder.trimHistory(emptyList()).isEmpty())
    }

    @Test
    fun apiKey_validationAndCleaning() {
        val raw = "  \"AIzaSyA1234567890abcdefghijklmnopqrstuv\"  "
        assertEquals("AIzaSyA1234567890abcdefghijklmnopqrstuv", SettingsValidator.sanitizeApiKey(raw))
        assertNull(SettingsValidator.sanitizeApiKey("short"))
        assertNull(SettingsValidator.sanitizeApiKey("has spaces inside but is long enough 12345!!"))
        assertNull(SettingsValidator.sanitizeApiKey(""))
    }

    @Test
    fun model_validation() {
        assertEquals("gemini-2.5-flash", SettingsValidator.sanitizeModel("models/gemini-2.5-flash"))
        assertNotNull(SettingsValidator.sanitizeModel("gemini-2.5-pro"))
        assertNull(SettingsValidator.sanitizeModel("bad model/../x"))
        assertNull(SettingsValidator.sanitizeModel("a"))
    }

    @Test
    fun numbers_areClamped() {
        assertEquals(2f, SettingsValidator.clampTemperature(9f), 0f)
        assertEquals(0f, SettingsValidator.clampTemperature(-1f), 0f)
        assertEquals(256, SettingsValidator.clampMaxTokens(1))
        assertEquals(8192, SettingsValidator.clampMaxTokens(100000))
    }

    @Test
    fun nickname_isSanitized() {
        assertEquals("Raj Kumar", SettingsValidator.sanitizeNickname("  Raj \n  Kumar  "))
        assertEquals(30, SettingsValidator.sanitizeNickname("x".repeat(100)).length)
    }
}
