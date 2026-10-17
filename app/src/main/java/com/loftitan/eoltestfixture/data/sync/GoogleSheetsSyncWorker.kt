package com.loftitan.eoltestfixture.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.loftitan.eoltestfixture.data.local.AppDatabase
import com.loftitan.eoltestfixture.data.local.SyncStatus

private const val TAG = "GoogleSheetsSyncWorker"
const val PREFS_NAME = "eol_fixture_prefs"
const val KEY_GOOGLE_SCRIPT_URL = "google_script_url"

class GoogleSheetsSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val apiClient = GoogleSheetsApiClient()
    private val dao = AppDatabase.getDatabase(appContext).testRecordDao()

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting Google Sheets sync worker cycle...")

        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val endpointUrl = prefs.getString(KEY_GOOGLE_SCRIPT_URL, "") ?: ""

        if (endpointUrl.isBlank()) {
            Log.w(TAG, "Sync skipped: No Google Apps Script URL configured in Settings.")
            return Result.success()
        }

        val pendingRecords = dao.getRecordsBySyncStatus(SyncStatus.PENDING)
        if (pendingRecords.isEmpty()) {
            Log.d(TAG, "No pending records to sync.")
            return Result.success()
        }

        Log.i(TAG, "Found ${pendingRecords.size} pending records for upload.")
        var anyFailure = false

        for (record in pendingRecords) {
            val result = apiClient.uploadTestRecord(endpointUrl, record)
            if (result.isSuccess) {
                dao.updateSyncStatus(record.testId, SyncStatus.SYNCED, null)
                Log.d(TAG, "Successfully synced record ${record.serialNumber} (${record.testId})")
            } else {
                anyFailure = true
                val err = result.exceptionOrNull()?.message ?: "Unknown upload failure"
                dao.updateSyncStatus(record.testId, SyncStatus.PENDING, err)
                Log.w(TAG, "Failed to sync record ${record.testId}: $err")
            }
        }

        return if (anyFailure) {
            Result.retry()
        } else {
            Result.success()
        }
    }
}
