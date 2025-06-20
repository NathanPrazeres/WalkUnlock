package com.nathanprazeres.walkunlock.utils

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.nathanprazeres.walkunlock.models.LockedApp
import com.nathanprazeres.walkunlock.services.ForegroundAppService
import com.nathanprazeres.walkunlock.ui.AppBlockedActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class AppLockManager(
    private val context: Context,
    private val stepCounterManager: StepCounterManager,
    private val lockedAppManager: LockedAppManager
) {
    companion object {
        private const val TAG = "AppLockManager"
        private const val USAGE_CHECK_INTERVAL = 10000L
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var foregroundAppJob: Job? = null
    private var usageTrackingJob: Job? = null
    private val handler = Handler(Looper.getMainLooper())

    private val usageSessions = ConcurrentHashMap<String, AppUsageSession>()

    private val _isMonitoring = MutableStateFlow(false)
    private val _blockedApps = MutableStateFlow<Set<String>>(emptySet())
    private val _currentForegroundApp = MutableStateFlow<String?>(null)

    data class AppUsageSession(
        val packageName: String,
        val startTime: Long,
        var lastUsageCheckTime: Long,
        var totalUsageTime: Long = 0L,
        var stepsCostSoFar: Int = 0
    )

    init {
        startMonitoring()
    }

    fun startMonitoring() {
        if (_isMonitoring.value) return

        Log.d(TAG, "Starting app monitoring with accessibility service")
        _isMonitoring.value = true

        // Launch a job to monitor app changes
        foregroundAppJob = scope.launch {
            ForegroundAppService.currentForegroundApp.collect { packageName ->
                _currentForegroundApp.value = packageName

                if (packageName != null && packageName != context.packageName) {
                    handleAppChange(packageName)
                } else {
                    // No app or our own app is foreground, stop tracking usage
                    stopUsageTracking()
                }
            }
        }
    }

    private suspend fun handleAppChange(packageName: String) {
        val lockedApps = lockedAppManager.lockedAppsFlow.firstOrNull() ?: emptyList()
        val lockedApp = lockedApps.find { it.packageName == packageName }

        if (lockedApp != null) {
            handleLockedAppUsage(lockedApp)
        } else {
            // Clean up session if app is no longer locked
            usageSessions.remove(packageName)
            stopUsageTracking()
        }
    }

    private fun handleLockedAppUsage(lockedApp: LockedApp) {
        val currentTime = System.currentTimeMillis()
        val packageName = lockedApp.packageName

        // Check if app should be blocked FIRST
        val availableSteps = getAvailableSteps()
        val canUseApp = availableSteps >= lockedApp.costPerMinute

        Log.d(
            TAG,
            "Checking ${lockedApp.appName}: available=$availableSteps, required=${lockedApp.costPerMinute}, canUse=$canUseApp"
        )

        if (!canUseApp) {
            blockApp(lockedApp)
            return
        }

        usageSessions.getOrPut(packageName) {
            AppUsageSession(
                packageName = packageName,
                startTime = currentTime,
                lastUsageCheckTime = currentTime
            )
        }

        startUsageTracking(lockedApp)

        // Remove from blocked apps if it was previously blocked
        _blockedApps.value = _blockedApps.value - packageName
    }

    private fun startUsageTracking(lockedApp: LockedApp) {
        stopUsageTracking()

        usageTrackingJob = scope.launch {
            while (_currentForegroundApp.value == lockedApp.packageName && _isMonitoring.value) {
                // Check if app is still in foreground and we're still monitoring
                updateUsageAndCheckSteps(lockedApp)

                delay(USAGE_CHECK_INTERVAL)
            }
        }
    }

    private fun stopUsageTracking() {
        usageTrackingJob?.cancel()
        usageTrackingJob = null
    }

    private fun updateUsageAndCheckSteps(lockedApp: LockedApp) {
        val currentTime = System.currentTimeMillis()
        val packageName = lockedApp.packageName
        val session = getUsageSession(packageName) ?: return

        Log.d(TAG, "Updating usage for ${lockedApp.appName}")

        // Update total usage time
        val timeSinceLastCheck = currentTime - session.lastUsageCheckTime
        session.totalUsageTime += timeSinceLastCheck
        session.lastUsageCheckTime = currentTime

        // Calculate how many minutes have been used
        val totalMinutesUsed = (session.totalUsageTime / TimeUnit.MINUTES.toMillis(1)).toInt()
        val stepsCostForTotalMinutes = totalMinutesUsed * lockedApp.costPerMinute
        val stepsToDeduct = stepsCostForTotalMinutes - session.stepsCostSoFar

        Log.d(
            TAG,
            "Usage update - Total time: ${session.totalUsageTime}ms, Minutes: $totalMinutesUsed, Steps to deduct: $stepsToDeduct"
        )

        if (stepsToDeduct > 0) {
            // Check if we have enough available steps before deducting
            val currentAvailableSteps = getAvailableSteps()
            val actualStepsToDeduct = minOf(stepsToDeduct, currentAvailableSteps)
            val remainingSteps = currentAvailableSteps - actualStepsToDeduct

            if (actualStepsToDeduct > 0) {
                Log.d(TAG, "Deducting $actualStepsToDeduct steps for ${lockedApp.appName}")
                stepCounterManager.redeemSteps(actualStepsToDeduct)
                session.stepsCostSoFar += actualStepsToDeduct

                showUsageNotification(lockedApp, totalMinutesUsed, remainingSteps)
            }

            // Check if user should be blocked after redemption
            if (remainingSteps < lockedApp.costPerMinute) {
                Log.d(TAG, "User ran out of steps, blocking app")
                blockApp(lockedApp)
                return
            }
        }
    }

    private fun blockApp(lockedApp: LockedApp) {
        Log.d(TAG, "Blocking app: ${lockedApp.appName}")

        _blockedApps.value = _blockedApps.value + lockedApp.packageName

        // Stop tracking usage when blocking
        stopUsageTracking()

        handler.post {
            val intent = Intent(context, AppBlockedActivity::class.java).apply {
                putExtra("blocked_app", lockedApp)
                putExtra("available_steps", getAvailableSteps())
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        }

        usageSessions.remove(lockedApp.packageName)
    }

    private fun showUsageNotification(lockedApp: LockedApp, minutesUsed: Int, remainingSteps: Int) {
        handler.post {
            val message =
                "${lockedApp.appName}: Used ${minutesUsed}min, $remainingSteps steps remaining"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun hasAccessibilityPermission(): Boolean {
        return ForegroundAppService.getInstance() != null
    }

    fun getUsageSession(packageName: String): AppUsageSession? {
        return usageSessions[packageName]
    }

    fun getAvailableSteps(): Int {
        return stepCounterManager.totalSteps.value - stepCounterManager.redeemedSteps.value
    }
}
