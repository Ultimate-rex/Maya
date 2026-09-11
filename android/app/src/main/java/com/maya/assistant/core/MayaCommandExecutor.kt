package com.maya.assistant.core

import android.content.Context
import org.json.JSONObject

data class ExecutionOutcome(val chatResult: ChatResult, val displayText: String, val generatedPassword: String? = null)

/**
 * Shared "brain" used by both the foreground UI (MainActivity) and the
 * background wake-word listener (VoiceListenerService), so the same
 * Gemini call -> decide action -> perform it -> speak it -> log it pipeline
 * runs identically whether you typed, tapped the mic, or said "Hey Maya".
 */
class MayaCommandExecutor(private val context: Context) {

    private val geminiClient = GeminiClient(context)
    private val appLauncher = AppLauncher(context)
    private val deviceController = DeviceController(context)
    private val mediaControl = MediaControlManager(context)
    private val batteryReader = BatteryReader(context)
    private val mayaTimer = MayaTimer()
    private val localTts = LocalTts(context)
    private val localStore = LocalJsonStore(context)

    private val history = mutableListOf<Pair<String, String>>()

    suspend fun process(userText: String, speakReply: Boolean = true): ExecutionOutcome {
        val result = geminiClient.chat(userText, history.toList())
        history.add("user" to userText)
        history.add("assistant" to result.say)
        // Keep only the last 20 turns so the request payload stays small.
        while (history.size > 20) history.removeAt(0)

        val entry = JSONObject().apply {
            put("user", userText)
            put("maya", result.say)
            put("intent", result.intent)
            put("timestamp", System.currentTimeMillis())
        }
        localStore.appendToArray("conversations", entry)

        var displayText = result.say
        var generatedPassword: String? = null

        when (result.intent) {
            "OPEN_APP" -> (result.parameters["app"] as? String)?.let { appLauncher.openAppByName(it) }
            "WEB_SEARCH" -> (result.parameters["query"] as? String)?.let { appLauncher.webSearch(it) }
            "MEDIA_PLAY_PAUSE" -> mediaControl.playPause()
            "MEDIA_NEXT" -> mediaControl.next()
            "MEDIA_PREVIOUS" -> mediaControl.previous()
            "FLASHLIGHT" -> {
                val on = (result.parameters["on"] as? Boolean) ?: true
                deviceController.setFlashlight(on)
            }
            "VOLUME" -> (result.parameters["percent"] as? Double)?.toInt()?.let { deviceController.setMediaVolume(it) }
            "BRIGHTNESS" -> (result.parameters["percent"] as? Double)?.toInt()?.let { deviceController.setBrightness(it) }
            "ALARM_SET" -> {
                val hour = (result.parameters["hour"] as? Double)?.toInt()
                val minute = (result.parameters["minute"] as? Double)?.toInt()
                if (hour != null && minute != null) appLauncher.setAlarm(hour, minute)
            }
            "TIMER_SET" -> (result.parameters["seconds"] as? Double)?.toLong()?.let { seconds ->
                mayaTimer.start(seconds * 1000, onTick = {}, onFinish = { localTts.speak("Time's up!") })
            }
            "BATTERY_STATUS" -> {
                val info = batteryReader.read()
                displayText = "Battery is at ${info.percent}%" + if (info.isCharging) ", currently charging." else "."
            }
            "GENERATE_PASSWORD" -> {
                val length = (result.parameters["length"] as? Double)?.toInt() ?: 16
                val symbols = (result.parameters["symbols"] as? Boolean) ?: true
                generatedPassword = PasswordGenerator.generate(length, symbols)
                displayText = "Here's your password: $generatedPassword"
            }
        }

        if (speakReply) localTts.speak(displayText)

        return ExecutionOutcome(result, displayText, generatedPassword)
    }

    fun speakOnly(text: String) {
        localTts.speak(text)
    }

    fun shutdown() {
        localTts.shutdown()
    }
}
