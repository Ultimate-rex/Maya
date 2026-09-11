package com.maya.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.maya.assistant.core.MayaCommandExecutor
import com.maya.assistant.core.PermissionManager
import com.maya.assistant.core.SoundEffects
import com.maya.assistant.core.SpeechToText
import com.maya.assistant.ui.HistoryScreen
import com.maya.assistant.ui.MayaCore
import com.maya.assistant.ui.MayaState
import com.maya.assistant.ui.MayaTheme
import com.maya.assistant.ui.SettingsScreen
import com.maya.assistant.ui.ToolsScreen
import kotlinx.coroutines.launch

private data class Turn(val speaker: String, val text: String)

class MainActivity : ComponentActivity() {

    private lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionManager = PermissionManager(this)

        setContent {
            MayaTheme {
                var permissionsGranted by remember {
                    mutableStateOf(permissionManager.hasPermission(android.Manifest.permission.RECORD_AUDIO))
                }
                var screen by remember { mutableStateOf("home") }

                LaunchedEffect(Unit) {
                    if (!permissionsGranted) {
                        permissionManager.requestCorePermissions { results ->
                            permissionsGranted = results[android.Manifest.permission.RECORD_AUDIO] == true
                        }
                    }
                }

                when (screen) {
                    "settings" -> SettingsScreen(onSaved = { screen = "home" })
                    "tools" -> ToolsScreen(onClose = { screen = "home" })
                    "history" -> HistoryScreen(onClose = { screen = "home" })
                    else -> MayaHomeScreen(
                        permissionsGranted = permissionsGranted,
                        onOpenSettings = { screen = "settings" },
                        onOpenTools = { screen = "tools" },
                        onOpenHistory = { screen = "history" },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MayaHomeScreen(
    permissionsGranted: Boolean,
    onOpenSettings: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val commandExecutor = remember { MayaCommandExecutor(context) }
    val speechToText = remember { SpeechToText(context) }
    val soundEffects = remember { SoundEffects(context) }

    var mayaState by remember { mutableStateOf(MayaState.IDLE) }
    var inputText by remember { mutableStateOf("") }
    var generatedPassword by remember { mutableStateOf<String?>(null) }
    val turns = remember { mutableStateListOf<Turn>() }
    val listState = rememberLazyListState()

    fun sendToMaya(text: String) {
        if (text.isBlank()) return
        turns.add(Turn("You", text))
        mayaState = MayaState.THINKING
        scope.launch {
            try {
                val outcome = commandExecutor.process(text)
                turns.add(Turn("Maya", outcome.displayText))
                generatedPassword = outcome.generatedPassword
                mayaState = MayaState.SPEAKING
                listState.animateScrollToItem((turns.size - 1).coerceAtLeast(0))
            } catch (e: Exception) {
                turns.add(Turn("Maya", "Couldn't reach Gemini: ${e.message}"))
                mayaState = MayaState.ERROR
            }
        }
    }

    fun startListening() {
        if (!permissionsGranted) return
        mayaState = MayaState.LISTENING
        soundEffects.playListenStart()
        scope.launch {
            try {
                val transcript = speechToText.listenOnce()
                soundEffects.playListenStop()
                if (transcript.isNotBlank()) {
                    sendToMaya(transcript)
                } else {
                    mayaState = MayaState.IDLE
                }
            } catch (e: Exception) {
                soundEffects.playError()
                turns.add(Turn("Maya", "Voice recognition failed: ${e.message}"))
                mayaState = MayaState.ERROR
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { soundEffects.release() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Maya") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Filled.History, contentDescription = "History")
                    }
                    IconButton(onClick = onOpenTools) {
                        Icon(Icons.Filled.Build, contentDescription = "Tools")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0A0812), Color(0xFF160D2A), Color(0xFF0A0812)),
                    )
                )
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                MayaCore(state = mayaState, modifier = Modifier.padding(vertical = 8.dp))

                AnimatedContent(targetState = mayaState, label = "state-label") { state ->
                    Text(
                        text = when (state) {
                            MayaState.IDLE -> "Ready"
                            MayaState.LISTENING -> "Listening\u2026"
                            MayaState.THINKING -> "Thinking\u2026"
                            MayaState.SPEAKING -> "Speaking"
                            MayaState.ERROR -> "Something went wrong"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFB58CFF),
                    )
                }

                generatedPassword?.let { pwd ->
                    Text(
                        text = "Password: $pwd",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                if (!permissionsGranted) {
                    Text(
                        "Microphone permission is needed for voice input.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC94E4E),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(turns) { turn ->
                        val isUser = turn.speaker == "You"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUser) Color(0xFF3A2566) else Color(0xFF1D1630),
                                ),
                                modifier = Modifier.fillMaxWidth(0.82f),
                            ) {
                                Text(
                                    text = turn.text,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Type instead") },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = {
                        if (inputText.isNotBlank()) {
                            sendToMaya(inputText)
                            inputText = ""
                        }
                    }) {
                        Icon(Icons.Filled.Mic, contentDescription = "Send typed text")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val infiniteTransition = rememberInfiniteTransition(label = "mic-pulse")
                val pulse by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.18f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(700, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "pulse-scale",
                )
                val ringScale = if (mayaState == MayaState.LISTENING) pulse else 1f

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(bottom = 24.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size((96 * ringScale).dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFF9B5CF0).copy(alpha = 0.35f), Color.Transparent),
                                )
                            ),
                    )
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFFB58CFF), Color(0xFF6A3EC9)),
                                )
                            )
                            .clickable(enabled = permissionsGranted) { startListening() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Mic,
                            contentDescription = "Tap to speak",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }
                Text(
                    "Tap to speak",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFB58CFF),
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
        }
    }
}
