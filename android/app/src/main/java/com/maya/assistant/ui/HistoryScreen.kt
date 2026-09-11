package com.maya.assistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.maya.assistant.core.LocalJsonStore
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class HistoryEntry(val user: String, val maya: String, val intent: String, val timestamp: Long)

/**
 * Reads Maya's conversation log straight from on-device JSON storage
 * (Android/data/com.maya.assistant/files/Maya/conversations.json) - the
 * same file the app writes to after every exchange. Nothing here talks to
 * a server; it's just displaying what's already on your phone.
 */
@Composable
fun HistoryScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val store = remember { LocalJsonStore(context) }
    var entries by remember { mutableStateOf(loadHistory(store)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conversation history") },
                navigationIcon = {
                    TextButton(onClick = onClose) { Text("Back") }
                },
                actions = {
                    TextButton(onClick = { entries = loadHistory(store) }) { Text("Refresh") }
                },
            )
        },
    ) { padding ->
        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No conversations yet.", style = MaterialTheme.typography.bodyMedium)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            items(entries.reversed()) { entry ->
                Card {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(formatTime(entry.timestamp), style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("You: ${entry.user}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Maya: ${entry.maya}", style = MaterialTheme.typography.bodyMedium)
                        if (entry.intent != "NONE") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Action: ${entry.intent}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

private fun loadHistory(store: LocalJsonStore): List<HistoryEntry> {
    val arr: JSONArray = try { store.readArray("conversations") } catch (e: Exception) { JSONArray() }
    return (0 until arr.length()).mapNotNull { i ->
        try {
            val obj = arr.getJSONObject(i)
            HistoryEntry(
                user = obj.optString("user"),
                maya = obj.optString("maya"),
                intent = obj.optString("intent", "NONE"),
                timestamp = obj.optLong("timestamp", 0L),
            )
        } catch (e: Exception) {
            null
        }
    }
}

private fun formatTime(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
