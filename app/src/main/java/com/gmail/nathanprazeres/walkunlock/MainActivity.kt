package com.gmail.nathanprazeres.walkunlock

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.gmail.nathanprazeres.walkunlock.ui.WalkUnlockHomeScreen
import com.gmail.nathanprazeres.walkunlock.ui.theme.WalkUnlockTheme
import com.gmail.nathanprazeres.walkunlock.utils.LockedAppManager
import com.gmail.nathanprazeres.walkunlock.utils.StepCounterManager


class MainActivity : ComponentActivity() {
    private lateinit var stepCounterManager: StepCounterManager
    private lateinit var lockedAppManager: LockedAppManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startStepCounter()
        } else {
            // TODO: Handle permission denial
            Toast.makeText(baseContext, "Step counter permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        stepCounterManager = StepCounterManager(this)
        lockedAppManager = LockedAppManager(this)

        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED -> startStepCounter()
            else -> requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        setContent {
            WalkUnlockTheme {
                WalkUnlockHomeScreen(
                    stepCounterManager = stepCounterManager,
                    lockedAppManager = lockedAppManager
                )
            }
        }
    }

    private fun startStepCounter() {
        stepCounterManager.startListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        stepCounterManager.stopListening()
    }
}
