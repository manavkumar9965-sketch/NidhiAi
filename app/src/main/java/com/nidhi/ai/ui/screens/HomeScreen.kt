package com.nidhi.ai.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nidhi.ai.data.ai.AiErrorMessages
import com.nidhi.ai.domain.AssistantStatus
import com.nidhi.ai.domain.ChatUiState
import com.nidhi.ai.ui.components.ChatBubble
import com.nidhi.ai.ui.components.ErrorBanner
import com.nidhi.ai.ui.components.NidhiLogo
import com.nidhi.ai.ui.components.NidhiOrb
import com.nidhi.ai.ui.components.QuickActionCard
import com.nidhi.ai.ui.components.StatusChip

private data class QuickAction(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val enabled: Boolean
)

@Composable
fun HomeScreen(
    state: ChatUiState,
    nickname: String,
    animated: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onMic: () -> Unit,
    handsFree: Boolean,
    onToggleHandsFree: () -> Unit,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
    onClearChat: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val busy = state.status == AssistantStatus.THINKING
    val empty = state.messages.isEmpty()

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NidhiLogo(animated = animated, modifier = Modifier.weight(1f))
            if (!empty) {
                TextButton(onClick = onClearChat) { Text("Naya chat") }
            }
            IconButton(onClick = onToggleHandsFree) {
                Text(
                    text = if (handsFree) "🔴" else "🎧",
                    modifier = Modifier.semantics {
                        contentDescription = if (handsFree) "Hands-free band karo" else "Hands-free chalu karo"
                    }
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings kholo")
            }
        }

        // Orb + status
        val orbSize by animateDpAsState(if (empty) 160.dp else 72.dp, label = "orbSize")
        Column(
            Modifier.fillMaxWidth().animateContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NidhiOrb(status = state.status, animated = animated, size = orbSize)
            Spacer(Modifier.height(6.dp))
            StatusChip(state.status)
        }
        Spacer(Modifier.height(8.dp))

        // Content
        if (empty) {
            EmptyState(
                nickname = nickname,
                onOpenSettings = onOpenSettings,
                modifier = Modifier.weight(1f)
            )
        } else {
            val listState = rememberLazyListState()
            val lastLength = state.messages.lastOrNull()?.text?.length ?: 0
            LaunchedEffect(state.messages.size, lastLength) {
                listState.scrollToItem(state.messages.lastIndex)
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.messages, key = { it.id }) { message ->
                    ChatBubble(
                        message = message,
                        pending = busy && message.id == state.messages.last().id && !message.isUser
                    )
                }
            }
        }

        state.error?.let { error ->
            ErrorBanner(
                error = error,
                showSettingsAction = AiErrorMessages.needsSettings(error.kind),
                onRetry = onRetry,
                onOpenSettings = onOpenSettings,
                onDismiss = onDismissError,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Input bar
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = state.input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Nidhi se kuch bhi pucho…") },
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(onSend = { onSend() })
            )
            Spacer(Modifier.width(8.dp))
            val voiceActive = handsFree ||
                state.status == AssistantStatus.LISTENING ||
                state.status == AssistantStatus.SPEAKING
            if (!busy) {
                FilledTonalIconButton(onClick = onMic) {
                    Text(
                        text = if (voiceActive) "⏹" else "🎤",
                        modifier = Modifier.semantics {
                            contentDescription = if (voiceActive) "Voice band karo" else "Bolkar pucho"
                        }
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            if (busy) {
                FilledIconButton(onClick = onStop) {
                    Icon(Icons.Filled.Close, contentDescription = "Jawab rok do")
                }
            } else {
                FilledIconButton(onClick = onSend, enabled = state.input.isNotBlank()) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Message bhejo")
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    nickname: String,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val actions = listOf(
        QuickAction("▶️", "YouTube", "App kholo", true),
        QuickAction("👥", "Facebook", "App kholo", true),
        QuickAction("📱", "Apps", "Jaldi aa raha hai", false),
        QuickAction("💻", "Coding", "Jaldi aa raha hai", false),
        QuickAction("⚙️", "Settings", "API key, model", true)
    )
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val name = nickname.trim()
        Text(
            text = if (name.isEmpty()) "Namaste! 😊 Main Nidhi hoon." else "Namaste $name! 😊 Main Nidhi hoon.",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Batao, aaj kya karna hai? Neeche likh ke pucho.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(actions, key = { it.title }) { a ->
                QuickActionCard(
                    emoji = a.emoji,
                    title = a.title,
                    subtitle = a.subtitle,
                    enabled = a.enabled,
                    onClick = { when (a.title) { "Settings" -> onOpenSettings(); "YouTube" -> openAppByPackage(context, "com.google.android.youtube"); "Facebook" -> openAppByPackage(context, "com.facebook.katana"); else -> {} } }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.height(1.dp))
    }
}

private fun openAppByPackage(context: android.content.Context, pkg: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(pkg)
    if (intent != null) {
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } else {
        android.widget.Toast.makeText(context, "App installed nahi hai", android.widget.Toast.LENGTH_SHORT).show()
    }
}
