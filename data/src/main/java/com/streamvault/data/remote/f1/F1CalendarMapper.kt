package com.streamvault.data.remote.f1

import com.streamvault.domain.model.F1Calendar
import com.streamvault.domain.model.F1RaceWeekend
import com.streamvault.domain.model.F1Session
import com.streamvault.domain.model.F1SessionStatus
import com.streamvault.domain.model.F1SessionType
import java.time.Instant

object F1SessionDurations {
    const val FP_MS = 90L * 60 * 1000
    const val QUALIFYING_MS = 60L * 60 * 1000
    const val SPRINT_QUALIFYING_MS = 60L * 60 * 1000
    const val SPRINT_MS = 50L * 60 * 1000
    const val RACE_MS = 120L * 60 * 1000

    fun forType(type: F1SessionType): Long = when (type) {
        F1SessionType.FP1, F1SessionType.FP2, F1SessionType.FP3 -> FP_MS
        F1SessionType.SPRINT_QUALIFYING -> SPRINT_QUALIFYING_MS
        F1SessionType.SPRINT -> SPRINT_MS
        F1SessionType.QUALIFYING -> QUALIFYING_MS
        F1SessionType.RACE -> RACE_MS
    }
}

fun computeSessionStatus(startMs: Long, type: F1SessionType, nowMs: Long): F1SessionStatus {
    val duration = F1SessionDurations.forType(type)
    return when {
        nowMs < startMs -> F1SessionStatus.SCHEDULED
        nowMs < startMs + duration -> F1SessionStatus.LIVE
        else -> F1SessionStatus.COMPLETED
    }
}

fun JolpicaRaceTableResponse.toF1Calendar(nowMs: Long, fetchedAtMs: Long): F1Calendar {
    val seasonStr = mrData.raceTable.races.firstOrNull()?.date?.take(4) ?: ""
    val season = seasonStr.toIntOrNull() ?: 0
    val races = mrData.raceTable.races.map { it.toF1RaceWeekend(nowMs) }
    return F1Calendar(season = season, races = races, fetchedAtMs = fetchedAtMs)
}

fun JolpicaRace.toF1RaceWeekend(nowMs: Long): F1RaceWeekend {
    val sessions = buildList {
        firstPractice?.let { add(it.toF1Session(F1SessionType.FP1, nowMs)) }
        secondPractice?.let { add(it.toF1Session(F1SessionType.FP2, nowMs)) }
        sprintQualifying?.let { add(it.toF1Session(F1SessionType.SPRINT_QUALIFYING, nowMs)) }
        thirdPractice?.let { add(it.toF1Session(F1SessionType.FP3, nowMs)) }
        qualifying?.let { add(it.toF1Session(F1SessionType.QUALIFYING, nowMs)) }
        sprint?.let { add(it.toF1Session(F1SessionType.SPRINT, nowMs)) }
        // Race uses top-level date/time fields
        val raceTime = time
        if (!raceTime.isNullOrBlank()) {
            val raceStart = parseUtcMs("$date${if (raceTime.startsWith("T")) "" else "T"}$raceTime")
            if (raceStart != null) {
                val duration = F1SessionDurations.RACE_MS
                add(
                    F1Session(
                        type = F1SessionType.RACE,
                        startTimeUtcMs = raceStart,
                        durationMs = duration,
                        status = computeSessionStatus(raceStart, F1SessionType.RACE, nowMs)
                    )
                )
            }
        }
    }.sortedBy { it.startTimeUtcMs }

    return F1RaceWeekend(
        round = round.toIntOrNull() ?: 0,
        raceName = raceName,
        circuitName = circuit.circuitName,
        country = circuit.location.country,
        sessions = sessions
    )
}

private fun JolpicaSessionTime.toF1Session(type: F1SessionType, nowMs: Long): F1Session {
    val timeStr = if (time.startsWith("T")) time else "T$time"
    val startMs = parseUtcMs("$date$timeStr") ?: 0L
    val duration = F1SessionDurations.forType(type)
    return F1Session(
        type = type,
        startTimeUtcMs = startMs,
        durationMs = duration,
        status = computeSessionStatus(startMs, type, nowMs)
    )
}

private fun parseUtcMs(isoDateTime: String): Long? = try {
    Instant.parse(isoDateTime).toEpochMilli()
} catch (e: Exception) {
    null
}
