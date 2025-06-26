package com.nathanprazeres.walkunlock.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

fun GetAppVersion(
    context: Context,
): String? {
    return try {
        val packageManager = context.packageManager
        val packageName = context.packageName
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            packageManager.getPackageInfo(packageName, 0)
        }
        packageInfo.versionName
    } catch (_: Exception) {
        null
    }
}
