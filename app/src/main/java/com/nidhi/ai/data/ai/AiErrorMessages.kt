package com.nidhi.ai.data.ai

/** Turns an error kind into a short Hinglish message for the user. Never contains the API key. */
object AiErrorMessages {
    fun forUser(kind: AiErrorKind, detail: String?): String = when (kind) {
        AiErrorKind.MISSING_KEY ->
            "Gemini API key set nahi hai. Settings me jaakar key daalo."
        AiErrorKind.INVALID_KEY ->
            "API key galat, expire ya bina permission ki lag rahi hai. Settings me check karo."
        AiErrorKind.MODEL_NOT_FOUND ->
            "Ye model nahi mila. Settings me model ID badal ke dekho."
        AiErrorKind.RATE_LIMIT ->
            "Bahut zyada requests ho gayi (rate limit / quota). Thodi der baad retry karo."
        AiErrorKind.SERVER ->
            "Gemini server abhi busy hai. Thodi der baad retry karo."
        AiErrorKind.NETWORK ->
            "Internet connection check karo, phir retry karo."
        AiErrorKind.TIMEOUT ->
            "Jawab aane me bahut time laga. Retry karo."
        AiErrorKind.BLOCKED ->
            "Ye request Gemini ke safety filter ne roki. Alag tarah se pooch ke dekho."
        AiErrorKind.BAD_REQUEST ->
            "Request me kuch galat tha" + (detail?.let { ": ${it.take(160)}" } ?: ".")
        AiErrorKind.EMPTY_RESPONSE ->
            "Khaali jawab aaya. Ek baar retry karo."
        AiErrorKind.UNKNOWN ->
            "Kuch gadbad ho gayi" + (detail?.let { ": ${it.take(160)}" } ?: ". Retry karke dekho.")
    }

    /** Which errors should offer a shortcut to the Settings screen. */
    fun needsSettings(kind: AiErrorKind): Boolean =
        kind == AiErrorKind.MISSING_KEY ||
            kind == AiErrorKind.INVALID_KEY ||
            kind == AiErrorKind.MODEL_NOT_FOUND
}
