package com.nathanprazeres.walkunlock.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


class ForegroundAppService : AccessibilityService() {

    companion object {
        private val _currentForegroundApp = MutableStateFlow<String?>(null)
        val currentForegroundApp: StateFlow<String?> = _currentForegroundApp.asStateFlow()

        @Volatile
        private var serviceInstance: ForegroundAppService? = null

        fun getInstance(): ForegroundAppService? = serviceInstance
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInstance = this

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }

        setServiceInfo(info)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()

            if (packageName != null && event.className != null) {
                // Only update if it's actually a different app
                if (packageName != _currentForegroundApp.value) {
                    _currentForegroundApp.value = packageName
                    Log.d("ForegroundAppService", "Foreground app changed to: $packageName")
                }
            }
        }
    }

    override fun onInterrupt() {
        // Not needed
    }

    override fun onDestroy() {
        super.onDestroy()
        shutdown()
    }

    fun shutdown() {
        serviceInstance = null
        _currentForegroundApp.value = null

        try {
            disableSelf()
        } catch (_: Exception) { }
    }
}
