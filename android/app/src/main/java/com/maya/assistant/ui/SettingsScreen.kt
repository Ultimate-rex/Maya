package com.maya.assistant.ui

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.maya.assistant.core.VoiceListenerService
import com.maya.assistant.network.SecureConfigStore

@Composable
fun SettingsScreen(onSaved: () -> Unit) {
    val context = LocalContext.current
    var apiKey by remember { mutableStateOf(SecureConfigStore.getGeminiApiKey(context) ?: "") }
    var showKey by remember { mutableStateOf(false) }
    var wakeWordEnabled by remember { mutableStateOf(SecureConfigStore.isWakeWordEnabled(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Connect your Gemini API key", style = MaterialTheme.typography.titleLarge)
        Text(
            "Maya calls Google's Gemini API directly from this phone using your key. " +
                "It's stored encrypted, on-device only (Android Keystore) - it is never sent " +
                "anywhere except in your own requests to Google.",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            "Get a free key at aistudio.google.com/apikey. Keep this app and its key personal.",
            style = MaterialTheme.typography.bodySmall,
        )

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("Gemini API key") },
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = showKey, onCheckedChange = { showKey = it })
            Text("Show key")
        }

        Button(
            onClick = {
                SecureConfigStore.saveGeminiApiKey(context, apiKey.trim())
                onSaved()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text("Always listen for \"Hey Maya\"", style = MaterialTheme.typography.titleMedium)
        Text(
            "Runs a background listening loop so you can talk to Maya without opening the " +
                "app or tapping anything first. Uses more battery than opening the app to talk. " +
                "This is a real, working continuous-listening loop using Android's built-in " +
                "speech recognizer - not a dedicated low-power wake-word chip, so it won't be " +
                "quite as battery-efficient as one.",
            style = MaterialTheme.typography.bodySmall,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Enabled")
            Switch(
                checked = wakeWordEnabled,
                onCheckedChange = { enabled ->
                    wakeWordEnabled = enabled
                    SecureConfigStore.setWakeWordEnabled(context, enabled)
                    val serviceIntent = Intent(context, VoiceListenerService::class.java)
                    if (enabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    } else {
                        context.stopService(serviceIntent)
                    }
                },
            )
        }
    }
}
