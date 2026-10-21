package com.streamvault.data.remote.f1

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

private const val JOLPICA_URL = "https://api.jolpi.ca/ergast/f1/current.json"

interface JolpicaApiService {
    suspend fun fetchCurrentSeasonSchedule(): JolpicaRaceTableResponse
}

class OkHttpJolpicaApiService(
    private val okHttpClient: OkHttpClient
) : JolpicaApiService {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun fetchCurrentSeasonSchedule(): JolpicaRaceTableResponse =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(JOLPICA_URL)
                .header("Accept", "application/json")
                .build()

            val responseBody = okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Jolpica API error: HTTP ${response.code}")
                }
                response.body?.string()
                    ?: throw IllegalStateException("Empty response from Jolpica API")
            }

            json.decodeFromString(JolpicaRaceTableResponse.serializer(), responseBody)
        }
}
