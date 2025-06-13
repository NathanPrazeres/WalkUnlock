package com.nathanprazeres.walkunlock

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.nathanprazeres.walkunlock.ui.WalkUnlockHomeScreen
import com.nathanprazeres.walkunlock.ui.theme.WalkUnlockTheme
import com.nathanprazeres.walkunlock.utils.LockedAppManager
import com.nathanprazeres.walkunlock.utils.StepCounterManager


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

        if (activityRecognitionGranted && postNotificationGranted) {
            startStepCounter()
            // TODO: ask/check for battery optimization override permission
        } else {
            val deniedPermissions = permissions.filter { !it.value }.keys
            Toast.makeText(
                this,
                "Required permissions denied: ${deniedPermissions.joinToString()}",
                Toast.LENGTH_LONG
            ).show()
            // TODO: maybe request permissions again?
        }
    }

    private fun requestPermissions() {
        val permissionsToRequest = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isEmpty()) {
            startStepCounter()
            // TODO: ask/check for battery optimization override permission
        } else {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
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
                    stepCounterManager = stepCounterManager, lockedAppManager = lockedAppManager
                )
            }
        }
    }

    private fun startStepCounter() {
        stepCounterManager.startService()
    }

    override fun onDestroy() {
        super.onDestroy()
        stepCounterManager.unbindService()
    }
}
