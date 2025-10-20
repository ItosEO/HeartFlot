package com.itos.heartflot.data

import kotlinx.serialization.Serializable

@Serializable
data class HeartRateRecord(
    val timestamp: Long,
    val heartRate: Int
)

@Serializable
data class RecordSession(
    val sessionId: String,
    val startTime: Long,
    val endTime: Long,
    val deviceName: String?,
    val deviceAddress: String?,
    val records: List<HeartRateRecord> = emptyList()
) {
    val recordCount: Int get() = records.size
    
    val averageHeartRate: Int get() = 
        if (records.isEmpty()) 0 
        else records.map { it.heartRate }.average().toInt()
    
    val minHeartRate: Int get() = 
        records.minOfOrNull { it.heartRate } ?: 0
    
    val maxHeartRate: Int get() = 
        records.maxOfOrNull { it.heartRate } ?: 0
}

