package com.streamvault.app.ui.screens.f1

import com.streamvault.domain.model.F1ChannelGroup
import com.streamvault.domain.model.F1RaceWeekend
import com.streamvault.domain.model.F1Session
import com.streamvault.domain.model.F1SessionStatus
import com.streamvault.domain.model.Movie
import com.streamvault.domain.model.Series

data class F1ScreenState(
    val isCalendarLoading: Boolean = true,
    val isChannelsLoading: Boolean = true,
    val isMoviesLoading: Boolean = true,
    val isSeriesLoading: Boolean = true,
    val calendarError: String? = null,
    val currentOrNextWeekend: F1RaceWeekend? = null,
    val allRaces: List<F1RaceWeekend> = emptyList(),
    val channels: F1ChannelGroup = F1ChannelGroup(emptyList(), emptyList(), emptyList()),
    val movies: List<Movie> = emptyList(),
    val series: List<Series> = emptyList(),
    val nowMs: Long = System.currentTimeMillis()
) {
    val isLoading: Boolean get() = isCalendarLoading || isChannelsLoading

    val currentLiveSession: F1Session?
        get() = allRaces.flatMap { it.sessions }
            .firstOrNull { it.status == F1SessionStatus.LIVE }

    val nextScheduledSession: F1Session?
        get() = currentOrNextWeekend?.sessions
            ?.firstOrNull { it.status == F1SessionStatus.SCHEDULED }
}
