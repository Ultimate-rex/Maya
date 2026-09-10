package com.maya.assistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.maya.assistant.network.SecureConfigStore

@Composable
fun SettingsScreen(onSaved: () -> Unit) {
    val context = LocalContext.current
    var url by remember { mutableStateOf(SecureConfigStore.getBaseUrl(context) ?: "") }
    var token by remember { mutableStateOf(SecureConfigStore.getToken(context) ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Connect Maya to your server", style = MaterialTheme.typography.titleLarge)
        Text(
            "Enter the backend you deployed (see backend/README.md). " +
                "Your Gemini/Groq/OpenAI keys live only on that server's .env file - " +
                "never enter them here.",
            style = MaterialTheme.typography.bodySmall,
        )

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Server URL, e.g. https://your-server.com") },
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Access token (MAYA_ACCESS_TOKEN from your .env)") },
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = {
                SecureConfigStore.saveServerConfig(context, url.trim(), token.trim())
                onSaved()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save")
        }
    }
}
