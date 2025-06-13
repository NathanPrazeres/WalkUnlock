package com.nathanprazeres.walkunlock.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.nathanprazeres.walkunlock.services.StepCounterService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch


class StepCounterManager(private val context: Context) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private var stepCounterService: StepCounterService? = null
    private var isServiceBound = false

    val totalSteps = MutableStateFlow(0)
    val redeemedSteps = MutableStateFlow(0)
    val availableSteps = MutableStateFlow(0)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as StepCounterService.LocalBinder
            stepCounterService = binder.getService()
            isServiceBound = true

            // Synchronize step counter with service
            stepCounterService?.let { serviceInstance ->
                coroutineScope.launch {
                    serviceInstance.totalSteps.collect {
                        totalSteps.value = it
                    }
                }

                coroutineScope.launch {
                    serviceInstance.redeemedSteps.collect {
                        redeemedSteps.value = it
                    }
                }

                coroutineScope.launch {
                    serviceInstance.availableSteps.collect {
                        availableSteps.value = it
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            stepCounterService = null
            isServiceBound = false
        }
    }

    fun startService() {
        val intent = Intent(context, StepCounterService::class.java).apply {
            action = StepCounterService.ACTION_START_SERVICE
        }
        context.startForegroundService(intent)

        val bindIntent = Intent(context, StepCounterService::class.java)
        context.bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun stopService() {
        unbindService()

        val intent = Intent(context, StepCounterService::class.java).apply {
            action = StepCounterService.ACTION_STOP_SERVICE
        }
        context.startService(intent)
    }

    fun unbindService() {
        if (isServiceBound) {
            context.unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    fun redeemSteps(amount: Int) {
        stepCounterService?.redeemSteps(amount)
    }
}
