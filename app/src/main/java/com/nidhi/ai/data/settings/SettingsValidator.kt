package com.nidhi.ai.data.settings

/** Input validation for everything the user can type into Settings. */
object SettingsValidator {
    private val KEY_REGEX = Regex("^[A-Za-z0-9_.-]{20,256}$")
    private val MODEL_REGEX = Regex("^[A-Za-z0-9._-]{3,64}$")

    /** Returns the cleaned key, or null if it does not look like an API key. */
    fun sanitizeApiKey(raw: String): String? {
        val cleaned = raw.trim().trim('"', '\'').filterNot { it.isWhitespace() }
        return cleaned.takeIf { KEY_REGEX.matches(it) }
    }

    /** Accepts "gemini-2.5-flash" and "models/gemini-2.5-flash". */
    fun sanitizeModel(raw: String): String? {
        val cleaned = raw.trim().removePrefix("models/")
        return cleaned.takeIf { MODEL_REGEX.matches(it) }
    }

    fun clampTemperature(value: Float): Float = value.coerceIn(0f, 2f)

    fun clampMaxTokens(value: Int): Int = value.coerceIn(256, 8192)

    fun sanitizeNickname(raw: String): String =
        raw.filterNot { it.isISOControl() }.replace(Regex("\\s+"), " ").trim().take(30)

    fun sanitizeCustomInstructions(raw: String): String = raw.trim().take(1000)
}
