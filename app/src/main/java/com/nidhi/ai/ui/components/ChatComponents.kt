package com.nidhi.ai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nidhi.ai.domain.AssistantStatus
import com.nidhi.ai.domain.ChatError
import com.nidhi.ai.domain.ChatMessage
import com.nidhi.ai.ui.theme.NidhiColors

@Composable
fun ChatBubble(message: ChatMessage, pending: Boolean, modifier: Modifier = Modifier) {
    val isUser = message.isUser
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        val shape = RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomStart = if (isUser) 18.dp else 4.dp,
            bottomEnd = if (isUser) 4.dp else 18.dp
        )
        val bubbleModifier = if (isUser) {
            Modifier
                .widthIn(max = 320.dp)
                .background(
                    Brush.linearGradient(listOf(NidhiColors.Purple, NidhiColors.Blue)),
                    shape
                )
        } else {
            Modifier
                .widthIn(max = 340.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, shape)
        }
        Box(bubbleModifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            val shown = if (message.text.isEmpty() && pending) "…" else message.text
            val color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
            SelectionContainer {
                Text(
                    text = shown,
                    color = color,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun StatusChip(status: AssistantStatus, modifier: Modifier = Modifier) {
    val dot = when (status) {
        AssistantStatus.IDLE -> NidhiColors.Blue
        AssistantStatus.LISTENING -> NidhiColors.Cyan
        AssistantStatus.THINKING -> NidhiColors.Purple
        AssistantStatus.SPEAKING -> Color(0xFF00E676)
    }
    Surface(
        modifier = modifier.semantics { contentDescription = "Status: ${status.label}" },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, dot.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).background(dot, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun QuickActionCard(
    emoji: String,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .width(132.dp)
            .semantics {
                contentDescription = if (enabled) title else "$title, $subtitle"
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            1.dp,
            (if (enabled) NidhiColors.Cyan else MaterialTheme.colorScheme.outline).copy(alpha = 0.5f)
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.6f)
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.7f)
            )
        }
    }
}

@Composable
fun ErrorBanner(
    error: ChatError,
    showSettingsAction: Boolean,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = error.message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text("Band karo") }
                if (showSettingsAction) {
                    OutlinedButton(onClick = onOpenSettings) { Text("Settings") }
                    Spacer(Modifier.width(8.dp))
                }
                Button(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}
