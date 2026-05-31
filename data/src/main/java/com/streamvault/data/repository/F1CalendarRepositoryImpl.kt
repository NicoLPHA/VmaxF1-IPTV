package com.streamvault.data.repository

import android.content.SharedPreferences
import com.streamvault.data.di.F1CalendarPrefs
import com.streamvault.data.remote.f1.JolpicaApiService
import com.streamvault.data.remote.f1.computeSessionStatus
import com.streamvault.data.remote.f1.toF1Calendar
import com.streamvault.domain.model.F1Calendar
import com.streamvault.domain.model.F1RaceWeekend
import com.streamvault.domain.model.F1SessionStatus
import com.streamvault.domain.model.Result
import com.streamvault.domain.repository.F1CalendarRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

private const val KEY_CALENDAR_JSON = "f1_calendar_json"
private const val KEY_FETCHED_AT = "f1_calendar_fetched_at_ms"
private const val CACHE_TTL_MS = 7L * 24 * 60 * 60 * 1000

@Singleton
class F1CalendarRepositoryImpl @Inject constructor(
    private val apiService: JolpicaApiService,
    @F1CalendarPrefs private val prefs: SharedPreferences
) : F1CalendarRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getCalendar(forceRefresh: Boolean): Result<F1Calendar> =
        withContext(Dispatchers.IO) {
            val nowMs = System.currentTimeMillis()

            if (!forceRefresh) {
                val cached = loadFromCache(nowMs)
                if (cached != null) return@withContext Result.success(cached)
            }

            try {
                val response = apiService.fetchCurrentSeasonSchedule()
                val calendar = response.toF1Calendar(nowMs, fetchedAtMs = nowMs)
                saveToCache(calendar)
                Result.success(calendar)
            } catch (e: Exception) {
                val stale = loadFromCache(nowMs, ignoreExpiry = true)
                if (stale != null) Result.success(stale)
                else Result.error("Could not load race calendar: ${e.message}", e)
            }
        }

    override suspend fun getCurrentOrNextRaceWeekend(): Result<F1RaceWeekend?> {
        val calResult = getCalendar()
        if (calResult is Result.Error) return Result.error(calResult.message, calResult.exception)
        val calendar = (calResult as Result.Success).data
        val nowMs = System.currentTimeMillis()
        val current = calendar.races.firstOrNull { weekend ->
            weekend.sessions.any { it.status == F1SessionStatus.LIVE }
        }
        if (current != null) return Result.success(current)
        val next = calendar.races
            .filter { weekend -> weekend.sessions.any { it.startTimeUtcMs > nowMs } }
            .minByOrNull { weekend -> weekend.sessions.minOf { it.startTimeUtcMs } }
        return Result.success(next)
    }

    private fun loadFromCache(nowMs: Long, ignoreExpiry: Boolean = false): F1Calendar? {
        val fetchedAt = prefs.getLong(KEY_FETCHED_AT, 0L)
        if (!ignoreExpiry && nowMs - fetchedAt >= CACHE_TTL_MS) return null
        val jsonStr = prefs.getString(KEY_CALENDAR_JSON, null) ?: return null
        return try {
            val cache = json.decodeFromString<F1CalendarCache>(jsonStr)
            cache.toF1Calendar(nowMs)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveToCache(calendar: F1Calendar) {
        val cache = calendar.toCache()
        val jsonStr = json.encodeToString(cache)
        prefs.edit()
            .putString(KEY_CALENDAR_JSON, jsonStr)
            .putLong(KEY_FETCHED_AT, calendar.fetchedAtMs)
            .apply()
    }
}

// --- Cache serialization mirror (data layer only) ---

@Serializable
private data class F1CalendarCache(
    val season: Int,
    val fetchedAtMs: Long,
    val races: List<F1RaceWeekendCache>
)

@Serializable
private data class F1RaceWeekendCache(
    val round: Int,
    val raceName: String,
    val circuitName: String,
    val country: String,
    val sessions: List<F1SessionCache>
)

@Serializable
private data class F1SessionCache(
    val type: String,
    val startTimeUtcMs: Long,
    val durationMs: Long
)

private fun F1Calendar.toCache(): F1CalendarCache = F1CalendarCache(
    season = season,
    fetchedAtMs = fetchedAtMs,
    races = races.map { race ->
        F1RaceWeekendCache(
            round = race.round,
            raceName = race.raceName,
            circuitName = race.circuitName,
            country = race.country,
            sessions = race.sessions.map { session ->
                F1SessionCache(
                    type = session.type.name,
                    startTimeUtcMs = session.startTimeUtcMs,
                    durationMs = session.durationMs
                )
            }
        )
    }
)

private fun F1CalendarCache.toF1Calendar(nowMs: Long): F1Calendar {
    val domainRaces = races.map { race ->
        val domainSessions = race.sessions.mapNotNull { s ->
            val type = try {
                com.streamvault.domain.model.F1SessionType.valueOf(s.type)
            } catch (e: Exception) {
                return@mapNotNull null
            }
            com.streamvault.domain.model.F1Session(
                type = type,
                startTimeUtcMs = s.startTimeUtcMs,
                durationMs = s.durationMs,
                status = computeSessionStatus(s.startTimeUtcMs, type, nowMs)
            )
        }
        com.streamvault.domain.model.F1RaceWeekend(
            round = race.round,
            raceName = race.raceName,
            circuitName = race.circuitName,
            country = race.country,
            sessions = domainSessions
        )
    }
    return F1Calendar(season = season, races = domainRaces, fetchedAtMs = fetchedAtMs)
}
