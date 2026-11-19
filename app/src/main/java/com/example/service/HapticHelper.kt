package com.example.service

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object HapticHelper {
    fun tap(context: Context) {
        vibratePattern(context, longArrayOf(0, 15), intArrayOf(0, 90))
    }

    fun error(context: Context) {
        vibratePattern(context, longArrayOf(0, 22, 40, 22), intArrayOf(0, 140, 0, 160))
    }

    fun celebrate(context: Context) {
        vibratePattern(context, longArrayOf(0, 28, 30, 28, 30, 55), intArrayOf(0, 110, 0, 150, 0, 220))
    }

    @Suppress("DEPRECATION")
    private fun vibratePattern(context: Context, timings: LongArray, amplitudes: IntArray) {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                vibrator.vibrate(timings, -1)
            }
        }
    }
}
