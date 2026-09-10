package com.maya.assistant.core

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.provider.Settings
import android.net.Uri

/**
 * Direct device control, limited to what Android actually permits a normal
 * app to change without special/system privileges. Anything else (Wi-Fi,
 * Bluetooth, hotspot toggling) is intentionally handled via
 * AppLauncher.openSettingsPanel instead, because modern Android blocks
 * silent toggling of those from third-party apps for the user's own security.
 */
class DeviceController(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun setFlashlight(on: Boolean) {
        val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
        cameraManager.setTorchMode(cameraId, on)
    }

    fun setMediaVolume(percent: Int) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = (max * percent.coerceIn(0, 100) / 100.0).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
    }

    fun adjustMediaVolume(raise: Boolean) {
        val direction = if (raise) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
    }

    /** Screen brightness requires WRITE_SETTINGS, which Android only grants through
     * an explicit user-approved settings screen - Maya requests that once, then can adjust. */
    fun canWriteSystemSettings(): Boolean = Settings.System.canWrite(context)

    fun requestWriteSettingsPermission() {
        val intent = android.content.Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        intent.data = Uri.parse("package:${context.packageName}")
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun setBrightness(percent: Int) {
        if (!canWriteSystemSettings()) {
            requestWriteSettingsPermission()
            return
        }
        val value = (255 * percent.coerceIn(0, 100) / 100.0).toInt()
        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
    }
}
