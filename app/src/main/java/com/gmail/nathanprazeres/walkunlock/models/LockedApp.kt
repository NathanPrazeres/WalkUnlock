package com.gmail.nathanprazeres.walkunlock.models

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class LockedApp(
    val appName: String,
    val packageName: String,
    val costPerMinute: Int,
    val icon: Bitmap
) : Parcelable
