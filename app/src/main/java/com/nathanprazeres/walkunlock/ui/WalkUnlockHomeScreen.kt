package com.nathanprazeres.walkunlock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.nathanprazeres.walkunlock.managers.LockedAppManager
import com.nathanprazeres.walkunlock.managers.StepCounterManager
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkUnlockHomeScreen(
    stepCounterManager: StepCounterManager,
    lockedAppManager: LockedAppManager
) {
    val totalStepsState by stepCounterManager.totalSteps.collectAsState()
    val redeemedStepsState by stepCounterManager.redeemedSteps.collectAsState()
    val lockedAppsState by lockedAppManager.lockedAppsFlow.collectAsState(initial = emptyList())

    var showAddScreen by rememberSaveable { mutableStateOf(false) }
    var showSettingsScreen by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("${context.applicationInfo.loadLabel(context.packageManager)}") },
            actions = {
                IconButton(onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    showSettingsScreen = true
                }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        )
    }, floatingActionButton = {
        FloatingActionButton(onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            showAddScreen = true
        }) {
            Icon(Icons.Default.Add, contentDescription = "Add")
        }
    }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StepCard(
                availableSteps = totalStepsState - redeemedStepsState,
                totalSteps = totalStepsState
            )

            Text("Locked Apps", style = MaterialTheme.typography.titleMedium)

            lockedAppsState.forEach { app ->
                LockedAppCard(
                    app = app, availableSteps = totalStepsState - redeemedStepsState, onRemove = {
                        coroutineScope.launch {
                            try {
                                lockedAppManager.removeLockedApp(app.packageName)
                            } catch (_: Exception) {
                                // This should be unreachable since errors are handled inside .removeLockedApp
                            }
                        }
                    }
                )
            }
        }
    }

    if (showAddScreen) {
        AddLockedAppScreen(onBackClick = { showAddScreen = false }, onAppSelected = { selectedApp ->
            coroutineScope.launch {
                try {
                    lockedAppManager.addLockedApp(selectedApp)
                    showAddScreen = false
                } catch (_: Exception) {
                    // This should be unreachable since I handle errors inside .addLockedApp
                    showAddScreen = false
                }
            }
        })
    } else if (showSettingsScreen) {
        SettingsScreen(
            onBackClick = { showSettingsScreen = false },
            stepCounterManager = stepCounterManager
        )
    }
}
