package com.gmail.nathanprazeres.walkunlock.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "step_prefs")

class StepCounterManager(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    private val totalStepsKey = intPreferencesKey("total_steps")
    private val redeemedStepsKey = intPreferencesKey("redeemed_steps")

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val totalStepsFlow = context.dataStore.data.map { it[totalStepsKey] ?: 0 }
    private val redeemedStepsFlow = context.dataStore.data.map { it[redeemedStepsKey] ?: 0 }

    val totalSteps = MutableStateFlow(0)
    val redeemedSteps = MutableStateFlow(0)
    val availableSteps = MutableStateFlow(0)

    init {
        coroutineScope.launch {
            totalStepsFlow.collect { steps ->
                totalSteps.value = steps
                updateAvailableSteps()
            }
        }

        coroutineScope.launch {
            redeemedStepsFlow.collect { steps ->
                redeemedSteps.value = steps
                updateAvailableSteps()
            }
        }
    }

    fun startListening() {
        // TODO: remove logs (they are for debug purposes only)
        Log.d("StepSensor", "Available sensors: ${sensorManager.getSensorList(Sensor.TYPE_ALL)}")
        Log.d("StepSensor", "Step detector sensor available: ${stepSensor != null}")

        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_DETECTOR) {
            val steps = event.values[0].toInt()
            coroutineScope.launch {
                context.dataStore.edit { preferences ->
                    val currentTotal = preferences[totalStepsKey] ?: 0
                    preferences[totalStepsKey] = currentTotal + steps
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // no need to implement this as I always want the most generous accuracy
    }

    fun redeemSteps(amount: Int) {
        coroutineScope.launch {
            context.dataStore.edit { preferences ->
                val currentRedeemed = preferences[redeemedStepsKey] ?: 0
                preferences[redeemedStepsKey] = currentRedeemed + amount
            }
        }
    }

    private fun updateAvailableSteps() {
        availableSteps.value = (totalSteps.value - redeemedSteps.value).coerceAtLeast(0)
    }
}
