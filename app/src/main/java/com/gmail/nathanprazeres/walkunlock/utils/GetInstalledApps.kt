package com.gmail.nathanprazeres.walkunlock.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import androidx.core.graphics.drawable.toBitmap
import com.gmail.nathanprazeres.walkunlock.models.LockedApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


object InstalledApps {
    private var cachedApps: List<LockedApp>? = null
    private var isLoading: Boolean =
        false // To account for simultaneous requests (shouldn't be possible either way)

    suspend fun getApps(context: Context, refresh: Boolean = false): List<LockedApp> {
        return withContext(Dispatchers.IO) {
            if (cachedApps == null || refresh) {
                synchronized(this@InstalledApps) {
                    if (cachedApps == null || refresh) {
                        if (!isLoading) {
                            isLoading = true
                            try {
                                val apps = getInstalledApps(context)
                                cachedApps = apps
                            } catch (_: Exception) {
                                // If loading fails, return cached apps or empty list
                                cachedApps = cachedApps ?: emptyList()
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                }
            }
            cachedApps ?: emptyList()
        }
    }

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
}
