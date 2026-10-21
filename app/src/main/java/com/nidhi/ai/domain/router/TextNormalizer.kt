package com.nidhi.ai.domain.router

/** Lowercases, strips punctuation, and removes the "Ravan" wake word. */
object TextNormalizer {
    private val punctuation = Regex("[.,!?;:]+")
    private val whitespace = Regex("\\s+")

    fun normalize(input: String): String {
        val cleaned = input.lowercase()
            .replace(punctuation, " ")
            .replace(whitespace, " ")
            .trim()
        return when {
            cleaned == "ravan" -> ""
            cleaned.startsWith("ravan ") -> cleaned.removePrefix("ravan ").trim()
            else -> cleaned
        }
    }
}
