package com.nathanprazeres.walkunlock.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nathanprazeres.walkunlock.MainActivity
import com.nathanprazeres.walkunlock.R
import com.nathanprazeres.walkunlock.managers.AppLockManager
import com.nathanprazeres.walkunlock.managers.LockedAppManager
import com.nathanprazeres.walkunlock.managers.StepCounterManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


private val Context.dataStore by preferencesDataStore(name = "step_prefs")

class WalkUnlockService() : Service(), SensorEventListener {

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "step_counter_channel"
        const val ACTION_START_SERVICE = "START_SERVICE"
        const val ACTION_STOP_SERVICE = "STOP_SERVICE"
    }

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private lateinit var notificationManager: NotificationManager

    private val totalStepsKey = intPreferencesKey("total_steps")
    private val redeemedStepsKey = intPreferencesKey("redeemed_steps")

    val totalSteps = MutableStateFlow(0)
    val redeemedSteps = MutableStateFlow(0)
    val availableSteps = MutableStateFlow(0)

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Add mutex for thread safety
    private val stepMutex = Mutex()

    private lateinit var appLockManager: AppLockManager
    private lateinit var lockedAppManager: LockedAppManager

    inner class LocalBinder : Binder() {
        fun getService(): WalkUnlockService = this@WalkUnlockService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    // NOTIFICATION MANAGEMENT
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = when {
                Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU -> NotificationManager.IMPORTANCE_MIN
                // NOTE: I'm not sure if this works on all versions below T but it works on my device
                else -> NotificationManager.IMPORTANCE_UNSPECIFIED // Bad practice but I think it looks better
            }

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Step Counter",
                importance
            ).apply {
                description = "Tracks your steps in the background"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Step Balance: ${availableSteps.value}")
            .setContentText("Steps Taken: ${totalSteps.value}\nBalance Used: ${redeemedSteps.value}")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setShowWhen(false)
            .setSilent(true)
            .build()
    }

    private fun startForegroundService() {
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun updateNotification() {
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    // STEP MANAGEMENT
    private fun loadStoredSteps() {
        serviceScope.launch {
            // Each dataStore.data creates it's own flow so using combine
            // will wait for both to emit before we perform any fetches
            // meaning that they should be in sync and race condition safe.
            combine(
                dataStore.data,
                dataStore.data
            ) { prefs1, prefs2 ->
                val total = prefs1[totalStepsKey] ?: 0
                val redeemed = prefs2[redeemedStepsKey] ?: 0
                Pair(total, redeemed)
            }.collect { (total, redeemed) ->
                stepMutex.withLock {
                    totalSteps.value = total
                    redeemedSteps.value = redeemed
                    availableSteps.value = maxOf(0, total - redeemed)
                }
                updateNotification()
            }
        }
    }

    private fun startStepCounting() {
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun stopStepCounting() {
        sensorManager.unregisterListener(this)
    }

    fun redeemSteps(amount: Int) {
        serviceScope.launch {
            stepMutex.withLock {
                val currentTotal = totalSteps.value
                val currentRedeemed = redeemedSteps.value
                val newRedeemed = currentRedeemed + amount

                // Safety check: don't allow redeemed steps to exceed total steps
                if (newRedeemed > currentTotal) {
                    Log.w(
                        "WalkUnlockService",
                        "Attempted to redeem $amount steps, but would exceed total. Total: $currentTotal, Current redeemed: $currentRedeemed"
                    )
                    return@withLock
                }

                dataStore.edit { preferences ->
                    preferences[redeemedStepsKey] = newRedeemed
                }
            }
        }
    }

    // APP LOCK MANAGEMENT
    fun initializeAppLockManager(stepCounterManager: StepCounterManager) {
        appLockManager = AppLockManager(
            context = this,
            stepCounterManager = stepCounterManager,
            lockedAppManager = lockedAppManager
        )
    }

    fun getAppLockManager(): AppLockManager? =
        if (::appLockManager.isInitialized) appLockManager else null

    // SENSOR MANAGEMENT
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_DETECTOR) {
            val steps = event.values[0].toInt()
            serviceScope.launch {
                stepMutex.withLock {
                    val currentTotal = totalSteps.value
                    val newTotal = currentTotal + steps

                    dataStore.edit { preferences ->
                        preferences[totalStepsKey] = newTotal
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No need to implement this as I always want the most generous accuracy
    }

    // SERVICE MANAGEMENT
    override fun onCreate() {
        super.onCreate()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        lockedAppManager = LockedAppManager(this)

        createNotificationChannel()
        loadStoredSteps()
    }

    override fun onDestroy() {
        super.onDestroy()

        stopStepCounting()
        serviceScope.launch { } // This should cancel all running coroutines
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVICE -> {
                startForegroundService()
                startStepCounting()
            }

            ACTION_STOP_SERVICE -> {
                stopSelf()
                stopStepCounting()
            }
        }

        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)
        restartServiceIntent.action = ACTION_START_SERVICE

        // (Attempt to) restart the service if it's ever killed
        startService(restartServiceIntent)
    }

    fun forceShutdown() {
        // Stop app lock manager if initialized
        if (::appLockManager.isInitialized) {
            appLockManager.stopMonitoring()
        }

        stopStepCounting()
        serviceScope.launch { }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
}
