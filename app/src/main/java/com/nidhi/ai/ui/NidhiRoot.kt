package com.nidhi.ai.ui

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nidhi.ai.AppContainer
import com.nidhi.ai.ui.screens.HomeScreen
import com.nidhi.ai.ui.screens.SettingsScreen
import com.nidhi.ai.ui.theme.NidhiTheme
import com.nidhi.ai.ui.theme.isDark
import com.nidhi.ai.domain.AssistantStatus
import com.nidhi.ai.voice.SpeechInput
import com.nidhi.ai.voice.SpeechOutput
import com.nidhi.ai.voice.VoiceCommands

private enum class Screen { HOME, SETTINGS }

private const val MAX_SILENT_RETRIES = 3

@Composable
fun NidhiRoot(container: AppContainer) {
    val settingsVm: SettingsViewModel = viewModel(
        factory = viewModelFactory { initializer { SettingsViewModel(container.settings, container.ai) } }
    )
    val chatVm: ChatViewModel = viewModel(
        factory = viewModelFactory { initializer { ChatViewModel(container.settings, container.ai) } }
    )
    val settingsState by settingsVm.state.collectAsStateWithLifecycle()
    val chatState by chatVm.state.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }

    // ---- Voice (Stage 2) + Hands-free mode ----
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val speechIn = remember { SpeechInput(context) }
    val speechOut = remember { SpeechOutput(context) }
    var hindiWarned by rememberSaveable { mutableStateOf(false) }
    var handsFree by remember { mutableStateOf(false) }
    var pendingHandsFree by remember { mutableStateOf(false) }
    var silentRetries by remember { mutableStateOf(0) }
    var resumeJob by remember { mutableStateOf<Job?>(null) }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

    fun hasMicPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    /** Turns hands-free off and stops everything voice-related. */
    fun exitHandsFree(message: String? = null) {
        handsFree = false
        resumeJob?.cancel()
        speechIn.stop()
        speechOut.stop()
        chatVm.onInputChange("")
        chatVm.setVoiceStatus(AssistantStatus.IDLE)
        if (message != null) toast(message)
    }

    fun startListening() {
        if (!speechIn.isAvailable()) {
            exitHandsFree("Is phone me speech recognition nahi mila. Google app install/update karo.")
            return
        }
        speechOut.stop()
        chatVm.setVoiceStatus(AssistantStatus.LISTENING)
        speechIn.start(
            onPartial = { chatVm.onInputChange(it) },
            onResult = { spoken ->
                silentRetries = 0
                val reply = VoiceCommands.handle(context, spoken)
                if (reply != null) chatVm.addLocalExchange(spoken, reply) else chatVm.sendVoice(spoken)
            },
            onFailure = { msg, retryable ->
                chatVm.onInputChange("")
                if (!handsFree) {
                    toast(msg)
                } else if (retryable) {
                    silentRetries++
                    if (silentRetries >= MAX_SILENT_RETRIES) {
                        exitHandsFree("Kaafi der se kuch suna nahi, isliye hands-free band kar diya.")
                    } else {
                        resumeJob?.cancel()
                        resumeJob = scope.launch {
                            delay(400)
                            if (handsFree) startListening()
                        }
                    }
                } else {
                    exitHandsFree(msg)
                }
            },
            onFinished = {
                if (chatVm.state.value.status == AssistantStatus.LISTENING) {
                    chatVm.setVoiceStatus(AssistantStatus.IDLE)
                }
            }
        )
    }

    // After Nidhi finishes speaking, hands-free mode listens again.
    DisposableEffect(Unit) {
        speechOut.onSpeakingChanged = { speaking ->
            chatVm.setVoiceStatus(if (speaking) AssistantStatus.SPEAKING else AssistantStatus.IDLE)
            if (!speaking && handsFree) {
                resumeJob?.cancel()
                resumeJob = scope.launch {
                    delay(700) // let the speaker tail fade so Nidhi doesn't hear herself
                    if (handsFree && !speechIn.isListening &&
                        chatVm.state.value.status == AssistantStatus.IDLE
                    ) startListening()
                }
            }
        }
        onDispose {
            resumeJob?.cancel()
            speechIn.destroy()
            speechOut.shutdown()
        }
    }

    LaunchedEffect(chatVm) {
        chatVm.speakEvents.collect { text ->
            val started = speechOut.speak(text)
            if (!speechOut.hindiAvailable && !hindiWarned) {
                hindiWarned = true
                toast("Hindi voice install nahi hai. Phone Settings > Text-to-speech me Hindi voice download karo.")
            }
            if (!started && handsFree) {
                resumeJob?.cancel()
                resumeJob = scope.launch {
                    delay(500)
                    if (handsFree && !speechIn.isListening &&
                        chatVm.state.value.status == AssistantStatus.IDLE
                    ) startListening()
                }
            }
        }
    }

    // If Gemini fails during hands-free, stop instead of looping.
    LaunchedEffect(chatState.error) {
        if (chatState.error != null && handsFree) exitHandsFree("Jawab nahi aaya, isliye hands-free band kar diya.")
    }

    // Keep the screen on while hands-free is active.
    DisposableEffect(handsFree) {
        view.keepScreenOn = handsFree
        onDispose { view.keepScreenOn = false }
    }

    // Hands-free works only while the app is on screen.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && handsFree) exitHandsFree()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (pendingHandsFree) { silentRetries = 0; handsFree = true }
            pendingHandsFree = false
            startListening()
        } else {
            pendingHandsFree = false
            toast("Mic permission ke bina main sun nahi sakti. Tum likh ke bhi pooch sakte ho.")
        }
    }

    fun onMicClick() {
        when {
            handsFree -> exitHandsFree()
            chatState.status == AssistantStatus.SPEAKING -> speechOut.stop()
            speechIn.isListening -> {
                speechIn.stop()
                chatVm.onInputChange("")
                chatVm.setVoiceStatus(AssistantStatus.IDLE)
            }
            hasMicPermission() -> startListening()
            else -> micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun onToggleHandsFree() {
        when {
            handsFree -> exitHandsFree()
            hasMicPermission() -> { silentRetries = 0; handsFree = true; startListening() }
            else -> { pendingHandsFree = true; micPermission.launch(Manifest.permission.RECORD_AUDIO) }
        }
    }

    val themeMode = settingsState.settings.theme
    NidhiTheme(themeMode) {
        // Keep status/navigation bar icon colours readable for the chosen theme.
        val dark = themeMode.isDark()
        val activity = LocalContext.current as ComponentActivity
        SideEffect {
            val transparent = android.graphics.Color.TRANSPARENT
            activity.enableEdgeToEdge(
                statusBarStyle = if (dark) SystemBarStyle.dark(transparent)
                else SystemBarStyle.light(transparent, transparent),
                navigationBarStyle = if (dark) SystemBarStyle.dark(transparent)
                else SystemBarStyle.light(transparent, transparent)
            )
        }

        BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }

        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
                when (screen) {
                    Screen.HOME -> HomeScreen(
                        state = chatState,
                        nickname = settingsState.settings.nickname,
                        animated = settingsState.settings.animationsEnabled,
                        onInputChange = chatVm::onInputChange,
                        onSend = chatVm::send,
                        onMic = ::onMicClick,
                        handsFree = handsFree,
                        onToggleHandsFree = ::onToggleHandsFree,
                        onStop = { if (handsFree) exitHandsFree(); chatVm.stop() },
                        onRetry = chatVm::retry,
                        onDismissError = chatVm::dismissError,
                        onClearChat = chatVm::clearChat,
                        onOpenSettings = { screen = Screen.SETTINGS }
                    )

                    Screen.SETTINGS -> SettingsScreen(
                        state = settingsState,
                        onBack = { screen = Screen.HOME },
                        onKeyInputChange = settingsVm::onKeyInputChange,
                        onSaveKey = settingsVm::saveKey,
                        onRemoveKey = settingsVm::removeKey,
                        onDraftChange = settingsVm::onDraftChange,
                        onSaveDraft = settingsVm::saveDraft,
                        onTestConnection = settingsVm::testConnection,
                        onThemeChange = settingsVm::setTheme,
                        onAnimationsChange = settingsVm::setAnimations
                    )
                }
            }
        }
    }
}
