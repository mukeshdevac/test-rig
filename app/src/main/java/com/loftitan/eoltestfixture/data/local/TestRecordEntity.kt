package com.loftitan.eoltestfixture.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SyncStatus {
    PENDING,
    SYNCED,
    FAILED
}

@Entity(tableName = "test_records")
data class TestRecordEntity(
    @PrimaryKey
    val testId: String,
    val timestamp: Long,
    val operatorId: String,
    val serialNumber: String,
    val overallResult: String, // "PASS" or "FAIL"
    val durationMs: Long,
    val quiescentCurrentMa: Float,
    val peakCurrentMa: Float,
    val busVoltageV: Float,
    val diagnosticsJson: String,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val syncErrorMessage: String? = null
)
