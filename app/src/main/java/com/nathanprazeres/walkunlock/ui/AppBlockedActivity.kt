package com.nathanprazeres.walkunlock.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nathanprazeres.walkunlock.MainActivity
import com.nathanprazeres.walkunlock.models.LockedApp
import com.nathanprazeres.walkunlock.ui.theme.WalkUnlockTheme
import com.nathanprazeres.walkunlock.utils.vibrate


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

@Composable
fun AppBlockedScreen(
    blockedApp: LockedApp?, availableSteps: Int, onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        vibrate(context)
    }

    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ), modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (blockedApp != null) {
                        Image(
                            bitmap = blockedApp.icon.asImageBitmap(),
                            contentDescription = "${blockedApp.appName} icon",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "${blockedApp.appName} is Locked",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "You need more steps to use this app.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Available steps: $availableSteps",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )

                        Text(
                            text = "Steps needed: ${blockedApp.costPerMinute}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Go for a walk to earn more steps!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    OutlinedButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onBackPressed()
                        }) {
                        Text(
                            text = "Return to ${context.applicationInfo.loadLabel(context.packageManager)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }

    BackHandler {
        onBackPressed()
    }
}
