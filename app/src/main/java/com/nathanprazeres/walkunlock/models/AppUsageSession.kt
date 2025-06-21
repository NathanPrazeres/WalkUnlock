package com.nathanprazeres.walkunlock.models

data class AppUsageSession(
    val packageName: String,
    val startTime: Long,
    var lastUsageCheckTime: Long,
    var totalUsageTime: Long = 0L,
    var stepsCostSoFar: Int = 0
)
