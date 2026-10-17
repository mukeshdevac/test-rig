package com.loftitan.eoltestfixture.data.repository

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.loftitan.eoltestfixture.data.local.AppDatabase
import com.loftitan.eoltestfixture.data.local.TestRecordDao
import com.loftitan.eoltestfixture.data.local.TestRecordEntity
import com.loftitan.eoltestfixture.data.sync.GoogleSheetsSyncWorker
import com.loftitan.eoltestfixture.data.sync.KEY_GOOGLE_SCRIPT_URL
import com.loftitan.eoltestfixture.data.sync.PREFS_NAME
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

private const val UNIQUE_SYNC_WORK_NAME = "google_sheets_sync_work"


class TestFixtureRepository(private val context: Context) {

    private val dao: TestRecordDao = AppDatabase.getDatabase(context).testRecordDao()
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val workManager = WorkManager.getInstance(context)

    val allRecordsFlow: Flow<List<TestRecordEntity>> = dao.getAllRecordsFlow()
    val pendingCountFlow: Flow<Int> = dao.getPendingSyncCountFlow()

    suspend fun saveTestRecord(record: TestRecordEntity) {
        dao.insertRecord(record)
        triggerImmediateSync()
    }

    suspend fun clearHistory() {
        dao.clearAllRecords()
    }

    fun triggerImmediateSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<GoogleSheetsSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniqueWork(
            UNIQUE_SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    fun getGoogleScriptUrl(): String {
        return prefs.getString(KEY_GOOGLE_SCRIPT_URL, "") ?: ""
    }

    fun saveGoogleScriptUrl(url: String) {
        prefs.edit().putString(KEY_GOOGLE_SCRIPT_URL, url.trim()).apply()
    }
}
