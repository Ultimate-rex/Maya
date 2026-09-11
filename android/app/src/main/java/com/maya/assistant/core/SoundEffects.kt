package com.maya.assistant.core

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Short audio + haptic cues for mic state changes, generated on-device with
 * Android's ToneGenerator (no bundled audio files needed) plus a brief
 * vibration pulse - the same building blocks system keyboards and
 * assistants use for mic feedback.
 */
class SoundEffects(private val context: Context) {

    private val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun playListenStart() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        vibrate(30)
    }

    fun playListenStop() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 100)
        vibrate(20)
    }

    fun playError() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_NACK, 200)
        vibrate(longArrayOf(0, 40, 60, 40))
    }

    private fun vibrate(millis: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(millis)
        }
    }

    private fun vibrate(pattern: LongArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    fun release() {
        toneGenerator.release()
    }
}
