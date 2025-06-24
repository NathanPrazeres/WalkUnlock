package com.nathanprazeres.walkunlock.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.core.net.toUri
import com.nathanprazeres.walkunlock.models.LockedApp


class AppLauncher {
    companion object {
        private var currentToast: Toast? = null

        fun openApp(context: Context, app: LockedApp) {
            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                ?.addCategory(Intent.CATEGORY_LAUNCHER)
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                currentToast?.cancel()
                currentToast =
                    Toast.makeText(context, "${app.appName} isn't installed", Toast.LENGTH_SHORT)
                currentToast?.show()
            }
        }

        fun openSettings(context: Context) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:${context.packageName}".toUri()
            }
            context.startActivity(intent)
        }
    }
}
