package com.loftitan.eoltestfixture.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TestRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: TestRecordEntity)

    @Update
    suspend fun updateRecord(record: TestRecordEntity)

    @Query("SELECT * FROM test_records ORDER BY timestamp DESC")
    fun getAllRecordsFlow(): Flow<List<TestRecordEntity>>

    @Query("SELECT * FROM test_records WHERE syncStatus = :status ORDER BY timestamp ASC")
    suspend fun getRecordsBySyncStatus(status: SyncStatus): List<TestRecordEntity>

    @Query("UPDATE test_records SET syncStatus = :newStatus, syncErrorMessage = :error WHERE testId = :id")
    suspend fun updateSyncStatus(id: String, newStatus: SyncStatus, error: String? = null)

    @Query("SELECT COUNT(*) FROM test_records WHERE syncStatus = 'PENDING'")
    fun getPendingSyncCountFlow(): Flow<Int>

    @Query("DELETE FROM test_records")
    suspend fun clearAllRecords()
}
