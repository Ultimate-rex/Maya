package com.maya.assistant.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Real, working wake-word-style continuous listening: repeatedly runs a
 * short speech-recognition session in the background and checks whether
 * the wake phrase ("hey maya" / "wake up maya") appears in what was heard.
 *
 * This is NOT a true offline neural wake-word engine (like Porcupine) -
 * those need a licensed model file this project doesn't bundle. Instead it
 * loops Android's on-device SpeechRecognizer, which is genuinely functional
 * and needs no extra key, at the cost of slightly higher battery use than a
 * dedicated wake-word chip/model would use.
 *
 * If the wake phrase is found, whatever follows it in the same sentence is
 * treated as the command ("hey maya what's my battery" -> "what's my
 * battery"). If nothing follows, Maya says "Yes?" and listens once more for
 * the actual command.
 */
class VoiceListenerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob())
    private var listenJob: Job? = null

    private lateinit var speechToText: SpeechToText
    private lateinit var commandExecutor: MayaCommandExecutor
    private lateinit var soundEffects: SoundEffects
    private val mainHandler = Handler(Looper.getMainLooper())

    private val wakePhrases = listOf("hey maya", "wake up maya", "okay maya", "ok maya")

    override fun onCreate() {
        super.onCreate()
        speechToText = SpeechToText(this)
        commandExecutor = MayaCommandExecutor(this)
        soundEffects = SoundEffects(this)
        startForeground(NOTIFICATION_ID, buildNotification("Listening for \"Hey Maya\"\u2026"))
        startListenLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        listenJob?.cancel()
        commandExecutor.shutdown()
        soundEffects.release()
    }

    private fun startListenLoop() {
        listenJob?.cancel()
        listenJob = serviceScope.launch {
            while (true) {
                try {
                    val heard = speechToText.listenOnce().lowercase().trim()
                    if (heard.isEmpty()) continue

                    val matchedPhrase = wakePhrases.firstOrNull { heard.contains(it) }
                    if (matchedPhrase != null) {
                        soundEffects.playListenStart()
                        val remainder = heard.substringAfter(matchedPhrase).trim()
                        updateNotification("Heard the wake word\u2026")

                        val command = if (remainder.isNotBlank()) {
                            remainder
                        } else {
                            commandExecutor.speakOnly("Yes?")
                            speechToText.listenOnce().trim()
                        }

                        if (command.isNotBlank()) {
                            updateNotification("Working on: \"$command\"")
                            commandExecutor.process(command)
                        }
                        updateNotification("Listening for \"Hey Maya\"\u2026")
                    }
                } catch (e: Exception) {
                    // Recognition errors (timeout, no match, etc.) are expected while idle -
                    // just loop and try again rather than crashing the service.
                }
            }
        }
    }

    private fun updateNotification(text: String) {
        mainHandler.post {
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, buildNotification(text))
        }
    }

    private fun buildNotification(text: String): Notification {
        val channelId = "maya_voice_listener"
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(channelId, "Maya Voice Listening", NotificationManager.IMPORTANCE_MIN)
        )
        return Notification.Builder(this, channelId)
            .setContentTitle("Maya")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 43
    }
}
