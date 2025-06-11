package com.nathanprazeres.walkunlock.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nathanprazeres.walkunlock.models.LockedApp
import com.nathanprazeres.walkunlock.utils.InstalledApps
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLockedAppScreen(
    onBackClick: () -> Unit,
    onAppSelected: (LockedApp) -> Unit
) {
    val context = LocalContext.current

    var apps by remember { mutableStateOf<List<LockedApp>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedApp by rememberSaveable { mutableStateOf<LockedApp?>(null) }
    var costPerMinute by rememberSaveable { mutableStateOf("10") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    BackHandler {
        when {
            selectedApp != null -> selectedApp = null
            else -> onBackClick()
        }
    }

    // Function to load/refresh apps
    suspend fun loadApps(refresh: Boolean = false) {
        try {
            isLoading = true
            hasError = false
            apps = InstalledApps.getApps(context, refresh)
        } catch (_: Exception) {
            hasError = true
            apps = emptyList()
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadApps()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select App to Lock") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Refresh button
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                loadApps(refresh = true)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh apps")
                    }

                    if (selectedApp != null && costPerMinute.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                selectedApp?.let {
                                    try {
                                        onAppSelected(
                                            LockedApp(
                                                it.appName,
                                                it.packageName,
                                                costPerMinute.toIntOrNull() ?: 10,
                                                it.icon
                                            )
                                        )
                                        onBackClick()
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "Failed to add App: $e",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onBackClick()
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Confirm")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Loading apps...")
                        }
                    }
                }

                hasError -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Error loading apps",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        loadApps(refresh = true)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Retry")
                            }
                        }
                    }
                }

                else -> {
                    // Selected app indicator
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedApp != null) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedApp != null) {
                                Image(
                                    bitmap = selectedApp!!.icon.asImageBitmap(),
                                    contentDescription = "${selectedApp!!.appName} icon",
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = "Selected: ${selectedApp!!.appName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else {
                                // Placeholder when no app is selected
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "No app selected",
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = "Please select an Application",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Steps per minute input
                    OutlinedTextField(
                        value = costPerMinute,
                        onValueChange = { costPerMinute = it.filter { c -> c.isDigit() } },
                        label = { Text("Steps per minute") },
                        supportingText = { Text("Number of steps required per minute of usage") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Search Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search apps") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // App List Header
                    Text(
                        "Choose an app:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // App List
                    val filteredApps = apps.filter {
                        it.appName.contains(searchQuery, ignoreCase = true)
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredApps) { app ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedApp = app },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedApp?.packageName == app.packageName) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = if (selectedApp?.packageName == app.packageName) 4.dp else 1.dp
                                )
                            ) {
                                ListItem(
                                    headlineContent = { Text(app.appName) },
                                    supportingContent = { Text(app.packageName) },
                                    leadingContent = {
                                        Image(
                                            bitmap = app.icon.asImageBitmap(),
                                            contentDescription = "${app.appName} icon",
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    },
                                    trailingContent = {
                                        if (selectedApp?.packageName == app.packageName) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
