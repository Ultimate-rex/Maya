package com.maya.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.maya.assistant.core.AppLauncher
import com.maya.assistant.core.BatteryReader
import com.maya.assistant.core.DeviceController
import com.maya.assistant.core.LocalJsonStore
import com.maya.assistant.core.MediaControlManager
import com.maya.assistant.core.MayaTimer
import com.maya.assistant.core.PasswordGenerator
import com.maya.assistant.core.PermissionManager
import com.maya.assistant.core.VoiceRecorder
import com.maya.assistant.network.ApiClient
import com.maya.assistant.network.ChatRequest
import com.maya.assistant.ui.MayaCore
import com.maya.assistant.ui.MayaState
import com.maya.assistant.ui.MayaTheme
import com.maya.assistant.ui.SettingsScreen
import com.maya.assistant.ui.ToolsScreen
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject

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
                var showSettings by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    if (!permissionsGranted) {
                        permissionManager.requestCorePermissions { results ->
                            permissionsGranted = results[android.Manifest.permission.RECORD_AUDIO] == true
                        }
                    }
                }

                if (showSettings) {
                    SettingsScreen(onSaved = { showSettings = false })
                } else {
                    MayaHomeScreen(
                        permissionsGranted = permissionsGranted,
                        onOpenSettings = { showSettings = true },
                    )
                }
            }
        }
    }
}

@Composable
fun MayaHomeScreen(permissionsGranted: Boolean, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appLauncher = remember { AppLauncher(context) }
    val localStore = remember { LocalJsonStore(context) }
    val voiceRecorder = remember { VoiceRecorder(context) }
    val deviceController = remember { DeviceController(context) }
    val mediaControl = remember { MediaControlManager(context) }
    val batteryReader = remember { BatteryReader(context) }
    val mayaTimer = remember { MayaTimer() }

    var mayaState by remember { mutableStateOf(MayaState.IDLE) }
    var lastReply by remember { mutableStateOf("Say something, or type below to try Maya.") }
    var inputText by remember { mutableStateOf("") }
    var generatedPassword by remember { mutableStateOf<String?>(null) }
    var showTools by remember { mutableStateOf(false) }

    fun sendToMaya(text: String) {
        if (text.isBlank()) return
        mayaState = MayaState.THINKING
        scope.launch {
            try {
                val api = ApiClient.create(context)
                val response = api.chat(ChatRequest(text = text))
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    lastReply = body.say
                    mayaState = MayaState.SPEAKING

                    // Save to on-device JSON log (phone storage, JSON-based, no DB)
                    val entry = JSONObject().apply {
                        put("user", text)
                        put("maya", body.say)
                        put("intent", body.intent)
                    }
                    localStore.appendToArray("conversations", entry)

                    // Perform the action the backend decided on, using only real Android intents
                    when (body.intent) {
                        "OPEN_APP" -> (body.parameters["app"] as? String)?.let { appLauncher.openAppByName(it) }
                        "WEB_SEARCH" -> (body.parameters["query"] as? String)?.let { appLauncher.webSearch(it) }
                        "MEDIA_PLAY_PAUSE" -> mediaControl.playPause()
                        "MEDIA_NEXT" -> mediaControl.next()
                        "MEDIA_PREVIOUS" -> mediaControl.previous()
                        "FLASHLIGHT" -> {
                            val on = (body.parameters["on"] as? Boolean) ?: true
                            deviceController.setFlashlight(on)
                        }
                        "VOLUME" -> {
                            val percent = (body.parameters["percent"] as? Double)?.toInt()
                            if (percent != null) deviceController.setMediaVolume(percent)
                        }
                        "BRIGHTNESS" -> {
                            val percent = (body.parameters["percent"] as? Double)?.toInt()
                            if (percent != null) deviceController.setBrightness(percent)
                        }
                        "ALARM_SET" -> {
                            val hour = (body.parameters["hour"] as? Double)?.toInt()
                            val minute = (body.parameters["minute"] as? Double)?.toInt()
                            if (hour != null && minute != null) appLauncher.setAlarm(hour, minute)
                        }
                        "TIMER_SET" -> {
                            val seconds = (body.parameters["seconds"] as? Double)?.toLong()
                            if (seconds != null) {
                                mayaTimer.start(seconds * 1000, onTick = {}, onFinish = {
                                    lastReply = "Time's up!"
                                })
                            }
                        }
                        "BATTERY_STATUS" -> {
                            val info = batteryReader.read()
                            lastReply = "Battery is at ${info.percent}%" +
                                if (info.isCharging) ", currently charging." else "."
                        }
                        "GENERATE_PASSWORD" -> {
                            val length = (body.parameters["length"] as? Double)?.toInt() ?: 16
                            val symbols = (body.parameters["symbols"] as? Boolean) ?: true
                            generatedPassword = PasswordGenerator.generate(length, symbols)
                        }
                    }
                } else {
                    lastReply = "Maya's server didn't respond correctly. Check your settings."
                    mayaState = MayaState.ERROR
                }
            } catch (e: Exception) {
                lastReply = "Couldn't reach Maya's server: ${e.message}"
                mayaState = MayaState.ERROR
            }
        }
    }

    fun startListening() {
        if (!permissionsGranted) return
        mayaState = MayaState.LISTENING
        voiceRecorder.start()
    }

    fun stopListeningAndSend() {
        val file = voiceRecorder.stop() ?: return
        mayaState = MayaState.THINKING
        scope.launch {
            try {
                val api = ApiClient.create(context)
                val body = file.asRequestBody("audio/m4a".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("audio", file.name, body)
                val transcribeResponse = api.transcribe(part)
                val transcript = transcribeResponse.body()?.text
                if (transcribeResponse.isSuccessful && !transcript.isNullOrBlank()) {
                    sendToMaya(transcript)
                } else {
                    lastReply = "Couldn't understand that - try again."
                    mayaState = MayaState.ERROR
                }
            } catch (e: Exception) {
                lastReply = "Voice transcription failed: ${e.message}"
                mayaState = MayaState.ERROR
            } finally {
                file.delete()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Maya") },
                actions = {
                    IconButton(onClick = { showTools = true }) {
                        Icon(Icons.Filled.Build, contentDescription = "Tools")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        if (showTools) {
            ToolsScreen(onClose = { showTools = false })
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            MayaCore(state = mayaState, modifier = Modifier.padding(top = 24.dp))

            Text(
                text = lastReply,
                modifier = Modifier.padding(vertical = 24.dp),
                style = MaterialTheme.typography.bodyLarge,
            )

            generatedPassword?.let { pwd ->
                Text(
                    text = "Generated password: $pwd",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (!permissionsGranted) {
                Text(
                    "Microphone permission is needed for voice input.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Type a command") },
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

            Text(
                "Hold the button below to talk to Maya",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )

            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(72.dp)
                    .pointerInput(permissionsGranted) {
                        detectTapGestures(
                            onPress = {
                                startListening()
                                tryAwaitRelease()
                                stopListeningAndSend()
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                FilledIconButton(onClick = {}, enabled = false) {
                    Icon(Icons.Filled.Mic, contentDescription = "Hold to talk")
                }
            }
        }
    }
}
