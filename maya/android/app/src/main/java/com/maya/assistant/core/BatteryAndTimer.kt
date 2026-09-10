package com.maya.assistant.core

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.CountDownTimer
import android.os.PowerManager

data class BatteryInfo(val percent: Int, val isCharging: Boolean, val isPowerSaveMode: Boolean)

/** Reads live battery status via the standard ACTION_BATTERY_CHANGED sticky broadcast. */
class BatteryReader(private val context: Context) {
    fun read(): BatteryInfo {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else -1

        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return BatteryInfo(percent, isCharging, pm.isPowerSaveMode)
    }
}

/**
 * A simple countdown timer. For durations that must survive the app being
 * killed, pair this with AlarmManager.setExactAndAllowWhileIdle instead -
 * this in-process version is enough for "10 minute timer" while Maya is open.
 */
class MayaTimer {
    private var timer: CountDownTimer? = null

    fun start(durationMillis: Long, onTick: (secondsLeft: Long) -> Unit, onFinish: () -> Unit) {
        timer?.cancel()
        timer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) = onTick(millisUntilFinished / 1000)
            override fun onFinish() = onFinish()
        }.start()
    }

    fun cancel() {
        timer?.cancel()
        timer = null
    }
}
