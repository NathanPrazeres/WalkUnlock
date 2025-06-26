package com.nathanprazeres.walkunlock.ui

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.nathanprazeres.walkunlock.utils.AppLauncher
import com.nathanprazeres.walkunlock.managers.StepCounterManager
import com.nathanprazeres.walkunlock.utils.GetAppVersion
import com.nathanprazeres.walkunlock.utils.ThemePreferences
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    stepCounterManager: StepCounterManager? = null
) {
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val themePreferences = ThemePreferences(context)
    val useSystemTheme by themePreferences.useSystemTheme.collectAsState(initial = true)
    val userDarkTheme by themePreferences.isDarkTheme.collectAsState(initial = false)

    var showShutdownDialog by remember { mutableStateOf(false) }

    val appName = context.applicationInfo.loadLabel(context.packageManager)

    BackHandler {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        onBackClick()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Follow system theme",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Use device's dark/light mode setting",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Switch(
                            checked = useSystemTheme,
                            onCheckedChange = { isChecked ->
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                coroutineScope.launch {
                                    themePreferences.setUseSystemTheme(isChecked)
                                }
                            }
                        )
                    }

                    if (!useSystemTheme) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = !userDarkTheme,
                                onClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    coroutineScope.launch {
                                        themePreferences.setDarkTheme(false)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "☀\uFE0F Light theme",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = userDarkTheme,
                                onClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    coroutineScope.launch {
                                        themePreferences.setDarkTheme(true)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "\uD83C\uDF12 Dark theme",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Text(
                text = "General",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    AppLauncher.openSettings(context)
                }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "$appName Version",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${GetAppVersion(context)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )

                            Spacer(modifier = Modifier.width(16.dp))
                        }
                    }
                }
            }

            Text(
                text = "DANGER ZONE",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            showShutdownDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Shutdown",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Shutdown $appName",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Stops all background services and monitoring",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }

    // Shutdown confirmation dialog
    if (showShutdownDialog) {
        AlertDialog(
            onDismissRequest = { showShutdownDialog = false },
            title = {
                Text(
                    text = "Shutdown $appName",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to shutdown $appName?\n\nThis will:\n· Stop step counting\n· Stop app monitoring\n· Stop all background services\n\nYou'll need to restart the app to resume functionality.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        showShutdownDialog = false
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        coroutineScope.launch {
                            try {
                                stepCounterManager?.shutdownApp()

                                Toast.makeText(
                                    context,
                                    "$appName has been shut down",
                                    Toast.LENGTH_LONG
                                ).show()

                                (context as? ComponentActivity)?.finishAffinity()
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Error shutting down: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text("Shutdown")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showShutdownDialog = false }
                ) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }
}
