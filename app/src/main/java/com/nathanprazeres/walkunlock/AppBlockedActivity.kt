package com.nathanprazeres.walkunlock

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.nathanprazeres.walkunlock.models.LockedApp
import com.nathanprazeres.walkunlock.ui.AppBlockedScreen
import com.nathanprazeres.walkunlock.ui.theme.WalkUnlockTheme


class AppBlockedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var blockedApp: LockedApp?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            blockedApp = intent.getParcelableExtra<LockedApp>("blocked_app", LockedApp::class.java)
        } else {
            @Suppress("DEPRECATION")
            blockedApp = intent.getParcelableExtra<LockedApp>("blocked_app")
        }

        val availableSteps = intent.getIntExtra("available_steps", 0)

        setContent {
            WalkUnlockTheme {
                AppBlockedScreen(
                    blockedApp = blockedApp, availableSteps = availableSteps, onBackPressed = {
                        val intent = Intent(baseContext, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }

                        baseContext.startActivity(intent)
                    }
                )
            }
        }
    }
}
