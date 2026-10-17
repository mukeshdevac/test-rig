package com.loftitan.eoltestfixture.data.sync

import android.util.Log
import com.loftitan.eoltestfixture.data.local.TestRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "GoogleSheetsApiClient"

class GoogleSheetsApiClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {

    suspend fun uploadTestRecord(endpointUrl: String, record: TestRecordEntity): Result<Unit> = withContext(Dispatchers.IO) {
        if (endpointUrl.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Google Apps Script Endpoint URL is not configured."))
        }

        try {
            val payload = JSONObject().apply {
                put("testId", record.testId)
                put("timestamp", record.timestamp)
                put("operatorId", record.operatorId)
                put("serialNumber", record.serialNumber)
                put("overallResult", record.overallResult)
                put("durationMs", record.durationMs)
                put("quiescentCurrentMa", record.quiescentCurrentMa)
                put("peakCurrentMa", record.peakCurrentMa)
                put("busVoltageV", record.busVoltageV)
                put("diagnosticsJson", record.diagnosticsJson)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = payload.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(endpointUrl)
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    val msg = response.message
                    Log.w(TAG, "HTTP error $code: $msg")
                    return@withContext Result.failure(Exception("HTTP error $code: $msg"))
                }

                val responseBody = response.body?.string() ?: ""
                Log.d(TAG, "Google Apps Script Response: $responseBody")

                if (responseBody.contains("SUCCESS", ignoreCase = true)) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Apps Script did not return SUCCESS: $responseBody"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}
