package com.nathanprazeres.walkunlock.utils

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator

fun vibrate(context: Context) {
    val vibrator = context.getSystemService(Vibrator::class.java)

    if (vibrator?.hasVibrator() == true) {
        val vibrationPattern = longArrayOf(
            0, 200, 150, 200, 150, 300
        )

        val amplitudes = intArrayOf(
            0, 255, 0, 255, 0, 255
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrationEffect = VibrationEffect.createWaveform(
                vibrationPattern, amplitudes, -1
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val attributes = VibrationAttributes.Builder()
                    .setUsage(VibrationAttributes.USAGE_ACCESSIBILITY)
                    .build()

                vibrator.vibrate(vibrationEffect, attributes)
            } else {
                vibrator.vibrate(vibrationEffect)
            }
        } else {
            // Fallback for older devices
            @Suppress("DEPRECATION")
            vibrator.vibrate(vibrationPattern, -1)
        }
    }
}
