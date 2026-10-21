package com.nidhi.ai.voice

import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Tiny, safe voice-command router: "YouTube kholo", "Facebook kholo" ...
 * Only opens apps through their normal launcher intent. Anything else returns null
 * so the text goes to Gemini as a normal chat message.
 */
object VoiceCommands {

    private val openWords = listOf("kholo", "khol do", "kholna", "khol", "open", "chalu", "start", "launch")

    private data class Target(val keywords: List<String>, val label: String, val pkg: String?)

    private val targets = listOf(
        Target(listOf("youtube", "यूट्यूब"), "YouTube", "com.google.android.youtube"),
        Target(listOf("facebook", "फेसबुक"), "Facebook", "com.facebook.katana"),
        Target(listOf("whatsapp", "व्हाट्सऐप", "वॉट्सऐप", "व्हाट्सएप"), "WhatsApp", "com.whatsapp"),
        Target(listOf("chrome", "क्रोम"), "Chrome", "com.android.chrome"),
        Target(listOf("camera", "कैमरा"), "Camera", null),
        Target(listOf("settings", "सेटिंग"), "Phone ki Settings", null)
    )

    private val openWordsHindi = listOf("खोलो", "खोल", "चालू", "ओपन")

    /** Returns Nidhi's reply if this was an app command, otherwise null. */
    fun handle(context: Context, spoken: String): String? {
        val t = spoken.lowercase().trim()
        if (t.split(Regex("\\s+")).size > 6) return null // long sentences go to Gemini
        if (openWords.none { it in t } && openWordsHindi.none { it in t }) return null
        val target = targets.firstOrNull { tg -> tg.keywords.any { it in t } } ?: return null

        val intent: Intent? = when (target.label) {
            "Camera" -> Intent("android.media.action.STILL_IMAGE_CAMERA")
            "Phone ki Settings" -> Intent(Settings.ACTION_SETTINGS)
            else -> context.packageManager.getLaunchIntentForPackage(target.pkg.orEmpty())
        }
        if (intent == null) return "${target.label} is phone me install nahi mila. 😕"
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            "Theek hai, ${target.label} khol rahi hoon! 😊"
        } catch (e: Exception) {
            "${target.label} nahi khul paya. Ek baar khud khol ke dekho."
        }
    }
}
