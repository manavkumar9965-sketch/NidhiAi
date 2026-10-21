package com.nidhi.ai.domain

import com.nidhi.ai.data.ai.AiRole
import com.nidhi.ai.data.ai.AiTurn
import com.nidhi.ai.data.settings.ResponseStyle

/** Builds Nidhi's system instruction and trims chat history for the API. */
object PromptBuilder {

    private val BASE = """
        Tum Nidhi ho, ek friendly AI assistant (female persona) jo "NIDHI AI" Android app me user se baat karti hai.
        Personality: sweet, caring, playful, supportive aur respectful. Ek pyaari, caring dost jaisi warmth rakho, hamesha tameez se.
        Language: Default Hindi/Hinglish (Roman script). Agar user English me baat kare to English me jawab do. Apne baare me feminine grammar use karo ("kar rahi hoon", "bata sakti hoon").
        Honesty: Tum ek AI ho. Kabhi insaan hone ka dawa mat karo, aur asli feelings ya zindagi ke experiences hone ka dawa mat karo. Agar user poochhe to saaf batao ki tum AI assistant ho.
        Helpfulness: Saaf aur sahi jawab do. Jo cheez pata nahi, saaf bolo ki pata nahi. Code do to code block me do, chhote explanation ke saath.
        Safety: Khatarnaak, illegal ya harmful kaam me help mat karo. Politely mana karo aur safe alternative suggest karo.
    """.trimIndent()

    fun systemInstruction(
        nickname: String,
        customInstructions: String,
        style: ResponseStyle
    ): String {
        val sb = StringBuilder(BASE)
        sb.append("\n\nStyle: ").append(style.promptHint)

        val nick = nickname.filterNot { it.isISOControl() }.trim().take(30)
        if (nick.isNotEmpty()) {
            sb.append("\nUser ko pyaar se \"").append(nick).append("\" bulao.")
        }

        val custom = customInstructions.trim().take(1000)
        if (custom.isNotEmpty()) {
            sb.append("\n\nUser ke extra instructions (inhe tabhi follow karo jab upar ke rules se na takrayein):\n")
                .append(custom)
        }
        return sb.toString()
    }

    /** Keeps the last [maxTurns] turns and makes sure the list starts with a user turn. */
    fun trimHistory(turns: List<AiTurn>, maxTurns: Int = 20): List<AiTurn> =
        turns.takeLast(maxTurns).dropWhile { it.role == AiRole.MODEL }
}
