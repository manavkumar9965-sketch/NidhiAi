package com.nidhi.ai.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale

/**
 * Speaks replies with Android's built-in TextToSpeech.
 * Uses an installed Hindi voice (preferring one whose name says "female") if the phone has one.
 * No custom voice is bundled: quality depends on the TTS engine installed on the phone.
 */
class SpeechOutput(context: Context) {

    /** Called with true when speech starts and false when it ends/stops. May run on a background thread. */
    var onSpeakingChanged: ((Boolean) -> Unit)? = null

    /** True when a Hindi voice is installed. Read after the first speak() call. */
    var hindiAvailable: Boolean = true
        private set

    private var ready = false
    private var pending: String? = null
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                setup()
            } else {
                tts = null
            }
        }
    }

    private fun setup() {
        val engine = tts ?: return
        val result = engine.setLanguage(Locale("hi", "IN"))
        hindiAvailable = result != TextToSpeech.LANG_MISSING_DATA &&
            result != TextToSpeech.LANG_NOT_SUPPORTED
        if (!hindiAvailable) engine.setLanguage(Locale.getDefault())
        if (hindiAvailable) pickVoice(engine)
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { onSpeakingChanged?.invoke(true) }
            override fun onDone(utteranceId: String?) { onSpeakingChanged?.invoke(false) }
            override fun onStop(utteranceId: String?, interrupted: Boolean) { onSpeakingChanged?.invoke(false) }

            @Deprecated("Required by the abstract class")
            override fun onError(utteranceId: String?) { onSpeakingChanged?.invoke(false) }
        })
        ready = true
        pending?.let { pending = null; speak(it) }
    }

    private fun pickVoice(engine: TextToSpeech) {
        val hindi: List<Voice> = engine.voices
            ?.filter { it.locale.language == "hi" }
            .orEmpty()
        val chosen = hindi.firstOrNull { it.name.contains("female", ignoreCase = true) }
            ?: hindi.firstOrNull { !it.isNetworkConnectionRequired }
            ?: hindi.firstOrNull()
        if (chosen != null) engine.voice = chosen
    }

    /** Returns true if speech was started or queued, false if nothing will be spoken. */
    fun speak(rawText: String): Boolean {
        val text = clean(rawText)
        if (text.isEmpty()) return false
        val engine = tts ?: return false
        if (!ready) { pending = text; return true }
        return engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nidhi-reply") == TextToSpeech.SUCCESS
    }

    fun stop() {
        pending = null
        tts?.stop()
        onSpeakingChanged?.invoke(false)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }

    private fun clean(text: String): String =
        text.replace(Regex("[\\p{So}*_#`>~\\uFE0F\\u200D]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(3500)
}
