package com.nathanprazeres.walkunlock

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.nathanprazeres.walkunlock.ui.WalkUnlockHomeScreen
import com.nathanprazeres.walkunlock.ui.theme.WalkUnlockTheme
import com.nathanprazeres.walkunlock.utils.LockedAppManager
import com.nathanprazeres.walkunlock.utils.StepCounterManager
import android.R.style.Theme_Material_Dialog_Alert as DIALOG_ALERT_THEME


class MainActivity : ComponentActivity() {
    private lateinit var stepCounterManager: StepCounterManager
    private lateinit var lockedAppManager: LockedAppManager

    private val requiredPermissions = mutableSetOf<String>().apply {
        add(Manifest.permission.ACTIVITY_RECOGNITION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val activityRecognitionGranted =
            permissions[Manifest.permission.ACTIVITY_RECOGNITION] == true
        val postNotificationGranted =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissions[Manifest.permission.POST_NOTIFICATIONS] == true
            else true

        if (activityRecognitionGranted) {
            startStepCounter()
        }
        if (!activityRecognitionGranted || !postNotificationGranted) {
            val deniedPermissions = permissions.filter { !it.value }.keys
            if (deniedPermissions.isEmpty()) return@registerForActivityResult

            AlertDialog.Builder(this, DIALOG_ALERT_THEME)
                .setTitle("Required Permissions")
                .setMessage(
                    "WalkUnlock requires the following permissions in order to function correctly:\n\t- ${
                        deniedPermissions.joinToString(
                            "\n\t- "
                        ) { s -> s.split('.').reversed()[0] }
                    }\nPlease allow these in the settings."
                )
                .setPositiveButton("Go To Settings") { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = "package:$packageName".toUri()
                    }
                    startActivity(intent)
                }
                .setNegativeButton("Skip") { _, _ ->
                    Toast.makeText(
                        this,
                        "Warning: Required permissions denied",
                        Toast.LENGTH_LONG
                    ).show()
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun requestPermissions() {
        val permissionsToRequest = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isEmpty()) {
            startStepCounter()
        } else {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }

        Handler(Looper.getMainLooper()).postDelayed({
            checkBatteryOptimization()
        }, 750)
    }

    private fun checkAccessibilityPermission() {
        val appLockManager = stepCounterManager.getAppLockManager()
        if (appLockManager != null && !appLockManager.hasAccessibilityPermission()) {
            AlertDialog.Builder(this, DIALOG_ALERT_THEME)
                .setTitle("Accessibility Permission Required")
                .setMessage("WalkUnlock needs accessibility permission to monitor app usage and enforce step requirements for locked apps.\n\nThis permission is necessary in order for this app to work.")
                .setPositiveButton("Grant Permission") { _, _ ->
                    appLockManager.requestAccessibilityPermission()
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager

            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                AlertDialog.Builder(this, DIALOG_ALERT_THEME)
                    .setTitle("Battery Optimization")
                    .setMessage("To ensure WalkUnlock works properly in the background, please disable battery optimization for this app.\n\nThis will:\n- Keep step counting accurate\n- Allow app blocking to work reliably")
                    .setPositiveButton("Continue") { _, _ ->
                        requestIgnoreBatteryOptimization()
                    }
                    .setNegativeButton("Skip") { _, _ ->
                        Toast.makeText(
                            this,
                            "Warning: App may not work reliably in background",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    private fun requestIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                // Try direct permission request (may not work on all devices)
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = "package:$packageName".toUri()
                }
                startActivity(intent)
            } catch (_: Exception) {
                // Fallback to general settings (probably unnecessary)
                try {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    startActivity(intent)

                    Toast.makeText(
                        this,
                        "Please find WalkUnlock in the list and disable battery optimization",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (_: Exception) {
                    Toast.makeText(
                        this,
                        "Warning: Unable to open battery optimization settings",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        stepCounterManager = StepCounterManager(this)
        lockedAppManager = LockedAppManager(this)

        requestPermissions()

        setContent {
            WalkUnlockTheme {
                WalkUnlockHomeScreen(
                    stepCounterManager = stepCounterManager, lockedAppManager = lockedAppManager, openApp = this::openApp
                )
            }
        }
    }

    private fun startStepCounter() {
        stepCounterManager.startService()

        Handler(Looper.getMainLooper()).postDelayed({
            checkAccessibilityPermission()
        }, 1000)
    }

    override fun onDestroy() {
        super.onDestroy()
        stepCounterManager.unbindService()
    }

    private fun openApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.addCategory(Intent.CATEGORY_LAUNCHER)
        try {
            startActivity(intent)
        } catch (_: Exception) {}
    }
}
