package com.nidhi.ai.domain.router

import com.nidhi.ai.domain.model.Command

sealed interface RouteDecision {
    /** Handled locally, no internet needed. */
    data class Local(val command: Command) : RouteDecision
    /** RAVAN must ask the user before acting. */
    data class Clarify(val question: String) : RouteDecision
    /** Not a local command, so it goes to Gemini (Stage 6). */
    data class SendToAi(val text: String) : RouteDecision
}

object CommandRouter {
    fun route(input: String): RouteDecision {
        if (TextNormalizer.normalize(input).isBlank()) {
            return RouteDecision.Clarify("Haan, bolo. Main sun raha hoon.")
        }
        return when (val outcome = LocalIntentParser.parse(input)) {
            is ParseOutcome.Matched -> RouteDecision.Local(outcome.command)
            is ParseOutcome.NeedsClarification -> RouteDecision.Clarify(outcome.question)
            ParseOutcome.NoMatch -> RouteDecision.SendToAi(input.trim())
        }
    }
}
