package com.streamvault.domain.repository

import com.streamvault.domain.model.F1Calendar
import com.streamvault.domain.model.F1RaceWeekend
import com.streamvault.domain.model.Result

interface F1CalendarRepository {
    suspend fun getCalendar(forceRefresh: Boolean = false): Result<F1Calendar>
    suspend fun getCurrentOrNextRaceWeekend(): Result<F1RaceWeekend?>
}
