package com.maya.assistant.core

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.provider.Settings

class MediaControlManager(private val context: Context) {

    fun hasNotificationAccess(): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return enabled?.contains(context.packageName) == true
    }

    fun openNotificationAccessSettings() {
        val intent = android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun activeController(): MediaController? {
        if (!hasNotificationAccess()) return null
        val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val component = ComponentName(context, MediaListenerService::class.java)
        return try {
            manager.getActiveSessions(component).firstOrNull()
        } catch (e: SecurityException) {
            null
        }
    }

    fun playPause() {
        val controller = activeController() ?: return
        val playing = controller.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING
        if (playing) controller.transportControls.pause() else controller.transportControls.play()
    }

    fun next() = activeController()?.transportControls?.skipToNext()
    fun previous() = activeController()?.transportControls?.skipToPrevious()
    fun stop() = activeController()?.transportControls?.stop()
}
