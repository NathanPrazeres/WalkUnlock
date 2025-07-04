package com.nathanprazeres.walkunlock.managers

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import com.nathanprazeres.walkunlock.AppBlockedActivity
import com.nathanprazeres.walkunlock.models.AppUsageSession
import com.nathanprazeres.walkunlock.models.LockedApp
import com.nathanprazeres.walkunlock.services.ForegroundAppService
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
    private val scope = CoroutineScope(Dispatchers.IO)
    private var foregroundAppJob: Job? = null
    private var usageTrackingJob: Job? = null
    private val handler = Handler(Looper.getMainLooper())

    private val usageSessions = ConcurrentHashMap<String, AppUsageSession>()

    private val isMonitoring = MutableStateFlow(false)
    private val blockedApps = MutableStateFlow<Set<String>>(emptySet())
    private val currentForegroundApp = MutableStateFlow<String?>(null)

    init {
        startMonitoring()
    }

    fun startMonitoring() {
        if (isMonitoring.value) return

        isMonitoring.value = true

        // Launch a job to monitor app changes
        foregroundAppJob = scope.launch {
            ForegroundAppService.currentForegroundApp.collect { packageName ->
                val previousPackageName = currentForegroundApp.value ?: ""
                currentForegroundApp.value = packageName

                if (packageName != null && packageName != context.packageName) {
                    handleAppChange(packageName, previousPackageName)
                } else {
                    // No app or our own app is foreground, stop tracking usage
                    stopUsageTracking()
                }
            }
        }
    }

    private suspend fun handleAppChange(packageName: String, previousPackageName: String) {
        val lockedApps = lockedAppManager.lockedAppsFlow.firstOrNull() ?: emptyList()
        val lockedApp = lockedApps.find { it.packageName == packageName }

        if (lockedApp != null) {
            handleLockedAppUsage(lockedApp)
        } else {
            // Clean up session if app is no longer locked
            usageSessions.remove(previousPackageName)
            stopUsageTracking()
        }
    }

    private fun handleLockedAppUsage(lockedApp: LockedApp) {
        val currentTime = System.currentTimeMillis()
        val packageName = lockedApp.packageName

        // Check if app should be blocked FIRST
        val availableSteps = getAvailableSteps()
        val canUseApp = availableSteps >= lockedApp.costPerMinute

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
        blockedApps.value = blockedApps.value - packageName
    }

    private fun startUsageTracking(lockedApp: LockedApp) {
        stopUsageTracking()

        usageTrackingJob = scope.launch {
            while (currentForegroundApp.value == lockedApp.packageName && isMonitoring.value) {
                // Check if app is still in foreground and we're still monitoring
                updateUsageAndCheckSteps(lockedApp)

                delay(TimeUnit.MINUTES.toMillis(1))
            }
        }
    }

    private fun stopUsageTracking() {
        usageTrackingJob?.cancel()
        usageTrackingJob = null
    }

    private fun updateUsageAndCheckSteps(lockedApp: LockedApp) {
        if (blockedApps.value.contains(lockedApp.packageName)) {
            return
        }

        val currentTime = System.currentTimeMillis()
        val packageName = lockedApp.packageName
        val session = getUsageSession(packageName) ?: return

        // Update total usage time
        val timeSinceLastCheck = currentTime - session.lastUsageCheckTime
        session.totalUsageTime += timeSinceLastCheck
        session.lastUsageCheckTime = currentTime

        // Calculate how many minutes have been used
        val totalMinutesUsed = (session.totalUsageTime / TimeUnit.MINUTES.toMillis(1)).toInt()
        val stepsCostForTotalMinutes = totalMinutesUsed * lockedApp.costPerMinute
        val stepsToDeduct = stepsCostForTotalMinutes - session.stepsCostSoFar

        if (stepsToDeduct > 0) {
            // Check if we have enough available steps before deducting
            val currentAvailableSteps = getAvailableSteps()
            val actualStepsToDeduct = minOf(stepsToDeduct, currentAvailableSteps)
            val remainingSteps = currentAvailableSteps - actualStepsToDeduct

            if (actualStepsToDeduct > 0) {
                stepCounterManager.redeemSteps(actualStepsToDeduct)
                session.stepsCostSoFar += actualStepsToDeduct

                showUsageNotification(lockedApp, totalMinutesUsed, remainingSteps)
            }

            // Check if user should be blocked after redemption
            if (remainingSteps < lockedApp.costPerMinute) {
                blockApp(lockedApp)
            }
        }
    }

    private fun blockApp(lockedApp: LockedApp) {
        blockedApps.value = blockedApps.value + lockedApp.packageName

        // Stop tracking usage when blocking
        stopUsageTracking()
        usageSessions.remove(lockedApp.packageName)

        handler.post {
            val intent = Intent(context, AppBlockedActivity::class.java).apply {
                putExtra("blocked_app", lockedApp)
                putExtra("available_steps", getAvailableSteps())
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        }
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

    fun stopMonitoring() {
        isMonitoring.value = false

        foregroundAppJob?.cancel()
        stopUsageTracking()

        foregroundAppJob = null
        usageSessions.clear()
        blockedApps.value = emptySet()
        currentForegroundApp.value = null
    }
}
