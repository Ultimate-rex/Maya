package com.maya.assistant.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.IBinder
import androidx.core.content.ContextCompat

/**
 * Runs as a foreground service so it keeps receiving these public system
 * broadcasts even while Maya's UI isn't in front. It only reacts to the
 * state changes below - it does not read screen content, notifications, or
 * any app's data.
 */
class TriggerService : Service() {

    private lateinit var ruleStore: TriggerRuleStore
    private lateinit var appLauncher: AppLauncher
    private lateinit var localTts: LocalTts
    private var lastBatteryPercent = -1

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> fire(TriggerEvent.CHARGING_STARTED)
                Intent.ACTION_POWER_DISCONNECTED -> fire(TriggerEvent.CHARGING_STOPPED)
                Intent.ACTION_BATTERY_CHANGED -> handleBattery(intent)
                Intent.ACTION_SCREEN_ON -> fire(TriggerEvent.SCREEN_ON)
                Intent.ACTION_SCREEN_OFF -> fire(TriggerEvent.SCREEN_OFF)
                ConnectivityManager.CONNECTIVITY_ACTION -> handleWifi()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        ruleStore = TriggerRuleStore(LocalJsonStore(this))
        appLauncher = AppLauncher(this)
        localTts = LocalTts(this)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        }
        // Android 13+ requires specifying RECEIVER_EXPORTED or RECEIVER_NOT_EXPORTED.
        // All these broadcasts are system-only, so RECEIVER_NOT_EXPORTED is correct
        // and this call is safe on older API levels too (ContextCompat handles it).
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        localTts.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handleBattery(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percent = if (level >= 0 && scale > 0) level * 100 / scale else return
        if (percent == lastBatteryPercent) return
        lastBatteryPercent = percent

        ruleStore.all().filter { it.enabled }.forEach { rule ->
            when (rule.event) {
                TriggerEvent.BATTERY_BELOW -> if (rule.threshold != null && percent <= rule.threshold) execute(rule)
                TriggerEvent.BATTERY_ABOVE -> if (rule.threshold != null && percent >= rule.threshold) execute(rule)
                else -> {}
            }
        }
    }

    private fun handleWifi() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }
        val connected = caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true
        fire(if (connected) TriggerEvent.WIFI_CONNECTED else TriggerEvent.WIFI_DISCONNECTED)
    }

    private fun fire(event: TriggerEvent) {
        ruleStore.all().filter { it.enabled && it.event == event }.forEach { execute(it) }
    }

    private fun execute(rule: TriggerRule) {
        when (rule.actionType) {
            TriggerActionType.SPEAK_LINE -> localTts.speak(rule.actionParam)
            TriggerActionType.OPEN_APP -> appLauncher.openAppByName(rule.actionParam)
        }
    }

    private fun buildNotification(): Notification {
        val channelId = "maya_triggers"
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(channelId, "Maya Automation", NotificationManager.IMPORTANCE_MIN)
        )
        return Notification.Builder(this, channelId)
            .setContentTitle("Maya is watching your automation rules")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 42
    }
}
