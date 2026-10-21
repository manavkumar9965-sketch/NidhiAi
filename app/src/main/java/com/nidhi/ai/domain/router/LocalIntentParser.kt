package com.nidhi.ai.domain.router

import com.nidhi.ai.domain.model.Command

sealed interface ParseOutcome {
    data class Matched(val command: Command) : ParseOutcome
    data class NeedsClarification(val question: String) : ParseOutcome
    data object NoMatch : ParseOutcome
}

/** Deterministic, offline parser for simple Hindi, Hinglish and English commands. */
object LocalIntentParser {

    private val appAliases: Map<String, List<String>> = mapOf(
        "youtube" to listOf("youtube", "yt", "you tube"),
        "whatsapp" to listOf("whatsapp", "whatsap", "watsapp"),
        "facebook" to listOf("facebook", "fb"),
        "instagram" to listOf("instagram", "insta"),
        "settings" to listOf("settings", "setting"),
        "camera" to listOf("camera", "kamera"),
        "maps" to listOf("google maps", "maps", "map"),
        "chrome" to listOf("chrome", "browser"),
        "gallery" to listOf("gallery", "photos"),
        "spotify" to listOf("spotify"),
        "calculator" to listOf("calculator", "calc")
    )

    private val websiteRegex = Regex("(?:chrome|browser) (?:mein|me|par|pe) (.+?) (?:kholo|khol do|open karo|open)")
    private val torchOnRegex = Regex("(?:torch|flashlight|flash) (?:on|chalu)(?: karo| kar do)?")
    private val torchOffRegex = Regex("(?:torch|flashlight|flash) (?:off|band)(?: karo| kar do)?")
    private val volumeUpRegex = Regex("(?:volume|awaaz) (?:badhao|badha do|up|tez karo|zyada karo)")
    private val volumeDownRegex = Regex("(?:volume|awaaz) (?:kam karo|kam kar do|kam|down|ghatao|low karo)")
    private val backRegex = Regex("(?:back jao|peeche jao|wapas jao|back)(?: karo)?")
    private val homeRegex = Regex("(?:home screen par jao|home par jao|home jao|home screen|home)(?: karo)?")
    private val openAppRegex = Regex("(.+?) (?:kholo|khol do|open karo|open)")
    private val reminderAmountRegex = Regex("(\\d+) ?(minute|minutes|min|mins|hour|hours|ghante|ghanta)")
    private val timeRegex = Regex("(\\d{1,2})(?::(\\d{2}))? ?(am|pm|baje|baj)?")

    private val reminderFillerWords = setOf(
        "mujhe", "mujhko", "baad", "bad", "yaad", "dilana", "dilao", "dila", "do", "karo", "ko", "me", "mein"
    )

    fun parse(raw: String): ParseOutcome {
        val text = TextNormalizer.normalize(raw)
        if (text.isBlank()) return ParseOutcome.NoMatch

        websiteRegex.matchEntire(text)?.let {
            return ParseOutcome.Matched(Command.OpenWebsite(it.groupValues[1].trim()))
        }
        if (torchOnRegex.matchEntire(text) != null) return ParseOutcome.Matched(Command.TorchOn)
        if (torchOffRegex.matchEntire(text) != null) return ParseOutcome.Matched(Command.TorchOff)
        if (volumeUpRegex.matchEntire(text) != null) return ParseOutcome.Matched(Command.VolumeUp)
        if (volumeDownRegex.matchEntire(text) != null) return ParseOutcome.Matched(Command.VolumeDown)
        if (text.contains("battery") || text.contains("bettery")) {
            return ParseOutcome.Matched(Command.BatteryStatus)
        }
        if (backRegex.matchEntire(text) != null) return ParseOutcome.Matched(Command.GoBack)
        if (homeRegex.matchEntire(text) != null) return ParseOutcome.Matched(Command.GoHome)
        if (text.contains("alarm") && (text.contains("lagao") || text.contains("laga") || text.contains("set"))) {
            return parseAlarm(text)
        }
        if (text.contains("yaad dil")) return parseReminder(text)

        openAppRegex.matchEntire(text)?.let {
            val app = canonicalApp(it.groupValues[1].trim())
            return ParseOutcome.Matched(Command.OpenApp(app))
        }
        return ParseOutcome.NoMatch
    }

    private fun parseAlarm(text: String): ParseOutcome {
        val match = timeRegex.find(text)
            ?: return ParseOutcome.NeedsClarification(
                "Kis time par alarm lagaun? Jaise: 'alarm lagao 7 baje' ya 'alarm lagao 7:30 am'."
            )
        var hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].ifEmpty { "0" }.toInt()
        val suffix = match.groupValues[3]

        if (suffix == "pm" && hour < 12) hour += 12
        if (suffix == "am" && hour == 12) hour = 0

        if (hour !in 0..23 || minute !in 0..59) {
            return ParseOutcome.NeedsClarification("Ye time samajh nahi aaya. Ek sahi time bolo, jaise '7 baje'.")
        }
        return ParseOutcome.Matched(Command.SetAlarm(hour, minute))
    }

    private fun parseReminder(text: String): ParseOutcome {
        val match = reminderAmountRegex.find(text)
            ?: return ParseOutcome.NeedsClarification("Kitne minute baad yaad dilaun?")

        val amount = match.groupValues[1].toInt()
        val unit = match.groupValues[2]
        val minutes = if (unit.startsWith("h") || unit.startsWith("ghant")) amount * 60 else amount

        val message = text.removeRange(match.range)
            .split(" ")
            .filter { it.isNotBlank() && it !in reminderFillerWords }
            .joinToString(" ")
            .ifBlank { "Reminder" }

        return ParseOutcome.Matched(Command.Reminder(minutes, message))
    }

    private fun canonicalApp(raw: String): String =
        appAliases.entries.firstOrNull { raw in it.value }?.key ?: raw
}
