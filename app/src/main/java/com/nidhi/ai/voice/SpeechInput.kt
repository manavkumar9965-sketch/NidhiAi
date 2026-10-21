package com.nidhi.ai.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Push-to-talk speech input using Android's SpeechRecognizer.
 * Must be created and used on the main thread. Needs RECORD_AUDIO permission
 * (the caller asks for it) and a recognition service (Google app) on the phone.
 */
class SpeechInput(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null
    private var cancelledByUser = false

    var isListening: Boolean = false
        private set

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun start(
        languageTag: String = "hi-IN",
        onPartial: (String) -> Unit,
        onResult: (String) -> Unit,
        onFailure: (String, Boolean) -> Unit,
        onFinished: () -> Unit
    ) {
        stop()
        cancelledByUser = false
        val sr = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = sr
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onPartialResults(partialResults: Bundle?) {
                partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?.let(onPartial)
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                release()
                if (text.isNotEmpty()) onResult(text) else onFailure(messageFor(SpeechRecognizer.ERROR_NO_MATCH), true)
                onFinished()
            }

            override fun onError(error: Int) {
                isListening = false
                val silent = cancelledByUser
                release()
                if (!silent) {
                    val retryable = error == SpeechRecognizer.ERROR_NO_MATCH ||
                        error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                    onFailure(messageFor(error), retryable)
                }
                onFinished()
            }
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        isListening = true
        sr.startListening(intent)
    }

    /** Stops listening and discards any result (cancel button / leaving the screen). */
    fun stop() {
        if (recognizer != null) {
            cancelledByUser = true
            recognizer?.cancel()
            release()
        }
        isListening = false
    }

    fun destroy() = stop()

    private fun release() {
        recognizer?.destroy()
        recognizer = null
    }

    private fun messageFor(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
            "Kuch sunai nahi diya. Mic dabake phir bolo."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "Internet slow hai, voice samajh nahi aayi. Phir try karo."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
            "Mic ki permission chahiye. Phone Settings me Nidhi AI ko Microphone allow karo."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice service busy hai. Thodi der baad try karo."
        SpeechRecognizer.ERROR_AUDIO -> "Mic me problem aayi. Phir try karo."
        else -> "Voice recognition nahi chali. Phir try karo."
    }
}
