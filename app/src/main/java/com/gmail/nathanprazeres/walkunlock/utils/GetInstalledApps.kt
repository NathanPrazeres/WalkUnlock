package com.gmail.nathanprazeres.walkunlock.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import androidx.core.graphics.drawable.toBitmap
import com.gmail.nathanprazeres.walkunlock.models.LockedApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun getInstalledApps(context: Context): List<LockedApp> {
    val mainIntent = Intent(Intent.ACTION_MAIN, null)
    mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)

    val packageManager = context.packageManager
    val pkgAppsList: List<ResolveInfo> = packageManager.queryIntentActivities(mainIntent, 0)

    return pkgAppsList
        .mapNotNull { appInfo ->
            try {
                val appName = appInfo.loadLabel(packageManager).toString()
                val packageName = appInfo.activityInfo.packageName
                val icon = appInfo.loadIcon(packageManager).toBitmap()

                LockedApp(
                    appName = appName,
                    packageName = packageName,
                    costPerMinute = 0,
                    icon = icon
                )
            } catch (_: Exception) {
                null
            }
        }
        .distinctBy { it.packageName } // Remove duplicates
        .sortedBy { it.appName.lowercase() }
}

object InstalledApps {
    private var cachedApps: List<LockedApp>? = null

    suspend fun getApps(context: Context, refresh: Boolean = false): List<LockedApp> {
        return if (cachedApps == null || refresh) {
            val apps = withContext(Dispatchers.IO) {
                getInstalledApps(context)
            }
            cachedApps = apps
            apps
        } else {
            cachedApps!!
        }
    }
}
