package com.nidhi.ai.data.settings

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    DARK("Dark"),
    LIGHT("Light");

    companion object {
        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name == name } ?: DARK
    }
}

enum class ResponseStyle(val label: String, val promptHint: String) {
    SHORT("Chhota", "Jawab chhote rakho: 1-3 vaakya, jab tak user detail na maange."),
    BALANCED("Balanced", "Jawab seedhe aur madhyam lambai ke rakho."),
    DETAILED("Detail", "Jawab detail me do, jahan helpful ho wahan steps aur examples ke saath.");

    companion object {
        fun fromName(name: String?): ResponseStyle =
            entries.firstOrNull { it.name == name } ?: BALANCED
    }
}

data class AppSettings(
    val model: String = DEFAULT_MODEL,
    val temperature: Float = 0.8f,
    val maxOutputTokens: Int = 1024,
    val responseStyle: ResponseStyle = ResponseStyle.BALANCED,
    val nickname: String = "",
    val customInstructions: String = "",
    val theme: ThemeMode = ThemeMode.DARK,
    val animationsEnabled: Boolean = true,
    val hasApiKey: Boolean = false
) {
    companion object {
        /** Model IDs change over time; the user can type any ID in Settings. */
        const val DEFAULT_MODEL = "gemini-2.5-flash"
        val SUGGESTED_MODELS = listOf("gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-2.5-pro")
    }
}
