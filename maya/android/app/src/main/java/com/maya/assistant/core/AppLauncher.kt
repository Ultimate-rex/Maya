package com.maya.assistant.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Launches apps and performs Android actions using only public, documented
 * Intent APIs - the same mechanism the phone's own launcher and Google
 * Assistant use. This does not read or control other apps' UI; it just
 * hands off to them, same as tapping their icon.
 */
class AppLauncher(private val context: Context) {

    /** Opens an installed app by matching its visible label, e.g. "Chrome", "Spotify". */
    fun openAppByName(name: String): Boolean {
        val pm = context.packageManager
        val launchables = pm.getInstalledApplications(0)
        val match = launchables.firstOrNull { appInfo ->
            pm.getApplicationLabel(appInfo).toString().equals(name, ignoreCase = true)
        } ?: return false

        val intent = pm.getLaunchIntentForPackage(match.packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }

    fun openUrl(url: String) {
        val normalized = if (url.startsWith("http")) url else "https://$url"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun webSearch(query: String) {
        val intent = Intent(Intent.ACTION_WEB_SEARCH)
        intent.putExtra("query", query)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            openUrl("https://www.google.com/search?q=" + Uri.encode(query))
        }
    }

    fun openYoutubeSearch(query: String) {
        openUrl("https://www.youtube.com/results?search_query=" + Uri.encode(query))
    }

    /** Opens a specific system settings screen instead of trying to flip toggles directly -
     * Android does not let third-party apps silently change Wi-Fi/Bluetooth/hotspot state
     * on modern versions, so Maya opens the right panel and lets the user tap it. */
    fun openSettingsPanel(panel: SettingsPanel) {
        val action = when (panel) {
            SettingsPanel.WIFI -> Settings.ACTION_WIFI_SETTINGS
            SettingsPanel.BLUETOOTH -> Settings.ACTION_BLUETOOTH_SETTINGS
            SettingsPanel.HOTSPOT -> Settings.ACTION_WIRELESS_SETTINGS
            SettingsPanel.LOCATION -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            SettingsPanel.SOUND -> Settings.ACTION_SOUND_SETTINGS
            SettingsPanel.DISPLAY -> Settings.ACTION_DISPLAY_SETTINGS
            SettingsPanel.MAIN -> Settings.ACTION_SETTINGS
        }
        val intent = Intent(action)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun setAlarm(hour: Int, minute: Int, label: String = "Maya alarm") {
        val intent = Intent(android.provider.AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(android.provider.AlarmClock.EXTRA_HOUR, hour)
            putExtra(android.provider.AlarmClock.EXTRA_MINUTES, minute)
            putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, label)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

enum class SettingsPanel { WIFI, BLUETOOTH, HOTSPOT, LOCATION, SOUND, DISPLAY, MAIN }
