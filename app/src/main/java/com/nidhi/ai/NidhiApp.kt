package com.nidhi.ai

import android.app.Application
import android.content.Context
import com.nidhi.ai.data.ai.AiProvider
import com.nidhi.ai.data.ai.GeminiService
import com.nidhi.ai.data.settings.SecretCipher
import com.nidhi.ai.data.settings.SettingsRepository

/** Tiny manual dependency container (no DI framework needed for this app size). */
class AppContainer(context: Context) {
    val settings: SettingsRepository = SettingsRepository(context.applicationContext, SecretCipher())

    /** Swap this line to change the AI provider. */
    val ai: AiProvider = GeminiService()
}

class NidhiApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
