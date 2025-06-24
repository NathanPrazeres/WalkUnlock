package com.nathanprazeres.walkunlock.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
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
    }
}
