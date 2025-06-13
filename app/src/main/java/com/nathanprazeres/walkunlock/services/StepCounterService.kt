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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


private val Context.dataStore by preferencesDataStore(name = "step_prefs")

class StepCounterService() : Service(), SensorEventListener {
    // I just found out this is the "right" way of declaring constants in a class
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

    inner class LocalBinder : Binder() {
        fun getService(): StepCounterService = this@StepCounterService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    // NOTIFICATION MANAGEMENT
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Step Counter",
                NotificationManager.IMPORTANCE_UNSPECIFIED // Bad practice but I think it looks better
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
    private fun updateAvailableSteps() {
        availableSteps.value = (totalSteps.value - redeemedSteps.value).coerceAtLeast(0)
    }

    private fun loadStoredSteps() {
        serviceScope.launch {
            dataStore.data.map { it[totalStepsKey] ?: 0 }.collect { steps ->
                totalSteps.value = steps
                updateAvailableSteps()
                updateNotification()
            }
        }

        serviceScope.launch {
            dataStore.data.map { it[redeemedStepsKey] ?: 0 }.collect { steps ->
                redeemedSteps.value = steps
                updateAvailableSteps()
                updateNotification()
            }
        }
    }

    private fun startStepCounting() {
        // TODO: remove logs (they are for debug purposes only)
        Log.d("StepSensor", "Available sensors: ${sensorManager.getSensorList(Sensor.TYPE_ALL)}")
        Log.d("StepSensor", "Step detector sensor available: ${stepSensor != null}")

        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun stopStepCounting() {
        sensorManager.unregisterListener(this)
    }

    fun redeemSteps(amount: Int) {
        serviceScope.launch {
            dataStore.edit { preferences ->
                val currentRedeemed = preferences[redeemedStepsKey] ?: 0
                val updatedRedeemed = currentRedeemed + amount
                preferences[redeemedStepsKey] = updatedRedeemed
                redeemedSteps.value = updatedRedeemed
                updateAvailableSteps()
            }
        }
    }

    // SENSOR MANAGEMENT
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_DETECTOR) {
            val steps = event.values[0].toInt()
            serviceScope.launch {
                dataStore.edit { preferences ->
                    val currentTotal = preferences[totalStepsKey] ?: 0
                    val updatedTotal = currentTotal + steps
                    preferences[totalStepsKey] = updatedTotal
                    totalSteps.value = updatedTotal
                    updateAvailableSteps()
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

        createNotificationChannel()
        loadStoredSteps()

        // TODO: remove debug logs
        Log.d("StepCounterService", "Service created")
    }

    override fun onDestroy() {
        super.onDestroy()

        stopStepCounting()
        serviceScope.launch { } // This should cancel all running coroutines

        // TODO: remove debug logs
        Log.d("StepCounterService", "Service destroyed")
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
}