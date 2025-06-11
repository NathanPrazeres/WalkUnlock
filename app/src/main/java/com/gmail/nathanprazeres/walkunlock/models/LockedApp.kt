package com.gmail.nathanprazeres.walkunlock.models

import android.graphics.Bitmap


data class LockedApp(
    val appName: String, val packageName: String, val costPerMinute: Int, val icon: Bitmap
)
