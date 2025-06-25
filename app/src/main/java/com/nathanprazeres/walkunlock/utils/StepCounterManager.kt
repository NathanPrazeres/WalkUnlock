package com.nathanprazeres.walkunlock.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.nathanprazeres.walkunlock.services.WalkUnlockService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch


class StepCounterManager(private val context: Context) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private var walkUnlockService: WalkUnlockService? = null
    private var isServiceBound = false

    val totalSteps = MutableStateFlow(0)
    val redeemedSteps = MutableStateFlow(0)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as WalkUnlockService.LocalBinder
            walkUnlockService = binder.getService()
            isServiceBound = true

            // Initialize app lock manager
            walkUnlockService?.initializeAppLockManager(this@StepCounterManager)

            // Synchronize step counter with service
            walkUnlockService?.let { serviceInstance ->
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
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            walkUnlockService = null
            isServiceBound = false
        }
    }

    fun startService() {
        val intent = Intent(context, WalkUnlockService::class.java).apply {
            action = WalkUnlockService.ACTION_START_SERVICE
        }
        context.startForegroundService(intent)

        bindService()
    }

    fun bindService() {
        val bindIntent = Intent(context, WalkUnlockService::class.java)
        context.bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun stopService() {
        unbindService()

        val intent = Intent(context, WalkUnlockService::class.java).apply {
            action = WalkUnlockService.ACTION_STOP_SERVICE
        }
        context.startService(intent)
    }

    fun unbindService() {
        if (isServiceBound) {
            context.unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    fun shutdownApp() {
        walkUnlockService?.getAppLockManager()?.stopMonitoring()
        walkUnlockService?.forceShutdown()

        stopService()

        totalSteps.value = 0
        redeemedSteps.value = 0
    }

    fun redeemSteps(amount: Int) {
        walkUnlockService?.redeemSteps(amount)
    }

    fun getAppLockManager(): AppLockManager? {
        return walkUnlockService?.getAppLockManager()
    }
}
