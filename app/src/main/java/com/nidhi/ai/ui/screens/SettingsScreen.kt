package com.nidhi.ai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.nidhi.ai.data.settings.AppSettings
import com.nidhi.ai.data.settings.ResponseStyle
import com.nidhi.ai.data.settings.ThemeMode
import com.nidhi.ai.ui.SettingsUiState
import com.nidhi.ai.ui.SettingsDraft
import com.nidhi.ai.ui.TestState
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onKeyInputChange: (String) -> Unit,
    onSaveKey: () -> Unit,
    onRemoveKey: () -> Unit,
    onDraftChange: ((SettingsDraft) -> SettingsDraft) -> Unit,
    onSaveDraft: () -> Unit,
    onTestConnection: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onAnimationsChange: (Boolean) -> Unit
) {
    val draft = state.draft

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wapas jao")
            }
            Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ---- Gemini ----
            Section("Gemini AI") {
                Text(
                    text = if (state.settings.hasApiKey) "API key: saved ✓ (••••••••, chhupi hui)"
                    else "API key: abhi set nahi hai",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = state.keyInput,
                    onValueChange = onKeyInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (state.settings.hasApiKey) "Nayi API key (replace)" else "Gemini API key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrect = false)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSaveKey, enabled = state.keyInput.isNotBlank()) { Text("Key save karo") }
                    if (state.settings.hasApiKey) {
                        OutlinedButton(onClick = onRemoveKey) { Text("Key hatao") }
                    }
                }
                Text(
                    "Key sirf is phone par Android Keystore se encrypt hokar rakhi jaati hai aur sirf Google ke Gemini server ko bheji jaati hai. " +
                        "Free key aistudio.google.com se mil sakti hai. Dhyan rahe: agar kabhi key ko APK ke andar daal kar baantoge, to koi bhi use nikal sakta hai. " +
                        "Public app ke liye apna secure backend proxy use karo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = draft.model,
                    onValueChange = { v -> onDraftChange { it.copy(model = v.take(64)) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Model ID") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(autoCorrect = false)
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppSettings.SUGGESTED_MODELS.forEach { id ->
                        FilterChip(
                            selected = draft.model == id,
                            onClick = { onDraftChange { it.copy(model = id) } },
                            label = { Text(id.removePrefix("gemini-"), maxLines = 1) }
                        )
                    }
                }
                Text(
                    "Model ID badalte rehte hain. Agar 'model nahi mila' aaye to ai.google.dev/gemini-api/docs/models par naya ID dekh kar yahan daalo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = onTestConnection, enabled = state.testState != TestState.Running) {
                        Text("Connection test karo")
                    }
                    Spacer(Modifier.width(12.dp))
                    when (val t = state.testState) {
                        TestState.Running -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        TestState.Success -> Text("Sab theek hai ✓", color = MaterialTheme.colorScheme.primary)
                        else -> Unit
                    }
                }
                (state.testState as? TestState.Failure)?.let {
                    Text(it.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // ---- Chat behaviour ----
            Section("Jawab ka style") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ResponseStyle.entries.forEach { s ->
                        FilterChip(
                            selected = draft.style == s,
                            onClick = { onDraftChange { it.copy(style = s) } },
                            label = { Text(s.label) }
                        )
                    }
                }
                Text("Creativity (temperature): ${"%.1f".format(draft.temperature)}", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = draft.temperature,
                    onValueChange = { v -> onDraftChange { it.copy(temperature = (v * 10).roundToInt() / 10f) } },
                    valueRange = 0f..2f
                )
                Text("Max jawab length: ${draft.maxTokens} tokens", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = draft.maxTokens.toFloat(),
                    onValueChange = { v -> onDraftChange { it.copy(maxTokens = ((v / 64).roundToInt() * 64).coerceIn(256, 8192)) } },
                    valueRange = 256f..8192f
                )
            }

            // ---- Personality ----
            Section("Nidhi ki personality") {
                OutlinedTextField(
                    value = draft.nickname,
                    onValueChange = { v -> onDraftChange { it.copy(nickname = v.take(30)) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tumhara nickname (optional)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = draft.customInstructions,
                    onValueChange = { v -> onDraftChange { it.copy(customInstructions = v.take(1000)) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Custom instructions (optional)") },
                    minLines = 3,
                    maxLines = 6,
                    supportingText = { Text("${draft.customInstructions.length}/1000") }
                )
            }

            Button(onClick = onSaveDraft, modifier = Modifier.fillMaxWidth()) {
                Text("Ye settings save karo")
            }
            state.message?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            }

            // ---- Appearance ----
            Section("Appearance") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { m ->
                        FilterChip(
                            selected = state.settings.theme == m,
                            onClick = { onThemeChange(m) },
                            label = { Text(m.label) }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Animations", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = state.settings.animationsEnabled,
                        onCheckedChange = onAnimationsChange
                    )
                }
            }

            // ---- Privacy ----
            Section("Privacy") {
                Text(
                    "Stage 1 me chat sirf app ke memory me rehti hai. App band karne ya 'Naya chat' dabane par chat hat jaati hai. " +
                        "Kuch bhi disk par save nahi hota (sirf settings aur encrypted key).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}
