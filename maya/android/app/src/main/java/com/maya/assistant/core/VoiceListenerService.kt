package com.maya.assistant.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Foreground service placeholder for future wake-word listening ("Wake up
 * Maya"). Wiring a real wake-word engine (e.g. openWakeWord/Porcupine) is
 * listed as a next step in README.md - this class exists so the manifest
 * entry and permission model are already correct when you add it.
 */
class VoiceListenerService : Service() {

    override fun onCreate() {
        super.onCreate()
        val channelId = "maya_voice_listener"
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(channelId, "Maya Voice Listening", NotificationManager.IMPORTANCE_MIN)
        )
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Maya is listening for the wake word")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 43
    }
}
